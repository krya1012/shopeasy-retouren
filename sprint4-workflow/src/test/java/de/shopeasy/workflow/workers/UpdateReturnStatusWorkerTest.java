package de.shopeasy.workflow.workers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.shopeasy.workflow.status.StatusService;
import de.shopeasy.workflow.support.Fixtures;
import de.shopeasy.workflow.support.InMemoryReturnsApi;
import de.shopeasy.workflow.support.RecordingEventSink;
import io.camunda.client.exception.JobError;

/** Tests how the status worker reports its problems to the engine. */
class UpdateReturnStatusWorkerTest {

    private InMemoryReturnsApi api;
    private RecordingEventSink sink;
    private UpdateReturnStatusWorker worker;

    @BeforeEach
    void setUp() {
        api = new InMemoryReturnsApi();
        sink = new RecordingEventSink();
        worker = new UpdateReturnStatusWorker(new StatusService(api, sink, Clock.systemUTC()), Fixtures.errors());
        api.add(Fixtures.report("RET-1", "ORD-1", null, "NEW", "SIZE_TOO_SMALL", "a@example.com", Fixtures.BLAZER));
    }

    private static Map<String, Object> vars(Object statuses) {
        return Map.of("returnId", "RET-1", "newStatuses", statuses);
    }

    @Test
    void setsTheStatusesAndPublishesTheEvents() {
        worker.handle(Fixtures.job(vars(List.of("APPROVED", "LABEL_SENT")), 3));

        assertThat(api.statusOf("RET-1")).isEqualTo("LABEL_SENT");
        assertThat(sink.summary()).containsExactly("RET-1 NEW->APPROVED", "RET-1 APPROVED->LABEL_SENT");
    }

    @Test
    void forbiddenChangeIsAPermanentError() {
        assertThatThrownBy(() -> worker.handle(Fixtures.job(vars(List.of("RECEIVED")), 3)))
                .isInstanceOfSatisfying(JobError.class, e -> {
                    assertThat(e.getRetries()).isEqualTo(0);
                    assertThat(e.getErrorMessage()).contains("not allowed");
                });
    }

    @Test
    void missingStatusListIsAPermanentError() {
        assertThatThrownBy(() -> worker.handle(Fixtures.job(Map.of("returnId", "RET-1"), 3)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isEqualTo(0));
    }

    @Test
    void returnStoreDownIsATemporaryError() {
        api.setDown(true);

        assertThatThrownBy(() -> worker.handle(Fixtures.job(vars(List.of("APPROVED")), 3)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isNull());
    }

    @Test
    void brokerDownIsATemporaryErrorAndTheRepeatWorks() {
        sink.setDown(true);
        assertThatThrownBy(() -> worker.handle(Fixtures.job(vars(List.of("REJECTED")), 3)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isNull());

        sink.setDown(false);
        worker.handle(Fixtures.job(vars(List.of("REJECTED")), 2));

        assertThat(api.statusChanges).containsExactly("RET-1:REJECTED"); // changed only once
        assertThat(sink.events).hasSize(1);
    }
}
