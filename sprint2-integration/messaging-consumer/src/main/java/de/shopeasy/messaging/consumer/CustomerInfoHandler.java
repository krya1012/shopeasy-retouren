package de.shopeasy.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Second receiver of the status events: it informs customers. Here it only writes a log line; in a real system
 * it would send an e-mail. It has its own list of processed events, so nobody is informed twice.
 *
 * <p>This receiver does not check status changes. It trusts the events, because the management receiver
 * ({@link StatusUpdateHandler}) already does that. It only gets the statuses customers care about (see the
 * bindings in {@link RabbitConfig}).
 */
@Component
public class CustomerInfoHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerInfoHandler.class);

    private final EventParser parser;
    private final ProcessedEvents processed = new ProcessedEvents();

    public CustomerInfoHandler(EventParser parser) {
        this.parser = parser;
    }

    /**
     * @return true if the customer was informed, false for a duplicate
     * @throws InvalidEventException if the message is not understood
     */
    public synchronized boolean handle(byte[] body) {
        StatusEvent event = parser.parse(body);
        if (processed.contains(event.eventId())) {
            LOG.info("Customer info: duplicate ignored (event {})", event.eventId());
            return false;
        }
        processed.markProcessed(event.eventId());
        LOG.info("Customer info: customer informed, return {} is now {}", event.returnId(), event.newStatus());
        return true;
    }
}
