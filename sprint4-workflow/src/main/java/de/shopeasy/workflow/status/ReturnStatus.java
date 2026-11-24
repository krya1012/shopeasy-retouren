package de.shopeasy.workflow.status;

/**
 * Status of a return: the same values as in the Sprint 2 modules and in {@code contracts/status-event.schema.json}.
 * Own copy on purpose (no shared code); the test {@code StatusTransitionsContractTest} compares the allowed changes
 * with the shared contract file.
 */
public enum ReturnStatus {
    NEW, APPROVED, REJECTED, IN_REVIEW, LABEL_SENT, RECEIVED, EXPIRED,
    INSPECTED, ACCEPTED, MANUAL_REVIEW, REFUNDED, CLOSED
}
