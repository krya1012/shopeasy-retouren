package de.shopeasy.workflow.status;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import de.shopeasy.workflow.returns.ReturnsApi;
import de.shopeasy.workflow.returns.ReturnsApiException;

/**
 * Moves a return through a list of statuses: publishes the event "status changed" (RabbitMQ) and sets the status in the
 * return store (REST mock). Used by the worker {@code update-return-status} and, for the status INSPECTED, by
 * {@code check-condition}.
 *
 * <p><b>A repeated job does no harm.</b> The engine repeats a job if the worker failed or did not answer. The service
 * decides by the status the return has when the job starts: if the return already has one of the statuses of the list,
 * that status and all before it in the list are done and are skipped. For the rest, in order:
 * <ol>
 *   <li>The change must be allowed (table of the status changes), otherwise the job fails at once (permanent error)
 *       and nothing is sent.</li>
 *   <li><b>First the event, then the change.</b> If the broker is not reachable, nothing has changed yet and the repeat
 *       starts clean. If the change fails after the event went out, the repeat sends the event again; it has the same
 *       {@code eventId} (derived from return and new status), so the receiver ignores the duplicate.</li>
 *   <li>The return store sets the status with {@code expectedStatus}; it refuses with 409 if somebody else changed
 *       the return in the meantime (permanent error).</li>
 * </ol>
 * The other order (change, then event) would lose the event if the broker fails after the change: the repeat finds the
 * status already set and could not tell which status came before.
 */
@Service
public class StatusService {

    private final ReturnsApi api;
    private final StatusEventSink sink;
    private final Clock clock;

    public StatusService(ReturnsApi api, StatusEventSink sink, Clock clock) {
        this.api = api;
        this.sink = sink;
        this.clock = clock;
    }

    /**
     * @param statuses the new statuses in order, e.g. {@code ["APPROVED", "LABEL_SENT"]}
     * @throws IllegalArgumentException if a status is unknown (a mistake in the model)
     * @throws ReturnsApiException if the change is not allowed or the return store refuses or is not reachable
     * @throws EventNotDeliveredException if the broker does not take an event
     */
    public void advance(String returnId, List<String> statuses) {
        List<ReturnStatus> targets = statuses.stream().map(ReturnStatus::valueOf).toList();
        ReturnStatus current = ReturnStatus.valueOf(api.getReturn(returnId).status());
        int appliedUpTo = targets.indexOf(current);
        for (int i = appliedUpTo + 1; i < targets.size(); i++) {
            ReturnStatus target = targets.get(i);
            if (!StatusTransitions.isAllowed(current, target)) {
                throw new ReturnsApiException("Status change of " + returnId + " not allowed: " + current + " -> "
                        + target, 409, true, null);
            }
            sink.publish(StatusEvent.of(returnId, current, target, Instant.now(clock)));
            api.updateStatus(returnId, target.name(), current.name());
            current = target;
        }
    }
}
