package de.shopeasy.messaging.consumer;

import java.time.Instant;
import java.util.UUID;

/**
 * Das Event "Status einer Retoure hat sich geändert". Das Schema steht in der README und ist der Vertrag mit dem
 * Producer. Dies ist die eigene Kopie des Consumers.
 */
public record StatusEvent(
        UUID eventId,
        String eventType,
        String returnId,
        ReturnStatus oldStatus,
        ReturnStatus newStatus,
        Instant occurredAt,
        String source) {
}
