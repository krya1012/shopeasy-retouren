package de.shopeasy.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Connects the queues with the handlers. Spring starts a listener for every method marked with
 * {@code @RabbitListener}; Spring calls the method for each message that arrives in the queue.
 *
 * <p><b>How acknowledging works:</b> if the method ends normally, Spring confirms the message (ack) and RabbitMQ
 * removes it from the queue. If the method throws an exception, the message is not confirmed. We throw
 * {@link AmqpRejectAndDontRequeueException}, which means "reject it and do not deliver it again". RabbitMQ then
 * moves the message to the dead letter queue (configured in {@link RabbitConfig}), and the consumer does not get
 * stuck on a message it can never understand.
 *
 * <p>Nobody reads from the dead letter queue on purpose: the messages stay there until a person checks them
 * (RabbitMQ web page, "Get messages").
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

    /** Receives every status change from the management queue. */
    @RabbitListener(queues = RabbitConfig.QUEUE_MANAGEMENT)
    void onStatusEvent(Message message) {
        try {
            statusUpdate.handle(message.getBody());
        } catch (InvalidEventException e) {
            LOG.warn("Management rejects message: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }

    /** Receives only the status changes customers are informed about. */
    @RabbitListener(queues = RabbitConfig.QUEUE_CUSTOMER_INFO)
    void onCustomerEvent(Message message) {
        try {
            customerInfo.handle(message.getBody());
        } catch (InvalidEventException e) {
            LOG.warn("Customer info rejects message: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}
