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
