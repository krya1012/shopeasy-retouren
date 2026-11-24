package de.shopeasy.restmock;

import java.util.List;

/**
 * Body of {@code POST /api/v1/returns}: a return that is not reported by a marketplace but by the web shop or
 * (typed in by a clerk) from an e-mail. Name and price of an article are not part of the request; the mock takes
 * them from the order.
 *
 * @param orderId       the order the articles belong to (required)
 * @param customerEmail e-mail of the customer (required)
 * @param items         the articles to return, at least one (required)
 * @param reason        optional: the reason code; missing means "the customer did not say"
 * @param reasonText    optional: the reason in the customer's words
 */
public record CreateReturn(String orderId, String customerEmail, List<Line> items, ReasonCode reason,
                           String reasonText) {

    /** One article to return: its article number and how many. */
    public record Line(String sku, int quantity) {
    }
}
