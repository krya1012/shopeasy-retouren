package de.shopeasy.messaging.consumer;

import static de.shopeasy.messaging.consumer.ReturnStatus.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines which status changes are allowed (see TASK-ANALYSIS.md, section 5.3). Everything else is rejected, for
 * example {@code NEW -> REFUNDED}: money must not be refunded before the article was inspected.
 *
 * <pre>
 * NEW -> APPROVED | REJECTED | IN_REVIEW      IN_REVIEW -> APPROVED | REJECTED
 * APPROVED -> LABEL_SENT -> RECEIVED | EXPIRED
 * RECEIVED -> INSPECTED -> ACCEPTED | REJECTED | MANUAL_REVIEW
 * MANUAL_REVIEW -> ACCEPTED | REJECTED
 * ACCEPTED -> REFUNDED -> CLOSED              REJECTED | EXPIRED -> CLOSED
 * </pre>
 *
 * <p>Java note: an {@link EnumMap} is a map whose keys are enum values; it is fast and keeps the order of the
 * enum. {@code Set.of(...)} creates an unchangeable set.
 */
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
        // utility class: only static methods, no objects needed
    }

    public static boolean isAllowed(ReturnStatus from, ReturnStatus to) {
        return ALLOWED.get(from).contains(to);
    }
}
