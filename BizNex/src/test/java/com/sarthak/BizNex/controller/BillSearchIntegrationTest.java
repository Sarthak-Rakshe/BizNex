package com.sarthak.BizNex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarthak.BizNex.dto.BillDto;
import com.sarthak.BizNex.dto.BillItemDto;
import com.sarthak.BizNex.dto.CustomerDto;
import com.sarthak.BizNex.dto.ProductDto;
import com.sarthak.BizNex.entity.Bill;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BillSearchIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private Long customerId;
    private Long productAId;
    private Long productBId;

    private String uniqueContact(){
        long v = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        return Long.toString(v);
    }

    @BeforeEach
    void setup() throws Exception {
        // create one customer with unique contact
        CustomerDto c = CustomerDto.builder()
                .customerName("Search Customer")
                .customerContact(uniqueContact())
                .customerEmail("sc@test.com")
                .customerAddress("Addr")
                .customerCredits(0.0)
                .build();
        String cResp = mockMvc.perform(post("/api/v1/customers")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("u").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        customerId = objectMapper.readValue(cResp, CustomerDto.class).getCustomerId();

        // create two products (admin) with unique productCode values
        ProductDto pA = ProductDto.builder().productName("Widget Alpha").productCategory("cat").pricePerItem(10.0).productQuantity(100).productCode("ALPHA-"+System.nanoTime()).build();
        ProductDto pB = ProductDto.builder().productName("Widget Beta").productCategory("cat").pricePerItem(5.0).productQuantity(100).productCode("BETA-"+System.nanoTime()).build();
        String pAResp = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String pBResp = mockMvc.perform(post("/api/v1/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        productAId = objectMapper.readValue(pAResp, ProductDto.class).getProductId();
        productBId = objectMapper.readValue(pBResp, ProductDto.class).getProductId();

        // create 7 bills
        for(int i=0;i<7;i++){
            BillItemDto item = BillItemDto.builder()
                    .billItemProduct(ProductDto.builder().productId(i % 2 ==0 ? productAId : productBId).build())
                    .billItemQuantity(1)
                    .build();
            BillDto bill = BillDto.builder()
                    .customer(CustomerDto.builder().customerId(customerId).build())
                    .billItems(List.of(item))
                    .billStatus(Bill.BillStatus.COMPLETE)
                    .paymentMethod(i % 2 ==0 ? Bill.PaymentMethod.CASH : Bill.PaymentMethod.ONLINE)
                    .build();
            mockMvc.perform(post("/api/v1/billing")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("b").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bill)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Bill search returns totalElements across full dataset and supports empty query fallback")
    void billSearchPagedAggregatesProperly() throws Exception {
        // Non-empty query by customer name substring 'search'
        mockMvc.perform(get("/api/v1/billing/search")
                        .param("query","search")
                        .param("page","0")
                        .param("size","3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(7)))
                .andExpect(jsonPath("$.totalPages", is(3)));

        // Empty query fallback lists all bills
        mockMvc.perform(get("/api/v1/billing/search")
                        .param("query"," ")
                        .param("size","5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(7)));
    }
}
