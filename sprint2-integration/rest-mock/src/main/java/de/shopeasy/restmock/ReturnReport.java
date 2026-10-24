package de.shopeasy.restmock;

import java.time.Instant;
import java.util.List;

/**
 * A return report from the marketplace. The format is the one from the task (slide 9).
 * Records are immutable (they cannot be changed after creation), so a "change" creates a copy.
 */
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

    /** Returns a copy with the new status and a new {@code updatedAt}. */
    public ReturnReport withStatus(ReturnStatus newStatus, Instant now) {
        return new ReturnReport(returnId, orderId, marketplace, newStatus, reason, reasonText,
                requestedAt, now, items, customer, returnLabelUrl);
    }
}
