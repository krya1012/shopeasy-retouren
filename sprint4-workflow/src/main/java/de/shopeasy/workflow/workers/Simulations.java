package de.shopeasy.workflow.workers;

/**
 * Triggers for the simulated systems (AI assessment, shipping API, payment provider), so every error case of the
 * model can be shown in the demo. Same idea as the prefix {@code RET-FAIL} in the Sprint 2 gRPC service: a special value
 * in the data switches the behaviour. Here the trigger is the customer's e-mail address; every other address behaves
 * normally. (In Sprint 5 the AI assessment becomes a real agent; label and payment stay simulated.)
 */
final class Simulations {

    /** Fraud score of a normal customer. */
    static final double NORMAL_SCORE = 0.1;
    /** Fraud score of {@link #SUSPECT}: above the limit 0.7 of the rules. */
    static final double SUSPECT_SCORE = 0.85;

    /** The AI assessment gives a high fraud score. */
    static final String SUSPECT = "suspect@example.com";
    /** The AI assessment is down: no score. */
    static final String AI_DOWN = "ai-down@example.com";
    /** The shipping API is down. */
    static final String LABEL_DOWN = "label-down@example.com";
    /** The refund fails at once: no IBAN on file. */
    static final String NO_IBAN = "no-iban@example.com";
    /** The refund fails at once: the card has expired. */
    static final String CARD_EXPIRED = "card-expired@example.com";
    /** The payment provider does not answer (the refund fails after the retries). */
    static final String PSP_TIMEOUT = "psp-timeout@example.com";

    private Simulations() {
    }
}
