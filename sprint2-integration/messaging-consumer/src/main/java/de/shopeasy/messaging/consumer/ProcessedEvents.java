package de.shopeasy.messaging.consumer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers the {@code eventId}s that a receiver has already processed. With this list a message that is sent
 * twice is processed only once. This property is called idempotency.
 *
 * <p>The list lives in memory and is empty after a restart. In production it would be stored in a database.
 *
 * <p>Java note: {@code ConcurrentHashMap.newKeySet()} creates a set that several threads can use at the same
 * time without breaking it. RabbitMQ listeners run in their own threads.
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
