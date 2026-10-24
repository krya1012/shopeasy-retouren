package de.shopeasy.restmock;

import static de.shopeasy.restmock.ReturnStatus.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines which status changes are allowed (see TASK-ANALYSIS.md, section 5.3). This is the REST mock's own copy
 * of the table in the messaging-consumer; there is no shared code between the modules on purpose.
 *
 * <p>Java note: an {@link EnumMap} is a map whose keys are enum values. {@code Set.of(...)} creates a set that
 * cannot be changed.
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
        // utility class: only static methods
    }

    static boolean isAllowed(ReturnStatus from, ReturnStatus to) {
        return ALLOWED.get(from).contains(to);
    }
}
