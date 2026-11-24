package de.shopeasy.workflow.workers;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.shopeasy.workflow.returns.ReturnsApiException;
import de.shopeasy.workflow.status.EventNotDeliveredException;
import de.shopeasy.workflow.status.StatusService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;

/**
 * Job type {@code update-return-status}: all send tasks of the model ({@code t_deny}, {@code t_notify},
 * {@code t_notify_in}, {@code t_expired}, {@code t_end_notify}, {@code t_inrev}, {@code t_mrev}). The input variable
 * {@code newStatuses} (set by the input mapping of the task) is the list of statuses to set, in order.
 *
 * <p>Errors: the return store refuses a change (409, unknown return): permanent, incident at once. The return store or
 * the broker is not reachable: temporary, tried again after the pause. See {@link StatusService}.
 */
@Component
public class UpdateReturnStatusWorker {

    private final StatusService statusService;
    private final WorkflowErrors errors;

    public UpdateReturnStatusWorker(StatusService statusService, WorkflowErrors errors) {
        this.statusService = statusService;
        this.errors = errors;
    }

    @JobWorker(type = "update-return-status")
    @SuppressWarnings("unchecked")
    public void handle(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        try {
            String returnId = Vars.required(vars, "returnId");
            Object statuses = vars.get("newStatuses");
            if (!(statuses instanceof List<?>) || ((List<?>) statuses).isEmpty()) {
                throw new IllegalArgumentException("The process variable 'newStatuses' is missing or empty");
            }
            statusService.advance(returnId, (List<String>) statuses);
        } catch (ReturnsApiException e) {
            throw errors.fromApi(e);
        } catch (EventNotDeliveredException e) {
            throw errors.temporary(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw errors.permanent(e.getMessage(), e);
        }
    }
}
