package de.shopeasy.messaging.producer;

/** Status einer Retoure (siehe aufgabenanalyse.md, 5.3). */
public enum ReturnStatus {
    NEW, APPROVED, REJECTED, IN_REVIEW, LABEL_SENT, RECEIVED, EXPIRED,
    INSPECTED, ACCEPTED, MANUAL_REVIEW, REFUNDED, CLOSED
}
