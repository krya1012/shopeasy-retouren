package de.shopeasy.workflow.returns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Tests the HTTP calls and the translation of errors with a fake server (no network). */
class RestReturnsApiTest {

    private static final String REPORT = """
            {"returnId":"RET-2026-00457","orderId":"ORD-2026-118932","marketplace":"ZALANDO","status":"NEW",
             "reason":"SIZE_TOO_SMALL","reasonText":"Passt nicht","requestedAt":"2026-09-18T09:14:00Z",
             "updatedAt":"2026-09-18T09:14:00Z",
             "items":[{"sku":"SH-BLZ-042-M","productName":"Blazer Slim Fit","quantity":1,"unitPrice":89.9,"currency":"EUR"}],
             "customer":{"customerId":"CUST-88213","email":"j.mueller@example.com"},"returnLabelUrl":null,"unknown":1}
            """;

    private MockRestServiceServer server;
    private RestReturnsApi api;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://mock");
        server = MockRestServiceServer.bindTo(builder).build();
        api = new RestReturnsApi(builder.build());
    }

    @Test
    void getReturnReadsTheFormatOfTheMock() {
        server.expect(requestTo("http://mock/api/v1/returns/RET-2026-00457")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(REPORT, MediaType.APPLICATION_JSON));

        ReturnReport report = api.getReturn("RET-2026-00457");

        assertThat(report.returnId()).isEqualTo("RET-2026-00457");
        assertThat(report.marketplace()).isEqualTo("ZALANDO");
        assertThat(report.status()).isEqualTo("NEW");
        assertThat(report.requestedAt()).hasToString("2026-09-18T09:14:00Z");
        assertThat(report.items().get(0).unitPrice()).isEqualByComparingTo("89.9");
        assertThat(report.customer().email()).isEqualTo("j.mueller@example.com");
        assertThat(report.returnLabelUrl()).isNull();
    }

    @Test
    void unknownReturnIsAPermanentError() {
        server.expect(requestTo("http://mock/api/v1/returns/RET-NONE"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":404,\"detail\":\"Return RET-NONE not found.\"}"));

        assertThatThrownBy(() -> api.getReturn("RET-NONE")).isInstanceOfSatisfying(ReturnsApiException.class, e -> {
            assertThat(e.isPermanent()).isTrue();
            assertThat(e.httpStatus()).isEqualTo(404);
            assertThat(e.getMessage()).contains("not found");
        });
    }

    @Test
    void createReturnSendsTheBodyAndTheIdempotencyKey() {
        server.expect(requestTo("http://mock/api/v1/returns")).andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "case-4711"))
                .andExpect(jsonPath("$.orderId").value("ORD-2026-119001"))
                .andExpect(jsonPath("$.customerEmail").value("a@example.com"))
                .andExpect(jsonPath("$.items[0].sku").value("SH-BLZ-042-M"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.reason").value("SIZE_TOO_SMALL"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(REPORT));

        ReturnReport report = api.createReturn(new CreateReturnRequest("ORD-2026-119001", "a@example.com",
                List.of(new CreateReturnRequest.Line("SH-BLZ-042-M", 1)), "SIZE_TOO_SMALL", null), "case-4711");

        assertThat(report.returnId()).isEqualTo("RET-2026-00457");
        server.verify();
    }

    @Test
    void invalidReturnIsAPermanentError() {
        server.expect(requestTo("http://mock/api/v1/returns"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"detail\":\"The field 'orderId' is missing.\"}"));

        assertThatThrownBy(() -> api.createReturn(new CreateReturnRequest("", "a@example.com", List.of(), null, null), "k"))
                .isInstanceOfSatisfying(ReturnsApiException.class, e -> assertThat(e.isPermanent()).isTrue());
    }

    @Test
    void unknownOrderIsEmptyAnyOtherErrorIsThrown() {
        server.expect(requestTo("http://mock/api/v1/orders/ORD-NONE")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://mock/api/v1/orders/ORD-X")).andRespond(withServerError());

        assertThat(api.findOrder("ORD-NONE")).isEmpty();
        assertThatThrownBy(() -> api.findOrder("ORD-X"))
                .isInstanceOfSatisfying(ReturnsApiException.class, e -> assertThat(e.isPermanent()).isFalse());
    }

    @Test
    void orderIsRead() {
        server.expect(requestTo("http://mock/api/v1/orders/ORD-1")).andRespond(withSuccess("""
                {"orderId":"ORD-1","marketplace":null,"orderedAt":"2026-11-15T10:00:00Z","deliveredAt":"2026-11-19T10:00:00Z",
                 "items":[{"sku":"A","productName":"A","quantity":2,"unitPrice":10.5,"currency":"EUR"}],
                 "customer":{"customerId":"C","email":"a@example.com"},"totalAmount":21.0,"currency":"EUR"}
                """, MediaType.APPLICATION_JSON));

        Order order = api.findOrder("ORD-1").orElseThrow();

        assertThat(order.deliveredAt()).hasToString("2026-11-19T10:00:00Z");
        assertThat(order.marketplace()).isNull();
        assertThat(order.items()).hasSize(1);
    }

    @Test
    void listOfNewReturnsOnlyKeepsMarketplaceReturns() {
        String own = REPORT.replace("\"marketplace\":\"ZALANDO\"", "\"marketplace\":null")
                .replace("RET-2026-00457", "RET-2026-00463");
        server.expect(requestTo("http://mock/api/v1/returns?status=NEW")).andRespond(
                withSuccess("{\"data\":[" + REPORT + "," + own + "]}", MediaType.APPLICATION_JSON));

        List<ReturnReport> list = api.listNewMarketplaceReturns();

        assertThat(list).extracting(ReturnReport::returnId).containsExactly("RET-2026-00457");
    }

    @Test
    void updateStatusSendsStatusAndExpectedStatus() {
        server.expect(requestTo("http://mock/api/v1/returns/RET-1")).andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"status\":\"APPROVED\",\"expectedStatus\":\"NEW\"}"))
                .andRespond(withSuccess(REPORT, MediaType.APPLICATION_JSON));

        api.updateStatus("RET-1", "APPROVED", "NEW");

        server.verify();
    }

    @Test
    void conflictIsAPermanentError() {
        server.expect(requestTo("http://mock/api/v1/returns/RET-1"))
                .andRespond(withStatus(HttpStatus.CONFLICT).body("{\"detail\":\"Status is REJECTED, not the expected NEW\"}"));

        assertThatThrownBy(() -> api.updateStatus("RET-1", "APPROVED", "NEW"))
                .isInstanceOfSatisfying(ReturnsApiException.class, e -> {
                    assertThat(e.isPermanent()).isTrue();
                    assertThat(e.httpStatus()).isEqualTo(409);
                    assertThat(e.getMessage()).contains("not the expected NEW");
                });
    }

    @Test
    void serverErrorAndNoAnswerAreTemporaryErrors() {
        server.expect(requestTo("http://mock/api/v1/returns/RET-1")).andRespond(withServerError());
        server.expect(requestTo("http://mock/api/v1/returns/RET-1")).andRespond(withException(new IOException("refused")));

        assertThatThrownBy(() -> api.getReturn("RET-1")).isInstanceOfSatisfying(ReturnsApiException.class,
                e -> assertThat(e.isPermanent()).isFalse());
        assertThatThrownBy(() -> api.getReturn("RET-1")).isInstanceOfSatisfying(ReturnsApiException.class, e -> {
            assertThat(e.isPermanent()).isFalse();
            assertThat(e.httpStatus()).isEqualTo(0);
            assertThat(e.getMessage()).contains("not reachable");
        });
    }
}
