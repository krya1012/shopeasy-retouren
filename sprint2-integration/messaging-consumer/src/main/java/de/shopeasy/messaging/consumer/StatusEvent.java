package de.shopeasy.messaging.consumer;

import java.time.Instant;
import java.util.UUID;

/**
 * The event "the status of a return has changed". It arrives as JSON in a message. The schema is described in the
 * README and is the contract with the producer. This is the consumer's own copy of the class: the two modules
 * share no code on purpose, so they stay independent (decoupled).
 *
 * <p>A {@code record} is a short way to write a class that only holds data. Java creates the constructor, the
 * getters (for example {@code eventId()}), {@code equals} and {@code toString} automatically.
 *
 * @param eventId    unique id of this event; used to detect duplicates
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
}
