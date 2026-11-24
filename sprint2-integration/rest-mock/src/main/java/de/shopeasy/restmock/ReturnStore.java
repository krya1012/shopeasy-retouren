package de.shopeasy.restmock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    /** Idempotency key of a POST -> id of the return it created, so a repeated POST creates nothing new. */
    private final Map<String, String> createdByKey = new ConcurrentHashMap<>();
    /** Number of the next created return; the test returns use 00457 to 00462. */
    private final AtomicInteger nextNumber = new AtomicInteger(463);

    /** The result of {@link #create}: the return and whether this call created it (false: repeated call). */
    public record Created(ReturnReport report, boolean isNew) {
    }

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

    /**
     * Creates a return for the web shop or e-mail channel (status NEW, no marketplace). The articles get name and
     * price from the order if the order and the article exist; otherwise the name is the article number and the
     * price is 0.00 (price unknown). The method is {@code synchronized}: the check of the idempotency key and the
     * creation are one step, so two parallel calls with the same key cannot create two returns.
     *
     * @param idempotencyKey optional: the same key again returns the first return and creates nothing
     */
    public synchronized Created create(CreateReturn request, String idempotencyKey) {
        if (idempotencyKey != null && createdByKey.containsKey(idempotencyKey)) {
            return new Created(returns.get(createdByKey.get(idempotencyKey)), false);
        }
        Optional<Order> order = findOrder(request.orderId());
        List<Item> items = request.items().stream().map(line -> toItem(line, order)).toList();
        int number = nextNumber.getAndIncrement();
        String returnId = String.format("RET-2026-%05d", number);
        String customerId = order.map(o -> o.customer().customerId()).orElse("CUST-" + (90000 + number));
        Instant now = Instant.now();
        ReturnReport report = new ReturnReport(returnId, request.orderId(), null, ReturnStatus.NEW, request.reason(),
                request.reasonText(), now, now, items, new Customer(customerId, request.customerEmail()), null);
        returns.put(returnId, report);
        if (idempotencyKey != null) {
            createdByKey.put(idempotencyKey, returnId);
        }
        return new Created(report, true);
    }

    private static Item toItem(CreateReturn.Line line, Optional<Order> order) {
        return order.flatMap(o -> o.items().stream().filter(i -> i.sku().equals(line.sku())).findFirst())
                .map(i -> new Item(i.sku(), i.productName(), line.quantity(), i.unitPrice(), i.currency()))
                .orElseGet(() -> new Item(line.sku(), line.sku(), line.quantity(), BigDecimal.ZERO, "EUR"));
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

        // Orders of the web shop (no return yet) for returns reported by the web shop or by e-mail (POST). The
        // return date of such a return is "now", so the delivery dates are relative to the start of the mock.
        Instant now = Instant.now();
        addShopOrder("ORD-2026-119001", now, 5, "CUST-90001", "j.mueller@example.com",
                item("SH-BLZ-042-M", "Blazer Slim Fit", 1, "89.9"));
        addShopOrder("ORD-2026-119002", now, 40, "CUST-90002", "k.hoffmann@example.com",
                item("SH-JKT-088-L", "Winterjacke", 1, "129.0"));
        addShopOrder("ORD-2026-119003", now, 3, "CUST-90003", "l.neumann@example.com",
                item("SH-SNK-120-42", "Sneaker Urban", 1, "74.5"),
                item("SH-PNT-031-32", "Chino Hose", 1, "49.9"));
        addShopOrder("ORD-2026-119004", now, 4, "CUST-90004", "m.richter@example.com",
                item("SH-JKT-088-L", "Winterjacke", 2, "129.0"),
                item("SH-BLZ-042-M", "Blazer Slim Fit", 1, "89.9"));
    }

    /** Adds an order of the web shop that was delivered {@code deliveredDaysAgo} days before {@code now}. */
    private void addShopOrder(String orderId, Instant now, int deliveredDaysAgo, String customerId, String email,
                              Item... items) {
        BigDecimal total = Arrays.stream(items)
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant delivered = now.minusSeconds(deliveredDaysAgo * 24L * 3600);
        orders.put(orderId, new Order(orderId, null, delivered.minusSeconds(4L * 24 * 3600), delivered,
                List.of(items), new Customer(customerId, email), total, "EUR"));
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
