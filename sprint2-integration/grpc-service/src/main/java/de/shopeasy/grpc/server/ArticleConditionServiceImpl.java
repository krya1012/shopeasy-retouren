package de.shopeasy.grpc.server;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.shopeasy.grpc.ArticleConditionServiceGrpc;
import de.shopeasy.grpc.ConditionCheckBatchRequest;
import de.shopeasy.grpc.ConditionCheckBatchResponse;
import de.shopeasy.grpc.ConditionCheckRequest;
import de.shopeasy.grpc.ConditionCheckResponse;
import de.shopeasy.grpc.GetCheckResultRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * The gRPC service. It judges the condition with {@link ConditionRules} and keeps the results in memory.
 *
 * <p><b>gRPC basics:</b>
 * <ul>
 *   <li>The interface comes from the lecturer's {@code article_condition_service.proto}. The build tool generates
 *       Java classes from it (messages like {@link ConditionCheckRequest} and the base class
 *       {@code ArticleConditionServiceImplBase}). We only write the logic by extending that base class.</li>
 *   <li>A method does not return its result. It gets a {@link StreamObserver} and calls
 *       {@code onNext(result)} and {@code onCompleted()} to answer, or {@code onError(...)} to fail.</li>
 *   <li>Errors are {@link Status} codes: {@code INVALID_ARGUMENT} (wrong data), {@code ALREADY_EXISTS} (check
 *       exists already), {@code NOT_FOUND} (no result), {@code UNAVAILABLE} (simulated outage, return_id starts
 *       with "RET-FAIL"), {@code INTERNAL} (unexpected error). See TASK-ANALYSIS.md, section 5.2.</li>
 * </ul>
 *
 * <p><b>Simulated scenarios:</b> "RET-FAIL..." fails with UNAVAILABLE on the first call only. "RET-SLOW..." answers
 * only after {@link #DEFAULT_SLOW_MILLIS} ms, so the client runs into its deadline.
 *
 * <p>Threads: gRPC calls our methods from several threads at the same time. Shared data (the maps) is therefore
 * only changed inside {@code synchronized} methods.
 */
public class ArticleConditionServiceImpl extends ArticleConditionServiceGrpc.ArticleConditionServiceImplBase {

    private static final Logger LOG = Logger.getLogger(ArticleConditionServiceImpl.class.getName());
    static final String OUTAGE_PREFIX = "RET-FAIL";
    static final String SLOW_PREFIX = "RET-SLOW";
    public static final long DEFAULT_SLOW_MILLIS = 5000;

    private final Clock clock;
    private final long slowMillis;
    private final Map<String, ConditionCheckResponse> results = new HashMap<>();
    private final Set<String> alreadyFailed = new HashSet<>();

    /** The {@link Clock} is passed in so tests can use a fixed time. */
    public ArticleConditionServiceImpl(Clock clock) {
        this(clock, DEFAULT_SLOW_MILLIS);
    }

    /** The waiting time of the scenario "RET-SLOW" can be set, so tests stay fast. */
    public ArticleConditionServiceImpl(Clock clock, long slowMillis) {
        this.clock = clock;
        this.slowMillis = slowMillis;
    }

    /** Checks one article. */
    @Override
    public void checkCondition(ConditionCheckRequest request, StreamObserver<ConditionCheckResponse> observer) {
        reply(observer, () -> {
            simulateSlowResponse(List.of(request));
            return checkAll(List.of(request), false).get(0);
        });
    }

    /** Checks several articles at once. All or nothing: one error rejects the whole batch. */
    @Override
    public void checkConditionBatch(ConditionCheckBatchRequest request,
                                    StreamObserver<ConditionCheckBatchResponse> observer) {
        reply(observer, () -> {
            if (request.getItemsCount() == 0) {
                throw RequestValidator.invalid("items must not be empty");
            }
            simulateSlowResponse(request.getItemsList());
            return ConditionCheckBatchResponse.newBuilder()
                    .addAllResults(checkAll(request.getItemsList(), true))
                    .build();
        });
    }

    /** Returns the result of an earlier check, or NOT_FOUND. */
    @Override
    public synchronized void getCheckResult(GetCheckResultRequest request,
                                            StreamObserver<ConditionCheckResponse> observer) {
        reply(observer, () -> {
            if (request.getReturnId().isBlank() || request.getSku().isBlank()) {
                throw RequestValidator.invalid("return_id and sku must not be empty");
            }
            ConditionCheckResponse result = results.get(key(request.getReturnId(), request.getSku()));
            if (result == null) {
                throw Status.NOT_FOUND
                        .withDescription("No check for " + request.getReturnId() + " / " + request.getSku())
                        .asRuntimeException();
            }
            return result;
        });
    }

    /**
     * Validates and judges all requests. All or nothing: if one item has an error, nothing is stored
     * (the .proto has no error field per item).
     */
    private synchronized List<ConditionCheckResponse> checkAll(List<ConditionCheckRequest> requests, boolean batch) {
        // 1. Validate the input
        for (int i = 0; i < requests.size(); i++) {
            try {
                RequestValidator.validate(requests.get(i));
            } catch (StatusRuntimeException e) {
                throw withPosition(e, i, batch);
            }
        }
        // 2. Simulated outage: on the first call per return_id
        for (ConditionCheckRequest request : requests) {
            if (request.getReturnId().startsWith(OUTAGE_PREFIX) && alreadyFailed.add(request.getReturnId())) {
                throw Status.UNAVAILABLE.withDescription("Service temporarily unavailable (simulated)")
                        .asRuntimeException();
            }
        }
        // 3. Duplicate requests: already stored, or twice in the same batch
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < requests.size(); i++) {
            String key = key(requests.get(i).getReturnId(), requests.get(i).getSku());
            if (results.containsKey(key) || !keys.add(key)) {
                throw withPosition(Status.ALREADY_EXISTS
                        .withDescription("Check for " + requests.get(i).getReturnId() + " / "
                                + requests.get(i).getSku() + " exists already").asRuntimeException(), i, batch);
            }
        }
        // 4. Judge and store
        List<ConditionCheckResponse> responses = new ArrayList<>();
        for (ConditionCheckRequest request : requests) {
            ConditionRules.Result rule = ConditionRules.evaluate(request);
            // The generated message classes are immutable: we build a new one with a builder.
            ConditionCheckResponse response = ConditionCheckResponse.newBuilder()
                    .setReturnId(request.getReturnId())
                    .setSku(request.getSku())
                    .setDecision(rule.decision())
                    .setConfidenceScore(rule.confidenceScore())
                    .setReason(rule.reason())
                    .setRequiresManualReview(rule.requiresManualReview())
                    .setCheckedAt(clock.instant().toString())
                    .build();
            results.put(key(request.getReturnId(), request.getSku()), response);
            responses.add(response);
            LOG.info(() -> "Check " + response.getReturnId() + " / " + response.getSku() + ": "
                    + response.getDecision() + " (" + response.getConfidenceScore() + ")");
        }
        return responses;
    }

    /**
     * Simulates a slow service. This is deliberately done outside the lock, so other calls do not wait.
     * If the client has already given up, the check is still stored.
     */
    private void simulateSlowResponse(List<ConditionCheckRequest> requests) {
        if (requests.stream().noneMatch(r -> r.getReturnId().startsWith(SLOW_PREFIX))) {
            return;
        }
        try {
            Thread.sleep(slowMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** For a batch, adds the position of the faulty item to the error message. */
    private static StatusRuntimeException withPosition(StatusRuntimeException e, int index, boolean batch) {
        if (!batch) {
            return e;
        }
        return e.getStatus().withDescription("items[" + index + "]: " + e.getStatus().getDescription())
                .asRuntimeException();
    }

    private static String key(String returnId, String sku) {
        return returnId + "|" + sku;
    }

    /**
     * Answers with the result or with the matching gRPC error. This one place turns exceptions into gRPC status
     * codes. An unexpected error becomes INTERNAL, without details for the client (details go to the log).
     */
    private static <T> void reply(StreamObserver<T> observer, Supplier<T> action) {
        try {
            T result = action.get();
            if (observer instanceof ServerCallStreamObserver<T> call && call.isCancelled()) {
                LOG.info("Client cancelled the call (deadline), the answer is not sent any more");
                return;
            }
            observer.onNext(result);
            observer.onCompleted();
        } catch (StatusRuntimeException e) {
            observer.onError(e);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error", e);
            observer.onError(Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        }
    }
}
