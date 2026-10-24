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
 * Sendet Status-Events an den Exchange und stellt sicher, dass sie angekommen sind.
 *
 * <p>Fehlerbehandlung:
 * <ul>
 *   <li>Broker nicht erreichbar: bis zu 3 Versuche mit Wartezeit.</li>
 *   <li>Broker bestätigt nicht (nack) oder gar nicht (Timeout): Fehler.</li>
 *   <li>Keine Queue nimmt die Nachricht an (zurückgegeben): Fehler.</li>
 * </ul>
 * In allen Fällen endet es mit einer {@link EventNotDeliveredException}, nie still.
 */
@Component
public class StatusEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(StatusEventPublisher.class);
    static final int MAX_ATTEMPTS = 3;
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbit;
    private final JsonMapper json;
    private final long backoffMillis;

    @Autowired
    public StatusEventPublisher(RabbitTemplate rabbit, JsonMapper json) {
        this(rabbit, json, 1000);
    }

    /** Nur für Tests: kurze Wartezeit zwischen den Versuchen. */
    StatusEventPublisher(RabbitTemplate rabbit, JsonMapper json, long backoffMillis) {
        this.rabbit = rabbit;
        this.json = json;
        this.backoffMillis = backoffMillis;
    }

    public void publish(StatusEvent event) {
        byte[] body;
        try {
            body = json.writeValueAsBytes(event);
        } catch (JacksonException e) {
            throw new EventNotDeliveredException("Event " + event.eventId() + " lässt sich nicht in JSON umwandeln", e);
        }
        send(event.eventId().toString(), event.routingKey(), body);
    }

    /** Sendet beliebige Bytes. Nur für das Demo-Szenario "kaputte Nachricht". */
    public void publishRaw(String routingKey, String body) {
        send("roh-" + System.nanoTime(), routingKey, body.getBytes(StandardCharsets.UTF_8));
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
                LOG.info("Gesendet: {} -> {} (Broker hat bestätigt)", messageId, routingKey);
                return;
            } catch (AmqpException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new EventNotDeliveredException("Broker nach " + MAX_ATTEMPTS
                            + " Versuchen nicht erreichbar: " + e.getMessage(), e);
                }
                LOG.warn("Senden fehlgeschlagen ({}), Versuch {} von {}", e.getMessage(), attempt + 1, MAX_ATTEMPTS);
                pause(backoffMillis * attempt);
            }
        }
    }

    private void sendOnce(Message message, String messageId, String routingKey) {
        CorrelationData correlation = new CorrelationData(messageId);
        rabbit.send(RabbitConfig.EXCHANGE, routingKey, message, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.ack()) {
                throw new EventNotDeliveredException("Broker hat die Nachricht nicht angenommen: " + confirm.reason());
            }
            if (correlation.getReturned() != null) {
                throw new EventNotDeliveredException("Keine Queue hat die Nachricht angenommen (" + routingKey
                        + "): " + correlation.getReturned().getReplyText());
            }
        } catch (TimeoutException e) {
            throw new EventNotDeliveredException("Keine Bestätigung vom Broker innerhalb von "
                    + CONFIRM_TIMEOUT_SECONDS + " s", e);
        } catch (ExecutionException e) {
            throw new EventNotDeliveredException("Bestätigung fehlgeschlagen: " + e.getCause(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventNotDeliveredException("Senden abgebrochen", e);
        }
    }

    /** Wartet zwischen zwei Versuchen. Wird der Thread unterbrochen, wird abgebrochen und nicht weiter versucht. */
    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventNotDeliveredException("Senden abgebrochen", e);
        }
    }
}
