package com.sarthak.BizNex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarthak.BizNex.dto.ProductDto;
import com.sarthak.BizNex.repository.ProductRepository;
import com.sarthak.BizNex.repository.BillItemRepository;
import com.sarthak.BizNex.repository.BillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    BillItemRepository billItemRepository;

    @Autowired
    BillRepository billRepository;

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("GET /api/v1/products returns 200 with empty content array when no products")
    void getAllProductsEmpty() throws Exception {
        billItemRepository.deleteAll();
        billRepository.deleteAll();
        productRepository.deleteAll();
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("PATCH /api/v1/products/{id} allows zero quantity update")
    void patchProductZeroQuantity() throws Exception {
        billItemRepository.deleteAll();
        billRepository.deleteAll();
        productRepository.deleteAll();
        // create product
        ProductDto create = ProductDto.builder()
                .productName("Widget")
                .productCategory("tools")
                .pricePerItem(10.0)
                .productQuantity(5)
                .build();
        String body = objectMapper.writeValueAsString(create);
        String response = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").exists())
                .andReturn().getResponse().getContentAsString();
        ProductDto created = objectMapper.readValue(response, ProductDto.class);

        // patch to zero quantity
        ProductDto patchDto = ProductDto.builder().productQuantity(0).build();
        mockMvc.perform(patch("/api/v1/products/" + created.getProductId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productQuantity", is(0)));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("POST reactivates soft-deleted product with same name/category and preserves code and id")
    void postReactivatesSoftDeletedProduct() throws Exception {
        billItemRepository.deleteAll();
        billRepository.deleteAll();
        productRepository.deleteAll();

        // Create initial product
        ProductDto create = ProductDto.builder()
                .productName("Widget")
                .productCategory("tools")
                .pricePerItem(10.0)
                .productQuantity(5)
                .productDescription("v1")
                .build();
        String initialResp = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.productCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        ProductDto initial = objectMapper.readValue(initialResp, ProductDto.class);

        // Soft-delete it
        mockMvc.perform(delete("/api/v1/products/" + initial.getProductId()))
                .andExpect(status().isOk());

        // Re-post with updated fields
        ProductDto recreate = ProductDto.builder()
                .productName("Widget")
                .productCategory("tools")
                .pricePerItem(12.5)
                .productQuantity(7)
                .productDescription("v2")
                .build();
        String recreateResp = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(initial.getProductId().intValue())))
                .andExpect(jsonPath("$.productCode", is(initial.getProductCode())))
                .andExpect(jsonPath("$.productName", is("Widget")))
                .andExpect(jsonPath("$.productCategory", is("tools")))
                .andExpect(jsonPath("$.pricePerItem", is(12.5)))
                .andExpect(jsonPath("$.productQuantity", is(7)))
                .andReturn().getResponse().getContentAsString();
        ProductDto reactivated = objectMapper.readValue(recreateResp, ProductDto.class);

        // Ensure it appears in listings
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].productId", is(reactivated.getProductId().intValue())));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("POST duplicate active product returns 409 Conflict")
    void postDuplicateActiveProductReturnsConflict() throws Exception {
        billItemRepository.deleteAll();
        billRepository.deleteAll();
        productRepository.deleteAll();

        ProductDto create = ProductDto.builder()
                .productName("Gadget")
                .productCategory("tools")
                .pricePerItem(20.0)
                .productQuantity(3)
                .build();
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Product already exists")));
    }
}
