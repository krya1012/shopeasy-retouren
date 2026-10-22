package de.shopeasy.grpc.server;

import de.shopeasy.grpc.ConditionCheckRequest;
import de.shopeasy.grpc.Decision;

/**
 * Die Regeln der Zustandsprüfung (Vorschlag aus aufgabenanalyse.md, 5.1).
 * Der Score ist bei eindeutigen Fällen hoch und bei Grenzfällen niedrig.
 */
final class ConditionRules {

    /** Ergebnis der Regeln: Entscheidung, Sicherheit und Begründung. */
    record Result(Decision decision, double confidenceScore, String reason) {
        boolean requiresManualReview() {
            return decision == Decision.MANUAL_REVIEW;
        }
    }

    private ConditionRules() {
    }

    /** Erwartet eine geprüfte Anfrage (siehe {@link RequestValidator}). */
    static Result evaluate(ConditionCheckRequest request) {
        return switch (request.getObservedCondition()) {
            case AS_NEW -> request.getTagsPresent()
                    ? new Result(Decision.ACCEPT, 0.95, "Neuwertig, Etikett vorhanden")
                    : new Result(Decision.MANUAL_REVIEW, 0.50, "Neuwertig, aber Etikett fehlt");
            case MINOR_WEAR -> new Result(Decision.MANUAL_REVIEW, 0.55, "Leichte Gebrauchsspuren, Grenzfall");
            case MISSING_TAGS -> new Result(Decision.MANUAL_REVIEW, 0.50, "Etiketten fehlen, Grenzfall");
            case VISIBLY_WORN -> new Result(Decision.REJECT, 0.90, "Deutlich getragen");
            case DAMAGED -> new Result(Decision.REJECT, 0.95, "Artikel ist beschädigt");
            case MISSING_PARTS -> new Result(Decision.REJECT, 0.90, "Zubehör oder Teile fehlen");
            default -> throw new IllegalArgumentException("Unbekannter Zustand: " + request.getObservedCondition());
        };
    }
}
