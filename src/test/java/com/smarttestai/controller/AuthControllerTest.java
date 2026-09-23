package com.smarttestai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttestai.dto.request.LoginRequest;
import com.smarttestai.dto.request.RegisterRequest;
import com.smarttestai.dto.response.AuthResponse;
import com.smarttestai.dto.response.UserResponse;
import com.smarttestai.entity.Role;
import com.smarttestai.exception.GlobalExceptionHandler;
import com.smarttestai.exception.UserAlreadyExistsException;
import com.smarttestai.security.CustomUserDetailsService;
import com.smarttestai.security.JwtAuthenticationEntryPoint;
import com.smarttestai.security.JwtAuthenticationFilter;
import com.smarttestai.security.JwtService;
import com.smarttestai.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/v1/auth/register - 201 Created on valid payload")
    void register_Valid_Returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("Password123!")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("mocked.jwt.token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(UserResponse.builder()
                        .id(1L)
                        .name("John Doe")
                        .email("john@example.com")
                        .role(Role.ROLE_USER)
                        .createdAt(Instant.now())
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("mocked.jwt.token")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is("john@example.com")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - 400 Bad Request on invalid email")
    void register_InvalidEmail_Returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("invalid-email")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - 409 Conflict on duplicate email")
    void register_DuplicateEmail_Returns409() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("Password123!")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Email is already registered: john@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - 200 OK on valid credentials")
    void login_Valid_Returns200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("Password123!")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .token("mocked.jwt.token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(UserResponse.builder()
                        .id(1L)
                        .email("john@example.com")
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("mocked.jwt.token")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - 401 Unauthorized on invalid credentials")
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("INVALID_CREDENTIALS")));
    }
}
