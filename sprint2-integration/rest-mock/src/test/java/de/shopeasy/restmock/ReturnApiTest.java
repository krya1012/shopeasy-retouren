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

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReturnApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listeOhneFilterLiefertAlleRetouren() throws Exception {
        mvc.perform(get("/api/v1/returns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void filterNachStatusUndMarktplatz() throws Exception {
        mvc.perform(get("/api/v1/returns").param("status", "NEW").param("marketplace", "ZALANDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].marketplace").value("ZALANDO"))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void ungueltigerFilterIstEinFehler() throws Exception {
        mvc.perform(get("/api/v1/returns").param("marketplace", "EBAY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void einzelneRetoureHatDasFormatAusDerAufgabe() throws Exception {
        mvc.perform(get("/api/v1/returns/RET-2026-00457"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnId").value("RET-2026-00457"))
                .andExpect(jsonPath("$.orderId").value("ZAL-100457"))
                .andExpect(jsonPath("$.marketplace").value("ZALANDO"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.reason").value("SIZE_TOO_SMALL"))
                .andExpect(jsonPath("$.reasonText").isString())
                .andExpect(jsonPath("$.requestedAt").value("2026-10-12T08:15:00Z"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.items[0].sku").value("SH-BLZ-042-M"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(89.90))
                .andExpect(jsonPath("$.items[0].currency").value("EUR"))
                .andExpect(jsonPath("$.customer.customerId").exists())
                .andExpect(jsonPath("$.customer.email").exists())
                .andExpect(jsonPath("$.returnLabelUrl").doesNotExist());
    }

    @Test
    void unbekannteRetoureGibt404() throws Exception {
        mvc.perform(get("/api/v1/returns/RET-GIBT-ES-NICHT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void bestellungUeberOrderId() throws Exception {
        mvc.perform(get("/api/v1/orders/ZAL-100458"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ZAL-100458"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(84.85));
    }

    @Test
    void unbekannteBestellungGibt404() throws Exception {
        mvc.perform(get("/api/v1/orders/GIBT-ES-NICHT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusAendernSetztNeuenStatusUndUpdatedAt() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-2026-00457")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/api/v1/returns/RET-2026-00457"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.updatedAt").value(org.hamcrest.Matchers.not("2026-10-12T08:15:00Z")));
    }

    @Test
    void ungueltigerStatusGibt400() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-2026-00457")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"QUATSCH\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fehlenderStatusGibt400() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-2026-00457")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusAendernBeiUnbekannterRetoureGibt404() throws Exception {
        mvc.perform(patch("/api/v1/returns/RET-GIBT-ES-NICHT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isNotFound());
    }
}
