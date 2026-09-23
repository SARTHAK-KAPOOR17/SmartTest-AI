package com.smarttestai.integration;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.request.LoginRequest;
import com.smarttestai.dto.request.RegisterRequest;
import com.smarttestai.dto.response.AuthResponse;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.User;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProjectSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String getProjectsUrl() {
        return "http://localhost:" + port + "/api/v1/projects";
    }

    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    @BeforeEach
    void cleanUp() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String registerAndGetToken(String name, String email, String password) {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                getAuthUrl() + "/register",
                registerRequest,
                AuthResponse.class
        );

        return response.getBody().getToken();
    }

    private String createAdminAndGetToken() {
        User admin = User.builder()
                .name("Super Admin")
                .email("admin@smarttestai.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ROLE_ADMIN)
                .build();
        userRepository.save(admin);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@smarttestai.com")
                .password("AdminPass123!")
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                getAuthUrl() + "/login",
                loginRequest,
                AuthResponse.class
        );

        return response.getBody().getToken();
    }

    @Test
    @DisplayName("Unauthenticated requests to /api/v1/projects return 401 Unauthorized")
    void unauthenticatedRequest_Returns401() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                getProjectsUrl(),
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Multi-tenant isolation: User A cannot see or delete User B's project; Admin can see all")
    void multiTenantProjectOwnershipIsolationTest() {
        // 1. Setup User A, User B, and Admin
        String tokenA = registerAndGetToken("User A", "usera@test.com", "Password123!");
        String tokenB = registerAndGetToken("User B", "userb@test.com", "Password123!");
        String tokenAdmin = createAdminAndGetToken();

        // 2. User A creates Project A
        CreateProjectRequest projectARequest = CreateProjectRequest.builder()
                .name("Project A")
                .description("Owned by User A")
                .build();
        HttpEntity<CreateProjectRequest> entityA = new HttpEntity<>(projectARequest, createAuthHeaders(tokenA));
        ResponseEntity<ProjectResponse> projectAResponse = restTemplate.postForEntity(
                getProjectsUrl(),
                entityA,
                ProjectResponse.class
        );
        assertThat(projectAResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long projectAId = projectAResponse.getBody().getId();
        assertThat(projectAResponse.getBody().getOwnerEmail()).isEqualTo("usera@test.com");

        // 3. User B creates Project B
        CreateProjectRequest projectBRequest = CreateProjectRequest.builder()
                .name("Project B")
                .description("Owned by User B")
                .build();
        HttpEntity<CreateProjectRequest> entityB = new HttpEntity<>(projectBRequest, createAuthHeaders(tokenB));
        ResponseEntity<ProjectResponse> projectBResponse = restTemplate.postForEntity(
                getProjectsUrl(),
                entityB,
                ProjectResponse.class
        );
        assertThat(projectBResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long projectBId = projectBResponse.getBody().getId();
        assertThat(projectBResponse.getBody().getOwnerEmail()).isEqualTo("userb@test.com");

        // 4. User A lists projects -> receives only Project A
        HttpEntity<Void> requestUserA = new HttpEntity<>(createAuthHeaders(tokenA));
        ResponseEntity<List<ProjectResponse>> listA = restTemplate.exchange(
                getProjectsUrl(),
                HttpMethod.GET,
                requestUserA,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(listA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listA.getBody()).hasSize(1);
        assertThat(listA.getBody().get(0).getId()).isEqualTo(projectAId);

        // 5. User B lists projects -> receives only Project B
        HttpEntity<Void> requestUserB = new HttpEntity<>(createAuthHeaders(tokenB));
        ResponseEntity<List<ProjectResponse>> listB = restTemplate.exchange(
                getProjectsUrl(),
                HttpMethod.GET,
                requestUserB,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(listB.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listB.getBody()).hasSize(1);
        assertThat(listB.getBody().get(0).getId()).isEqualTo(projectBId);

        // 6. User A attempts to GET Project B -> receives 404 (IDOR prevention)
        ResponseEntity<ErrorResponse> getBByA = restTemplate.exchange(
                getProjectsUrl() + "/" + projectBId,
                HttpMethod.GET,
                requestUserA,
                ErrorResponse.class
        );
        assertThat(getBByA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 7. User A attempts to DELETE Project B -> receives 404
        ResponseEntity<ErrorResponse> deleteBByA = restTemplate.exchange(
                getProjectsUrl() + "/" + projectBId,
                HttpMethod.DELETE,
                requestUserA,
                ErrorResponse.class
        );
        assertThat(deleteBByA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 8. Admin lists projects -> receives BOTH Project A and Project B
        HttpEntity<Void> requestAdmin = new HttpEntity<>(createAuthHeaders(tokenAdmin));
        ResponseEntity<List<ProjectResponse>> listAdmin = restTemplate.exchange(
                getProjectsUrl(),
                HttpMethod.GET,
                requestAdmin,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(listAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listAdmin.getBody()).hasSize(2);

        // 9. Admin deletes Project A -> 204 No Content
        ResponseEntity<Void> deleteAByAdmin = restTemplate.exchange(
                getProjectsUrl() + "/" + projectAId,
                HttpMethod.DELETE,
                requestAdmin,
                Void.class
        );
        assertThat(deleteAByAdmin.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 10. Verify Project A is deleted
        ResponseEntity<ErrorResponse> getDeletedA = restTemplate.exchange(
                getProjectsUrl() + "/" + projectAId,
                HttpMethod.GET,
                requestAdmin,
                ErrorResponse.class
        );
        assertThat(getDeletedA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
