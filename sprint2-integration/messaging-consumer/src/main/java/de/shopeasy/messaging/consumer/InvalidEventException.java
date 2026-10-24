package de.shopeasy.messaging.consumer;

/**
 * The consumer does not understand a message, or the message is wrong from a business point of view
 * (for example a forbidden status change). Such a message is rejected and goes to the dead letter queue.
 *
 * <p>It extends {@link RuntimeException}, an "unchecked" exception: methods do not have to declare it with
 * {@code throws}.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
