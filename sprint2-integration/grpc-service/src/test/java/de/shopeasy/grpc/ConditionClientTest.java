package de.shopeasy.grpc;

import static de.shopeasy.grpc.TestRequests.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.shopeasy.grpc.client.ConditionClient;
import de.shopeasy.grpc.client.ConditionClientException;
import io.grpc.Status;

/** Fehlerbehandlung im Client: Wiederholung bei UNAVAILABLE, verständliche Meldungen sonst. */
class ConditionClientTest extends ServiceTestBase {

    @Test
    void clientWiederholtBeiAusfallUndBekommtDannDasErgebnis() {
        ConditionClient client = new ConditionClient(channel);
        ConditionCheckResponse r = client.checkCondition(
                request("RET-FAIL-1", "SKU-1", ObservedCondition.AS_NEW, true));
        assertEquals(Decision.ACCEPT, r.getDecision());
    }

    @Test
    void clientWirftBeiFalschenDatenOhneWiederholung() {
        ConditionClient client = new ConditionClient(channel);
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.checkCondition(request("", "SKU-1", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.INVALID_ARGUMENT, e.getCode());
        assertTrue(e.getMessage().startsWith("Falsche Daten"));
    }

    @Test
    void clientMeldetDuplikat() {
        ConditionClient client = new ConditionClient(channel);
        client.checkCondition(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true));
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.checkCondition(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.ALREADY_EXISTS, e.getCode());
    }

    @Test
    void clientMeldetUnbekanntesErgebnis() {
        ConditionClient client = new ConditionClient(channel);
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.getCheckResult("RET-X", "SKU-X"));
        assertEquals(Status.Code.NOT_FOUND, e.getCode());
    }

    @Test
    void clientGibtNachDreiVersuchenAuf() {
        // Kein Server hinter dem Kanal: UNAVAILABLE bei jedem Versuch
        io.grpc.ManagedChannel dead = io.grpc.inprocess.InProcessChannelBuilder.forName("gibt-es-nicht")
                .directExecutor().build();
        ConditionClient client = new ConditionClient(dead);
        ConditionClientException e = assertThrows(ConditionClientException.class,
                () -> client.checkCondition(request("RET-1", "SKU-1", ObservedCondition.AS_NEW, true)));
        assertEquals(Status.Code.UNAVAILABLE, e.getCode());
        assertTrue(e.getMessage().contains("3 Versuchen"));
        dead.shutdownNow();
    }
}
