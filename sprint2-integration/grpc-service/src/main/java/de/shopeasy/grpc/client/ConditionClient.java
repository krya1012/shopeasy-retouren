package de.shopeasy.grpc.client;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

import de.shopeasy.grpc.ArticleConditionServiceGrpc;
import de.shopeasy.grpc.ArticleConditionServiceGrpc.ArticleConditionServiceBlockingStub;
import de.shopeasy.grpc.ConditionCheckBatchRequest;
import de.shopeasy.grpc.ConditionCheckRequest;
import de.shopeasy.grpc.ConditionCheckResponse;
import de.shopeasy.grpc.GetCheckResultRequest;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Client for the ArticleConditionService.
 *
 * <p><b>gRPC basics:</b> a {@link Channel} is the connection to the server. A <i>stub</i> is a generated object
 * that has one Java method per service method; calling it sends the request over the channel. A <i>blocking</i>
 * stub waits until the answer is there. A <i>deadline</i> is the latest time we wait for an answer; after that
 * the call fails with {@code DEADLINE_EXCEEDED}.
 *
 * <p><b>Error handling:</b> every call has a deadline. On UNAVAILABLE (service briefly not reachable) the call is
 * repeated up to three times. Other errors are not repeated: wrong data stays wrong, repeating could cause
 * something wrong (a duplicate), and after a missed deadline the service may have stored the check already.
 * In the end a {@link ConditionClientException} with a readable message is thrown.
 */
public class ConditionClient {

    private static final Logger LOG = Logger.getLogger(ConditionClient.class.getName());
    static final int MAX_ATTEMPTS = 3;
    static final long DEFAULT_DEADLINE_MILLIS = 3000;
    private static final long BACKOFF_MILLIS = 200;

    private final ArticleConditionServiceBlockingStub stub;
    private final long deadlineMillis;

    public ConditionClient(Channel channel) {
        this(channel, DEFAULT_DEADLINE_MILLIS);
    }

    /** The deadline can be set, so tests stay fast. */
    public ConditionClient(Channel channel, long deadlineMillis) {
        this.stub = ArticleConditionServiceGrpc.newBlockingStub(channel);
        this.deadlineMillis = deadlineMillis;
    }

    public ConditionCheckResponse checkCondition(ConditionCheckRequest request) {
        return call(s -> s.checkCondition(request));
    }

    public List<ConditionCheckResponse> checkConditionBatch(List<ConditionCheckRequest> requests) {
        return call(s -> s.checkConditionBatch(
                ConditionCheckBatchRequest.newBuilder().addAllItems(requests).build())).getResultsList();
    }

    public ConditionCheckResponse getCheckResult(String returnId, String sku) {
        return call(s -> s.getCheckResult(
                GetCheckResultRequest.newBuilder().setReturnId(returnId).setSku(sku).build()));
    }

    /**
     * Runs one remote call with a deadline and the retry rule. The call is passed in as a function
     * ({@code s -> s.checkCondition(...)}), so retry and error handling exist only once for all three methods.
     */
    private <T> T call(Function<ArticleConditionServiceBlockingStub, T> rpc) {
        for (int attempt = 1; ; attempt++) {
            try {
                return rpc.apply(stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS));
            } catch (StatusRuntimeException e) {
                boolean retry = e.getStatus().getCode() == Status.Code.UNAVAILABLE && attempt < MAX_ATTEMPTS;
                if (!retry) {
                    throw translate(e, attempt);
                }
                LOG.warning("Service unavailable, attempt " + (attempt + 1) + " of " + MAX_ATTEMPTS);
                sleep(BACKOFF_MILLIS * attempt);
            }
        }
    }

    /** Turns a gRPC error into an exception with a message a person can understand. */
    private ConditionClientException translate(StatusRuntimeException e, int attempts) {
        Status status = e.getStatus();
        String detail = status.getDescription() == null ? "" : status.getDescription();
        String message = switch (status.getCode()) {
            case INVALID_ARGUMENT -> "Invalid data: " + detail;
            case ALREADY_EXISTS -> "Duplicate request: " + detail;
            case NOT_FOUND -> "Not found: " + detail;
            case UNAVAILABLE -> "Service not reachable after " + attempts + " attempts: " + detail;
            case DEADLINE_EXCEEDED -> "Timeout, no answer within " + deadlineMillis
                    + " ms (the service may have stored the check anyway, do not repeat blindly)";
            default -> "Unexpected error (" + status.getCode() + "): " + detail;
        };
        return new ConditionClientException(status.getCode(), message, e);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConditionClientException(Status.Code.CANCELLED, "Cancelled", e);
        }
    }
}
