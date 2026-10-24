package de.shopeasy.messaging.consumer;

/**
 * All states a return can have. The values are the same as in the REST mock and in the producer.
 * Which change from one state to the next is allowed is defined in {@link StatusTransitions}
 * (see TASK-ANALYSIS.md, section 5.3).
 *
 * <p>An {@code enum} is a fixed list of allowed values. Jackson (the JSON library) converts the text
 * {@code "APPROVED"} in a message into {@code ReturnStatus.APPROVED}. An unknown text causes an error,
 * so the consumer notices wrong messages.
 */
public enum ReturnStatus {
    NEW, APPROVED, REJECTED, IN_REVIEW, LABEL_SENT, RECEIVED, EXPIRED,
    INSPECTED, ACCEPTED, MANUAL_REVIEW, REFUNDED, CLOSED
}
