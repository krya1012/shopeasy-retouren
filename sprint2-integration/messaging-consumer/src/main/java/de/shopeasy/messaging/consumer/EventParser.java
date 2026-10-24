package de.shopeasy.messaging.consumer;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns the bytes of a message into a {@link StatusEvent} and checks that all required fields are present.
 * Anything it does not understand becomes an {@link InvalidEventException}.
 *
 * <p><b>Spring Boot basics:</b> the {@link JsonMapper} (from the Jackson library) is created in
 * {@link RabbitConfig}. Spring passes it in through the constructor ("constructor injection"), so this class
 * never creates it itself and a test can pass its own mapper.
 */
@Component
public class EventParser {

    private final JsonMapper json;

    public EventParser(JsonMapper json) {
        this.json = json;
    }

    /** @throws InvalidEventException if the JSON cannot be read or required fields are missing */
    public StatusEvent parse(byte[] body) {
        StatusEvent event;
        try {
            event = json.readValue(body, StatusEvent.class);
        } catch (JacksonException e) {
            throw new InvalidEventException("Message is not valid event JSON: " + e.getOriginalMessage(), e);
        }
        if (event == null) {
            throw new InvalidEventException("Message is empty");
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
            throw new InvalidEventException("Required field is missing: " + field);
        }
    }
}
