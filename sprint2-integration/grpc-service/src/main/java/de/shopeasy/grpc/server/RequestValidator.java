package de.shopeasy.grpc.server;

import java.time.DateTimeException;
import java.time.OffsetDateTime;

import de.shopeasy.grpc.ConditionCheckRequest;
import de.shopeasy.grpc.ObservedCondition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/** Prüft die Eingaben. Bei falschen Daten gibt es den gRPC-Status INVALID_ARGUMENT. */
final class RequestValidator {

    private RequestValidator() {
    }

    /** @throws StatusRuntimeException INVALID_ARGUMENT mit Beschreibung des Fehlers */
    static void validate(ConditionCheckRequest request) {
        if (request.getReturnId().isBlank()) {
            throw invalid("return_id darf nicht leer sein");
        }
        if (request.getSku().isBlank()) {
            throw invalid("sku darf nicht leer sein");
        }
        ObservedCondition condition = request.getObservedCondition();
        if (condition == ObservedCondition.OBSERVED_CONDITION_UNSPECIFIED
                || condition == ObservedCondition.UNRECOGNIZED) {
            throw invalid("observed_condition muss gesetzt sein");
        }
        try {
            OffsetDateTime.parse(request.getInspectedAt());
        } catch (DateTimeException e) {
            throw invalid("inspected_at ist kein ISO-8601-Zeitstempel: '" + request.getInspectedAt() + "'");
        }
    }

    static StatusRuntimeException invalid(String message) {
        return Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException();
    }
}
