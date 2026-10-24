package de.shopeasy.restmock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests the REST API without a real network. {@code @SpringBootTest} starts the whole application,
 * {@code @AutoConfigureMockMvc} gives us a {@link MockMvc} object that sends fake HTTP requests directly into it.
 * {@code @DirtiesContext} restarts the application after every test, so a PATCH in one test cannot change the
 * data of the next test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReturnApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listWithoutFilterReturnsAllReturns() throws Exception {
        mvc.perform(get("/api/v1/returns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));
    }

    @Test
    void filterByStatusAndMarketplace() throws Exception {
        mvc.perform(get("/api/v1/returns").param("status", "NEW").param("marketplace", "ZALANDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].marketplace").value("ZALANDO"))
                .andExpect(jsonPath("$.data[0].status").value("NEW"));
    }

    @Test
    void invalidFilterIsAnError() throws Exception {
        mvc.perform(get("/api/v1/returns").param("marketplace", "EBAY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void singleReturnHasTheFormatFromTheTask() throws Exception {
        mvc.perform(get("/api/v1/returns/RET-2026-00457"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnId").value("RET-2026-00457"))
                .andExpect(jsonPath("$.orderId").value("ORD-2026-118932"))
                .andExpect(jsonPath("$.marketplace").value("ZALANDO"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.reason").value("SIZE_TOO_SMALL"))
                .andExpect(jsonPath("$.reasonText").isString())
                .andExpect(jsonPath("$.requestedAt").value("2026-09-18T09:14:00Z"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.items[0].sku").value("SH-BLZ-042-M"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(89.9))
                .andExpect(jsonPath("$.items[0].currency").value("EUR"))
                .andExpect(jsonPath("$.customer.customerId").exists())
                .andExpect(jsonPath("$.customer.email").exists())
                .andExpect(jsonPath("$.returnLabelUrl").doesNotExist());
    }

    /** The example from slide 9: list inside the envelope "data", return RET-2026-00457 with exactly these values. */
    @Test
    void listMatchesTheExampleFromTheTask() throws Exception {
        mvc.perform(get("/api/v1/returns").param("status", "NEW").param("marketplace", "ZALANDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].returnId").value("RET-2026-00457"))
                .andExpect(jsonPath("$.data[0].orderId").value("ORD-2026-118932"))
                .andExpect(jsonPath("$.data[0].marketplace").value("ZALANDO"))
                .andExpect(jsonPath("$.data[0].status").value("NEW"))
                .andExpect(jsonPath("$.data[0].reason").value("SIZE_TOO_SMALL"))
                .andExpect(jsonPath("$.data[0].reasonText").value("Passt leider nicht, eine Nummer kleiner bitte."))
                .andExpect(jsonPath("$.data[0].requestedAt").value("2026-09-18T09:14:00Z"))
                .andExpect(jsonPath("$.data[0].updatedAt").value("2026-09-18T09:14:00Z"))
                .andExpect(jsonPath("$.data[0].items[0].sku").value("SH-BLZ-042-M"))
                .andExpect(jsonPath("$.data[0].items[0].productName").value("Blazer Slim Fit"))
                .andExpect(jsonPath("$.data[0].items[0].quantity").value(1))
                .andExpect(jsonPath("$.data[0].items[0].unitPrice").value(89.9))
                .andExpect(jsonPath("$.data[0].items[0].currency").value("EUR"))
                .andExpect(jsonPath("$.data[0].customer.customerId").value("CUST-88213"))
                .andExpect(jsonPath("$.data[0].customer.email").value("j.mueller@example.com"))
                .andExpect(jsonPath("$.data[0].returnLabelUrl").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void unknownReturnGives404() throws Exception {
        mvc.perform(get("/api/v1/returns/RET-DOES-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void orderByOrderId() throws Exception {
        mvc.perform(get("/api/v1/orders/ORD-2026-118940"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORD-2026-118940"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(84.85));
    }

    @Test
    void unknownOrderGives404() throws Exception {
        mvc.perform(get("/api/v1/orders/DOES-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changingStatusSetsNewStatusAndUpdatedAt() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-2026-00457")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/api/v1/returns/RET-2026-00457"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.updatedAt").value(org.hamcrest.Matchers.not("2026-09-18T09:14:00Z")));
    }

    @Test
    void invalidStatusGives400() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-2026-00457")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NONSENSE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingStatusGives400() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-2026-00457")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changingStatusOfUnknownReturnGives404() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-DOES-NOT-EXIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void forbiddenStatusChangeGives409AndChangesNothing() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-2026-00462")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/returns/RET-2026-00462"))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void sameStatusIsAllowedAndChangesNothing() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-2026-00459")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.updatedAt").value("2026-09-20T14:05:00Z"));
    }

    @Test
    void openApiDescriptionListsAllEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ShopEasy Return Reports (Mock)"))
                .andExpect(jsonPath("$.paths['/api/v1/returns']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/returns/{returnId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/orders/{orderId}']").exists());
    }
}
