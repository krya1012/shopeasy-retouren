package de.shopeasy.workflow;

import static io.camunda.process.test.api.CamundaAssert.assertThatDecision;
import static io.camunda.process.test.api.CamundaAssert.assertThatProcessInstance;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.EvaluateDecisionResponse;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.process.test.api.TestDeployment;
import io.camunda.process.test.api.assertions.DecisionSelectors;
import io.camunda.process.test.api.assertions.IncidentSelectors;
import io.camunda.process.test.api.assertions.ProcessInstanceSelector;
import io.camunda.process.test.api.assertions.ProcessInstanceSelectors;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Process tests for the executable returns process (Sprint 4). The real DMN decisions and the real BPMN model run
 * in an embedded Camunda engine (Camunda Process Test). The job workers are replaced by mocks, so no Sprint 2
 * service is needed; what the tests prove is the routing: gateways, rules, user tasks, error handling.
 *
 * <p>Each test is one demo case of the task: marketplace/e-mail entry, accepted, rejected, manual review, a service
 * that is not reachable.
 */
@SpringBootTest(classes = WorkflowApplication.class)
@CamundaSpringProcessTest
@TestDeployment(resources = {
        "processes/returns-process.bpmn",
        "processes/registration-check.dmn",
        "processes/acceptance-decision.dmn",
        "processes/forms/book-stock-erp.form",
        "processes/forms/capture-email-return.form",
        "processes/forms/create-label-manually.form",
        "processes/forms/decide-disposition.form",
        "processes/forms/inspect-condition.form",
        "processes/forms/resolve-refund.form",
        "processes/forms/review-borderline.form",
        "processes/forms/review-registration.form"
})
class ReturnsProcessTest {

    private static final String PROCESS_ID = "shopeasy-returns";
    private static final String RETURN_ID = "RET-2026-00457";
    private static final ProcessInstanceSelector INSTANCE = ProcessInstanceSelectors.byProcessId(PROCESS_ID);

    @Autowired
    private CamundaClient client;

    @Autowired
    private CamundaProcessTestContext context;

    /** Every call of the status worker (the list "newStatuses" of the job), in order. */
    private final List<List<String>> statusCalls = new CopyOnWriteArrayList<>();

    @BeforeEach
    void mockStatusAndAssessmentWorkers() {
        statusCalls.clear();
        context.mockJobWorker("update-return-status").withHandler((jobClient, job) -> {
            @SuppressWarnings("unchecked")
            final List<String> statuses = (List<String>) job.getVariablesAsMap().get("newStatuses");
            statusCalls.add(statuses);
            jobClient.newCompleteCommand(job).send().join();
        });
        context.mockJobWorker("assess-return").thenComplete(Map.of("reasonCategory", "SIZE", "fraudScore", 0.1));
    }

    // ------------------------------------------------------------------ the demo cases

    @Test
    void marketplaceReturnIsAcceptedAndRefunded() {
        mockCase(caseData(6, true, true, 89.9));
        context.mockJobWorker("create-return-label").thenComplete(Map.of("labelUrl", "https://labels.example/1.pdf"));
        mockCondition("ACCEPT", 0.95);
        context.mockJobWorker("issue-refund").thenComplete(Map.of("refundMethod", "PSP", "refundReference", "R-1"));

        startMarketplaceReturn();
        assertThatProcessInstance(INSTANCE).hasCompletedElements("t_create", "t_ai", "t_rule", "t_label", "t_notify")
                .hasActiveElements("gw_evt");

        arriveAndInspect("AS_NEW");
        assertThatProcessInstance(INSTANCE).hasCompletedElements("t_gr_rec", "t_decide");

        // stock booking (human, ERP page) and refund (service) run in parallel; both must finish
        context.completeUserTask("t_stock", Map.of("erpDocumentNumber", "4900001234"));
        assertThatProcessInstance(INSTANCE).isCompleted()
                .hasCompletedElements("t_refund", "t_stock", "t_end_notify", "end")
                .hasNotActivatedElements("t_man2", "t_reject", "t_deny");
        assertThat(statusCalls).containsExactly(
                List.of("APPROVED", "LABEL_SENT"),
                List.of("RECEIVED"),
                List.of("ACCEPTED", "REFUNDED", "CLOSED"));
    }

    @Test
    void returnAfterThePeriodIsRejectedAtRegistration() {
        mockCase(caseData(40, true, true, 89.9));

        startMarketplaceReturn();

        assertThatProcessInstance(INSTANCE).isCompleted()
                .hasCompletedElements("t_rule", "t_deny", "end_deny")
                .hasNotActivatedElements("t_label", "t_man1");
        assertThat(statusCalls).containsExactly(List.of("REJECTED", "CLOSED"));
    }

    @Test
    void unclearEmailReturnGoesToManualReviewTwiceAndIsAccepted() {
        // the e-mail does not state a reason: the case data is incomplete, the rule says UNCLEAR
        mockCase(caseData(6, true, false, 89.9));
        context.mockJobWorker("create-return-label").thenComplete(Map.of("labelUrl", "https://labels.example/2.pdf"));
        mockCondition("MANUAL_REVIEW", 0.55);
        context.mockJobWorker("issue-refund").thenComplete(Map.of("refundMethod", "PSP", "refundReference", "R-2"));

        publishStart("email-return-received", Map.of(
                "emailFrom", "kunde@example.com", "emailSubject", "Retoure", "emailBody", "Bitte Jacke zurücknehmen"));
        context.completeUserTask("t_parse", Map.of(
                "orderId", "ORD-2026-118932", "customerEmail", "kunde@example.com",
                "items", List.of(Map.of("sku", "SH-BLZ-042-M", "quantity", 1))));
        assertThatProcessInstance(INSTANCE).hasCompletedElements("t_parse", "t_create", "t_rule", "t_inrev");

        context.completeUserTask("t_man1", Map.of("rejectRegistration", false, "reviewNote", "Customer called, ok"));
        assertThatProcessInstance(INSTANCE).hasCompletedElements("t_man1", "t_label", "t_notify");

        arriveAndInspect("MINOR_WEAR");
        assertThatProcessInstance(INSTANCE).hasCompletedElements("t_decide", "t_mrev").hasActiveElements("t_man2");

        context.completeUserTask("t_man2", Map.of("accepted", true, "justification", "Wear is minimal"));
        context.completeUserTask("t_stock", Map.of("erpDocumentNumber", "4900001235"));
        assertThatProcessInstance(INSTANCE).isCompleted().hasCompletedElements("t_man2", "t_refund", "t_end_notify");
        assertThat(statusCalls).containsExactly(
                List.of("IN_REVIEW"),
                List.of("APPROVED", "LABEL_SENT"),
                List.of("RECEIVED"),
                List.of("MANUAL_REVIEW"),
                List.of("ACCEPTED", "REFUNDED", "CLOSED"));
    }

    @Test
    void clerkRejectsAnUnclearRegistration() {
        mockCase(caseData(6, false, true, 89.9));

        startMarketplaceReturn();
        context.completeUserTask("t_man1", Map.of("rejectRegistration", true, "reviewNote", "Order not found"));

        assertThatProcessInstance(INSTANCE).isCompleted().hasCompletedElements("t_inrev", "t_man1", "t_deny2", "end_deny2")
                .hasNotActivatedElements("t_label");
        assertThat(statusCalls).containsExactly(List.of("IN_REVIEW"), List.of("REJECTED", "CLOSED"));
    }

    @Test
    void returnIsRejectedAfterInspection() {
        mockCase(caseData(6, true, true, 89.9));
        context.mockJobWorker("create-return-label").thenComplete(Map.of("labelUrl", "https://labels.example/3.pdf"));
        mockCondition("REJECT", 0.95);

        startMarketplaceReturn();
        arriveAndInspect("DAMAGED");
        context.completeUserTask("t_reject", Map.of("disposition", "DONATE"));

        assertThatProcessInstance(INSTANCE).isCompleted()
                .hasCompletedElements("t_decide", "t_reject", "t_end_notify")
                .hasNotActivatedElements("t_stock", "t_refund", "t_man2");
        assertThat(statusCalls).containsExactly(
                List.of("APPROVED", "LABEL_SENT"), List.of("RECEIVED"), List.of("REJECTED", "CLOSED"));
    }

    // ------------------------------------------------------------------ error handling

    @Test
    void labelServiceDownLeadsToManualLabel() {
        mockCase(caseData(6, true, true, 89.9));
        context.mockJobWorker("create-return-label").thenThrowBpmnError("LABEL_SERVICE_UNAVAILABLE");

        startMarketplaceReturn();
        context.completeUserTask("t_label_man", Map.of("labelUrl", "https://labels.example/manual.pdf"));

        assertThatProcessInstance(INSTANCE).hasCompletedElements("b_label", "t_label_man", "t_notify")
                .hasActiveElements("gw_evt");
    }

    @Test
    void failedRefundLeadsToManualClarification() {
        mockCase(caseData(6, true, true, 89.9));
        context.mockJobWorker("create-return-label").thenComplete(Map.of("labelUrl", "https://labels.example/4.pdf"));
        mockCondition("ACCEPT", 0.95);
        context.mockJobWorker("issue-refund").thenThrowBpmnError("REFUND_FAILED");

        startMarketplaceReturn();
        arriveAndInspect("AS_NEW");
        context.completeUserTask("t_refund_man", Map.of("refundReference", "MANUAL-7"));
        context.completeUserTask("t_stock", Map.of("erpDocumentNumber", "4900001236"));

        assertThatProcessInstance(INSTANCE).isCompleted()
                .hasCompletedElements("b_refund", "t_refund_man", "t_stock", "t_end_notify");
        assertThat(statusCalls.get(statusCalls.size() - 1)).containsExactly("ACCEPTED", "REFUNDED", "CLOSED");
    }

    @Test
    void parcelThatNeverArrivesClosesTheCase() {
        mockCase(caseData(6, true, true, 89.9));
        context.mockJobWorker("create-return-label").thenComplete(Map.of("labelUrl", "https://labels.example/5.pdf"));

        startMarketplaceReturn();
        assertThatProcessInstance(INSTANCE).hasActiveElements("gw_evt");
        context.increaseTime(Duration.ofDays(22));

        assertThatProcessInstance(INSTANCE).isCompleted()
                .hasCompletedElements("ev_timeout", "t_expired", "end_expired")
                .hasNotActivatedElements("t_notify_in", "t_gr");
        assertThat(statusCalls).containsExactly(List.of("APPROVED", "LABEL_SENT"), List.of("EXPIRED", "CLOSED"));
    }

    @Test
    void conditionServiceDownCreatesIncidentAndContinuesAfterRetry() {
        mockCase(caseData(6, true, true, 89.9));
        context.mockJobWorker("create-return-label").thenComplete(Map.of("labelUrl", "https://labels.example/6.pdf"));
        context.mockJobWorker("issue-refund").thenComplete(Map.of("refundMethod", "PSP", "refundReference", "R-6"));
        final AtomicBoolean serviceUp = new AtomicBoolean(false);
        context.mockJobWorker("check-condition").withHandler((jobClient, job) -> {
            if (serviceUp.get()) {
                jobClient.newCompleteCommand(job)
                        .variables(Map.of("conditionDecision", "ACCEPT", "minConfidence", 0.95)).send().join();
            } else {
                // what the real worker does after its retries: no retries left, an incident appears in Operate
                jobClient.newFailCommand(job).retries(0).errorMessage("gRPC condition service unavailable")
                        .send().join();
            }
        });

        startMarketplaceReturn();
        arriveAndInspect(null);
        assertThatProcessInstance(INSTANCE).hasActiveIncidents().hasActiveElements("t_gr_rec");

        // an operator starts the service again and presses "Retry" in Operate
        serviceUp.set(true);
        context.resolveIncident(IncidentSelectors.byElementId("t_gr_rec"));
        assertThatProcessInstance(INSTANCE).hasCompletedElements("t_gr_rec", "t_decide").hasNoActiveIncidents();
    }

    // ------------------------------------------------------------------ rules (DMN), table-driven

    static Stream<Arguments> registrationChecks() {
        return Stream.of(
                Arguments.of("valid registration", Map.of("orderFound", true, "daysSinceDelivery", 6, "dataComplete", true,
                        "itemsInOrder", true, "fraudScore", 0.1), "ELIGIBLE"),
                Arguments.of("14 days is still in the period", Map.of("orderFound", true, "daysSinceDelivery", 14,
                        "dataComplete", true, "itemsInOrder", true, "fraudScore", 0.1), "ELIGIBLE"),
                Arguments.of("15 days is too late", Map.of("orderFound", true, "daysSinceDelivery", 15,
                        "dataComplete", true, "itemsInOrder", true, "fraudScore", 0.1), "NOT_ELIGIBLE"),
                Arguments.of("order not found", Map.of("orderFound", false), "UNCLEAR"),
                Arguments.of("reason missing", Map.of("orderFound", true, "daysSinceDelivery", 6, "dataComplete", false,
                        "itemsInOrder", true, "fraudScore", 0.1), "UNCLEAR"),
                Arguments.of("item not in order", Map.of("orderFound", true, "daysSinceDelivery", 6, "dataComplete", true,
                        "itemsInOrder", false, "fraudScore", 0.1), "UNCLEAR"),
                Arguments.of("high fraud score", Map.of("orderFound", true, "daysSinceDelivery", 6, "dataComplete", true,
                        "itemsInOrder", true, "fraudScore", 0.7), "UNCLEAR"),
                Arguments.of("AI outage (fraud score missing)", Map.of("orderFound", true, "daysSinceDelivery", 6,
                        "dataComplete", true, "itemsInOrder", true), "UNCLEAR"));
    }

    @ParameterizedTest(name = "registration-check: {0} -> {2}")
    @MethodSource("registrationChecks")
    void registrationCheckDecides(final String name, final Map<String, Object> variables, final String expected) {
        final EvaluateDecisionResponse response = evaluate("registration-check", variables);
        assertThatDecision(DecisionSelectors.byResponse(response)).isEvaluated();
        assertThat(response.getDecisionOutput()).contains("\"result\":\"" + expected + "\"");
    }

    static Stream<Arguments> acceptanceDecisions() {
        return Stream.of(
                Arguments.of("clear accept", Map.of("conditionDecision", "ACCEPT", "minConfidence", 0.95, "fraudScore", 0.1,
                        "returnValue", 90), "ACCEPT"),
                Arguments.of("condition says reject", Map.of("conditionDecision", "REJECT", "minConfidence", 0.95,
                        "fraudScore", 0.1, "returnValue", 90), "REJECT"),
                Arguments.of("condition says manual review", Map.of("conditionDecision", "MANUAL_REVIEW",
                        "minConfidence", 0.55, "fraudScore", 0.1, "returnValue", 90), "BORDERLINE"),
                Arguments.of("accept but high fraud score", Map.of("conditionDecision", "ACCEPT", "minConfidence", 0.95,
                        "fraudScore", 0.8, "returnValue", 90), "BORDERLINE"),
                Arguments.of("accept, high value, low confidence", Map.of("conditionDecision", "ACCEPT",
                        "minConfidence", 0.85, "fraudScore", 0.1, "returnValue", 350), "BORDERLINE"),
                Arguments.of("accept, high value, high confidence", Map.of("conditionDecision", "ACCEPT",
                        "minConfidence", 0.95, "fraudScore", 0.1, "returnValue", 350), "ACCEPT"),
                Arguments.of("no usable condition result", Map.of("fraudScore", 0.1, "returnValue", 90), "BORDERLINE"));
    }

    @ParameterizedTest(name = "acceptance-decision: {0} -> {2}")
    @MethodSource("acceptanceDecisions")
    void acceptanceDecisionDecides(final String name, final Map<String, Object> variables, final String expected) {
        final EvaluateDecisionResponse response = evaluate("acceptance-decision", variables);
        assertThat(response.getDecisionOutput()).contains("\"result\":\"" + expected + "\"");
    }

    // ------------------------------------------------------------------ helpers

    /** Case data as the worker "create-return-case" returns it. */
    private static Map<String, Object> caseData(final int daysSinceDelivery, final boolean orderFound,
            final boolean dataComplete, final double returnValue) {
        final Map<String, Object> data = new HashMap<>();
        data.put("returnId", RETURN_ID);
        data.put("orderId", "ORD-2026-118932");
        data.put("customerEmail", "j.mueller@example.com");
        data.put("items", List.of(Map.of("sku", "SH-BLZ-042-M", "quantity", 1)));
        data.put("reason", "SIZE_TOO_SMALL");
        data.put("orderFound", orderFound);
        data.put("daysSinceDelivery", daysSinceDelivery);
        data.put("itemsInOrder", true);
        data.put("dataComplete", dataComplete);
        data.put("returnValue", returnValue);
        return data;
    }

    private void mockCase(final Map<String, Object> data) {
        context.mockJobWorker("create-return-case").thenComplete(data);
    }

    private void mockCondition(final String decision, final double minConfidence) {
        context.mockJobWorker("check-condition")
                .thenComplete(Map.of("conditionDecision", decision, "minConfidence", minConfidence));
    }

    private void startMarketplaceReturn() {
        publishStart("marketplace-return-received", Map.of("returnId", RETURN_ID));
    }

    private void publishStart(final String messageName, final Map<String, Object> variables) {
        client.newPublishMessageCommand().messageName(messageName).withoutCorrelationKey().variables(variables)
                .send().join();
    }

    /** The warehouse scan arrives (message), then the warehouse records the condition of the item. */
    private void arriveAndInspect(final String condition) {
        assertThatProcessInstance(INSTANCE).hasActiveElements("gw_evt");
        client.newPublishMessageCommand().messageName("parcel-arrived").correlationKey(RETURN_ID).send().join();
        final Map<String, Object> inspection = new HashMap<>();
        inspection.put("sku", "SH-BLZ-042-M");
        inspection.put("quantity", 1);
        inspection.put("condition", condition == null ? "AS_NEW" : condition);
        inspection.put("tagsPresent", true);
        inspection.put("originalPackaging", true);
        context.completeUserTask("t_gr", Map.of("inspections", List.of(inspection)));
    }

    private EvaluateDecisionResponse evaluate(final String decisionId, final Map<String, Object> variables) {
        return client.newEvaluateDecisionCommand().decisionId(decisionId).variables(variables).send().join();
    }
}
