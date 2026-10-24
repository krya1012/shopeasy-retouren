package de.shopeasy.grpc;

import static de.shopeasy.grpc.TestRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/** The rules are tested through the service, so only the public interface is used. */
class ConditionRulesTest extends ServiceTestBase {

    private ConditionCheckResponse check(ObservedCondition condition, boolean tags) {
        return stub.checkCondition(request("RET-1", "SKU-1", condition, tags));
    }

    @Test
    void asNewWithTagIsAccepted() {
        ConditionCheckResponse r = check(ObservedCondition.AS_NEW, true);
        assertEquals(Decision.ACCEPT, r.getDecision());
        assertFalse(r.getRequiresManualReview());
        assertTrue(r.getConfidenceScore() >= 0.9);
    }

    @Test
    void asNewWithoutPackagingIsAcceptedWithLowerScore() {
        ConditionCheckResponse r = stub.checkCondition(
                request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true, false));
        assertEquals(Decision.ACCEPT, r.getDecision());
        assertFalse(r.getRequiresManualReview());
        assertEquals(0.85, r.getConfidenceScore());
        assertTrue(r.getReason().contains("without original packaging"));
    }

    @Test
    void asNewWithoutTagIsBorderline() {
        ConditionCheckResponse r = check(ObservedCondition.AS_NEW, false);
        assertEquals(Decision.MANUAL_REVIEW, r.getDecision());
        assertTrue(r.getRequiresManualReview());
    }

    @Test
    void minorWearIsBorderline() {
        assertEquals(Decision.MANUAL_REVIEW, check(ObservedCondition.MINOR_WEAR, true).getDecision());
    }

    @Test
    void missingTagsIsBorderline() {
        assertEquals(Decision.MANUAL_REVIEW, check(ObservedCondition.MISSING_TAGS, false).getDecision());
    }

    @Test
    void visiblyWornIsRejected() {
        ConditionCheckResponse r = check(ObservedCondition.VISIBLY_WORN, true);
        assertEquals(Decision.REJECT, r.getDecision());
        assertFalse(r.getRequiresManualReview());
    }

    @Test
    void damagedIsRejected() {
        assertEquals(Decision.REJECT, check(ObservedCondition.DAMAGED, true).getDecision());
    }

    @Test
    void missingPartsIsRejected() {
        assertEquals(Decision.REJECT, check(ObservedCondition.MISSING_PARTS, true).getDecision());
    }

    @Test
    void answerContainsIdsAndTimestamp() {
        ConditionCheckResponse r = check(ObservedCondition.AS_NEW, true);
        assertEquals("RET-1", r.getReturnId());
        assertEquals("SKU-1", r.getSku());
        assertEquals("2026-10-22T10:00:00Z", r.getCheckedAt());
        assertFalse(r.getReason().isBlank());
    }
}
