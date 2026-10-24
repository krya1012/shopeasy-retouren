package de.shopeasy.messaging.consumer;

import static de.shopeasy.messaging.consumer.ReturnStatus.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusTransitionsTest {

    @Test
    void allowedTransitions() {
        assertTrue(StatusTransitions.isAllowed(NEW, APPROVED));
        assertTrue(StatusTransitions.isAllowed(NEW, IN_REVIEW));
        assertTrue(StatusTransitions.isAllowed(LABEL_SENT, EXPIRED));
        assertTrue(StatusTransitions.isAllowed(INSPECTED, MANUAL_REVIEW));
        assertTrue(StatusTransitions.isAllowed(ACCEPTED, REFUNDED));
    }

    @Test
    void forbiddenTransitions() {
        assertFalse(StatusTransitions.isAllowed(NEW, REFUNDED));
        assertFalse(StatusTransitions.isAllowed(APPROVED, NEW));
        assertFalse(StatusTransitions.isAllowed(RECEIVED, ACCEPTED));
        assertFalse(StatusTransitions.isAllowed(NEW, NEW));
    }

    @Test
    void nothingLeavesClosed() {
        for (ReturnStatus to : ReturnStatus.values()) {
            assertFalse(StatusTransitions.isAllowed(CLOSED, to));
        }
    }
}
