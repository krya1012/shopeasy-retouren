package de.shopeasy.grpc;

import static de.shopeasy.grpc.TestRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.shopeasy.grpc.client.ConditionClient;
import de.shopeasy.grpc.client.ConditionClientException;
import io.grpc.Status;

/** Error handling in the client: retry on UNAVAILABLE, readable messages otherwise. */
class ConditionClientTest extends ServiceTestBase {

    @Test
    void clientRetriesOnOutageAndThenGetsTheResult() {
        ConditionClient client = new ConditionClient(channel);
        ConditionCheckResponse r = client.checkCondition(
                request("RET-FAIL-1", "SKU-1", ObservedCondition.AS_NEW, true));
        assertEquals(Decision.ACCEPT, r.getDecision());
    }

    @Test
    void clientThrowsOnInvalidDataWithoutRetry() {
        ConditionClient client = new ConditionClient(channel);
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.checkCondition(request("", "SKU-1", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.INVALID_ARGUMENT, e.getCode());
        assertTrue(e.getMessage().startsWith("Invalid data"));
    }

    @Test
    void clientReportsDuplicate() {
        ConditionClient client = new ConditionClient(channel);
        client.checkCondition(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true));
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.checkCondition(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.ALREADY_EXISTS, e.getCode());
    }

    @Test
    void clientReportsUnknownResult() {
        ConditionClient client = new ConditionClient(channel);
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.getCheckResult("RET-X", "SKU-X"));
        assertEquals(Status.Code.NOT_FOUND, e.getCode());
    }

    @Test
    void clientReportsMissedDeadlineAndDoesNotRetry() {
        ConditionClient client = new ConditionClient(channel, 100);
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.checkCondition(request("RET-SLOW-1", "SKU-1", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.DEADLINE_EXCEEDED, e.getCode());
        assertTrue(e.getMessage().startsWith("Timeout"));
    }

    @Test
    void clientGivesUpAfterThreeAttempts() {
        // No server behind the channel: UNAVAILABLE on every attempt
        io.grpc.ManagedChannel dead = io.grpc.inprocess.InProcessChannelBuilder.forName("does-not-exist")
                .build();
        ConditionClient client = new ConditionClient(dead);
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.checkCondition(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.UNAVAILABLE, e.getCode());
        assertTrue(e.getMessage().contains("3 attempts"));
        dead.shutdownNow();
    }
}
