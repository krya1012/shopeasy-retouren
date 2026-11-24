package de.shopeasy.workflow.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import de.shopeasy.workflow.returns.CreateReturnRequest;
import de.shopeasy.workflow.returns.Customer;
import de.shopeasy.workflow.returns.Item;
import de.shopeasy.workflow.returns.Order;
import de.shopeasy.workflow.returns.ReturnReport;
import de.shopeasy.workflow.returns.ReturnsApi;
import de.shopeasy.workflow.returns.ReturnsApiException;
import de.shopeasy.workflow.status.ReturnStatus;
import de.shopeasy.workflow.status.StatusTransitions;

/**
 * A return store in memory for tests: behaves like the REST mock (404 for unknown things, 409 for a wrong
 * {@code expectedStatus} or a forbidden change, the same status again is fine, POST with idempotency key) and can be
 * switched "down" to simulate an outage. The allowed changes are the table of the workflow, which a contract test
 * compares with the shared contract file of Sprint 2.
 */
public class InMemoryReturnsApi implements ReturnsApi {

    private final Map<String, ReturnReport> returns = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, String> createdByKey = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(1);
    private volatile boolean down;
    /** Every status change that was really made, as {@code "RET-1:APPROVED"}. */
    public final List<String> statusChanges = new CopyOnWriteArrayList<>();

    public void setDown(boolean down) {
        this.down = down;
    }

    /** Back to an empty store (the tests share one instance). */
    public synchronized void reset() {
        returns.clear();
        orders.clear();
        createdByKey.clear();
        counter.set(1);
        down = false;
        statusChanges.clear();
    }

    /** The ids of all returns that exist. */
    public java.util.Set<String> returnIds() {
        return java.util.Set.copyOf(returns.keySet());
    }

    public void add(ReturnReport report) {
        returns.put(report.returnId(), report);
    }

    public void add(Order order) {
        orders.put(order.orderId(), order);
    }

    public String statusOf(String returnId) {
        return returns.get(returnId).status();
    }

    public int returnCount() {
        return returns.size();
    }

    private void requireUp() {
        if (down) {
            throw new ReturnsApiException("REST mock not reachable (simulated outage)", 0, false, null);
        }
    }

    @Override
    public ReturnReport getReturn(String returnId) {
        requireUp();
        ReturnReport report = returns.get(returnId);
        if (report == null) {
            throw new ReturnsApiException("GET return " + returnId + ": HTTP 404", 404, true, null);
        }
        return report;
    }

    @Override
    public synchronized ReturnReport createReturn(CreateReturnRequest request, String idempotencyKey) {
        requireUp();
        if (idempotencyKey != null && createdByKey.containsKey(idempotencyKey)) {
            return returns.get(createdByKey.get(idempotencyKey));
        }
        Optional<Order> order = Optional.ofNullable(orders.get(request.orderId()));
        List<Item> items = request.items().stream().map(line -> order
                .flatMap(o -> o.items().stream().filter(i -> i.sku().equals(line.sku())).findFirst())
                .map(i -> new Item(i.sku(), i.productName(), line.quantity(), i.unitPrice(), i.currency()))
                .orElseGet(() -> new Item(line.sku(), line.sku(), line.quantity(), java.math.BigDecimal.ZERO, "EUR")))
                .toList();
        String returnId = "RET-TEST-" + counter.getAndIncrement();
        Instant now = Instant.now();
        ReturnReport report = new ReturnReport(returnId, request.orderId(), null, "NEW", request.reason(),
                request.reasonText(), now, now, items, new Customer("CUST-TEST", request.customerEmail()), null);
        returns.put(returnId, report);
        if (idempotencyKey != null) {
            createdByKey.put(idempotencyKey, returnId);
        }
        return report;
    }

    @Override
    public Optional<Order> findOrder(String orderId) {
        requireUp();
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public List<ReturnReport> listNewMarketplaceReturns() {
        requireUp();
        return returns.values().stream()
                .filter(r -> r.status().equals("NEW") && r.marketplace() != null)
                .sorted(java.util.Comparator.comparing(ReturnReport::returnId)).toList();
    }

    @Override
    public synchronized ReturnReport updateStatus(String returnId, String newStatus, String expectedStatus) {
        requireUp();
        ReturnReport current = getReturn(returnId);
        if (current.status().equals(newStatus)) {
            return current;
        }
        if (expectedStatus != null && !current.status().equals(expectedStatus)) {
            throw new ReturnsApiException("PATCH " + returnId + ": HTTP 409 Status is " + current.status()
                    + ", not the expected " + expectedStatus, 409, true, null);
        }
        if (!StatusTransitions.isAllowed(ReturnStatus.valueOf(current.status()), ReturnStatus.valueOf(newStatus))) {
            throw new ReturnsApiException("PATCH " + returnId + ": HTTP 409 Status change not allowed: "
                    + current.status() + " -> " + newStatus, 409, true, null);
        }
        ReturnReport changed = new ReturnReport(current.returnId(), current.orderId(), current.marketplace(),
                newStatus, current.reason(), current.reasonText(), current.requestedAt(), Instant.now(),
                current.items(), current.customer(), current.returnLabelUrl());
        returns.put(returnId, changed);
        statusChanges.add(returnId + ":" + newStatus);
        return changed;
    }
}
