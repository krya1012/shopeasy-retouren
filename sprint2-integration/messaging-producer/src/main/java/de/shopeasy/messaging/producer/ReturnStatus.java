package de.shopeasy.messaging.producer;

/**
 * All states a return can have. Same values as in the consumer and the REST mock (own copy, no shared code).
 * The allowed changes between them are checked by the consumer (see TASK-ANALYSIS.md, section 5.3).
 *
 * <p>An {@code enum} is a fixed list of allowed values. Jackson writes it into the JSON as text, e.g. "APPROVED".
 */
public enum ReturnStatus {
    NEW, APPROVED, REJECTED, IN_REVIEW, LABEL_SENT, RECEIVED, EXPIRED,
    INSPECTED, ACCEPTED, MANUAL_REVIEW, REFUNDED, CLOSED
}
