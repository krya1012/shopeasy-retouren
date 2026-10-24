package de.shopeasy.restmock;

/**
 * Status of a return. Same values as in the messaging modules (TASK-ANALYSIS.md, section 5.3).
 * Which changes are allowed is defined in {@link StatusTransitions}.
 *
 * <p>If a request contains a text that is not in this list (e.g. "NONSENSE"), Spring answers with HTTP 400.
 */
public enum ReturnStatus {
    NEW, APPROVED, REJECTED, IN_REVIEW, LABEL_SENT, RECEIVED, EXPIRED,
    INSPECTED, ACCEPTED, MANUAL_REVIEW, REFUNDED, CLOSED
}
