package de.shopeasy.workflow.workers;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;

/**
 * Job type {@code create-return-label} (step "Create return label", {@code t_label}): a simulated shipping API
 * (Sprint 1 assumes that the carrier has an API).
 *
 * <p>Error case: for {@code label-down@example.com} the API is "not reachable". While the job has retries left, the
 * worker fails temporarily (the engine tries again). On the last attempt it throws the BPMN error
 * {@code LABEL_SERVICE_UNAVAILABLE}: the boundary event {@code b_label} of the model catches it and a clerk creates the
 * label by hand (user task {@code t_label_man}).
 */
@Component
public class CreateReturnLabelWorker {

    static final String ERROR_CODE = "LABEL_SERVICE_UNAVAILABLE";

    private final WorkflowErrors errors;

    public CreateReturnLabelWorker(WorkflowErrors errors) {
        this.errors = errors;
    }

    @JobWorker(type = "create-return-label")
    public Map<String, Object> handle(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String returnId = Vars.required(vars, "returnId");
        if (Vars.email(vars).equals(Simulations.LABEL_DOWN)) {
            if (job.getRetries() > 1) {
                throw errors.temporary("Shipping API not reachable (" + (job.getRetries() - 1) + " attempts left)", null);
            }
            throw errors.bpmnError(ERROR_CODE, "Shipping API not reachable after all attempts");
        }
        return Map.of("labelUrl", "https://labels.example.com/" + returnId + ".pdf");
    }
}
