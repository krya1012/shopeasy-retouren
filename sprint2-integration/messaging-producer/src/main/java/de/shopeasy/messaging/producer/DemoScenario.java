package de.shopeasy.messaging.producer;

import static de.shopeasy.messaging.producer.ReturnStatus.*;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Plays the scenarios for the demo. In the consumer's log you can see what happens with each one.
 *
 * <p>Spring Boot note: this is a {@code @Component}; {@link ProducerApplication} asks Spring for this bean and
 * calls {@link #run()}. The {@link StatusEventPublisher} is injected through the constructor.
 */
@Component
public class DemoScenario {

    private static final Logger LOG = LoggerFactory.getLogger(DemoScenario.class);

    private final StatusEventPublisher publisher;

    public DemoScenario(StatusEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * The scenario for the self-service portal. It uses return ids that exist in the REST mock, because the
     * portal consumer applies every event to the mock with a PATCH (an unknown id gives 404). Both returns start
     * in the status NEW in the mock. Start the mock new before running this, otherwise the statuses no longer fit.
     * Start: {@code ./gradlew :messaging-producer:bootRun --args='--scenario=portal'}
     */
    public void runPortal() {
        LOG.info("=== P1. Complete path of the seeded return RET-2026-00457 (ends in CLOSED in the REST mock)");
        String id = "RET-2026-00457";
        ReturnStatus[] path = {NEW, APPROVED, LABEL_SENT, RECEIVED, INSPECTED, ACCEPTED, REFUNDED, CLOSED};
        StatusEvent approved = null;
        for (int i = 0; i < path.length - 1; i++) {
            StatusEvent event = StatusEvent.of(id, path[i], path[i + 1]);
            publisher.publish(event);
            if (event.newStatus() == APPROVED) {
                approved = event;
            }
        }

        LOG.info("=== P2. Late duplicate: the APPROVED event arrives again (same eventId, must be skipped)");
        publisher.publish(approved);

        LOG.info("=== P3. Borderline case on RET-2026-00458: MANUAL_REVIEW, then acceptance");
        String id2 = "RET-2026-00458";
        publisher.publish(StatusEvent.of(id2, NEW, APPROVED));
        publisher.publish(StatusEvent.of(id2, APPROVED, LABEL_SENT));
        publisher.publish(StatusEvent.of(id2, LABEL_SENT, RECEIVED));
        publisher.publish(StatusEvent.of(id2, RECEIVED, INSPECTED));
        publisher.publish(StatusEvent.of(id2, INSPECTED, MANUAL_REVIEW));
        publisher.publish(StatusEvent.of(id2, MANUAL_REVIEW, ACCEPTED));

        LOG.info("=== P4. Unknown return: the REST mock answers 404 (ends in the dead letter queue)");
        publisher.publish(StatusEvent.of("RET-2026-99999", NEW, APPROVED));
    }

    public void run() {
        LOG.info("=== 1. Complete path of a return (seven status changes)");
        String id = "RET-2026-" + (System.currentTimeMillis() % 100000);
        ReturnStatus[] path = {NEW, APPROVED, LABEL_SENT, RECEIVED, INSPECTED, ACCEPTED, REFUNDED, CLOSED};
        StatusEvent approved = null;
        for (int i = 0; i < path.length - 1; i++) {
            StatusEvent event = StatusEvent.of(id, path[i], path[i + 1]);
            publisher.publish(event);
            if (event.newStatus() == APPROVED) {
                approved = event;
            }
        }

        LOG.info("=== 2. Duplicate event: the same event (same eventId) is sent again");
        publisher.publish(approved);

        LOG.info("=== 3. Borderline case: the inspection gives MANUAL_REVIEW, then acceptance");
        String id2 = id + "-B";
        publisher.publish(StatusEvent.of(id2, NEW, APPROVED));
        publisher.publish(StatusEvent.of(id2, APPROVED, LABEL_SENT));
        publisher.publish(StatusEvent.of(id2, LABEL_SENT, RECEIVED));
        publisher.publish(StatusEvent.of(id2, RECEIVED, INSPECTED));
        publisher.publish(StatusEvent.of(id2, INSPECTED, MANUAL_REVIEW));
        publisher.publish(StatusEvent.of(id2, MANUAL_REVIEW, ACCEPTED));

        LOG.info("=== 4. Forbidden status change: NEW -> INSPECTED (ends in the dead letter queue)");
        publisher.publish(StatusEvent.of(id + "-C", NEW, INSPECTED));

        LOG.info("=== 5. Broken message: not JSON (ends in the dead letter queue)");
        publisher.publishRaw("return.status.UNKNOWN", "this is not JSON");

        LOG.info("=== 6. Incomplete message: returnId is missing (ends in the dead letter queue)");
        publisher.publishRaw("return.status.UNKNOWN",
                "{\"eventId\":\"" + UUID.randomUUID() + "\",\"newStatus\":\"APPROVED\"}");
    }
}
