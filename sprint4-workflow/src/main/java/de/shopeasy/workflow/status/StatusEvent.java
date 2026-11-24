package de.shopeasy.workflow.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * The event "the status of a return has changed", as JSON the same as in Sprint 2 ({@code contracts/status-event.schema.json}).
 *
 * <p>The {@code eventId} is not random: it is derived from return and new status. If a job is repeated after a
 * failure, the same event gets the same id, and the receiver (which ignores ids it has seen) does not process it twice.
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
    public static final String SOURCE = "ShopEasy-Returns-Workflow";
    private static final String ROUTING_KEY_PREFIX = "return.status.";

    public static StatusEvent of(String returnId, ReturnStatus oldStatus, ReturnStatus newStatus, Instant now) {
        UUID id = UUID.nameUUIDFromBytes((returnId + ":" + newStatus).getBytes(StandardCharsets.UTF_8));
        return new StatusEvent(id, TYPE, returnId, oldStatus, newStatus, now, SOURCE);
    }

    /** The routing key decides which queues get the event, e.g. {@code return.status.APPROVED}. */
    public String routingKey() {
        return ROUTING_KEY_PREFIX + newStatus;
    }
}
