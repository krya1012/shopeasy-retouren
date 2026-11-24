package de.shopeasy.workflow.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** An order as the REST mock delivers it. {@code deliveredAt} starts the return period. */
public record Order(
        String orderId,
        String marketplace,
        Instant orderedAt,
        Instant deliveredAt,
        List<Item> items,
        Customer customer,
        BigDecimal totalAmount,
        String currency) {
}
