package de.shopeasy.messaging.consumer;

import static de.shopeasy.messaging.consumer.EventFixtures.event;
import static de.shopeasy.messaging.consumer.EventFixtures.raw;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class CustomerInfoHandlerTest {

    private final CustomerInfoHandler handler = new CustomerInfoHandler(new EventParser(JsonMapper.builder().build()));

    @Test
    void kundinWirdInformiert() {
        assertTrue(handler.handle(event("RET-1", "NEW", "APPROVED")));
    }

    @Test
    void kundinWirdBeiDuplikatNichtZweimalInformiert() {
        UUID id = UUID.randomUUID();
        assertTrue(handler.handle(event(id, "RET-1", "NEW", "APPROVED")));
        assertFalse(handler.handle(event(id, "RET-1", "NEW", "APPROVED")));
    }

    @Test
    void kaputteNachrichtWirdAbgelehnt() {
        assertThrows(InvalidEventException.class, () -> handler.handle(raw("kein JSON")));
    }
}
