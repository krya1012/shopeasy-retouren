package de.shopeasy.workflow.support;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import de.shopeasy.workflow.config.WorkflowProperties;
import de.shopeasy.workflow.returns.Customer;
import de.shopeasy.workflow.returns.Item;
import de.shopeasy.workflow.returns.Order;
import de.shopeasy.workflow.returns.ReturnReport;
import de.shopeasy.workflow.workers.WorkflowErrors;
import io.camunda.client.api.response.ActivatedJob;
import org.mockito.Mockito;

/** Test data and small helpers shared by the tests. */
public final class Fixtures {

    public static final Item BLAZER = new Item("SH-BLZ-042-M", "Blazer Slim Fit", 1, new BigDecimal("89.9"), "EUR");

    private Fixtures() {
    }

    public static WorkflowProperties properties() {
        return new WorkflowProperties("http://localhost:8081", "localhost:9090", Duration.ZERO,
                new WorkflowProperties.Poller(true, Duration.ofSeconds(10)));
    }

    public static WorkflowErrors errors() {
        return new WorkflowErrors(properties());
    }

    /** An order of one or more items, delivered {@code deliveredDaysAgo} days before now. */
    public static Order order(String orderId, int deliveredDaysAgo, Item... items) {
        Instant delivered = Instant.now().minus(Duration.ofDays(deliveredDaysAgo));
        return new Order(orderId, null, delivered.minus(Duration.ofDays(4)), delivered, List.of(items),
                new Customer("CUST-1", "customer@example.com"), BigDecimal.ZERO, "EUR");
    }

    /** A return with the given status; the return date is now. */
    public static ReturnReport report(String returnId, String orderId, String marketplace, String status,
                                      String reason, String email, Item... items) {
        Instant now = Instant.now();
        return new ReturnReport(returnId, orderId, marketplace, status, reason, null, now, now, List.of(items),
                new Customer("CUST-1", email), null);
    }

    /** A fake job (the real one comes from the engine) with variables and the retries that are left. */
    public static ActivatedJob job(Map<String, Object> variables, int retries) {
        ActivatedJob job = Mockito.mock(ActivatedJob.class);
        Mockito.when(job.getVariablesAsMap()).thenReturn(variables);
        Mockito.when(job.getRetries()).thenReturn(retries);
        Mockito.when(job.getProcessInstanceKey()).thenReturn(4711L);
        return job;
    }
}
