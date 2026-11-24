package de.shopeasy.workflow.returns;

import java.util.List;
import java.util.Optional;

/**
 * What the workflow needs from the return store (the REST mock of Sprint 2). An interface, so tests can use a
 * simple in-memory version instead of HTTP.
 *
 * <p>Every method throws a {@link ReturnsApiException} if the call fails.
 */
public interface ReturnsApi {

    /** One return; a {@code ReturnsApiException} (permanent, 404) if it does not exist. */
    ReturnReport getReturn(String returnId);

    /**
     * Creates a return for the web shop or e-mail channel.
     *
     * @param idempotencyKey the same key again returns the first return and creates no second one
     */
    ReturnReport createReturn(CreateReturnRequest request, String idempotencyKey);

    /** The order, or empty if the store does not know it. */
    Optional<Order> findOrder(String orderId);

    /** Returns with status NEW that a marketplace reported (the web shop and e-mail returns are not in this list). */
    List<ReturnReport> listNewMarketplaceReturns();

    /**
     * Sets the status. The store refuses (409, permanent) if the return does not have the {@code expectedStatus} or
     * if the change is not allowed; the same status again is fine.
     */
    ReturnReport updateStatus(String returnId, String newStatus, String expectedStatus);
}
