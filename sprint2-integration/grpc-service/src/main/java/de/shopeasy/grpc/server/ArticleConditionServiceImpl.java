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
import io.grpc.stub.StreamObserver;

/**
 * Der gRPC-Service. Er bewertet den Zustand mit {@link ConditionRules} und merkt sich die Ergebnisse im Speicher.
 *
 * <p>Fehler (siehe aufgabenanalyse.md, 5.2):
 * INVALID_ARGUMENT (falsche Daten), ALREADY_EXISTS (Prüfung gibt es schon), NOT_FOUND (kein Ergebnis),
 * UNAVAILABLE (simulierter Ausfall, return_id beginnt mit "RET-FAIL").
 */
public class ArticleConditionServiceImpl extends ArticleConditionServiceGrpc.ArticleConditionServiceImplBase {

    private static final Logger LOG = Logger.getLogger(ArticleConditionServiceImpl.class.getName());
    static final String OUTAGE_PREFIX = "RET-FAIL";

    private final Clock clock;
    private final Map<String, ConditionCheckResponse> results = new HashMap<>();
    private final Set<String> alreadyFailed = new HashSet<>();

    public ArticleConditionServiceImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void checkCondition(ConditionCheckRequest request, StreamObserver<ConditionCheckResponse> observer) {
        reply(observer, () -> checkAll(List.of(request), false).get(0));
    }

    @Override
    public void checkConditionBatch(ConditionCheckBatchRequest request,
                                    StreamObserver<ConditionCheckBatchResponse> observer) {
        reply(observer, () -> {
            if (request.getItemsCount() == 0) {
                throw RequestValidator.invalid("items darf nicht leer sein");
            }
            return ConditionCheckBatchResponse.newBuilder()
                    .addAllResults(checkAll(request.getItemsList(), true))
                    .build();
        });
    }

    @Override
    public synchronized void getCheckResult(GetCheckResultRequest request,
                                            StreamObserver<ConditionCheckResponse> observer) {
        reply(observer, () -> {
            if (request.getReturnId().isBlank() || request.getSku().isBlank()) {
                throw RequestValidator.invalid("return_id und sku dürfen nicht leer sein");
            }
            ConditionCheckResponse result = results.get(key(request.getReturnId(), request.getSku()));
            if (result == null) {
                throw Status.NOT_FOUND
                        .withDescription("Keine Prüfung für " + request.getReturnId() + " / " + request.getSku())
                        .asRuntimeException();
            }
            return result;
        });
    }

    /**
     * Prüft und bewertet alle Anfragen. Alles oder nichts: Gibt es bei einer Position einen Fehler,
     * wird nichts gespeichert (die .proto hat kein Fehlerfeld je Position).
     */
    private synchronized List<ConditionCheckResponse> checkAll(List<ConditionCheckRequest> requests, boolean batch) {
        // 1. Eingaben prüfen
        for (int i = 0; i < requests.size(); i++) {
            try {
                RequestValidator.validate(requests.get(i));
            } catch (StatusRuntimeException e) {
                throw withPosition(e, i, batch);
            }
        }
        // 2. Simulierter Ausfall: beim ersten Aufruf je return_id
        for (ConditionCheckRequest request : requests) {
            if (request.getReturnId().startsWith(OUTAGE_PREFIX) && alreadyFailed.add(request.getReturnId())) {
                throw Status.UNAVAILABLE.withDescription("Service kurzzeitig nicht verfügbar (simuliert)")
                        .asRuntimeException();
            }
        }
        // 3. Doppelte Anfragen: schon gespeichert oder im selben Stapel zweimal
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < requests.size(); i++) {
            String key = key(requests.get(i).getReturnId(), requests.get(i).getSku());
            if (results.containsKey(key) || !keys.add(key)) {
                throw withPosition(Status.ALREADY_EXISTS
                        .withDescription("Prüfung für " + requests.get(i).getReturnId() + " / "
                                + requests.get(i).getSku() + " gibt es schon").asRuntimeException(), i, batch);
            }
        }
        // 4. Bewerten und speichern
        List<ConditionCheckResponse> responses = new ArrayList<>();
        for (ConditionCheckRequest request : requests) {
            ConditionRules.Result rule = ConditionRules.evaluate(request);
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
            LOG.info(() -> "Prüfung " + response.getReturnId() + " / " + response.getSku() + ": "
                    + response.getDecision() + " (" + response.getConfidenceScore() + ")");
        }
        return responses;
    }

    /** Hängt bei einem Stapel die Position an die Fehlermeldung. */
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

    /** Antwortet mit dem Ergebnis oder mit dem passenden gRPC-Fehler. Unerwartete Fehler werden zu INTERNAL. */
    private static <T> void reply(StreamObserver<T> observer, Supplier<T> action) {
        try {
            observer.onNext(action.get());
            observer.onCompleted();
        } catch (StatusRuntimeException e) {
            observer.onError(e);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unerwarteter Fehler", e);
            observer.onError(Status.INTERNAL.withDescription("Interner Fehler").asRuntimeException());
        }
    }
}
