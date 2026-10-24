package de.shopeasy.grpc;

import static de.shopeasy.grpc.TestRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/** Every error case of the service and the matching gRPC status code. */
class ErrorHandlingTest extends ServiceTestBase {

    private static Status.Code codeOf(StatusRuntimeException e) {
        return e.getStatus().getCode();
    }

    @Test
    void emptyReturnIdIsInvalidArgument() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> stub.checkCondition(request("", "SKU-1", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(e));
        assertTrue(e.getStatus().getDescription().contains("return_id"));
    }

    @Test
    void emptySkuIsInvalidArgument() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> stub.checkCondition(request("RET-1", " ", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(e));
    }

    @Test
    void unspecifiedConditionIsInvalidArgument() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> stub.checkCondition(
                request("RET-1", "SKU-1", ObservedCondition.OBSERVED_CONDITION_UNSPECIFIED, true)));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(e));
    }

    @Test
    void timestampThatIsNotIsoIsInvalidArgument() {
        ConditionCheckRequest bad = request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true).toBuilder()
                .setInspectedAt("yesterday").build();
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> stub.checkCondition(bad));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(e));
    }

    @Test
    void secondIdenticalCheckIsAlreadyExists() {
        ConditionCheckRequest r = request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true);
        stub.checkCondition(r);
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> stub.checkCondition(r));
        assertEquals(Status.Code.ALREADY_EXISTS, codeOf(e));
    }

    @Test
    void sameReturnOtherSkuIsAllowed() {
        stub.checkCondition(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true));
        stub.checkCondition(request("RET-1", "SKU-2", ObservedCondition.DAMAGED, true));
    }

    @Test
    void fetchResult() {
        ConditionCheckResponse created = stub.checkCondition(request("RET-1", "SKU-1", ObservedCondition.DAMAGED, true));
        ConditionCheckResponse loaded = stub.getCheckResult(
                GetCheckResultRequest.newBuilder().setReturnId("RET-1").setSku("SKU-1").build());
        assertEquals(created, loaded);
    }

    @Test
    void unknownResultIsNotFound() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> stub.getCheckResult(
                GetCheckResultRequest.newBuilder().setReturnId("RET-X").setSku("SKU-X").build()));
        assertEquals(Status.Code.NOT_FOUND, codeOf(e));
    }

    @Test
    void fetchWithoutIdsIsInvalidArgument() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> stub.getCheckResult(GetCheckResultRequest.getDefaultInstance()));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(e));
    }

    @Test
    void simulatedOutageOnFirstCallThenItWorks() {
        ConditionCheckRequest r = request("RET-FAIL-1", "SKU-1", ObservedCondition.AS_NEW, true);
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> stub.checkCondition(r));
        assertEquals(Status.Code.UNAVAILABLE, codeOf(e));
        assertEquals(Decision.ACCEPT, stub.checkCondition(r).getDecision());
    }

    @Test
    void slowServiceRunsIntoTheClientDeadline() {
        ConditionCheckRequest slow = request("RET-SLOW-1", "SKU-1", ObservedCondition.AS_NEW, true);
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> stub.withDeadlineAfter(100, java.util.concurrent.TimeUnit.MILLISECONDS).checkCondition(slow));
        assertEquals(Status.Code.DEADLINE_EXCEEDED, codeOf(e));
    }

    @Test
    void batchJudgesAllItems() {
        ConditionCheckBatchResponse response = stub.checkConditionBatch(ConditionCheckBatchRequest.newBuilder()
                .addItems(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true))
                .addItems(request("RET-1", "SKU-2", ObservedCondition.DAMAGED, true))
                .build());
        List<Decision> decisions = response.getResultsList().stream().map(ConditionCheckResponse::getDecision).toList();
        assertEquals(List.of(Decision.ACCEPT, Decision.REJECT), decisions);
    }

    @Test
    void batchWithErrorStoresNothingAndNamesThePosition() {
        ConditionCheckBatchRequest batch = ConditionCheckBatchRequest.newBuilder()
                .addItems(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true))
                .addItems(request("RET-1", "", ObservedCondition.AS_NEW, true))
                .build();
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> stub.checkConditionBatch(batch));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(e));
        assertTrue(e.getStatus().getDescription().startsWith("items[1]"));

        StatusRuntimeException notFound = assertThrows(StatusRuntimeException.class, () -> stub.getCheckResult(
                GetCheckResultRequest.newBuilder().setReturnId("RET-1").setSku("SKU-1").build()));
        assertEquals(Status.Code.NOT_FOUND, codeOf(notFound));
    }

    @Test
    void batchWithDuplicateItemIsAlreadyExists() {
        ConditionCheckRequest r = request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true);
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, () -> stub.checkConditionBatch(
                ConditionCheckBatchRequest.newBuilder().addItems(r).addItems(r).build()));
        assertEquals(Status.Code.ALREADY_EXISTS, codeOf(e));
    }

    @Test
    void emptyBatchIsInvalidArgument() {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                () -> stub.checkConditionBatch(ConditionCheckBatchRequest.getDefaultInstance()));
        assertEquals(Status.Code.INVALID_ARGUMENT, codeOf(e));
    }
}
