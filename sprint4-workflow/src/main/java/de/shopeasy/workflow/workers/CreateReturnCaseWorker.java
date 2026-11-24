package de.shopeasy.workflow.workers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import de.shopeasy.workflow.returns.CreateReturnRequest;
import de.shopeasy.workflow.returns.Item;
import de.shopeasy.workflow.returns.Order;
import de.shopeasy.workflow.returns.ReturnReport;
import de.shopeasy.workflow.returns.ReturnsApi;
import de.shopeasy.workflow.returns.ReturnsApiException;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;

/**
 * Job type {@code create-return-case} (step "Create return case", {@code t_create}).
 *
 * <p>All three channels end here. A return of a marketplace already exists in the return store (variable
 * {@code returnId} from the poller): it is read. A return of the web shop or an e-mail (data from the start message or
 * from the clerk's form) is created with {@code POST}. The header {@code Idempotency-Key} is derived from the process
 * instance, so a repeated job does not create a second return.
 *
 * <p>The worker also collects the facts that the rule {@code registration-check} needs: is the order known, how many
 * days passed from delivery to the return, are all items part of the order, is the reason known, what is the value.
 *
 * <p>Spring SDK basics: {@code @JobWorker(type = ...)} registers the method for the job type of the BPMN service task.
 * The returned map becomes process variables when the job completes; a thrown error is reported as a failed job.
 */
@Component
public class CreateReturnCaseWorker {

    private final ReturnsApi api;
    private final WorkflowErrors errors;

    public CreateReturnCaseWorker(ReturnsApi api, WorkflowErrors errors) {
        this.api = api;
        this.errors = errors;
    }

    @JobWorker(type = "create-return-case")
    public Map<String, Object> handle(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        try {
            String returnId = Vars.text(vars, "returnId");
            ReturnReport report = returnId != null
                    ? api.getReturn(returnId)
                    : api.createReturn(requestFrom(vars), "case-" + job.getProcessInstanceKey());
            return caseData(report, api.findOrder(report.orderId()));
        } catch (ReturnsApiException e) {
            throw errors.fromApi(e);
        } catch (IllegalArgumentException e) {
            throw errors.permanent(e.getMessage(), e);
        }
    }

    private static CreateReturnRequest requestFrom(Map<String, Object> vars) {
        List<CreateReturnRequest.Line> lines = Vars.objects(vars, "items").stream()
                .map(i -> new CreateReturnRequest.Line(Vars.required(i, "sku"), Vars.whole(i.get("quantity"))))
                .toList();
        return new CreateReturnRequest(Vars.required(vars, "orderId"), Vars.required(vars, "customerEmail"), lines,
                Vars.text(vars, "reason"), Vars.text(vars, "reasonText"));
    }

    private static Map<String, Object> caseData(ReturnReport report, Optional<Order> order) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("returnId", report.returnId());
        out.put("orderId", report.orderId());
        out.put("customerEmail", report.customer().email());
        out.put("customerId", report.customer().customerId());
        out.put("marketplace", report.marketplace());
        out.put("items", report.items().stream()
                .map(i -> Map.<String, Object>of("sku", i.sku(), "quantity", i.quantity())).toList());
        out.put("reason", report.reason());
        out.put("reasonText", report.reasonText());
        out.put("orderFound", order.isPresent());
        // days from delivery to the return (whole days); no order: unknown (the rule sends the case to a person)
        out.put("daysSinceDelivery", order.map(o -> Duration.between(o.deliveredAt(), report.requestedAt()).toDays())
                .orElse(null));
        out.put("itemsInOrder", order.isPresent() && report.items().stream().allMatch(
                i -> order.get().items().stream().anyMatch(o -> o.sku().equals(i.sku()))));
        out.put("dataComplete", report.reason() != null);
        out.put("returnValue", value(report.items()));
        return out;
    }

    /** Sum of price times quantity, rounded to cents. */
    private static double value(List<Item> items) {
        return items.stream().map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
