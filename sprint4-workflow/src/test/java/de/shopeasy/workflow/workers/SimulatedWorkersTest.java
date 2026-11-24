package de.shopeasy.workflow.workers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.shopeasy.workflow.support.Fixtures;
import io.camunda.client.exception.BpmnError;
import io.camunda.client.exception.JobError;

/** Tests the three simulated systems: AI assessment, shipping API, payment provider. */
class SimulatedWorkersTest {

    private final AssessReturnWorker assess = new AssessReturnWorker();
    private final CreateReturnLabelWorker label = new CreateReturnLabelWorker(Fixtures.errors());
    private final IssueRefundWorker refund = new IssueRefundWorker(Fixtures.errors());

    private static Map<String, Object> vars(String email) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("returnId", "RET-1");
        vars.put("customerEmail", email);
        vars.put("reason", "SIZE_TOO_SMALL");
        vars.put("returnValue", 89.9);
        return vars;
    }

    // ---- assess-return

    @Test
    void normalCustomerGetsALowScore() {
        Map<String, Object> out = assess.handle(Fixtures.job(vars("kunde@example.com"), 3));

        assertThat(out).containsEntry("fraudScore", 0.1).containsEntry("reasonCategory", "SIZE");
    }

    @Test
    void suspectCustomerGetsAHighScoreAddressIsNotCaseSensitive() {
        assertThat(assess.handle(Fixtures.job(vars("Suspect@Example.com"), 3))).containsEntry("fraudScore", 0.85);
    }

    @Test
    void assessmentOutageGivesNoScore() {
        Map<String, Object> out = assess.handle(Fixtures.job(vars("ai-down@example.com"), 3));

        assertThat(out).containsKey("fraudScore").containsEntry("fraudScore", null);
    }

    @Test
    void reasonCategories() {
        assertThat(AssessReturnWorker.category(null)).isEqualTo("UNKNOWN");
        assertThat(AssessReturnWorker.category("DEFECTIVE")).isEqualTo("QUALITY");
        assertThat(AssessReturnWorker.category("WRONG_ITEM")).isEqualTo("SHIPPING_ERROR");
        assertThat(AssessReturnWorker.category("CHANGED_MIND")).isEqualTo("CHANGED_MIND");
        assertThat(AssessReturnWorker.category("NOT_AS_DESCRIBED")).isEqualTo("EXPECTATION");
    }

    // ---- create-return-label

    @Test
    void labelUrlContainsTheReturnId() {
        assertThat(label.handle(Fixtures.job(vars("kunde@example.com"), 3)))
                .containsEntry("labelUrl", "https://labels.example.com/RET-1.pdf");
    }

    @Test
    void shippingApiDownIsRetriedAndThenBecomesABpmnError() {
        var down = vars("label-down@example.com");

        assertThatThrownBy(() -> label.handle(Fixtures.job(down, 3))).isInstanceOf(JobError.class);
        assertThatThrownBy(() -> label.handle(Fixtures.job(down, 2))).isInstanceOf(JobError.class);
        assertThatThrownBy(() -> label.handle(Fixtures.job(down, 1)))
                .isInstanceOfSatisfying(BpmnError.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo("LABEL_SERVICE_UNAVAILABLE"));
    }

    // ---- issue-refund

    @Test
    void refundGoesBackToThePaymentMethod() {
        assertThat(refund.handle(Fixtures.job(vars("kunde@example.com"), 3)))
                .containsEntry("refundMethod", "PSP").containsEntry("refundAmount", 89.9)
                .containsEntry("refundReference", "REF-RET-1");
    }

    @Test
    void missingIbanAndExpiredCardFailAtOnceEvenWithRetriesLeft() {
        for (String email : new String[] {"no-iban@example.com", "card-expired@example.com"}) {
            assertThatThrownBy(() -> refund.handle(Fixtures.job(vars(email), 3)))
                    .isInstanceOfSatisfying(BpmnError.class, e -> assertThat(e.getErrorCode())
                            .isEqualTo("REFUND_FAILED"));
        }
    }

    @Test
    void paymentProviderTimeoutIsRetriedAndThenBecomesABpmnError() {
        var timeout = vars("psp-timeout@example.com");

        assertThatThrownBy(() -> refund.handle(Fixtures.job(timeout, 3))).isInstanceOf(JobError.class);
        assertThatThrownBy(() -> refund.handle(Fixtures.job(timeout, 2))).isInstanceOf(JobError.class);
        assertThatThrownBy(() -> refund.handle(Fixtures.job(timeout, 1)))
                .isInstanceOfSatisfying(BpmnError.class, e -> assertThat(e.getErrorCode()).isEqualTo("REFUND_FAILED"));
    }
}
