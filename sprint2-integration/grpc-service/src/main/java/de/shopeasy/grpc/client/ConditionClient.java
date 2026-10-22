package de.shopeasy.grpc.client;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
 * Client für den ArticleConditionService.
 *
 * <p>Fehlerbehandlung: Jeder Aufruf hat eine Frist. Bei UNAVAILABLE (Service kurz nicht erreichbar) wird
 * bis zu dreimal wiederholt. Alle anderen Fehler wiederholt man nicht, weil sie beim nächsten Mal wieder
 * auftreten (falsche Daten), die Wiederholung etwas Falsches bewirken würde (Duplikat) oder die Prüfung beim
 * Service eventuell schon gespeichert ist (Frist überschritten).
 * Am Ende wird eine {@link ConditionClientException} mit verständlicher Meldung geworfen.
 */
public class ConditionClient {

    static final int MAX_ATTEMPTS = 3;
    static final long DEFAULT_DEADLINE_MILLIS = 3000;
    private static final long BACKOFF_MILLIS = 200;

    private final ArticleConditionServiceBlockingStub stub;
    private final long deadlineMillis;

    public ConditionClient(Channel channel) {
        this(channel, DEFAULT_DEADLINE_MILLIS);
    }

    /** Die Frist ist einstellbar, damit Tests schnell bleiben. */
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

    private <T> T call(Function<ArticleConditionServiceBlockingStub, T> rpc) {
        for (int attempt = 1; ; attempt++) {
            try {
                return rpc.apply(stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS));
            } catch (StatusRuntimeException e) {
                boolean retry = e.getStatus().getCode() == Status.Code.UNAVAILABLE && attempt < MAX_ATTEMPTS;
                if (!retry) {
                    throw translate(e, attempt);
                }
                System.out.println("   Service nicht verfügbar, neuer Versuch " + (attempt + 1) + " von " + MAX_ATTEMPTS);
                sleep(BACKOFF_MILLIS * attempt);
            }
        }
    }

    private ConditionClientException translate(StatusRuntimeException e, int attempts) {
        Status status = e.getStatus();
        String detail = status.getDescription() == null ? "" : status.getDescription();
        String message = switch (status.getCode()) {
            case INVALID_ARGUMENT -> "Falsche Daten: " + detail;
            case ALREADY_EXISTS -> "Doppelte Anfrage: " + detail;
            case NOT_FOUND -> "Nicht gefunden: " + detail;
            case UNAVAILABLE -> "Service nach " + attempts + " Versuchen nicht erreichbar: " + detail;
            case DEADLINE_EXCEEDED -> "Zeitüberschreitung, keine Antwort innerhalb von " + deadlineMillis
                    + " ms (die Prüfung kann beim Service trotzdem gespeichert sein, nicht blind wiederholen)";
            default -> "Unerwarteter Fehler (" + status.getCode() + "): " + detail;
        };
        return new ConditionClientException(status.getCode(), message, e);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConditionClientException(Status.Code.CANCELLED, "Abgebrochen", e);
        }
    }
}
