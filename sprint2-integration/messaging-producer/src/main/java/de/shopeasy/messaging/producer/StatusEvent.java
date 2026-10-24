package de.shopeasy.messaging.producer;

import java.time.Instant;
import java.util.UUID;

/**
 * The event "the status of a return has changed". It is sent as JSON; the schema is in the README.
 * The consumer has its own copy of this class on purpose: there is no shared code, so the two systems stay
 * independent (decoupled) and only agree on the JSON.
 *
 * <p>A {@code record} is a short way to write a class that only holds data. Java creates the constructor,
 * the getters (e.g. {@code eventId()}), {@code equals} and {@code toString} automatically.
 *
 * @param eventId    unique id of this event; the consumer uses it to detect duplicates
 * @param eventType  always "ReturnStatusChanged"
 * @param returnId   the return this event is about, e.g. "RET-2026-00457"
 * @param oldStatus  status before the change
 * @param newStatus  status after the change
 * @param occurredAt when the change happened
 * @param source     the system that sent the event
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
    public static final String SOURCE = "ShopEasy-Returns-Management";

    /** Creates a new event with a random id and the current time. */
    public static StatusEvent of(String returnId, ReturnStatus oldStatus, ReturnStatus newStatus) {
        return new StatusEvent(UUID.randomUUID(), TYPE, returnId, oldStatus, newStatus, Instant.now(), SOURCE);
    }

    /** The routing key decides which queues get the event, e.g. {@code return.status.APPROVED}. */
    public String routingKey() {
        return RabbitConfig.ROUTING_KEY_PREFIX + newStatus;
    }
}
