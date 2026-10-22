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

class StatusUpdateHandlerTest {

    private ReturnStatusRegistry registry;
    private StatusUpdateHandler handler;

    @BeforeEach
    void setUp() {
        registry = new ReturnStatusRegistry();
        handler = new StatusUpdateHandler(new EventParser(JsonMapper.builder().build()), registry);
    }

    @Test
    void erlaubterUebergangAktualisiertDenStatus() {
        assertEquals(StatusUpdateHandler.Outcome.PROCESSED, handler.handle(event("RET-1", "NEW", "APPROVED")));
        assertEquals(ReturnStatus.APPROVED, registry.current("RET-1"));
    }

    @Test
    void ganzerWegBisZumEnde() {
        String[] path = {"NEW", "APPROVED", "LABEL_SENT", "RECEIVED", "INSPECTED", "ACCEPTED", "REFUNDED", "CLOSED"};
        for (int i = 0; i < path.length - 1; i++) {
            handler.handle(event("RET-1", path[i], path[i + 1]));
        }
        assertEquals(ReturnStatus.CLOSED, registry.current("RET-1"));
    }

    @Test
    void grenzfallWegUeberManualReview() {
        String[] path = {"NEW", "APPROVED", "LABEL_SENT", "RECEIVED", "INSPECTED", "MANUAL_REVIEW", "ACCEPTED"};
        for (int i = 0; i < path.length - 1; i++) {
            handler.handle(event("RET-1", path[i], path[i + 1]));
        }
        assertEquals(ReturnStatus.ACCEPTED, registry.current("RET-1"));
    }

    @Test
    void doppeltesEventWirdIgnoriert() {
        UUID id = UUID.randomUUID();
        handler.handle(event(id, "RET-1", "NEW", "APPROVED"));
        handler.handle(event("RET-1", "APPROVED", "LABEL_SENT"));

        // dasselbe Event noch einmal, obwohl der Status inzwischen weiter ist
        assertEquals(StatusUpdateHandler.Outcome.DUPLICATE, handler.handle(event(id, "RET-1", "NEW", "APPROVED")));
        assertEquals(ReturnStatus.LABEL_SENT, registry.current("RET-1"));
    }

    @Test
    void nichtErlaubterUebergangWirdAbgelehntUndStatusBleibt() {
        InvalidEventException e = assertThrows(InvalidEventException.class,
                () -> handler.handle(event("RET-1", "NEW", "REFUNDED")));
        assertTrue(e.getMessage().contains("nicht erlaubt"));
        assertEquals(ReturnStatus.NEW, registry.current("RET-1"));
    }

    @Test
    void oldStatusPasstNichtZumBekanntenStatus() {
        InvalidEventException e = assertThrows(InvalidEventException.class,
                () -> handler.handle(event("RET-1", "RECEIVED", "INSPECTED")));
        assertTrue(e.getMessage().contains("passt nicht zum bekannten Status"));
    }

    @Test
    void abgelehnteNachrichtWirdNichtAlsVerarbeitetMarkiert() {
        UUID id = UUID.randomUUID();
        assertThrows(InvalidEventException.class, () -> handler.handle(event(id, "RET-1", "NEW", "REFUNDED")));
        assertThrows(InvalidEventException.class, () -> handler.handle(event(id, "RET-1", "NEW", "REFUNDED")));
    }

    @Test
    void keinJsonWirdAbgelehnt() {
        assertThrows(InvalidEventException.class, () -> handler.handle(raw("das ist kein JSON")));
    }

    @Test
    void leereNachrichtWirdAbgelehnt() {
        assertThrows(InvalidEventException.class, () -> handler.handle(raw("")));
    }

    @Test
    void unbekannterStatusWirdAbgelehnt() {
        assertThrows(InvalidEventException.class, () -> handler.handle(event("RET-1", "NEW", "QUATSCH")));
    }

    @Test
    void fehlendesPflichtfeldWirdAbgelehnt() {
        InvalidEventException e = assertThrows(InvalidEventException.class, () -> handler.handle(
                raw("{\"eventId\":\"" + UUID.randomUUID() + "\",\"newStatus\":\"APPROVED\"}")));
        assertTrue(e.getMessage().contains("returnId"));
    }

    @Test
    void unbekannteZusatzfelderStoerenNicht() {
        byte[] body = raw("{\"eventId\":\"" + UUID.randomUUID() + "\",\"returnId\":\"RET-1\","
                + "\"oldStatus\":\"NEW\",\"newStatus\":\"APPROVED\",\"occurredAt\":\"2026-10-22T10:00:00Z\","
                + "\"neuesFeld\":42}");
        assertEquals(StatusUpdateHandler.Outcome.PROCESSED, handler.handle(body));
    }
}
