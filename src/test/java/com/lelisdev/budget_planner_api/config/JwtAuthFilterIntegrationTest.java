package com.lelisdev.budget_planner_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lelisdev.budget_planner_api.dtos.CredentialsDto;
import com.lelisdev.budget_planner_api.dtos.UserLoginDto;
import com.lelisdev.budget_planner_api.models.User;
import com.lelisdev.budget_planner_api.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre a distinção de desenho do JwtAuthFilter: pedidos GET usam validação leve
 * (só claims do token), pedidos de escrita usam validação forte (confirma o user na BD).
 * Por isso o cenário de "user apagado" usa POST — um GET com o mesmo token continuaria
 * autenticado por desenho, não por bug.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(JwtAuthFilterIntegrationTest.TestPingConfig.class)
class JwtAuthFilterIntegrationTest {

    private static final String USERNAME = "jwt-filter-test-user";
    private static final String PASSWORD = "S3nhaDeTeste!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanup() {
        userRepository.findByUsername(USERNAME).ifPresent(userRepository::delete);
    }

    @Test
    void validLogin_thenAuthenticatedWriteRequest_returns200() throws Exception {
        userRepository.save(testUser());

        String token = login();

        mockMvc.perform(post("/api/_test/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenOfDeletedUser_onWriteRequest_returnsUnauthorized() throws Exception {
        userRepository.save(testUser());

        String token = login();

        userRepository.findByUsername(USERNAME).ifPresent(userRepository::delete);

        mockMvc.perform(post("/api/_test/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private User testUser() {
        return User.builder()
                .name("Test User")
                .username(USERNAME)
                .email("jwt-filter-test-user@example.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .build();
    }

    private String login() throws Exception {
        String body = objectMapper.writeValueAsString(new CredentialsDto(USERNAME, PASSWORD.toCharArray()));

        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        UserLoginDto userDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserLoginDto.class);
        return userDto.getToken();
    }

    @TestConfiguration
    static class TestPingConfig {

        @RestController
        static class PingController {

            @PostMapping("/api/_test/ping")
            public ResponseEntity<String> ping() {
                return ResponseEntity.ok("pong");
            }
        }
    }
}
