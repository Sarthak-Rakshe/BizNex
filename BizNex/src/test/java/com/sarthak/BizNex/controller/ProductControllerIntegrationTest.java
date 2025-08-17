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
}
