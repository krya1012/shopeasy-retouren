package de.shopeasy.restmock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Bestelldaten zu einer Retoure (die Aufgabe nennt dafür nur die Abfrage über die orderId). */
public record Order(
        String orderId,
        Marketplace marketplace,
        Instant orderedAt,
        Instant deliveredAt,
        List<Item> items,
        Customer customer,
        BigDecimal totalAmount,
        String currency) {
}
