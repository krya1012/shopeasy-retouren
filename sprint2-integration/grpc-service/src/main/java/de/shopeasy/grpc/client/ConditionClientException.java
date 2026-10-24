package de.shopeasy.grpc.client;

import io.grpc.Status;

/** Error when calling the service, with the gRPC status code and a readable message. */
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
