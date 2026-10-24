package de.shopeasy.restmock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Order data for a return (the task only mentions a call by orderId, so we designed this format ourselves). */
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
