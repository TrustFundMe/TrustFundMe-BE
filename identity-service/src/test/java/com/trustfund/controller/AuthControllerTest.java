package com.trustfund.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustfund.filter.JwtAuthenticationFilter;
import com.trustfund.model.User;
import com.trustfund.model.request.LoginRequest;
import com.trustfund.model.request.RegisterRequest;
import com.trustfund.model.response.AuthResponse;
import com.trustfund.model.response.UserInfo;
import com.trustfund.service.interfaceServices.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-that-is-at-least-32-characters-long-for-testing",
    "jwt.expiration=86400000",
    "jwt.refresh-expiration=604800000",
    "app.otp.expiration=10",
    "app.google.client-id=test-google-client-id"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ─── /register ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("returns 201 Created when registration succeeds")
        void returns201OnSuccess() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");
            request.setPassword("password123");
            request.setFullName("Test User");

            UserInfo userInfo = UserInfo.builder()
                    .id(1L)
                    .email("test@example.com")
                    .fullName("Test User")
                    .role(User.Role.USER)
                    .isActive(true)
                    .verified(false)
                    .build();

            AuthResponse response = AuthResponse.builder()
                    .accessToken("access_token")
                    .refreshToken("refresh_token")
                    .user(userInfo)
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("access_token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh_token"))
                    .andExpect(jsonPath("$.user.email").value("test@example.com"));
        }

        @Test
        @DisplayName("returns 400 Bad Request when email is blank")
        void returns400WhenEmailBlank() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("");
            request.setPassword("password123");
            request.setFullName("Test User");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when password is too short")
        void returns400WhenPasswordShort() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");
            request.setPassword("12345"); // less than 6 chars
            request.setFullName("Test User");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── /login ───────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 OK with tokens when credentials are valid")
        void returns200OnValidCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("correct_password");

            UserInfo userInfo = UserInfo.builder()
                    .id(1L)
                    .email("user@example.com")
                    .fullName("User")
                    .role(User.Role.USER)
                    .isActive(true)
                    .verified(false)
                    .build();

            AuthResponse response = AuthResponse.builder()
                    .accessToken("access_token")
                    .refreshToken("refresh_token")
                    .user(userInfo)
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access_token"))
                    .andExpect(jsonPath("$.user.email").value("user@example.com"));
        }

        @Test
        @DisplayName("returns 400 Bad Request when email is missing")
        void returns400WhenEmailMissing() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setPassword("password");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 Bad Request when password is missing")
        void returns400WhenPasswordMissing() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("user@example.com");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── /refresh ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshToken {

        @Test
        @DisplayName("returns 200 OK with new tokens")
        void returnsNewTokens() throws Exception {
            AuthResponse response = AuthResponse.builder()
                    .accessToken("new_access_token")
                    .refreshToken("new_refresh_token")
                    .user(UserInfo.builder().id(1L).email("user@example.com").build())
                    .build();

            when(authService.refreshToken("some_refresh_token")).thenReturn(response);

            mockMvc.perform(post("/api/auth/refresh")
                            .header("Refresh-Token", "some_refresh_token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new_access_token"))
                    .andExpect(jsonPath("$.refreshToken").value("new_refresh_token"));
        }
    }
}
