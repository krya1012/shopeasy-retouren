package de.shopeasy.workflow.workers;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import de.shopeasy.grpc.ConditionCheckRequest;
import de.shopeasy.grpc.ConditionCheckResponse;
import de.shopeasy.grpc.Decision;
import de.shopeasy.grpc.ObservedCondition;
import de.shopeasy.grpc.client.ConditionClient;
import de.shopeasy.grpc.client.ConditionClientException;
import de.shopeasy.workflow.returns.ReturnsApiException;
import de.shopeasy.workflow.status.EventNotDeliveredException;
import de.shopeasy.workflow.status.StatusService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.grpc.Status;

/**
 * Job type {@code check-condition} (step "Check condition (gRPC)", {@code t_gr_rec}): sends what the warehouse recorded
 * to the gRPC condition service of Sprint 2 and sets the status INSPECTED.
 *
 * <p><b>Why one call per item and not {@code CheckConditionBatch}:</b> the batch is all or nothing. If the job is
 * repeated (the answer got lost, a deadline was missed while the service had stored the check), the service answers
 * {@code ALREADY_EXISTS} for the whole batch. With one call per item the worker answers {@code ALREADY_EXISTS} by
 * reading the stored result ({@code GetCheckResult}); the job can be repeated safely.
 *
 * <p>Result for the rules: {@code conditionDecision} is the worst result of all items (REJECT before MANUAL_REVIEW
 * before ACCEPT), {@code minConfidence} the lowest score, {@code conditionResults} the single results.
 *
 * <p>Errors: the {@link ConditionClient} already repeats UNAVAILABLE three times. If the service is still down (or too
 * slow) the failure is temporary: the engine tries again, and after the last retry an incident appears in Operate.
 * INVALID_ARGUMENT (wrong data from the form) is permanent.
 */
@Component
public class CheckConditionWorker {

    private final ConditionClient client;
    private final StatusService statusService;
    private final WorkflowErrors errors;
    private final Clock clock;

    public CheckConditionWorker(ConditionClient client, StatusService statusService, WorkflowErrors errors,
                                Clock clock) {
        this.client = client;
        this.statusService = statusService;
        this.errors = errors;
        this.clock = clock;
    }

    @JobWorker(type = "check-condition")
    public Map<String, Object> handle(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        try {
            String returnId = Vars.required(vars, "returnId");
            List<Map<String, Object>> inspections = Vars.objects(vars, "inspections");
            if (inspections.isEmpty()) {
                throw new IllegalArgumentException("The process variable 'inspections' is missing or empty");
            }
            List<ConditionCheckResponse> results = new ArrayList<>();
            for (Map<String, Object> inspection : inspections) {
                results.add(check(returnId, inspection));
            }
            statusService.advance(returnId, List.of("INSPECTED"));
            return output(results);
        } catch (ConditionClientException e) {
            throw e.getCode() == Status.Code.INVALID_ARGUMENT
                    ? errors.permanent(e.getMessage(), e) : errors.temporary(e.getMessage(), e);
        } catch (ReturnsApiException e) {
            throw errors.fromApi(e);
        } catch (EventNotDeliveredException e) {
            throw errors.temporary(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw errors.permanent(e.getMessage(), e);
        }
    }

    /** One item: check it; if the service already has the check (job repeated), read the stored result. */
    private ConditionCheckResponse check(String returnId, Map<String, Object> inspection) {
        String sku = Vars.required(inspection, "sku");
        ConditionCheckRequest request = ConditionCheckRequest.newBuilder()
                .setReturnId(returnId)
                .setSku(sku)
                .setObservedCondition(ObservedCondition.valueOf(Vars.required(inspection, "condition")))
                .setTagsPresent(Vars.flag(inspection, "tagsPresent", true))
                .setOriginalPackaging(Vars.flag(inspection, "originalPackaging", true))
                .setInspectorNote(Vars.text(inspection, "inspectorNote") == null ? "" : Vars.text(inspection, "inspectorNote"))
                .setInspectedAt(Instant.now(clock).toString())
                .build();
        try {
            return client.checkCondition(request);
        } catch (ConditionClientException e) {
            if (e.getCode() == Status.Code.ALREADY_EXISTS) {
                return client.getCheckResult(returnId, sku);
            }
            throw e;
        }
    }

    private static Map<String, Object> output(List<ConditionCheckResponse> results) {
        Decision worst = Decision.ACCEPT;
        double lowest = 1.0;
        List<Map<String, Object>> single = new ArrayList<>();
        for (ConditionCheckResponse r : results) {
            if (severity(r.getDecision()) > severity(worst)) {
                worst = r.getDecision();
            }
            lowest = Math.min(lowest, r.getConfidenceScore());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("sku", r.getSku());
            entry.put("decision", r.getDecision().name());
            entry.put("confidenceScore", r.getConfidenceScore());
            entry.put("reason", r.getReason());
            single.add(entry);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("conditionDecision", worst == Decision.DECISION_UNSPECIFIED ? Decision.MANUAL_REVIEW.name() : worst.name());
        out.put("minConfidence", lowest);
        out.put("conditionResults", single);
        return out;
    }

    /** REJECT is worse than MANUAL_REVIEW, which is worse than ACCEPT; an unknown result counts as MANUAL_REVIEW. */
    private static int severity(Decision decision) {
        return switch (decision) {
            case REJECT -> 3;
            case MANUAL_REVIEW, DECISION_UNSPECIFIED, UNRECOGNIZED -> 2;
            case ACCEPT -> 1;
        };
    }
}
