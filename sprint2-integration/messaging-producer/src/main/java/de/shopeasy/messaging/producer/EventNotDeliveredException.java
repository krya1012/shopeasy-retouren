package de.shopeasy.messaging.producer;

/** Das Event konnte nicht zugestellt werden (Broker nicht erreichbar, nicht bestätigt oder keine Queue gefunden). */
public class EventNotDeliveredException extends RuntimeException {

    public EventNotDeliveredException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventNotDeliveredException(String message) {
        super(message);
    }
}
