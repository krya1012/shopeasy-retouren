package de.shopeasy.workflow.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.shopeasy.workflow.returns.ReturnsApiException;
import de.shopeasy.workflow.support.Fixtures;
import de.shopeasy.workflow.support.InMemoryReturnsApi;
import de.shopeasy.workflow.support.RecordingEventSink;

/** Tests the status changes including the cases where the engine repeats a job. */
class StatusServiceTest {

    private InMemoryReturnsApi api;
    private RecordingEventSink sink;
    private StatusService service;

    @BeforeEach
    void setUp() {
        api = new InMemoryReturnsApi();
        sink = new RecordingEventSink();
        service = new StatusService(api, sink, Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC));
    }

    private void givenReturn(String status) {
        api.add(Fixtures.report("RET-1", "ORD-1", null, status, "SIZE_TOO_SMALL", "a@example.com", Fixtures.BLAZER));
    }

    @Test
    void setsEveryStatusInOrderAndPublishesOneEventForEach() {
        givenReturn("NEW");

        service.advance("RET-1", List.of("APPROVED", "LABEL_SENT"));

        assertThat(api.statusOf("RET-1")).isEqualTo("LABEL_SENT");
        assertThat(api.statusChanges).containsExactly("RET-1:APPROVED", "RET-1:LABEL_SENT");
        assertThat(sink.summary()).containsExactly("RET-1 NEW->APPROVED", "RET-1 APPROVED->LABEL_SENT");
        StatusEvent event = sink.events.get(1);
        assertThat(event.routingKey()).isEqualTo("return.status.LABEL_SENT");
        assertThat(event.eventType()).isEqualTo("ReturnStatusChanged");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-11-24T10:00:00Z"));
    }

    @Test
    void theEventIdOnlyDependsOnReturnAndNewStatus() {
        givenReturn("NEW");
        service.advance("RET-1", List.of("APPROVED"));
        StatusEvent first = sink.events.get(0);

        assertThat(first.eventId()).isEqualTo(StatusEvent.of("RET-1", ReturnStatus.NEW, ReturnStatus.APPROVED,
                Instant.now()).eventId());
        assertThat(first.eventId()).isNotEqualTo(StatusEvent.of("RET-2", ReturnStatus.NEW, ReturnStatus.APPROVED,
                Instant.now()).eventId());
    }

    @Test
    void jobRepeatedAfterFullSuccessDoesNothing() {
        givenReturn("NEW");
        service.advance("RET-1", List.of("APPROVED", "LABEL_SENT"));

        service.advance("RET-1", List.of("APPROVED", "LABEL_SENT")); // the engine repeats the job

        assertThat(api.statusChanges).hasSize(2);
        assertThat(sink.events).hasSize(2);
    }

    @Test
    void jobRepeatedAfterPartialSuccessContinuesWhereItStopped() {
        givenReturn("APPROVED"); // first run set APPROVED, then failed

        service.advance("RET-1", List.of("APPROVED", "LABEL_SENT"));

        assertThat(api.statusChanges).containsExactly("RET-1:LABEL_SENT");
        assertThat(sink.summary()).containsExactly("RET-1 APPROVED->LABEL_SENT");
    }

    @Test
    void brokerOutageChangesNothingAndTheRepeatWorks() {
        givenReturn("LABEL_SENT");
        sink.setDown(true);
        assertThatThrownBy(() -> service.advance("RET-1", List.of("RECEIVED")))
                .isInstanceOf(EventNotDeliveredException.class);
        assertThat(api.statusOf("RET-1")).isEqualTo("LABEL_SENT"); // the event goes first: nothing has changed

        sink.setDown(false);
        service.advance("RET-1", List.of("RECEIVED")); // the engine repeats the job

        assertThat(sink.summary()).containsExactly("RET-1 LABEL_SENT->RECEIVED");
        assertThat(api.statusChanges).containsExactly("RET-1:RECEIVED");
    }

    @Test
    void returnStoreFailingAfterTheEventSendsTheSameEventAgainOnTheRepeat() {
        givenReturn("NEW");
        sink.afterPublish = () -> api.setDown(true); // the store fails right after the broker confirmed
        assertThatThrownBy(() -> service.advance("RET-1", List.of("REJECTED")))
                .isInstanceOfSatisfying(ReturnsApiException.class, e -> assertThat(e.isPermanent()).isFalse());
        assertThat(api.statusChanges).isEmpty();

        sink.afterPublish = null;
        api.setDown(false);
        service.advance("RET-1", List.of("REJECTED")); // the engine repeats the job

        assertThat(api.statusOf("RET-1")).isEqualTo("REJECTED");
        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(1).eventId()).isEqualTo(sink.events.get(0).eventId()); // receiver ignores one
    }

    @Test
    void forbiddenChangeSendsNoEvent() {
        givenReturn("NEW");

        assertThatThrownBy(() -> service.advance("RET-1", List.of("RECEIVED")))
                .isInstanceOfSatisfying(ReturnsApiException.class, e -> {
                    assertThat(e.isPermanent()).isTrue();
                    assertThat(e.httpStatus()).isEqualTo(409);
                });
        assertThat(sink.events).isEmpty();
    }

    @Test
    void returnChangedByAnotherPartyIsAPermanentError() {
        givenReturn("REJECTED");

        assertThatThrownBy(() -> service.advance("RET-1", List.of("APPROVED")))
                .isInstanceOfSatisfying(ReturnsApiException.class, e -> {
                    assertThat(e.isPermanent()).isTrue();
                    assertThat(e.httpStatus()).isEqualTo(409);
                });
        assertThat(sink.events).isEmpty();
    }

    @Test
    void unknownReturnIsAPermanentError() {
        assertThatThrownBy(() -> service.advance("RET-NONE", List.of("APPROVED")))
                .isInstanceOfSatisfying(ReturnsApiException.class, e -> assertThat(e.httpStatus()).isEqualTo(404));
    }

    @Test
    void returnStoreDownIsATemporaryError() {
        givenReturn("NEW");
        api.setDown(true);

        assertThatThrownBy(() -> service.advance("RET-1", List.of("APPROVED")))
                .isInstanceOfSatisfying(ReturnsApiException.class, e -> assertThat(e.isPermanent()).isFalse());
    }

    @Test
    void unknownStatusInTheModelIsRejected() {
        givenReturn("NEW");

        assertThatThrownBy(() -> service.advance("RET-1", List.of("FLYING")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(api.statusChanges).isEmpty();
    }
}
