package com.sarthak.BizNex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarthak.BizNex.dto.CustomerDto;
import com.sarthak.BizNex.repository.BillItemRepository;
import com.sarthak.BizNex.repository.BillRepository;
import com.sarthak.BizNex.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerCreditsAggregationIntegrationTest {

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

    private static final AtomicLong CONTACT_SEQ = new AtomicLong(8200000000L);
    private String nextContact(){ return Long.toString(CONTACT_SEQ.getAndIncrement()); }

    @BeforeEach
    void clean(){
        billItemRepository.deleteAll();
        billRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("/customers/credits returns totalCredits & averageCredits across all positive-credit customers, independent of page content")
    void creditsAggregatesExposeGlobalSumAndAverage() throws Exception {
        // positives: 10, 20, 30 -> sum 60 avg 20
        double[] creditsPos = {10.0,20.0,30.0};
        for (double c : creditsPos){
            CustomerDto dto = CustomerDto.builder()
                    .customerName("Pos"+ (int)c)
                    .customerContact(nextContact())
                    .customerEmail("pos"+(int)c+"@mail.com")
                    .customerAddress("Addr")
                    .customerCredits(c)
                    .build();
            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }
        // zeros not counted
        for(int i=0;i<3;i++){
            CustomerDto dto = CustomerDto.builder()
                    .customerName("Zero"+i)
                    .customerContact(nextContact())
                    .customerEmail("zero"+i+"@mail.com")
                    .customerAddress("Addr")
                    .customerCredits(0.0)
                    .build();
            mockMvc.perform(post("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/customers/credits")
                .param("page","0")
                .param("size","2") // only first 2 returned
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalCredits", is(closeTo(60.0,0.0001))))
                .andExpect(jsonPath("$.averageCredits", is(closeTo(20.0,0.0001))));
    }
}

