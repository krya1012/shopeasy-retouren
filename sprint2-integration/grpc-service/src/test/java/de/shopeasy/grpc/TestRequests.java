package de.shopeasy.grpc;

import java.time.Instant;

/** Hilfsmethoden, damit die Tests kurz bleiben. */
final class TestRequests {

    private TestRequests() {
    }

    /** Standard: Originalverpackung vorhanden. */
    static ConditionCheckRequest request(String returnId, String sku, ObservedCondition condition, boolean tags) {
        return request(returnId, sku, condition, tags, true);
    }

    static ConditionCheckRequest request(String returnId, String sku, ObservedCondition condition, boolean tags,
                                         boolean packaging) {
        return ConditionCheckRequest.newBuilder()
                .setReturnId(returnId)
                .setSku(sku)
                .setObservedCondition(condition)
                .setTagsPresent(tags)
                .setOriginalPackaging(packaging)
                .setInspectedAt(Instant.parse("2026-10-22T10:00:00Z").toString())
                .build();
    }
}
