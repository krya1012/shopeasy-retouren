package de.shopeasy.messaging.producer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends status events to the exchange and makes sure they arrived.
 *
 * <p><b>Error handling:</b>
 * <ul>
 *   <li>Broker not reachable: up to 3 attempts with a growing pause.</li>
 *   <li>Broker does not confirm (nack) or does not answer in time: error.</li>
 *   <li>No queue accepts the message (it comes back as "returned"): error.</li>
 * </ul>
 * In every case it ends with an {@link EventNotDeliveredException}, never silently.
 *
 * <p><b>Spring Boot and RabbitMQ basics:</b>
 * <ul>
 *   <li>{@code @Component}: Spring creates one object of this class. It gets the {@link RabbitTemplate}
 *       (Spring's tool for sending) and the {@link JsonMapper} through the constructor (dependency
 *       injection).</li>
 *   <li><i>Publisher confirm</i>: after a message is sent, the broker answers "I have it" (ack) or "I could not
 *       store it" (nack). This is switched on in {@code application.properties}. We wait for the answer using a
 *       {@link CorrelationData}, which is a label for one message that carries the answer back as a future.</li>
 *   <li><i>Mandatory / returns</i>: if no queue matches the routing key, the broker would normally drop the
 *       message silently. With {@code mandatory} it sends the message back, and we report an error.</li>
 *   <li><i>Persistent</i> messages are written to disk by the broker, so they survive a broker restart.</li>
 * </ul>
 */
@Component
public class StatusEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(StatusEventPublisher.class);
    static final int MAX_ATTEMPTS = 3;
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbit;
    private final JsonMapper json;
    private final long backoffMillis;

    /** Used by Spring. {@code @Autowired} marks which of the two constructors Spring must use. */
    @Autowired
    public StatusEventPublisher(RabbitTemplate rabbit, JsonMapper json) {
        this(rabbit, json, 1000);
    }

    /** For tests only: a short pause between the attempts. */
    StatusEventPublisher(RabbitTemplate rabbit, JsonMapper json, long backoffMillis) {
        this.rabbit = rabbit;
        this.json = json;
        this.backoffMillis = backoffMillis;
    }

    /** Converts the event to JSON and sends it. Returns only after the broker has confirmed it. */
    public void publish(StatusEvent event) {
        byte[] body;
        try {
            body = json.writeValueAsBytes(event);
        } catch (JacksonException e) {
            throw new EventNotDeliveredException("Event " + event.eventId() + " cannot be converted to JSON", e);
        }
        send(event.eventId().toString(), event.routingKey(), body);
    }

    /** Sends any bytes. Only used for the demo scenario "broken message". */
    public void publishRaw(String routingKey, String body) {
        send("raw-" + System.nanoTime(), routingKey, body.getBytes(StandardCharsets.UTF_8));
    }

    private void send(String messageId, String routingKey, byte[] body) {
        Message message = MessageBuilder.withBody(body)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(messageId)
                .build();

        for (int attempt = 1; ; attempt++) {
            try {
                sendOnce(message, messageId, routingKey);
                LOG.info("Sent: {} -> {} (broker confirmed)", messageId, routingKey);
                return;
            } catch (AmqpException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new EventNotDeliveredException("Broker not reachable after " + MAX_ATTEMPTS
                            + " attempts: " + e.getMessage(), e);
                }
                LOG.warn("Sending failed ({}), attempt {} of {}", e.getMessage(), attempt + 1, MAX_ATTEMPTS);
                pause(backoffMillis * attempt);
            }
        }
    }

    /** One attempt: send, then wait for the broker's answer. */
    private void sendOnce(Message message, String messageId, String routingKey) {
        CorrelationData correlation = new CorrelationData(messageId);
        rabbit.send(RabbitConfig.EXCHANGE, routingKey, message, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.ack()) {
                throw new EventNotDeliveredException("Broker did not accept the message: " + confirm.reason());
            }
            if (correlation.getReturned() != null) {
                throw new EventNotDeliveredException("No queue accepted the message (" + routingKey
                        + "): " + correlation.getReturned().getReplyText());
            }
        } catch (TimeoutException e) {
            throw new EventNotDeliveredException("No confirmation from the broker within "
                    + CONFIRM_TIMEOUT_SECONDS + " s", e);
        } catch (ExecutionException e) {
            throw new EventNotDeliveredException("Confirmation failed: " + e.getCause(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventNotDeliveredException("Sending interrupted", e);
        }
    }

    /** Waits between two attempts. If the thread is interrupted, it stops instead of trying again. */
    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventNotDeliveredException("Sending interrupted", e);
        }
    }
}
