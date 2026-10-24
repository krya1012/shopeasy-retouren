package de.shopeasy.messaging.consumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Stores the current status of every return (in memory, so it is empty after a restart; in production this would
 * be a database). A return we have never seen counts as {@code NEW}, because that is how it arrives from the
 * marketplace.
 *
 * <p><b>Spring Boot basics:</b> {@code @Component} tells Spring "create exactly one object of this class and
 * keep it". Other classes get this object through their constructor (see {@link StatusUpdateHandler}); nobody
 * writes {@code new ReturnStatusRegistry()}. This is called dependency injection.
 */
@Component
public class ReturnStatusRegistry {

    private final Map<String, ReturnStatus> statusByReturn = new ConcurrentHashMap<>();

    public ReturnStatus current(String returnId) {
        return statusByReturn.getOrDefault(returnId, ReturnStatus.NEW);
    }

    public void set(String returnId, ReturnStatus status) {
        statusByReturn.put(returnId, status);
    }
}
