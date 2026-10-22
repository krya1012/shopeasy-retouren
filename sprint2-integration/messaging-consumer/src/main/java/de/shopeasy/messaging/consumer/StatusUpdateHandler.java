package de.shopeasy.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Die Logik des Empfängers "Verwaltung": Er aktualisiert den Status einer Retoure.
 * Sie arbeitet nur mit den Bytes der Nachricht, deshalb kann man sie ohne Broker testen.
 */
@Component
public class StatusUpdateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StatusUpdateHandler.class);

    public enum Outcome { PROCESSED, DUPLICATE }

    private final EventParser parser;
    private final ReturnStatusRegistry registry;
    private final ProcessedEvents processed = new ProcessedEvents();

    public StatusUpdateHandler(EventParser parser, ReturnStatusRegistry registry) {
        this.parser = parser;
        this.registry = registry;
    }

    /** @throws InvalidEventException wenn die Nachricht nicht verstanden wird oder der Übergang nicht erlaubt ist */
    public synchronized Outcome handle(byte[] body) {
        StatusEvent event = parser.parse(body);

        // Zuerst auf Duplikate prüfen: Ein doppeltes Event würde sonst als "unerlaubter Übergang" abgelehnt.
        if (processed.contains(event.eventId())) {
            LOG.info("Duplikat ignoriert: Event {} wurde schon verarbeitet ({} {} -> {})",
                    event.eventId(), event.returnId(), event.oldStatus(), event.newStatus());
            return Outcome.DUPLICATE;
        }

        ReturnStatus current = registry.current(event.returnId());
        if (event.oldStatus() != current) {
            throw new InvalidEventException("Event passt nicht zum bekannten Status: " + event.returnId()
                    + " ist " + current + ", das Event geht aber von " + event.oldStatus() + " aus");
        }
        if (!StatusTransitions.isAllowed(event.oldStatus(), event.newStatus())) {
            throw new InvalidEventException("Übergang nicht erlaubt: " + event.returnId() + " "
                    + event.oldStatus() + " -> " + event.newStatus());
        }

        registry.set(event.returnId(), event.newStatus());
        processed.markProcessed(event.eventId());
        LOG.info("Status aktualisiert: {} {} -> {} (Event {})",
                event.returnId(), event.oldStatus(), event.newStatus(), event.eventId());
        return Outcome.PROCESSED;
    }
}
