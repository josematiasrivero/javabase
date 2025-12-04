package com.adavance.javabase.invoicing;

import com.adavance.javabase.invoicing.model.Invoice;
import com.adavance.javabase.invoicing.model.Item;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class InvoicingTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        this.objectMapper = new ObjectMapper();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void testInvoicingFlow() throws Exception {
        // 1. Create Item
        Item item = new Item();
        item.setName("Test Item");
        item.setCode("ITM-001");
        item.setPrice(new BigDecimal("100.00"));

        MvcResult itemResult = mockMvc.perform(post("/rest/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andReturn();

        String itemUuid = (String) objectMapper.readValue(itemResult.getResponse().getContentAsString(), Map.class).get("uuid");

        // 2. Create Invoice with Line referencing Item
        // Note: GenericRestController expects "itemId": "uuid" in the map for relationships
        Map<String, Object> lineData = Map.of(
                "quantity", 5,
                "itemId", itemUuid
        );

        Map<String, Object> invoiceData = Map.of(
                "number", "INV-2024-001",
                "date", Instant.now().toString(),
                "lines", List.of(lineData)
        );

        MvcResult invoiceResult = mockMvc.perform(post("/rest/invoice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines[0].quantity").value(5))
                .andReturn();

        String invoiceUuid = (String) objectMapper.readValue(invoiceResult.getResponse().getContentAsString(), Map.class).get("uuid");

        // 3. Get Invoice by UUID
        mockMvc.perform(get("/rest/invoice/" + invoiceUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("INV-2024-001"));

        // 4. Update Invoice
        Map<String, Object> updateData = Map.of(
                "number", "INV-2024-001-UPDATED"
        );
        
        mockMvc.perform(put("/rest/invoice/" + invoiceUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("INV-2024-001-UPDATED"));

        // 5. Delete Invoice
        mockMvc.perform(delete("/rest/Invoice/" + invoiceUuid))
                .andExpect(status().isNoContent());

        // 6. Verify Deletion
        mockMvc.perform(get("/rest/Invoice/" + invoiceUuid))
                .andExpect(status().isNotFound());
    }
}
