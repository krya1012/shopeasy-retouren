package de.shopeasy.messaging.producer;

/**
 * The event could not be delivered: the broker is not reachable, it did not confirm the message, or no queue
 * accepted it. The producer never fails silently; it always ends with this exception.
 */
public class EventNotDeliveredException extends RuntimeException {

    public EventNotDeliveredException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventNotDeliveredException(String message) {
        super(message);
    }
}
