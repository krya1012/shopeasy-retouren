package de.shopeasy.restmock;

import java.math.BigDecimal;

public record Item(String sku, String productName, int quantity, BigDecimal unitPrice, String currency) {
}
