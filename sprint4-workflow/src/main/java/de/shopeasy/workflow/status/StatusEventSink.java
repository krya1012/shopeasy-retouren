package de.shopeasy.workflow.status;

/** Where status events go. The real one sends them to RabbitMQ; tests record them. */
public interface StatusEventSink {

    /** Sends the event and returns only when it is safe. Throws {@link EventNotDeliveredException} if not. */
    void publish(StatusEvent event);
}
