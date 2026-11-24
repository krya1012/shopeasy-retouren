package de.shopeasy.workflow.workers;

import org.springframework.stereotype.Component;

import de.shopeasy.workflow.config.WorkflowProperties;
import de.shopeasy.workflow.returns.ReturnsApiException;
import io.camunda.client.exception.BpmnError;
import io.camunda.client.exception.CamundaError;
import io.camunda.client.exception.JobError;

/**
 * The three ways a worker reports a problem to the engine (throw the result):
 * <ul>
 *   <li><b>Temporary failure</b> ({@link #temporary}): "try again later". The engine gives the job to the worker again
 *       after {@code workflow.retry-backoff}, until the retries of the model ({@code retries="3"}) are used up. Then an
 *       <b>incident</b> appears in Operate; an operator fixes the problem and presses "Retry".</li>
 *   <li><b>Permanent failure</b> ({@link #permanent}): waiting does not help (wrong data, forbidden change). The job
 *       fails with 0 retries, so the incident appears at once.</li>
 *   <li><b>BPMN error</b> ({@link #bpmnError}): a business problem that the model handles with a boundary event
 *       (label service down, refund failed). The case continues on the error path, for example with a clerk.</li>
 * </ul>
 */
@Component
public class WorkflowErrors {

    private final WorkflowProperties properties;

    public WorkflowErrors(WorkflowProperties properties) {
        this.properties = properties;
    }

    /** Retries stay as the engine counts them (retries left minus one), with the configured pause. */
    public JobError temporary(String message, Throwable cause) {
        return CamundaError.jobError(message, null, null, properties.retryBackoff(), cause);
    }

    public JobError permanent(String message, Throwable cause) {
        return CamundaError.jobError(message, null, 0, java.time.Duration.ZERO, cause);
    }

    public BpmnError bpmnError(String code, String message) {
        return CamundaError.bpmnError(code, message);
    }

    /** Translates a failed call to the REST mock: permanent for 4xx, temporary otherwise. */
    public JobError fromApi(ReturnsApiException e) {
        return e.isPermanent() ? permanent(e.getMessage(), e) : temporary(e.getMessage(), e);
    }
}
