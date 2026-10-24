package de.shopeasy.grpc.server;

import de.shopeasy.grpc.ConditionCheckRequest;
import de.shopeasy.grpc.Decision;

/**
 * The rules of the condition check (proposal from TASK-ANALYSIS.md, section 5.1). They use the observed condition,
 * the tags and the original packaging. The score is high for clear cases and low for borderline cases.
 *
 * <pre>
 * AS_NEW + tags + original packaging   -> ACCEPT         0.95
 * AS_NEW + tags, no original packaging -> ACCEPT         0.85 (packaging is no condition for taking a return back)
 * AS_NEW, tags missing                 -> MANUAL_REVIEW  0.50
 * MINOR_WEAR / MISSING_TAGS            -> MANUAL_REVIEW  0.55 / 0.50
 * VISIBLY_WORN / DAMAGED / MISSING_PARTS -> REJECT       0.90 / 0.95 / 0.90
 * </pre>
 *
 * <p>Java note: {@code switch} with {@code ->} is a "switch expression": it returns a value. The compiler checks
 * that every case is handled.
 */
final class ConditionRules {

    /** Result of the rules: decision, confidence and a short reason. A record is a simple data holder. */
    record Result(Decision decision, double confidenceScore, String reason) {
        boolean requiresManualReview() {
            return decision == Decision.MANUAL_REVIEW;
        }
    }

    private ConditionRules() {
        // utility class: only static methods
    }

    /** Expects a validated request (see {@link RequestValidator}). */
    static Result evaluate(ConditionCheckRequest request) {
        return switch (request.getObservedCondition()) {
            case AS_NEW -> asNew(request);
            case MINOR_WEAR -> new Result(Decision.MANUAL_REVIEW, 0.55, "Minor signs of wear, borderline case");
            case MISSING_TAGS -> new Result(Decision.MANUAL_REVIEW, 0.50, "Tags are missing, borderline case");
            case VISIBLY_WORN -> new Result(Decision.REJECT, 0.90, "Clearly worn");
            case DAMAGED -> new Result(Decision.REJECT, 0.95, "Article is damaged");
            case MISSING_PARTS -> new Result(Decision.REJECT, 0.90, "Accessories or parts are missing");
            default -> throw new IllegalArgumentException("Unknown condition: " + request.getObservedCondition());
        };
    }

    /**
     * Without the original packaging we still accept (it is no condition for taking a return back),
     * but with a bit less confidence.
     */
    private static Result asNew(ConditionCheckRequest request) {
        if (!request.getTagsPresent()) {
            return new Result(Decision.MANUAL_REVIEW, 0.50, "As new, but tag is missing");
        }
        return request.getOriginalPackaging()
                ? new Result(Decision.ACCEPT, 0.95, "As new, tag present")
                : new Result(Decision.ACCEPT, 0.85, "As new, tag present, without original packaging");
    }
}
