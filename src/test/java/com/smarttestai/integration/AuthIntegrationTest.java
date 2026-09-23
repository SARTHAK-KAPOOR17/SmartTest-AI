package com.smarttestai.integration;

import com.smarttestai.dto.request.LoginRequest;
import com.smarttestai.dto.request.RegisterRequest;
import com.smarttestai.dto.response.AuthResponse;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.User;
import com.smarttestai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    @BeforeEach
    void cleanUp() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-End Auth test: Register -> Password Hashed -> Login -> Duplicate -> Invalid Creds")
    void fullAuthLifecycleTest() {
        // 1. Register User
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Integration User")
                .email("integration@smarttestai.com")
                .password("Password123!")
                .build();

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                getAuthUrl() + "/register",
                registerRequest,
                AuthResponse.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getToken()).isNotBlank();
        assertThat(registerResponse.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(registerResponse.getBody().getUser().getEmail()).isEqualTo("integration@smarttestai.com");
        assertThat(registerResponse.getBody().getUser().getRole()).isEqualTo(Role.ROLE_USER);

        // Verify password is encrypted in database
        User savedUser = userRepository.findByEmail("integration@smarttestai.com").orElseThrow();
        assertThat(savedUser.getPassword()).startsWith("$2a$");
        assertThat(savedUser.getPassword()).isNotEqualTo("Password123!");

        // 2. Login User
        LoginRequest loginRequest = LoginRequest.builder()
                .email("integration@smarttestai.com")
                .password("Password123!")
                .build();

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                getAuthUrl() + "/login",
                loginRequest,
                AuthResponse.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getToken()).isNotBlank();
        assertThat(loginResponse.getBody().getUser().getEmail()).isEqualTo("integration@smarttestai.com");

        // 3. Duplicate Registration Fails (409 Conflict)
        ResponseEntity<ErrorResponse> duplicateResponse = restTemplate.postForEntity(
                getAuthUrl() + "/register",
                registerRequest,
                ErrorResponse.class
        );

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateResponse.getBody()).isNotNull();
        assertThat(duplicateResponse.getBody().getError()).isEqualTo("EMAIL_ALREADY_EXISTS");

        // 4. Invalid Password Fails (401 Unauthorized)
        LoginRequest invalidLogin = LoginRequest.builder()
                .email("integration@smarttestai.com")
                .password("WrongPassword123!")
                .build();

        ResponseEntity<ErrorResponse> invalidLoginResponse = restTemplate.postForEntity(
                getAuthUrl() + "/login",
                invalidLogin,
                ErrorResponse.class
        );

        assertThat(invalidLoginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(invalidLoginResponse.getBody()).isNotNull();
        assertThat(invalidLoginResponse.getBody().getError()).isEqualTo("INVALID_CREDENTIALS");
    }
}
