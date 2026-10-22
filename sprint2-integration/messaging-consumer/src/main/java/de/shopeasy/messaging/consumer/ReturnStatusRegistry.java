package de.shopeasy.messaging.consumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Der aktuelle Status jeder Retoure. Eine unbekannte Retoure gilt als NEW (so kommt sie vom Marktplatz). */
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
