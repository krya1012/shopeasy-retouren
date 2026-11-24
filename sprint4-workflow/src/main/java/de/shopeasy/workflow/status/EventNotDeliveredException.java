package de.shopeasy.workflow.status;

/** The broker did not take the event (not reachable, no confirmation, no queue). The job is tried again. */
public class EventNotDeliveredException extends RuntimeException {

    public EventNotDeliveredException(String message) {
        super(message);
    }

    public EventNotDeliveredException(String message, Throwable cause) {
        super(message, cause);
    }
}
