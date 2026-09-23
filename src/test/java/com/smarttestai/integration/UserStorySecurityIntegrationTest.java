package com.smarttestai.integration;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.request.CreateUserStoryRequest;
import com.smarttestai.dto.request.LoginRequest;
import com.smarttestai.dto.request.RegisterRequest;
import com.smarttestai.dto.response.AuthResponse;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.dto.response.UserStoryResponse;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.User;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.UserRepository;
import com.smarttestai.repository.UserStoryRepository;
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
class UserStorySecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    private String getProjectsUrl() {
        return "http://localhost:" + port + "/api/v1/projects";
    }

    private String getStoriesUrl(Long projectId) {
        return "http://localhost:" + port + "/api/v1/projects/" + projectId + "/stories";
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

    private Long createProject(String token, String name) {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name(name)
                .description("Description for " + name)
                .build();

        HttpEntity<CreateProjectRequest> entity = new HttpEntity<>(request, createAuthHeaders(token));
        ResponseEntity<ProjectResponse> response = restTemplate.postForEntity(
                getProjectsUrl(),
                entity,
                ProjectResponse.class
        );
        return response.getBody().getId();
    }

    private Long createStory(String token, Long projectId, String title) {
        CreateUserStoryRequest request = CreateUserStoryRequest.builder()
                .title(title)
                .description("Story narrative for " + title)
                .acceptanceCriteria("Acceptance criteria for " + title)
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.DRAFT)
                .build();

        HttpEntity<CreateUserStoryRequest> entity = new HttpEntity<>(request, createAuthHeaders(token));
        ResponseEntity<UserStoryResponse> response = restTemplate.postForEntity(
                getStoriesUrl(projectId),
                entity,
                UserStoryResponse.class
        );
        return response.getBody().getId();
    }

    @BeforeEach
    void cleanUp() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        userStoryRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Unauthenticated requests to user stories return 401 Unauthorized")
    void unauthenticatedRequest_Returns401() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                getStoriesUrl(1L),
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Multi-tenant isolation & IDOR prevention across projects and user stories")
    void multiTenantStoryIsolationAndIdorPreventionTest() {
        // 1. Setup Users
        String tokenA = registerAndGetToken("User A", "usera@test.com", "Password123!");
        String tokenB = registerAndGetToken("User B", "userb@test.com", "Password123!");
        String tokenAdmin = createAdminAndGetToken();

        // 2. User A creates Project A and Story A
        Long projectAId = createProject(tokenA, "Project A");
        Long storyAId = createStory(tokenA, projectAId, "Story A");

        // 3. User B creates Project B and Story B
        Long projectBId = createProject(tokenB, "Project B");
        Long storyBId = createStory(tokenB, projectBId, "Story B");

        HttpEntity<Void> requestUserA = new HttpEntity<>(createAuthHeaders(tokenA));
        HttpEntity<Void> requestUserB = new HttpEntity<>(createAuthHeaders(tokenB));
        HttpEntity<Void> requestAdmin = new HttpEntity<>(createAuthHeaders(tokenAdmin));

        // 4. User A lists Project A stories -> receives ONLY Story A
        ResponseEntity<List<UserStoryResponse>> listA = restTemplate.exchange(
                getStoriesUrl(projectAId),
                HttpMethod.GET,
                requestUserA,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(listA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listA.getBody()).hasSize(1);
        assertThat(listA.getBody().get(0).getId()).isEqualTo(storyAId);

        // 5. User A attempts to list Project B stories -> 404 Not Found (no leakage)
        ResponseEntity<ErrorResponse> listBByA = restTemplate.exchange(
                getStoriesUrl(projectBId),
                HttpMethod.GET,
                requestUserA,
                ErrorResponse.class
        );
        assertThat(listBByA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 6. User A attempts to get Story B under Project B -> 404 Not Found
        ResponseEntity<ErrorResponse> getBByA = restTemplate.exchange(
                getStoriesUrl(projectBId) + "/" + storyBId,
                HttpMethod.GET,
                requestUserA,
                ErrorResponse.class
        );
        assertThat(getBByA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 7. User A attempts to probe Story B using Project A id (IDOR cross-probe) -> 404 Not Found
        ResponseEntity<ErrorResponse> crossProbe = restTemplate.exchange(
                getStoriesUrl(projectAId) + "/" + storyBId,
                HttpMethod.GET,
                requestUserA,
                ErrorResponse.class
        );
        assertThat(crossProbe.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 8. User A attempts to delete Story B -> 404 Not Found
        ResponseEntity<ErrorResponse> deleteBByA = restTemplate.exchange(
                getStoriesUrl(projectBId) + "/" + storyBId,
                HttpMethod.DELETE,
                requestUserA,
                ErrorResponse.class
        );
        assertThat(deleteBByA.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 9. Admin can access stories under both Project A and Project B
        ResponseEntity<List<UserStoryResponse>> adminListA = restTemplate.exchange(
                getStoriesUrl(projectAId),
                HttpMethod.GET,
                requestAdmin,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(adminListA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminListA.getBody()).hasSize(1);

        ResponseEntity<List<UserStoryResponse>> adminListB = restTemplate.exchange(
                getStoriesUrl(projectBId),
                HttpMethod.GET,
                requestAdmin,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(adminListB.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminListB.getBody()).hasSize(1);

        // 10. Admin can delete Story A -> 204 No Content
        ResponseEntity<Void> adminDelete = restTemplate.exchange(
                getStoriesUrl(projectAId) + "/" + storyAId,
                HttpMethod.DELETE,
                requestAdmin,
                Void.class
        );
        assertThat(adminDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
