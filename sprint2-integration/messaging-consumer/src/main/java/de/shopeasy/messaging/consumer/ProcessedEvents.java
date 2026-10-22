package de.shopeasy.messaging.consumer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Merkt sich die eventIds, die ein Empfänger schon verarbeitet hat. So wird ein doppelt gesendetes Event
 * nur einmal verarbeitet (Idempotenz). Die Liste liegt im Speicher und ist nach einem Neustart leer;
 * im Betrieb gehört sie in eine Datenbank.
 */
public class ProcessedEvents {

    private final Set<UUID> ids = ConcurrentHashMap.newKeySet();

    public boolean contains(UUID eventId) {
        return ids.contains(eventId);
    }

    public void markProcessed(UUID eventId) {
        ids.add(eventId);
    }
}
