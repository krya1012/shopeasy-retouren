package de.shopeasy.messaging.consumer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Helper methods that build JSON messages for the tests. */
final class EventFixtures {

    private EventFixtures() {
    }

    /** A valid event as JSON bytes, exactly like the producer sends it. */
    static byte[] event(UUID id, String returnId, String oldStatus, String newStatus) {
        return ("{\"eventId\":\"" + id + "\",\"eventType\":\"ReturnStatusChanged\",\"returnId\":\"" + returnId
                + "\",\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + newStatus
                + "\",\"occurredAt\":\"2026-10-22T10:00:00Z\",\"source\":\"test\"}").getBytes(StandardCharsets.UTF_8);
    }

    /** Same, with a new random event id. */
    static byte[] event(String returnId, String oldStatus, String newStatus) {
        return event(UUID.randomUUID(), returnId, oldStatus, newStatus);
    }

    /** Any text as message body, for broken messages. */
    static byte[] raw(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
