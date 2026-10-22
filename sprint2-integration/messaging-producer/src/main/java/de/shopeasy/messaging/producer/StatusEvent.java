package de.shopeasy.messaging.producer;

import java.time.Instant;
import java.util.UUID;

/**
 * Das Event "Status einer Retoure hat sich geändert". Es wird als JSON gesendet, das Schema steht in der README.
 * Der Consumer hat eine eigene Kopie, es gibt bewusst keinen gemeinsamen Code.
 */
public record StatusEvent(
        UUID eventId,
        String eventType,
        String returnId,
        ReturnStatus oldStatus,
        ReturnStatus newStatus,
        Instant occurredAt,
        String source) {

    public static final String TYPE = "ReturnStatusChanged";
    public static final String SOURCE = "ShopEasy-Retourenmanagement";

    public static StatusEvent of(String returnId, ReturnStatus oldStatus, ReturnStatus newStatus) {
        return new StatusEvent(UUID.randomUUID(), TYPE, returnId, oldStatus, newStatus, Instant.now(), SOURCE);
    }

    public String routingKey() {
        return RabbitConfig.ROUTING_KEY_PREFIX + newStatus;
    }
}
