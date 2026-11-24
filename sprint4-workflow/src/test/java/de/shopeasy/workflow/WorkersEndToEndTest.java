package de.shopeasy.workflow;

import static io.camunda.process.test.api.CamundaAssert.assertThatProcessInstance;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import de.shopeasy.grpc.client.ConditionClient;
import de.shopeasy.grpc.server.ArticleConditionServiceImpl;
import de.shopeasy.workflow.support.Fixtures;
import de.shopeasy.workflow.support.InMemoryReturnsApi;
import de.shopeasy.workflow.support.RecordingEventSink;
import io.camunda.client.CamundaClient;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.process.test.api.TestDeployment;
import io.camunda.process.test.api.assertions.IncidentSelectors;
import io.camunda.process.test.api.assertions.ProcessInstanceSelector;
import io.camunda.process.test.api.assertions.ProcessInstanceSelectors;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * The whole process with the REAL workers and the REAL gRPC service of Sprint 2 (started inside the test), in an
 * embedded Camunda engine. Only the return store and the broker are replaced by in-memory versions, so the test needs
 * no Docker services except the one Camunda Process Test starts itself.
 *
 * <p>Compared with {@code ReturnsProcessTest} (mocked workers) this proves that the worker code and the model fit:
 * variable names, job types, error codes, status sequences.
 */
@SpringBootTest(classes = WorkflowApplication.class, properties = {
        "workflow.retry-backoff=PT0S",
        "workflow.poller.interval=PT1S"
})
@CamundaSpringProcessTest
@Import(WorkersEndToEndTest.Fakes.class)
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
class WorkersEndToEndTest {

    private static final ProcessInstanceSelector INSTANCE = ProcessInstanceSelectors.byProcessId("shopeasy-returns");

    /** Replaces the services outside the application: return store, broker, gRPC connection. */
    @TestConfiguration
    static class Fakes {

        @Bean
        @Primary
        InMemoryReturnsApi inMemoryReturnsApi() {
            return new InMemoryReturnsApi();
        }

        @Bean
        @Primary
        RecordingEventSink recordingEventSink() {
            return new RecordingEventSink();
        }

        @Bean(destroyMethod = "close")
        InProcessConditionService inProcessConditionService() throws Exception {
            return new InProcessConditionService();
        }

        @Bean
        @Primary
        ConditionClient inProcessConditionClient(InProcessConditionService service) {
            return new ConditionClient(service.channel, 2000);
        }
    }

    /** The real gRPC service of Sprint 2 in this process (no network). */
    static class InProcessConditionService implements AutoCloseable {
        final Server server;
        final ManagedChannel channel;

        InProcessConditionService() throws Exception {
            String name = InProcessServerBuilder.generateName();
            Clock clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
            server = InProcessServerBuilder.forName(name).addService(new ArticleConditionServiceImpl(clock, 500))
                    .build().start();
            channel = InProcessChannelBuilder.forName(name).build();
        }

        @Override
        public void close() {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    @Autowired
    private CamundaClient client;
    @Autowired
    private CamundaProcessTestContext context;
    @Autowired
    private InMemoryReturnsApi store;
    @Autowired
    private RecordingEventSink events;

    @BeforeEach
    @AfterEach
    void resetFakes() {
        store.reset();
        events.reset();
    }

    // ------------------------------------------------------------------ channels, accepted, rejected

    @Test
    void marketplaceReturnStartedByThePollerRunsToTheEnd() {
        store.add(Fixtures.order("ORD-E1", 6, Fixtures.BLAZER));
        store.add(Fixtures.report("RET-E1", "ORD-E1", "ZALANDO", "NEW", "SIZE_TOO_SMALL", "j.mueller@example.com",
                Fixtures.BLAZER));

        assertThatProcessInstance(INSTANCE).hasActiveElements("gw_evt"); // the poller started it, label is done
        assertThat(store.statusOf("RET-E1")).isEqualTo("LABEL_SENT");

        client.newPublishMessageCommand().messageName("parcel-arrived").correlationKey("RET-E1").send().join();
        context.completeUserTask("t_gr", Map.of("inspections", List.of(Map.of("sku", "SH-BLZ-042-M", "quantity", 1,
                "condition", "AS_NEW", "tagsPresent", true, "originalPackaging", true, "inspectorNote", ""))));
        context.completeUserTask("t_stock", Map.of("erpDocumentNumber", "4900001234"));

        assertThatProcessInstance(INSTANCE).isCompleted().hasCompletedElements("t_gr_rec", "t_decide", "t_refund",
                "t_end_notify");
        assertThat(store.statusOf("RET-E1")).isEqualTo("CLOSED");
        assertThat(events.summary()).containsExactly(
                "RET-E1 NEW->APPROVED", "RET-E1 APPROVED->LABEL_SENT", "RET-E1 LABEL_SENT->RECEIVED",
                "RET-E1 RECEIVED->INSPECTED", "RET-E1 INSPECTED->ACCEPTED", "RET-E1 ACCEPTED->REFUNDED",
                "RET-E1 REFUNDED->CLOSED");
    }

    @Test
    void webshopReturnAfterThePeriodIsRejected() {
        store.add(Fixtures.order("ORD-OLD", 40, Fixtures.BLAZER));

        publishWebshopReturn("ORD-OLD", "kunde@example.com", "CHANGED_MIND");

        assertThatProcessInstance(INSTANCE).isCompleted().hasCompletedElements("t_rule", "t_deny", "end_deny");
        String returnId = store.returnIds().iterator().next();
        assertThat(store.statusOf(returnId)).isEqualTo("CLOSED");
        assertThat(events.summary()).containsExactly(returnId + " NEW->REJECTED", returnId + " REJECTED->CLOSED");
    }

    @Test
    void assessmentOutageSendsTheCaseToAPerson() {
        store.add(Fixtures.order("ORD-1", 5, Fixtures.BLAZER));

        publishWebshopReturn("ORD-1", "ai-down@example.com", "SIZE_TOO_SMALL");
        assertThatProcessInstance(INSTANCE).hasCompletedElements("t_ai", "t_rule", "t_inrev").hasActiveElements("t_man1");
        context.completeUserTask("t_man1", Map.of("rejectRegistration", true, "reviewNote", "No assessment, reject"));

        assertThatProcessInstance(INSTANCE).isCompleted().hasCompletedElements("t_man1", "t_deny2");
        String returnId = store.returnIds().iterator().next();
        assertThat(events.summary()).containsExactly(returnId + " NEW->IN_REVIEW", returnId + " IN_REVIEW->REJECTED",
                returnId + " REJECTED->CLOSED");
    }

    // ------------------------------------------------------------------ errors

    @Test
    void returnStoreDownCreatesAnIncidentAndTheCaseContinuesAfterTheRepair() {
        store.add(Fixtures.order("ORD-1", 5, Fixtures.BLAZER));
        store.setDown(true);

        publishWebshopReturn("ORD-1", "kunde@example.com", "SIZE_TOO_SMALL");
        assertThatProcessInstance(INSTANCE).hasActiveIncidents().hasActiveElements("t_create");

        store.setDown(false); // an operator starts the service again and presses "Retry" in Operate
        context.resolveIncident(IncidentSelectors.byElementId("t_create"));

        assertThatProcessInstance(INSTANCE).hasActiveElements("gw_evt").hasNoActiveIncidents();
        String returnId = store.returnIds().iterator().next();
        assertThat(store.statusOf(returnId)).isEqualTo("LABEL_SENT");
        assertThat(store.returnIds()).hasSize(1); // the repeated job did not create a second return
    }

    @Test
    void shippingApiDownLeadsToAManualLabel() {
        store.add(Fixtures.order("ORD-1", 5, Fixtures.BLAZER));

        publishWebshopReturn("ORD-1", "label-down@example.com", "SIZE_TOO_SMALL");
        assertThatProcessInstance(INSTANCE).hasCompletedElements("b_label").hasActiveElements("t_label_man");
        context.completeUserTask("t_label_man", Map.of("labelUrl", "https://labels.example.com/manual.pdf"));

        assertThatProcessInstance(INSTANCE).hasCompletedElements("t_label_man", "t_notify").hasActiveElements("gw_evt");
    }

    @Test
    void brokerDownCreatesAnIncidentAtTheFirstStatusStep() {
        store.add(Fixtures.order("ORD-1", 5, Fixtures.BLAZER));
        events.setDown(true);

        publishWebshopReturn("ORD-1", "kunde@example.com", "SIZE_TOO_SMALL");
        assertThatProcessInstance(INSTANCE).hasActiveIncidents().hasActiveElements("t_notify");
        String returnId = store.returnIds().iterator().next();
        assertThat(store.statusOf(returnId)).isEqualTo("NEW"); // the event goes first: nothing changed

        events.setDown(false);
        context.resolveIncident(IncidentSelectors.byElementId("t_notify"));

        assertThatProcessInstance(INSTANCE).hasActiveElements("gw_evt");
        assertThat(store.statusOf(returnId)).isEqualTo("LABEL_SENT");
        assertThat(events.summary()).containsExactly(returnId + " NEW->APPROVED", returnId + " APPROVED->LABEL_SENT");
    }

    // ------------------------------------------------------------------ poller

    @Test
    void pollerStartsOnlyOneInstanceForTheSameReturn() throws Exception {
        // The broker is down, so the instance stops with an incident before the first status change: the return
        // stays NEW and the poller finds it again at every poll (every second). Zeebe must ignore these messages.
        events.setDown(true);
        store.add(Fixtures.order("ORD-E7", 6, Fixtures.BLAZER));
        store.add(Fixtures.report("RET-E7", "ORD-E7", "AMAZON", "NEW", "DEFECTIVE", "kunde@example.com",
                Fixtures.BLAZER));

        assertThatProcessInstance(INSTANCE).hasActiveIncidents().hasActiveElements("t_notify");
        Thread.sleep(3500);

        assertThat(store.statusOf("RET-E7")).isEqualTo("NEW");
        long count = client.newProcessInstanceSearchRequest().filter(f -> f.processDefinitionId("shopeasy-returns"))
                .send().join().items().size();
        assertThat(count).isEqualTo(1);
    }

    // ------------------------------------------------------------------ helpers

    private void publishWebshopReturn(String orderId, String email, String reason) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("orderId", orderId);
        vars.put("customerEmail", email);
        vars.put("items", List.of(Map.of("sku", "SH-BLZ-042-M", "quantity", 1)));
        vars.put("reason", reason);
        client.newPublishMessageCommand().messageName("webshop-return-registered").withoutCorrelationKey()
                .variables(vars).send().join();
    }
}
