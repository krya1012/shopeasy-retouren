package de.shopeasy.workflow.returns;

import java.math.BigDecimal;

/** One article of a return or an order (as in the REST mock). Money is a {@link BigDecimal}, never a double. */
public record Item(String sku, String productName, int quantity, BigDecimal unitPrice, String currency) {
}
