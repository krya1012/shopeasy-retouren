package de.shopeasy.workflow.returns;

/**
 * A call to the REST mock failed.
 *
 * <p>{@code permanent} decides what the job worker does: a permanent error (the mock answered 4xx: unknown return,
 * invalid data, forbidden status change) will not go away by waiting, so the job fails at once and an incident
 * shows in Operate. A temporary error (no answer, 5xx) is tried again after a pause.
 */
public class ReturnsApiException extends RuntimeException {

    private final int httpStatus;
    private final boolean permanent;

    public ReturnsApiException(String message, int httpStatus, boolean permanent, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.permanent = permanent;
    }

    /** The HTTP status of the answer, or 0 if there was no answer. */
    public int httpStatus() {
        return httpStatus;
    }

    public boolean isPermanent() {
        return permanent;
    }
}
