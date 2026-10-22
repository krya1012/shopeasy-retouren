package de.shopeasy.messaging.producer;

import static de.shopeasy.messaging.producer.ReturnStatus.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Spielt die Szenarien für die Vorführung ab. Im Consumer-Log sieht man, was jeweils passiert. */
@Component
public class DemoScenario {

    private static final Logger LOG = LoggerFactory.getLogger(DemoScenario.class);

    private final StatusEventPublisher publisher;

    public DemoScenario(StatusEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void run() {
        LOG.info("=== 1. Vollständiger Weg einer Retoure (acht Statuswechsel)");
        String id = "RET-2026-" + (System.currentTimeMillis() % 100000);
        ReturnStatus[] path = {NEW, APPROVED, LABEL_SENT, RECEIVED, INSPECTED, ACCEPTED, REFUNDED, CLOSED};
        StatusEvent approved = null;
        for (int i = 0; i < path.length - 1; i++) {
            StatusEvent event = StatusEvent.of(id, path[i], path[i + 1]);
            publisher.publish(event);
            if (event.newStatus() == APPROVED) {
                approved = event;
            }
        }

        LOG.info("=== 2. Doppeltes Event: dasselbe Event (gleiche eventId) wird noch einmal gesendet");
        publisher.publish(approved);

        LOG.info("=== 3. Grenzfall: Prüfung ergibt MANUAL_REVIEW, danach Annahme");
        String id2 = id + "-B";
        publisher.publish(StatusEvent.of(id2, NEW, APPROVED));
        publisher.publish(StatusEvent.of(id2, APPROVED, LABEL_SENT));
        publisher.publish(StatusEvent.of(id2, LABEL_SENT, RECEIVED));
        publisher.publish(StatusEvent.of(id2, RECEIVED, INSPECTED));
        publisher.publish(StatusEvent.of(id2, INSPECTED, MANUAL_REVIEW));
        publisher.publish(StatusEvent.of(id2, MANUAL_REVIEW, ACCEPTED));

        LOG.info("=== 4. Nicht erlaubter Übergang: NEW -> INSPECTED (landet in der Fehler-Queue)");
        publisher.publish(StatusEvent.of(id + "-C", NEW, INSPECTED));

        LOG.info("=== 5. Kaputte Nachricht: kein JSON (landet in der Fehler-Queue)");
        publisher.publishRaw("retoure.status.UNBEKANNT", "das ist kein JSON");

        LOG.info("=== 6. Unvollständige Nachricht: returnId fehlt (landet in der Fehler-Queue)");
        publisher.publishRaw("retoure.status.UNBEKANNT",
                "{\"eventId\":\"" + java.util.UUID.randomUUID() + "\",\"newStatus\":\"APPROVED\"}");
    }
}
