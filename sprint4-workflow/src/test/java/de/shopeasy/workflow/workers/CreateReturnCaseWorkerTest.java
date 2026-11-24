package de.shopeasy.workflow.workers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.shopeasy.workflow.returns.Item;
import de.shopeasy.workflow.returns.Order;
import de.shopeasy.workflow.returns.ReturnReport;
import de.shopeasy.workflow.support.Fixtures;
import de.shopeasy.workflow.support.InMemoryReturnsApi;
import io.camunda.client.exception.JobError;

/** Tests the three entry channels and the facts for the rule registration-check. */
class CreateReturnCaseWorkerTest {

    private InMemoryReturnsApi api;
    private CreateReturnCaseWorker worker;

    @BeforeEach
    void setUp() {
        api = new InMemoryReturnsApi();
        worker = new CreateReturnCaseWorker(api, Fixtures.errors());
        api.add(Fixtures.order("ORD-1", 5, Fixtures.BLAZER));
    }

    private Map<String, Object> webshopReturn(String orderId, String sku, Object reason) {
        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("orderId", orderId);
        vars.put("customerEmail", "j.mueller@example.com");
        vars.put("items", List.of(Map.of("sku", sku, "quantity", 1)));
        vars.put("reason", reason);
        return vars;
    }

    @Test
    void marketplaceReturnIsReadFromTheStore() {
        // delivered 6 days before the return, one article, value 89.90
        Instant delivered = Instant.now().minus(Duration.ofDays(6));
        api.add(new Order("ORD-M", "ZALANDO", delivered.minus(Duration.ofDays(4)), delivered, List.of(Fixtures.BLAZER),
                null, new BigDecimal("89.9"), "EUR"));
        api.add(Fixtures.report("RET-M", "ORD-M", "ZALANDO", "NEW", "SIZE_TOO_SMALL", "j.mueller@example.com",
                Fixtures.BLAZER));

        Map<String, Object> out = worker.handle(Fixtures.job(Map.of("returnId", "RET-M"), 3));

        assertThat(out).containsEntry("returnId", "RET-M").containsEntry("orderId", "ORD-M")
                .containsEntry("customerEmail", "j.mueller@example.com").containsEntry("marketplace", "ZALANDO")
                .containsEntry("orderFound", true).containsEntry("daysSinceDelivery", 6L)
                .containsEntry("itemsInOrder", true).containsEntry("dataComplete", true)
                .containsEntry("returnValue", 89.9).containsEntry("reason", "SIZE_TOO_SMALL");
        assertThat(out.get("items")).isEqualTo(List.of(Map.of("sku", "SH-BLZ-042-M", "quantity", 1)));
        assertThat(api.returnCount()).isEqualTo(1);
    }

    @Test
    void webshopReturnIsCreatedInTheStore() {
        Map<String, Object> out = worker.handle(Fixtures.job(webshopReturn("ORD-1", "SH-BLZ-042-M", "SIZE_TOO_SMALL"), 3));

        assertThat(out.get("returnId").toString()).startsWith("RET-TEST-");
        assertThat(out).containsEntry("orderFound", true).containsEntry("daysSinceDelivery", 5L)
                .containsEntry("itemsInOrder", true).containsEntry("dataComplete", true)
                .containsEntry("returnValue", 89.9).containsEntry("marketplace", null);
        assertThat(api.statusOf(out.get("returnId").toString())).isEqualTo("NEW");
    }

    @Test
    void repeatedJobDoesNotCreateASecondReturn() {
        var job = Fixtures.job(webshopReturn("ORD-1", "SH-BLZ-042-M", "SIZE_TOO_SMALL"), 3);

        Object first = worker.handle(job).get("returnId");
        Object second = worker.handle(job).get("returnId");

        assertThat(second).isEqualTo(first);
        assertThat(api.returnCount()).isEqualTo(1);
    }

    @Test
    void returnAfterThePeriodShowsTheDays() {
        api.add(Fixtures.order("ORD-OLD", 40, Fixtures.BLAZER));

        Map<String, Object> out = worker.handle(Fixtures.job(webshopReturn("ORD-OLD", "SH-BLZ-042-M", "CHANGED_MIND"), 3));

        assertThat(out).containsEntry("orderFound", true).containsEntry("daysSinceDelivery", 40L);
    }

    @Test
    void unknownOrderIsReportedNotFound() {
        Map<String, Object> out = worker.handle(Fixtures.job(webshopReturn("ORD-NONE", "SH-BLZ-042-M", "DEFECTIVE"), 3));

        assertThat(out).containsEntry("orderFound", false).containsEntry("daysSinceDelivery", null)
                .containsEntry("itemsInOrder", false);
    }

    @Test
    void itemThatIsNotInTheOrderIsReported() {
        Map<String, Object> out = worker.handle(Fixtures.job(webshopReturn("ORD-1", "SH-XXX-001-M", "DEFECTIVE"), 3));

        assertThat(out).containsEntry("orderFound", true).containsEntry("itemsInOrder", false)
                .containsEntry("returnValue", 0.0);
    }

    @Test
    void missingReasonMeansDataIncomplete() {
        Map<String, Object> out = worker.handle(Fixtures.job(webshopReturn("ORD-1", "SH-BLZ-042-M", null), 3));

        assertThat(out).containsEntry("dataComplete", false).containsEntry("reason", null);
    }

    @Test
    void unknownMarketplaceReturnIsAPermanentError() {
        assertThatThrownBy(() -> worker.handle(Fixtures.job(Map.of("returnId", "RET-NONE"), 3)))
                .isInstanceOfSatisfying(JobError.class, e -> assertThat(e.getRetries()).isEqualTo(0));
    }

    @Test
    void missingOrderNumberIsAPermanentError() {
        Map<String, Object> vars = Map.of("customerEmail", "a@example.com", "items", List.of());

        assertThatThrownBy(() -> worker.handle(Fixtures.job(vars, 3)))
                .isInstanceOfSatisfying(JobError.class, e -> {
                    assertThat(e.getRetries()).isEqualTo(0);
                    assertThat(e.getErrorMessage()).contains("orderId");
                });
    }

    @Test
    void returnStoreDownIsATemporaryError() {
        api.setDown(true);

        assertThatThrownBy(() -> worker.handle(Fixtures.job(Map.of("returnId", "RET-M"), 3)))
                .isInstanceOfSatisfying(JobError.class, e -> {
                    assertThat(e.getRetries()).isNull(); // the engine counts: one retry less
                    assertThat(e.getErrorMessage()).contains("not reachable");
                });
    }

    @SuppressWarnings("unused")
    private static final Item UNUSED = Fixtures.BLAZER;
}
