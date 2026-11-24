package de.shopeasy.workflow.workers;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;

/**
 * Job type {@code issue-refund} (step "Issue refund / voucher", {@code t_refund}): a simulated payment provider.
 * Amount = {@code returnValue}; the refund goes back to the payment method ({@code PSP}).
 *
 * <p>Error cases, all ending in the BPMN error {@code REFUND_FAILED} (boundary event {@code b_refund}, then a clerk
 * clarifies in {@code t_refund_man}). The Sprint 3 data shows the causes: PSP timeout 34, interface error 14, IBAN
 * missing 12, card expired 10 of 64 failed refunds.
 * <ul>
 *   <li>{@code no-iban@example.com}, {@code card-expired@example.com}: wrong data, waiting does not help, so the error
 *       comes at once.</li>
 *   <li>{@code psp-timeout@example.com}: the provider does not answer; the worker fails temporarily while retries are
 *       left and throws the BPMN error on the last attempt.</li>
 * </ul>
 */
@Component
public class IssueRefundWorker {

    static final String ERROR_CODE = "REFUND_FAILED";

    private final WorkflowErrors errors;

    public IssueRefundWorker(WorkflowErrors errors) {
        this.errors = errors;
    }

    @JobWorker(type = "issue-refund")
    public Map<String, Object> handle(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String returnId = Vars.required(vars, "returnId");
        switch (Vars.email(vars)) {
            case Simulations.NO_IBAN -> throw errors.bpmnError(ERROR_CODE, "No IBAN on file");
            case Simulations.CARD_EXPIRED -> throw errors.bpmnError(ERROR_CODE, "Card has expired");
            case Simulations.PSP_TIMEOUT -> {
                if (job.getRetries() > 1) {
                    throw errors.temporary("Payment provider does not answer (" + (job.getRetries() - 1)
                            + " attempts left)", null);
                }
                throw errors.bpmnError(ERROR_CODE, "Payment provider does not answer");
            }
            default -> {
                // normal customer: refund done
            }
        }
        Object amount = vars.get("returnValue");
        return Map.of("refundMethod", "PSP", "refundAmount", amount == null ? 0 : amount,
                "refundReference", "REF-" + returnId);
    }
}
