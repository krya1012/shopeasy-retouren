package de.shopeasy.workflow.returns;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link ReturnsApi} over HTTP against the REST mock of Sprint 2.
 *
 * <p>Spring basics: {@code RestClient} is Spring's tool for HTTP calls ({@code get()}, {@code post()},
 * {@code patch()}); {@code retrieve().body(Type.class)} sends the request and converts the JSON answer to a Java
 * object. A 4xx or 5xx answer becomes a {@link RestClientResponseException}; a missing answer (connection refused,
 * timeout) becomes a {@link ResourceAccessException}. Both are translated here into one
 * {@link ReturnsApiException} that says whether waiting can help.
 */
@Component
public class RestReturnsApi implements ReturnsApi {

    private static final String RETURNS = "/api/v1/returns";

    private final RestClient rest;

    public RestReturnsApi(RestClient restMockClient) {
        this.rest = restMockClient;
    }

    @Override
    public ReturnReport getReturn(String returnId) {
        return call("GET return " + returnId,
                () -> rest.get().uri(RETURNS + "/{id}", returnId).retrieve().body(ReturnReport.class));
    }

    @Override
    public ReturnReport createReturn(CreateReturnRequest request, String idempotencyKey) {
        return call("POST return for order " + request.orderId(),
                () -> rest.post().uri(RETURNS).header("Idempotency-Key", idempotencyKey).body(request)
                        .retrieve().body(ReturnReport.class));
    }

    @Override
    public Optional<Order> findOrder(String orderId) {
        try {
            return Optional.ofNullable(call("GET order " + orderId,
                    () -> rest.get().uri("/api/v1/orders/{id}", orderId).retrieve().body(Order.class)));
        } catch (ReturnsApiException e) {
            if (e.httpStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public List<ReturnReport> listNewMarketplaceReturns() {
        ReturnList list = call("GET new returns",
                () -> rest.get().uri(RETURNS + "?status=NEW").retrieve().body(ReturnList.class));
        return list == null || list.data() == null ? List.of()
                : list.data().stream().filter(r -> r.marketplace() != null).toList();
    }

    @Override
    public ReturnReport updateStatus(String returnId, String newStatus, String expectedStatus) {
        return call("PATCH status of " + returnId + " to " + newStatus,
                () -> rest.patch().uri(RETURNS + "/{id}", returnId)
                        .body(new StatusUpdate(newStatus, expectedStatus)).retrieve().body(ReturnReport.class));
    }

    /** Runs one call and translates the errors. */
    private static <T> T call(String what, Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            String detail = body.length() > 200 ? body.substring(0, 200) : body;
            // 4xx: the request is wrong or the state does not fit; waiting does not help. 5xx: the mock has a problem.
            throw new ReturnsApiException(what + ": HTTP " + status + " " + detail, status, status < 500, e);
        } catch (ResourceAccessException e) {
            throw new ReturnsApiException(what + ": REST mock not reachable (" + e.getMessage() + ")", 0, false, e);
        }
    }

    /** The list answer of the mock: the reports are in the field {@code data}. */
    record ReturnList(List<ReturnReport> data) {
    }

    /** Body of the PATCH call. */
    record StatusUpdate(String status, String expectedStatus) {
    }
}
