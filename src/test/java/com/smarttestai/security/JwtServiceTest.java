package com.smarttestai.security;

import com.smarttestai.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // 256-bit test secret
        String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        long expirationMs = 3600000; // 1 hour
        jwtService = new JwtService(testSecret, expirationMs);

        userDetails = new CustomUserDetails(
                1L,
                "Test User",
                "test@smarttestai.com",
                "hashedpassword",
                Role.ROLE_USER,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("Should successfully generate a token and extract claims")
    void generateToken_And_ExtractClaims() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@smarttestai.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should validate valid token successfully")
    void isTokenValid_ValidToken_ReturnsTrue() {
        String token = jwtService.generateToken(userDetails);
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false when token username does not match userDetails")
    void isTokenValid_DifferentUser_ReturnsFalse() {
        String token = jwtService.generateToken(userDetails);

        CustomUserDetails differentUser = new CustomUserDetails(
                2L,
                "Other User",
                "other@smarttestai.com",
                "hashedpassword",
                Role.ROLE_USER,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        boolean isValid = jwtService.isTokenValid(token, differentUser);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for malformed token")
    void isTokenValid_MalformedToken_ReturnsFalse() {
        boolean isValid = jwtService.isTokenValid("malformed.jwt.token", userDetails);
        assertThat(isValid).isFalse();
    }
}
