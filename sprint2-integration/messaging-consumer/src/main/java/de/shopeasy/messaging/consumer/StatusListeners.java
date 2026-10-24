package de.shopeasy.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Verbindet die Queues mit den Handlern. Gelingt die Verarbeitung, bestätigt Spring die Nachricht (ack).
 * Versteht ein Handler die Nachricht nicht, lehnen wir sie ohne erneute Zustellung ab (reject). Der Broker legt
 * sie dann in die Fehler-Queue, und der Consumer bleibt nicht an ihr hängen.
 * Aus der Fehler-Queue liest absichtlich niemand: Die Nachrichten bleiben liegen, bis eine Person sie prüft
 * (in der RabbitMQ-Oberfläche unter "Get messages").
 */
@Component
public class StatusListeners {

    private static final Logger LOG = LoggerFactory.getLogger(StatusListeners.class);

    private final StatusUpdateHandler statusUpdate;
    private final CustomerInfoHandler customerInfo;

    public StatusListeners(StatusUpdateHandler statusUpdate, CustomerInfoHandler customerInfo) {
        this.statusUpdate = statusUpdate;
        this.customerInfo = customerInfo;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_VERWALTUNG)
    void onStatusEvent(Message message) {
        try {
            statusUpdate.handle(message.getBody());
        } catch (InvalidEventException e) {
            LOG.warn("Verwaltung lehnt Nachricht ab: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_KUNDENINFO)
    void onCustomerEvent(Message message) {
        try {
            customerInfo.handle(message.getBody());
        } catch (InvalidEventException e) {
            LOG.warn("Kundeninfo lehnt Nachricht ab: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}
