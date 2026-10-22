package de.shopeasy.messaging.consumer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** JSON-Nachrichten für die Tests. */
final class EventFixtures {

    private EventFixtures() {
    }

    static byte[] event(UUID id, String returnId, String oldStatus, String newStatus) {
        return ("{\"eventId\":\"" + id + "\",\"eventType\":\"ReturnStatusChanged\",\"returnId\":\"" + returnId
                + "\",\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + newStatus
                + "\",\"occurredAt\":\"2026-10-22T10:00:00Z\",\"source\":\"test\"}").getBytes(StandardCharsets.UTF_8);
    }

    static byte[] event(String returnId, String oldStatus, String newStatus) {
        return event(UUID.randomUUID(), returnId, oldStatus, newStatus);
    }

    static byte[] raw(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
