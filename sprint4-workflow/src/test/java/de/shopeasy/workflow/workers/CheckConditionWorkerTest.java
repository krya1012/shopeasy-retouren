package de.shopeasy.workflow.workers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.shopeasy.grpc.client.ConditionClient;
import de.shopeasy.grpc.server.ArticleConditionServiceImpl;
import de.shopeasy.workflow.status.StatusService;
import de.shopeasy.workflow.support.Fixtures;
import de.shopeasy.workflow.support.InMemoryReturnsApi;
import de.shopeasy.workflow.support.RecordingEventSink;
import io.camunda.client.exception.JobError;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * Tests the worker against the REAL gRPC service of Sprint 2, started inside this process (no network), like the tests
 * of the service itself. The return store and the broker are replaced by the in-memory versions.
 */
class CheckConditionWorkerTest {

    private Server server;
    private ManagedChannel channel;
    private InMemoryReturnsApi api;
    private RecordingEventSink sink;
    private CheckConditionWorker worker;

    @BeforeEach
    void setUp() throws Exception {
        String name = InProcessServerBuilder.generateName();
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
        server = InProcessServerBuilder.forName(name).addService(new ArticleConditionServiceImpl(clock, 500))
                .build().start();
        channel = InProcessChannelBuilder.forName(name).build();
        api = new InMemoryReturnsApi();
        sink = new RecordingEventSink();
        api.add(Fixtures.report("RET-1", "ORD-1", null, "RECEIVED", "SIZE_TOO_SMALL", "a@example.com", Fixtures.BLAZER));
        worker = new CheckConditionWorker(new ConditionClient(channel, 500),
                new StatusService(api, sink, clock), Fixtures.errors(), clock);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    private static Map<String, Object> inspection(String sku, String condition, boolean tags, boolean packaging) {
        return Map.of("sku", sku, "quantity", 1, "condition", condition, "tagsPresent", tags,
                "originalPackaging", packaging, "inspectorNote", "");
    }

    private static Map<String, Object> vars(Map<String, Object>... inspections) {
        return Map.of("returnId", "RET-1", "inspections", List.of(inspections));
    }

    @SafeVarargs
    private Map<String, Object> run(Map<String, Object>... inspections) {
        return worker.handle(Fixtures.job(vars(inspections), 3));
    }

    @Test
    void itemAsNewIsAcceptedAndTheStatusBecomesInspected() {
        Map<String, Object> out = run(inspection("SH-A", "AS_NEW", true, true));

        assertThat(out).containsEntry("conditionDecision", "ACCEPT").containsEntry("minConfidence", 0.95);
        assertThat(api.statusOf("RET-1")).isEqualTo("INSPECTED");
        assertThat(sink.summary()).containsExactly("RET-1 RECEIVED->INSPECTED");
    }

    @Test
    void worstItemDecidesAndTheLowestScoreIsKept() {
        Map<String, Object> out = run(inspection("SH-A", "AS_NEW", true, true), inspection("SH-B", "MINOR_WEAR", true, true));
        assertThat(out).containsEntry("conditionDecision", "MANUAL_REVIEW").containsEntry("minConfidence", 0.55);
        assertThat((List<?>) out.get("conditionResults")).hasSize(2);
    }

    @Test
    void rejectedItemMakesTheWholeReturnRejected() {
        Map<String, Object> out = run(inspection("SH-A", "AS_NEW", true, true), inspection("SH-B", "MINOR_WEAR", true, true),
                inspection("SH-C", "DAMAGED", true, true));

        assertThat(out).containsEntry("conditionDecision", "REJECT").containsEntry("minConfidence", 0.55);
    }

    @Test
    void repeatedJobGetsTheStoredResultsAndChangesNothingTwice() {
        Map<String, Object> first = run(inspection("SH-A", "AS_NEW", true, false));
        // the answer got lost: the engine gives the same job again. The service answers ALREADY_EXISTS for the check
        Map<String, Object> second = run(inspection("SH-A", "AS_NEW", true, false));

        assertThat(second).isEqualTo(first);
        assertThat(second).containsEntry("minConfidence", 0.85);
        assertThat(api.statusChanges).containsExactly("RET-1:INSPECTED");
        assertThat(sink.events).hasSize(1);
    }

    @Test
    void conditionTheServiceRejectsIsAPermanentError() {
        assertThatThrownBy(() -> run(inspection("SH-A", "OBSERVED_CONDITION_UNSPECIFIED", true, true)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isEqualTo(0));
        assertThat(api.statusOf("RET-1")).isEqualTo("RECEIVED");
    }

    @Test
    void unknownConditionTextIsAPermanentError() {
        assertThatThrownBy(() -> run(inspection("SH-A", "SHINY", true, true)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isEqualTo(0));
    }

    @Test
    void missingInspectionsIsAPermanentError() {
        assertThatThrownBy(() -> worker.handle(Fixtures.job(Map.of("returnId", "RET-1"), 3)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isEqualTo(0));
    }

    @Test
    void serviceNotReachableIsATemporaryErrorAndNothingChanges() {
        server.shutdownNow(); // the gRPC service is down

        assertThatThrownBy(() -> run(inspection("SH-A", "AS_NEW", true, true)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isNull());
        assertThat(api.statusOf("RET-1")).isEqualTo("RECEIVED");
        assertThat(sink.events).isEmpty();
    }

    @Test
    void returnStoreDownAfterTheCheckIsATemporaryErrorAndTheRepeatWorks() {
        api.setDown(true);
        assertThatThrownBy(() -> run(inspection("SH-A", "AS_NEW", true, true)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isNull());

        api.setDown(false);
        Map<String, Object> out = run(inspection("SH-A", "AS_NEW", true, true)); // check exists: ALREADY_EXISTS

        assertThat(out).containsEntry("conditionDecision", "ACCEPT");
        assertThat(api.statusOf("RET-1")).isEqualTo("INSPECTED");
    }
}
