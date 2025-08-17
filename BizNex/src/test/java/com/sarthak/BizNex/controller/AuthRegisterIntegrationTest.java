package com.sarthak.BizNex.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarthak.BizNex.entity.User;
import com.sarthak.BizNex.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // ensure isolation
class AuthRegisterIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Admin token allows registering a new USER account")
    void adminCanRegisterUser() throws Exception {
        userRepository.deleteAll();
        // Seed an admin user directly
        User admin = new User("admin","admin@example.com");
        admin.setUserPassword(passwordEncoder.encode("AdminPass1!"));
        admin.setUserRole(User.UserRole.ADMIN);
        admin.setUserContact("1234567890");
        admin.setUserSalary(100000);
        userRepository.save(admin);

        // Login to get token
        String loginJson = "{\"username\":\"admin\",\"userPassword\":\"AdminPass1!\"}";
        String authResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(authResponse).get("accessToken").asText();

        // Register new user
        String registerJson = "{" +
                "\"username\":\"newuser\"," +
                "\"userEmail\":\"newuser@example.com\"," +
                "\"userPassword\":\"UserPass1!\"," +
                "\"userRole\":\"USER\"," +
                "\"userContact\":\"0987654321\"," +
                "\"userSalary\":50000}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.userRole").value("USER"));
    }
}
