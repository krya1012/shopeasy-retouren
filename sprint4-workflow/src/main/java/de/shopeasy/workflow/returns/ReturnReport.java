package de.shopeasy.workflow.returns;

import java.time.Instant;
import java.util.List;

/**
 * A return as the REST mock delivers it. Status, reason and marketplace stay texts (the same values as the enums of
 * the mock); {@code marketplace} is null for returns of the web shop or e-mail channel, {@code reason} is null if the
 * customer gave none. Own copy of the format on purpose (no shared code with Sprint 2).
 */
public record ReturnReport(
        String returnId,
        String orderId,
        String marketplace,
        String status,
        String reason,
        String reasonText,
        Instant requestedAt,
        Instant updatedAt,
        List<Item> items,
        Customer customer,
        String returnLabelUrl) {
}
