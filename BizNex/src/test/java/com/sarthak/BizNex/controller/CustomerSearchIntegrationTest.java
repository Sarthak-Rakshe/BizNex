package com.sarthak.BizNex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarthak.BizNex.dto.CustomerDto;
import com.sarthak.BizNex.repository.CustomerRepository;
import com.sarthak.BizNex.repository.BillRepository;
import com.sarthak.BizNex.repository.BillItemRepository;
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

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerSearchIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    BillItemRepository billItemRepository;

    @Autowired
    BillRepository billRepository;

    private static final AtomicLong CONTACT_SEQ = new AtomicLong(8000000000L);

    private String nextContact(){
        return Long.toString(CONTACT_SEQ.getAndIncrement());
    }

    @BeforeEach
    void setup(){
        // Clean in dependency order: bill items -> bills -> customers
        billItemRepository.deleteAll();
        billRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("Customer search returns totalElements across full dataset, not just page size")
    void customerSearchPagedAggregatesProperly() throws Exception {
        // create 12 matching + 5 non-matching customers
        for(int i=0;i<12;i++){
            CustomerDto dto = CustomerDto.builder()
                    .customerName("Alpha Test" + i)
                    .customerContact(nextContact())
                    .customerEmail("alpha"+i+"@mail.com")
                    .customerAddress("Addr")
                    .customerCredits( i % 2 ==0 ? 50.0 : 0.0)
                    .build();
            mockMvc.perform(post("/api/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }
        for(int i=0;i<5;i++){
            CustomerDto dto = CustomerDto.builder()
                    .customerName("Beta User" + i)
                    .customerContact(nextContact())
                    .customerEmail("beta"+i+"@mail.com")
                    .customerAddress("Addr")
                    .customerCredits(0.0)
                    .build();
            mockMvc.perform(post("/api/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/customers/search")
                        .param("query","alpha")
                        .param("page","0")
                        .param("size","5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements", is(12)))
                .andExpect(jsonPath("$.totalPages", is(3)));

        // credits search should only include those with credits>0 among matches (6 even indices)
        mockMvc.perform(get("/api/v1/customers/credits/search")
                        .param("query","alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(6)));
    }
}
