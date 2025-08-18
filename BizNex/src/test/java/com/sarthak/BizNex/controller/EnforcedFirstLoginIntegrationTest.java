package com.sarthak.BizNex.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarthak.BizNex.entity.User;
import com.sarthak.BizNex.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnforcedFirstLoginIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setup(){
        userRepository.deleteAll();
        User u = new User("firstadmin", "firstadmin@example.com");
        u.setUserPassword(passwordEncoder.encode("TempPass1!"));
        u.setUserRole(User.UserRole.ADMIN);
        u.setUserContact("1112223333");
        u.setUserSalary(0);
        u.setMustChangePassword(true);
        userRepository.save(u);
    }

    @Test
    @DisplayName("First login shows mustChangePassword=true then allows self change and clears flag")
    void enforcedFirstLoginFlow() throws Exception {
        // Login with temp password
        String loginJson = "{\"username\":\"firstadmin\",\"userPassword\":\"TempPass1!\"}";
        String authResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(authResponse);
        String token = node.get("accessToken").asText();

        // Change password via first-login endpoint
        String changeJson = "{\"newPassword\":\"NewPass1!\"}";
        mockMvc.perform(patch("/api/v1/auth/first-login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(changeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true));

        // Login again with old password should fail (optional) - expect 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());

        // Login with new password should succeed and flag cleared
        String loginNew = "{\"username\":\"firstadmin\",\"userPassword\":\"NewPass1!\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginNew))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        // Endpoint again returns changed=false
        String secondAuth = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginNew))
                .andReturn().getResponse().getContentAsString();
        String token2 = objectMapper.readTree(secondAuth).get("accessToken").asText();
        mockMvc.perform(patch("/api/v1/auth/first-login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token2)
                        .content(changeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(false));

        // Verify DB flag cleared
        assertThat(userRepository.findByUsername("firstadmin").get().isMustChangePassword()).isFalse();
    }
}

