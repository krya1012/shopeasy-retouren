package de.shopeasy.grpc.client;

import io.grpc.Status;

/** Fehler beim Aufruf des Service, mit gRPC-Statuscode und verständlicher Meldung. */
public class ConditionClientException extends RuntimeException {

    private final Status.Code code;

    public ConditionClientException(Status.Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Status.Code getCode() {
        return code;
    }
}
