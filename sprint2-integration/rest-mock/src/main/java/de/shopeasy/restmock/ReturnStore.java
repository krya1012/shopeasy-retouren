package de.shopeasy.restmock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Holds the test data in memory. There is no database: after a restart the data is back to its initial state.
 *
 * <p>Spring Boot note: {@code @Component} makes Spring create one object of this class at startup; the
 * controllers get it through their constructor. The maps are {@link ConcurrentHashMap}s because several HTTP
 * requests can arrive at the same time (each in its own thread). The texts of the test returns are German
 * customer texts on purpose: they are shop data, not documentation.
 */
@Component
public class ReturnStore {

    private final Map<String, ReturnReport> returns = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public ReturnStore() {
        loadTestData();
    }

    /** Returns the reports that match the optional filters, sorted by returnId. */
    public List<ReturnReport> find(Optional<ReturnStatus> status, Optional<Marketplace> marketplace) {
        return returns.values().stream()
                .filter(r -> status.map(s -> r.status() == s).orElse(true))
                .filter(r -> marketplace.map(m -> r.marketplace() == m).orElse(true))
                .sorted(Comparator.comparing(ReturnReport::returnId))
                .toList();
    }

    public Optional<ReturnReport> findReturn(String returnId) {
        return Optional.ofNullable(returns.get(returnId));
    }

    public Optional<Order> findOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    /**
     * Sets the status and {@code updatedAt} if the change is allowed. If the status is already set, nothing
     * changes (so the same request may be repeated). {@code computeIfPresent} runs the check and the change as one
     * step inside the map, so no parallel call can slip a forbidden change in between.
     *
     * @param expectedStatus optional (may be null): the status the caller thinks the return has. A different
     *                       current status means the caller's information is old, so the change is refused.
     * @throws IllegalStateException if the change is not allowed (the message is meant for the caller)
     * @return empty if the return does not exist
     */
    public Optional<ReturnReport> updateStatus(String returnId, ReturnStatus newStatus, ReturnStatus expectedStatus) {
        return Optional.ofNullable(returns.computeIfPresent(returnId, (id, current) -> {
            if (current.status() == newStatus) {
                return current;
            }
            if (expectedStatus != null && current.status() != expectedStatus) {
                throw new IllegalStateException("Status is " + current.status() + ", not the expected "
                        + expectedStatus);
            }
            if (!StatusTransitions.isAllowed(current.status(), newStatus)) {
                throw new IllegalStateException("Status change not allowed: " + current.status() + " -> " + newStatus);
            }
            return current.withStatus(newStatus, Instant.now());
        }));
    }

    private void loadTestData() {
        // The first return is the example from slide 9 of the task (kept exactly as the lecturer wrote it).
        add("RET-2026-00457", "ORD-2026-118932", Marketplace.ZALANDO, ReturnStatus.NEW,
                ReasonCode.SIZE_TOO_SMALL, "Passt leider nicht, eine Nummer kleiner bitte.", "2026-09-18T09:14:00Z",
                null, "CUST-88213", "j.mueller@example.com",
                item("SH-BLZ-042-M", "Blazer Slim Fit", 1, "89.9"));
        add("RET-2026-00458", "ORD-2026-118940", Marketplace.ZALANDO, ReturnStatus.NEW,
                ReasonCode.NOT_AS_DESCRIBED, "Farbe weicht stark vom Foto ab", "2026-09-19T09:40:00Z",
                null, "CUST-88214", "b.schmidt@example.com",
                item("SH-DRS-017-S", "Sommerkleid", 1, "59.95"),
                item("SH-BLT-003-U", "Ledergürtel", 1, "24.9"));
        add("RET-2026-00459", "ORD-2026-118955", Marketplace.AMAZON, ReturnStatus.APPROVED,
                ReasonCode.DEFECTIVE, "Reißverschluss klemmt", "2026-09-20T14:05:00Z",
                "https://labels.example.com/RET-2026-00459.pdf", "CUST-88230", "c.weber@example.com",
                item("SH-JKT-088-L", "Winterjacke", 1, "129.0"));
        add("RET-2026-00460", "ORD-2026-118961", Marketplace.AMAZON, ReturnStatus.LABEL_SENT,
                ReasonCode.CHANGED_MIND, "Gefällt mir doch nicht", "2026-09-21T18:30:00Z",
                "https://labels.example.com/RET-2026-00460.pdf", "CUST-88245", "d.fischer@example.com",
                item("SH-SNK-120-42", "Sneaker Urban", 2, "74.5"));
        add("RET-2026-00461", "ORD-2026-118970", Marketplace.ZALANDO, ReturnStatus.RECEIVED,
                ReasonCode.SIZE_TOO_LARGE, "Hose ist zu weit", "2026-09-22T07:50:00Z",
                "https://labels.example.com/RET-2026-00461.pdf", "CUST-88251", "e.wagner@example.com",
                item("SH-PNT-031-32", "Chino Hose", 1, "49.9"));
        add("RET-2026-00462", "ORD-2026-118982", Marketplace.AMAZON, ReturnStatus.REJECTED,
                ReasonCode.WRONG_ITEM, "Falscher Artikel geliefert", "2026-09-23T11:20:00Z",
                null, "CUST-88260", "f.becker@example.com",
                item("SH-SHT-009-M", "Leinenhemd", 1, "39.9"));
    }

    /** Short helper for the test data: an item in EUR. */
    private static Item item(String sku, String productName, int quantity, String unitPrice) {
        return new Item(sku, productName, quantity, new BigDecimal(unitPrice), "EUR");
    }

    /** Adds a return and the matching order (the order total is the sum of the items). */
    private void add(String returnId, String orderId, Marketplace marketplace, ReturnStatus status,
                     ReasonCode reason, String reasonText, String requestedAt, String labelUrl,
                     String customerId, String email, Item... items) {
        Instant requested = Instant.parse(requestedAt);
        Customer customer = new Customer(customerId, email);
        returns.put(returnId, new ReturnReport(returnId, orderId, marketplace, status, reason, reasonText,
                requested, requested, List.of(items), customer, labelUrl));

        BigDecimal total = Arrays.stream(items)
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orders.put(orderId, new Order(orderId, marketplace, requested.minusSeconds(10L * 24 * 3600),
                requested.minusSeconds(6L * 24 * 3600), List.of(items), customer, total, "EUR"));
    }
}
