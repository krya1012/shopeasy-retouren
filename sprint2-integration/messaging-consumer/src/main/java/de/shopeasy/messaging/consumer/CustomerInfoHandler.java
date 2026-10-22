package de.shopeasy.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Zweiter Empfänger der Status-Events: informiert Kund:innen. Hier nur als Log-Ausgabe, im echten System wäre es
 * eine E-Mail. Er hat eine eigene Liste verarbeiteter Events, damit niemand zweimal informiert wird.
 */
@Component
public class CustomerInfoHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerInfoHandler.class);

    private final EventParser parser;
    private final ProcessedEvents processed = new ProcessedEvents();

    public CustomerInfoHandler(EventParser parser) {
        this.parser = parser;
    }

    /** @return true, wenn Kund:in informiert wurde, false bei einem Duplikat */
    public synchronized boolean handle(byte[] body) {
        StatusEvent event = parser.parse(body);
        if (processed.contains(event.eventId())) {
            LOG.info("Kundeninfo: Duplikat ignoriert (Event {})", event.eventId());
            return false;
        }
        processed.markProcessed(event.eventId());
        LOG.info("Kundeninfo: Kund:in informiert, Retoure {} ist jetzt {}", event.returnId(), event.newStatus());
        return true;
    }
}
