package de.shopeasy.restmock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/** Hält die Testdaten im Speicher. Es gibt keine Datenbank, der Stand ist nach einem Neustart wieder der alte. */
@Component
public class ReturnStore {

    private final Map<String, ReturnReport> returns = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public ReturnStore() {
        loadTestData();
    }

    public List<ReturnReport> find(Optional<ReturnStatus> status, Optional<Marketplace> marketplace) {
        return returns.values().stream()
                .filter(r -> status.map(s -> r.status() == s).orElse(true))
                .filter(r -> marketplace.map(m -> r.marketplace() == m).orElse(true))
                .sorted((a, b) -> a.returnId().compareTo(b.returnId()))
                .toList();
    }

    public Optional<ReturnReport> findReturn(String returnId) {
        return Optional.ofNullable(returns.get(returnId));
    }

    public Optional<Order> findOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    /** Setzt den Status und {@code updatedAt}. Leer, wenn es die Retoure nicht gibt. */
    public Optional<ReturnReport> updateStatus(String returnId, ReturnStatus newStatus) {
        return Optional.ofNullable(returns.computeIfPresent(returnId,
                (id, old) -> old.withStatus(newStatus, Instant.now())));
    }

    private void loadTestData() {
        add("RET-2026-00457", "ZAL-100457", Marketplace.ZALANDO, ReturnStatus.NEW,
                ReasonCode.SIZE_TOO_SMALL, "Blazer ist eine Nummer zu klein", "2026-10-12T08:15:00Z",
                null, "kunde.1001", "anna.beispiel@example.com",
                item("SH-BLZ-042-M", "Blazer Classic", 1, "89.90"));
        add("RET-2026-00458", "ZAL-100458", Marketplace.ZALANDO, ReturnStatus.NEW,
                ReasonCode.NOT_AS_DESCRIBED, "Farbe weicht stark vom Foto ab", "2026-10-12T09:40:00Z",
                null, "kunde.1002", "ben.muster@example.com",
                item("SH-DRS-017-S", "Sommerkleid", 1, "59.95"),
                item("SH-BLT-003-U", "Ledergürtel", 1, "24.90"));
        add("RET-2026-00459", "AMZ-302-5551234", Marketplace.AMAZON, ReturnStatus.APPROVED,
                ReasonCode.DEFECTIVE, "Reißverschluss klemmt", "2026-10-11T14:05:00Z",
                "https://labels.example.com/RET-2026-00459.pdf", "kunde.1003", "carla.test@example.com",
                item("SH-JKT-088-L", "Winterjacke", 1, "129.00"));
        add("RET-2026-00460", "AMZ-302-5557788", Marketplace.AMAZON, ReturnStatus.LABEL_SENT,
                ReasonCode.CHANGED_MIND, "Gefällt mir doch nicht", "2026-10-10T18:30:00Z",
                "https://labels.example.com/RET-2026-00460.pdf", "kunde.1004", "dennis.probe@example.com",
                item("SH-SNK-120-42", "Sneaker Urban", 2, "74.50"));
        add("RET-2026-00461", "ZAL-100461", Marketplace.ZALANDO, ReturnStatus.RECEIVED,
                ReasonCode.SIZE_TOO_LARGE, "Hose ist zu weit", "2026-10-08T07:50:00Z",
                "https://labels.example.com/RET-2026-00461.pdf", "kunde.1005", "eva.demo@example.com",
                item("SH-PNT-031-32", "Chino Hose", 1, "49.90"));
        add("RET-2026-00462", "AMZ-302-5559901", Marketplace.AMAZON, ReturnStatus.REJECTED,
                ReasonCode.WRONG_ITEM, "Falscher Artikel geliefert", "2026-10-05T11:20:00Z",
                null, "kunde.1006", "felix.stichprobe@example.com",
                item("SH-SHT-009-M", "Leinenhemd", 1, "39.90"));
    }

    private static Item item(String sku, String productName, int quantity, String unitPrice) {
        return new Item(sku, productName, quantity, new BigDecimal(unitPrice), "EUR");
    }

    private void add(String returnId, String orderId, Marketplace marketplace, ReturnStatus status,
                     ReasonCode reason, String reasonText, String requestedAt, String labelUrl,
                     String customerId, String email, Item... items) {
        Instant requested = Instant.parse(requestedAt);
        Customer customer = new Customer(customerId, email);
        returns.put(returnId, new ReturnReport(returnId, orderId, marketplace, status, reason, reasonText,
                requested, requested, List.of(items), customer, labelUrl));

        BigDecimal total = List.of(items).stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orders.put(orderId, new Order(orderId, marketplace, requested.minusSeconds(10L * 24 * 3600),
                requested.minusSeconds(6L * 24 * 3600), List.of(items), customer, total, "EUR"));
    }
}
