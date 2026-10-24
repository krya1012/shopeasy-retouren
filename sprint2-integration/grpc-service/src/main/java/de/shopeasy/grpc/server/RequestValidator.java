package de.shopeasy.grpc.server;

import java.time.DateTimeException;
import java.time.OffsetDateTime;

import de.shopeasy.grpc.ConditionCheckRequest;
import de.shopeasy.grpc.ObservedCondition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Checks the input of a request. Wrong data gives the gRPC status {@code INVALID_ARGUMENT}.
 *
 * <p>gRPC basics: every call ends with a status code (like HTTP status codes, but for gRPC). The server reports
 * an error by creating a {@link Status}, adding a description, and turning it into a
 * {@link StatusRuntimeException}. The client receives the same code and description.
 */
final class RequestValidator {

    private RequestValidator() {
        // utility class: only static methods
    }

    /** @throws StatusRuntimeException with code INVALID_ARGUMENT and a description of the problem */
    static void validate(ConditionCheckRequest request) {
        if (request.getReturnId().isBlank()) {
            throw invalid("return_id must not be empty");
        }
        if (request.getSku().isBlank()) {
            throw invalid("sku must not be empty");
        }
        ObservedCondition condition = request.getObservedCondition();
        // UNRECOGNIZED = the client sent a number that this version of the .proto does not know
        if (condition == ObservedCondition.OBSERVED_CONDITION_UNSPECIFIED
                || condition == ObservedCondition.UNRECOGNIZED) {
            throw invalid("observed_condition must be set");
        }
        try {
            OffsetDateTime.parse(request.getInspectedAt());
        } catch (DateTimeException e) {
            throw invalid("inspected_at is not an ISO-8601 timestamp: '" + request.getInspectedAt() + "'");
        }
    }

    static StatusRuntimeException invalid(String message) {
        return Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException();
    }
}
