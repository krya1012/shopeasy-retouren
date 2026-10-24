package de.shopeasy.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The logic of the receiver "management": it updates the status of a return.
 * It only works with the bytes of the message, so it can be tested without a broker.
 *
 * <p>Checks, in this order: (1) is the message valid, (2) was this event processed before (duplicate),
 * (3) does the event start from the status we know, (4) is the change allowed.
 *
 * <p><b>Spring Boot basics:</b> {@code @Component} makes this a bean. Spring sees that the constructor needs an
 * {@link EventParser} and a {@link ReturnStatusRegistry} and passes the existing beans in.
 */
@Component
public class StatusUpdateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(StatusUpdateHandler.class);

    /** Result of a successful call: the event was applied, or it was a duplicate and ignored. */
    public enum Outcome { PROCESSED, DUPLICATE }

    private final EventParser parser;
    private final ReturnStatusRegistry registry;
    private final ProcessedEvents processed = new ProcessedEvents();

    public StatusUpdateHandler(EventParser parser, ReturnStatusRegistry registry) {
        this.parser = parser;
        this.registry = registry;
    }

    /**
     * {@code synchronized} lets only one thread at a time run this method, so "check, then change" cannot be
     * disturbed by a second message arriving at the same moment.
     *
     * @throws InvalidEventException if the message is not understood or the status change is not allowed
     */
    public synchronized Outcome handle(byte[] body) {
        StatusEvent event = parser.parse(body);

        // Check for duplicates first: a repeated event would otherwise be rejected as "change not allowed".
        if (processed.contains(event.eventId())) {
            LOG.info("Duplicate ignored: event {} was already processed ({} {} -> {})",
                    event.eventId(), event.returnId(), event.oldStatus(), event.newStatus());
            return Outcome.DUPLICATE;
        }

        ReturnStatus current = registry.current(event.returnId());
        if (event.oldStatus() != current) {
            throw new InvalidEventException("Event does not match the known status: " + event.returnId()
                    + " is " + current + ", but the event starts from " + event.oldStatus());
        }
        if (!StatusTransitions.isAllowed(event.oldStatus(), event.newStatus())) {
            throw new InvalidEventException("Status change not allowed: " + event.returnId() + " "
                    + event.oldStatus() + " -> " + event.newStatus());
        }

        registry.set(event.returnId(), event.newStatus());
        processed.markProcessed(event.eventId());
        LOG.info("Status updated: {} {} -> {} (event {})",
                event.returnId(), event.oldStatus(), event.newStatus(), event.eventId());
        return Outcome.PROCESSED;
    }
}
