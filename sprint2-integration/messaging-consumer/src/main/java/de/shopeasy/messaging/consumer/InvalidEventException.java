package de.shopeasy.messaging.consumer;

/** Der Consumer versteht die Nachricht nicht oder sie ist fachlich falsch. Sie kommt in die Fehler-Queue. */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
