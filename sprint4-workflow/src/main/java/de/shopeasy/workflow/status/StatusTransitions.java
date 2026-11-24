package de.shopeasy.workflow.status;

import static de.shopeasy.workflow.status.ReturnStatus.ACCEPTED;
import static de.shopeasy.workflow.status.ReturnStatus.APPROVED;
import static de.shopeasy.workflow.status.ReturnStatus.CLOSED;
import static de.shopeasy.workflow.status.ReturnStatus.EXPIRED;
import static de.shopeasy.workflow.status.ReturnStatus.IN_REVIEW;
import static de.shopeasy.workflow.status.ReturnStatus.INSPECTED;
import static de.shopeasy.workflow.status.ReturnStatus.LABEL_SENT;
import static de.shopeasy.workflow.status.ReturnStatus.MANUAL_REVIEW;
import static de.shopeasy.workflow.status.ReturnStatus.NEW;
import static de.shopeasy.workflow.status.ReturnStatus.RECEIVED;
import static de.shopeasy.workflow.status.ReturnStatus.REFUNDED;
import static de.shopeasy.workflow.status.ReturnStatus.REJECTED;

import java.util.List;
import java.util.Map;

/**
 * The allowed status changes (copy of {@code sprint2-integration/contracts/status-transitions.json}). The workflow
 * checks a change before it sends the event (see {@link StatusService}); the return store checks it again.
 */
public final class StatusTransitions {

    static final Map<ReturnStatus, List<ReturnStatus>> ALLOWED = Map.ofEntries(
            Map.entry(NEW, List.of(APPROVED, REJECTED, IN_REVIEW)),
            Map.entry(IN_REVIEW, List.of(APPROVED, REJECTED)),
            Map.entry(APPROVED, List.of(LABEL_SENT)),
            Map.entry(LABEL_SENT, List.of(RECEIVED, EXPIRED)),
            Map.entry(RECEIVED, List.of(INSPECTED)),
            Map.entry(INSPECTED, List.of(ACCEPTED, REJECTED, MANUAL_REVIEW)),
            Map.entry(MANUAL_REVIEW, List.of(ACCEPTED, REJECTED)),
            Map.entry(ACCEPTED, List.of(REFUNDED)),
            Map.entry(REFUNDED, List.of(CLOSED)),
            Map.entry(REJECTED, List.of(CLOSED)),
            Map.entry(EXPIRED, List.of(CLOSED)),
            Map.entry(CLOSED, List.of()));

    private StatusTransitions() {
    }

    public static boolean isAllowed(ReturnStatus from, ReturnStatus to) {
        return ALLOWED.get(from).contains(to);
    }
}
