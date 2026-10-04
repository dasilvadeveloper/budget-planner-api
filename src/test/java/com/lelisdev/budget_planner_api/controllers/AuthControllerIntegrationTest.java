package com.lelisdev.budget_planner_api.controllers;

import com.lelisdev.budget_planner_api.models.User;
import com.lelisdev.budget_planner_api.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o contrato público desta versão: POST /api/login e GET /actuator/health abertos,
 * tudo o resto fechado, e erros de login sempre genéricos (E02) seja qual for a causa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final String USERNAME = "auth-controller-test-user";
    private static final String PASSWORD = "S3nhaDeTeste!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanup() {
        userRepository.findByUsername(USERNAME).ifPresent(userRepository::delete);
    }

    @Test
    void validCredentials_returnsUserAndToken() throws Exception {
        userRepository.save(testUser(true));

        login(USERNAME, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void wrongPassword_returnsE02() throws Exception {
        userRepository.save(testUser(true));

        login(USERNAME, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    @Test
    void unknownUser_returnsE02() throws Exception {
        login("user-that-does-not-exist", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    @Test
    void inactiveUser_returnsE02() throws Exception {
        userRepository.save(testUser(false));

        login(USERNAME, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    @Test
    void blankUsername_returnsE04() throws Exception {
        login("", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E04"));
    }

    @Test
    void malformedBody_returnsE04() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E04"));
    }

    // um token inválido que o cliente ainda anexe não pode impedir um novo login
    @Test
    void staleTokenOnLogin_isIgnored() throws Exception {
        userRepository.save(testUser(true));

        mockMvc.perform(post("/api/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(USERNAME, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void health_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void protectedEndpointWithoutToken_returnsE05() throws Exception {
        mockMvc.perform(get("/api/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E05"));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(username, password)));
    }

    private String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private User testUser(boolean active) {
        return User.builder()
                .name("Test User")
                .username(USERNAME)
                .email("auth-controller-test-user@example.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .active(active)
                .build();
    }
}
