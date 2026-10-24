package de.shopeasy.messaging.consumer;

import static de.shopeasy.messaging.consumer.EventFixtures.event;
import static de.shopeasy.messaging.consumer.EventFixtures.raw;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

/**
 * Tests the management handler without Spring and without a broker. {@code @BeforeEach} runs before every test
 * and creates a fresh handler, so the tests do not influence each other.
 */
class StatusUpdateHandlerTest {

    private ReturnStatusRegistry registry;
    private StatusUpdateHandler handler;

    @BeforeEach
    void setUp() {
        registry = new ReturnStatusRegistry();
        handler = new StatusUpdateHandler(new EventParser(JsonMapper.builder().build()), registry);
    }

    @Test
    void allowedTransitionUpdatesTheStatus() {
        assertEquals(StatusUpdateHandler.Outcome.PROCESSED, handler.handle(event("RET-1", "NEW", "APPROVED")));
        assertEquals(ReturnStatus.APPROVED, registry.current("RET-1"));
    }

    @Test
    void wholePathToTheEnd() {
        String[] path = {"NEW", "APPROVED", "LABEL_SENT", "RECEIVED", "INSPECTED", "ACCEPTED", "REFUNDED", "CLOSED"};
        for (int i = 0; i < path.length - 1; i++) {
            handler.handle(event("RET-1", path[i], path[i + 1]));
        }
        assertEquals(ReturnStatus.CLOSED, registry.current("RET-1"));
    }

    @Test
    void borderlineCaseGoesThroughManualReview() {
        String[] path = {"NEW", "APPROVED", "LABEL_SENT", "RECEIVED", "INSPECTED", "MANUAL_REVIEW", "ACCEPTED"};
        for (int i = 0; i < path.length - 1; i++) {
            handler.handle(event("RET-1", path[i], path[i + 1]));
        }
        assertEquals(ReturnStatus.ACCEPTED, registry.current("RET-1"));
    }

    @Test
    void duplicateEventIsIgnored() {
        UUID id = UUID.randomUUID();
        handler.handle(event(id, "RET-1", "NEW", "APPROVED"));
        handler.handle(event("RET-1", "APPROVED", "LABEL_SENT"));

        // the same event again, although the status has moved on in the meantime
        assertEquals(StatusUpdateHandler.Outcome.DUPLICATE, handler.handle(event(id, "RET-1", "NEW", "APPROVED")));
        assertEquals(ReturnStatus.LABEL_SENT, registry.current("RET-1"));
    }

    @Test
    void forbiddenTransitionIsRejectedAndStatusStays() {
        InvalidEventException e = assertThrows(InvalidEventException.class,
                () -> handler.handle(event("RET-1", "NEW", "REFUNDED")));
        assertTrue(e.getMessage().contains("not allowed"));
        assertEquals(ReturnStatus.NEW, registry.current("RET-1"));
    }

    @Test
    void oldStatusDoesNotMatchTheKnownStatus() {
        InvalidEventException e = assertThrows(InvalidEventException.class,
                () -> handler.handle(event("RET-1", "RECEIVED", "INSPECTED")));
        assertTrue(e.getMessage().contains("does not match the known status"));
    }

    @Test
    void rejectedMessageIsNotMarkedAsProcessed() {
        UUID id = UUID.randomUUID();
        assertThrows(InvalidEventException.class, () -> handler.handle(event(id, "RET-1", "NEW", "REFUNDED")));
        // still rejected as invalid, not ignored as a duplicate
        assertThrows(InvalidEventException.class, () -> handler.handle(event(id, "RET-1", "NEW", "REFUNDED")));
    }

    @Test
    void messageWithoutJsonIsRejected() {
        assertThrows(InvalidEventException.class, () -> handler.handle(raw("this is not JSON")));
    }

    @Test
    void emptyMessageIsRejected() {
        assertThrows(InvalidEventException.class, () -> handler.handle(raw("")));
    }

    @Test
    void unknownStatusIsRejected() {
        assertThrows(InvalidEventException.class, () -> handler.handle(event("RET-1", "NEW", "NONSENSE")));
    }

    @Test
    void missingRequiredFieldIsRejected() {
        InvalidEventException e = assertThrows(InvalidEventException.class, () -> handler.handle(
                raw("{\"eventId\":\"" + UUID.randomUUID() + "\",\"newStatus\":\"APPROVED\"}")));
        assertTrue(e.getMessage().contains("returnId"));
    }

    @Test
    void unknownExtraFieldsDoNotMatter() {
        byte[] body = raw("{\"eventId\":\"" + UUID.randomUUID() + "\",\"returnId\":\"RET-1\","
                + "\"oldStatus\":\"NEW\",\"newStatus\":\"APPROVED\",\"occurredAt\":\"2026-10-22T10:00:00Z\","
                + "\"newField\":42}");
        assertEquals(StatusUpdateHandler.Outcome.PROCESSED, handler.handle(body));
    }
}
