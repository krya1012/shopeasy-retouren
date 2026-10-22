package de.shopeasy.messaging.consumer;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Liest eine Nachricht als {@link StatusEvent} und prüft, ob alle Pflichtfelder da sind. */
@Component
public class EventParser {

    private final JsonMapper json;

    public EventParser(JsonMapper json) {
        this.json = json;
    }

    /** @throws InvalidEventException wenn das JSON nicht lesbar ist oder Pflichtfelder fehlen */
    public StatusEvent parse(byte[] body) {
        StatusEvent event;
        try {
            event = json.readValue(body, StatusEvent.class);
        } catch (JacksonException e) {
            throw new InvalidEventException("Nachricht ist kein gültiges Event-JSON: " + e.getOriginalMessage(), e);
        }
        if (event == null) {
            throw new InvalidEventException("Nachricht ist leer");
        }
        require(event.eventId() != null, "eventId");
        require(event.returnId() != null && !event.returnId().isBlank(), "returnId");
        require(event.newStatus() != null, "newStatus");
        require(event.oldStatus() != null, "oldStatus");
        require(event.occurredAt() != null, "occurredAt");
        return event;
    }

    private static void require(boolean ok, String field) {
        if (!ok) {
            throw new InvalidEventException("Pflichtfeld fehlt: " + field);
        }
    }
}
