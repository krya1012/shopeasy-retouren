package de.shopeasy.restmock;

import static de.shopeasy.restmock.ReturnStatus.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Welche Statuswechsel erlaubt sind (siehe aufgabenanalyse.md, 5.3). Eigene Kopie der Tabelle aus dem
 * messaging-consumer, es gibt bewusst keinen gemeinsamen Code zwischen den Modulen.
 */
final class StatusTransitions {

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

    static boolean isAllowed(ReturnStatus from, ReturnStatus to) {
        return ALLOWED.get(from).contains(to);
    }
}
