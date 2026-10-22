package de.shopeasy.grpc;

import static de.shopeasy.grpc.TestRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/** Die Regeln werden über den Service geprüft, damit nur die öffentliche Schnittstelle genutzt wird. */
class ConditionRulesTest extends ServiceTestBase {

    private ConditionCheckResponse check(ObservedCondition condition, boolean tags) {
        return stub.checkCondition(request("RET-1", "SKU-1", condition, tags));
    }

    @Test
    void neuwertigMitEtikettWirdAngenommen() {
        ConditionCheckResponse r = check(ObservedCondition.AS_NEW, true);
        assertEquals(Decision.ACCEPT, r.getDecision());
        assertFalse(r.getRequiresManualReview());
        assertTrue(r.getConfidenceScore() >= 0.9);
    }

    @Test
    void neuwertigOhneEtikettIstGrenzfall() {
        ConditionCheckResponse r = check(ObservedCondition.AS_NEW, false);
        assertEquals(Decision.MANUAL_REVIEW, r.getDecision());
        assertTrue(r.getRequiresManualReview());
    }

    @Test
    void leichteGebrauchsspurenSindGrenzfall() {
        assertEquals(Decision.MANUAL_REVIEW, check(ObservedCondition.MINOR_WEAR, true).getDecision());
    }

    @Test
    void fehlendeEtikettenSindGrenzfall() {
        assertEquals(Decision.MANUAL_REVIEW, check(ObservedCondition.MISSING_TAGS, false).getDecision());
    }

    @Test
    void deutlichGetragenWirdAbgelehnt() {
        ConditionCheckResponse r = check(ObservedCondition.VISIBLY_WORN, true);
        assertEquals(Decision.REJECT, r.getDecision());
        assertFalse(r.getRequiresManualReview());
    }

    @Test
    void beschaedigtWirdAbgelehnt() {
        assertEquals(Decision.REJECT, check(ObservedCondition.DAMAGED, true).getDecision());
    }

    @Test
    void fehlendeTeileWerdenAbgelehnt() {
        assertEquals(Decision.REJECT, check(ObservedCondition.MISSING_PARTS, true).getDecision());
    }

    @Test
    void antwortEnthaeltIdsUndZeitstempel() {
        ConditionCheckResponse r = check(ObservedCondition.AS_NEW, true);
        assertEquals("RET-1", r.getReturnId());
        assertEquals("SKU-1", r.getSku());
        assertEquals("2026-10-22T10:00:00Z", r.getCheckedAt());
        assertFalse(r.getReason().isBlank());
    }
}
