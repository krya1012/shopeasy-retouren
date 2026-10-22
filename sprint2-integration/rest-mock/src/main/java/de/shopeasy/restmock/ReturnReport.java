package de.shopeasy.restmock;

import java.time.Instant;
import java.util.List;

/** Retourenmeldung vom Marktplatz. Format laut Aufgabe (Folie 9). */
public record ReturnReport(
        String returnId,
        String orderId,
        Marketplace marketplace,
        ReturnStatus status,
        ReasonCode reason,
        String reasonText,
        Instant requestedAt,
        Instant updatedAt,
        List<Item> items,
        Customer customer,
        String returnLabelUrl) {

    /** Gibt eine Kopie mit neuem Status und neuem {@code updatedAt} zurück. */
    public ReturnReport withStatus(ReturnStatus newStatus, Instant now) {
        return new ReturnReport(returnId, orderId, marketplace, newStatus, reason, reasonText,
                requestedAt, now, items, customer, returnLabelUrl);
    }
}
