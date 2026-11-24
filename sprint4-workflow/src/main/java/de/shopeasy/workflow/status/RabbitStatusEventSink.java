package de.shopeasy.workflow.status;

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

import de.shopeasy.workflow.config.ClientConfig;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends status events to the exchange {@code returns.status} and makes sure they arrived (same behaviour as the
 * Sprint 2 {@code StatusEventPublisher}, own copy).
 *
 * <ul>
 *   <li>Broker not reachable: up to 3 attempts with a growing pause.</li>
 *   <li>Broker does not confirm (nack) or does not answer in 5 seconds: error.</li>
 *   <li>No queue takes the message (it comes back as "returned", because {@code mandatory} is set): error. This
 *       happens when no receiver (for example {@code messaging-consumer}) has created its queues yet.</li>
 * </ul>
 * Every error ends as an {@link EventNotDeliveredException}, never silently. Messages are persistent (the broker
 * writes them to disk) and carry the event id as message id.
 */
@Component
public class RabbitStatusEventSink implements StatusEventSink {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitStatusEventSink.class);
    static final int MAX_ATTEMPTS = 3;
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbit;
    private final JsonMapper json;
    private final long backoffMillis;

    /** Used by Spring. */
    @Autowired
    public RabbitStatusEventSink(RabbitTemplate rabbit, JsonMapper json) {
        this(rabbit, json, 1000);
    }

    /** For tests: a short pause between the attempts. */
    RabbitStatusEventSink(RabbitTemplate rabbit, JsonMapper json, long backoffMillis) {
        this.rabbit = rabbit;
        this.json = json;
        this.backoffMillis = backoffMillis;
    }

    @Override
    public void publish(StatusEvent event) {
        byte[] body;
        try {
            body = json.writeValueAsBytes(event);
        } catch (JacksonException e) {
            throw new EventNotDeliveredException("Event " + event.eventId() + " cannot be converted to JSON", e);
        }
        Message message = MessageBuilder.withBody(body)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(event.eventId().toString())
                .build();
        String messageId = event.eventId().toString();
        for (int attempt = 1; ; attempt++) {
            try {
                sendOnce(message, messageId, event.routingKey());
                LOG.info("Event sent: {} {} -> {} (broker confirmed)", event.returnId(), event.oldStatus(),
                        event.newStatus());
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

    /** One attempt: send, then wait for the broker's answer (publisher confirm). */
    private void sendOnce(Message message, String messageId, String routingKey) {
        CorrelationData correlation = new CorrelationData(messageId);
        rabbit.send(ClientConfig.EXCHANGE, routingKey, message, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.ack()) {
                throw new EventNotDeliveredException("Broker did not accept the message: " + confirm.reason());
            }
            if (correlation.getReturned() != null) {
                throw new EventNotDeliveredException("No queue accepted the message (" + routingKey + "): "
                        + correlation.getReturned().getReplyText() + ". Is messaging-consumer running?");
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

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventNotDeliveredException("Sending interrupted", e);
        }
    }
}
