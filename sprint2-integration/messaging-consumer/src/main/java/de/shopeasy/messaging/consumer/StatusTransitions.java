package de.shopeasy.messaging.consumer;

import static de.shopeasy.messaging.consumer.ReturnStatus.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Welche Statuswechsel erlaubt sind (siehe aufgabenanalyse.md, 5.3). */
public final class StatusTransitions {

    private static final Map<ReturnStatus, Set<ReturnStatus>> ALLOWED = new EnumMap<>(ReturnStatus.class);

    static {
        ALLOWED.put(NEW, Set.of(APPROVED, REJECTED, IN_REVIEW));
        ALLOWED.put(IN_REVIEW, Set.of(APPROVED, REJECTED));
        ALLOWED.put(APPROVED, Set.of(LABEL_SENT));
        ALLOWED.put(LABEL_SENT, Set.of(RECEIVED, EXPIRED));
        ALLOWED.put(RECEIVED, Set.of(INSPECTED));
        ALLOWED.put(INSPECTED, Set.of(ACCEPTED, REJECTED, MANUAL_REVIEW));
        ALLOWED.put(MANUAL_REVIEW, Set.of(ACCEPTED, REJECTED));
        ALLOWED.put(ACCEPTED, Set.of(REFUNDED));
        ALLOWED.put(REFUNDED, Set.of(CLOSED));
        ALLOWED.put(REJECTED, Set.of(CLOSED));
        ALLOWED.put(EXPIRED, Set.of(CLOSED));
        ALLOWED.put(CLOSED, Set.of());
    }

    private StatusTransitions() {
    }

    public static boolean isAllowed(ReturnStatus from, ReturnStatus to) {
        return ALLOWED.get(from).contains(to);
    }
}
