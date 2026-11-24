package de.shopeasy.workflow.returns;

import java.util.List;

/**
 * Body of {@code POST /api/v1/returns} (web shop and e-mail channel).
 *
 * @param reason     reason code or null (the customer gave none)
 * @param reasonText the reason in the customer's words or null
 */
public record CreateReturnRequest(String orderId, String customerEmail, List<Line> items, String reason,
                                  String reasonText) {

    /** One article to return. */
    public record Line(String sku, int quantity) {
    }
}
