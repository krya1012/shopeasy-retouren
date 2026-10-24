package de.shopeasy.restmock;

import java.math.BigDecimal;

/**
 * One article of an order or return. Money uses {@link BigDecimal} (exact decimal numbers), never
 * {@code double}, because {@code double} makes small rounding errors.
 */
public record Item(String sku, String productName, int quantity, BigDecimal unitPrice, String currency) {
}
