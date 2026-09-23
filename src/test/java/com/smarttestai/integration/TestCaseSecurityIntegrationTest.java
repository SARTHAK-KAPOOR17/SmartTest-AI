package com.smarttestai.integration;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.request.CreateTestCaseRequest;
import com.smarttestai.dto.request.CreateUserStoryRequest;
import com.smarttestai.dto.request.RegisterRequest;
import com.smarttestai.dto.request.TestStepDto;
import com.smarttestai.dto.response.AuthResponse;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.dto.response.TestCaseResponse;
import com.smarttestai.dto.response.UserStoryResponse;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.entity.User;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.TestCaseRepository;
import com.smarttestai.repository.UserRepository;
import com.smarttestai.repository.UserStoryRepository;
import com.smarttestai.security.JwtService;
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
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TestCaseSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userAToken;
    private String userBToken;
    private String adminToken;
    private Long projectAId;
    private Long storyAId;
    private Long testCaseAId;

    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    private String getProjectsUrl() {
        return "http://localhost:" + port + "/api/v1/projects";
    }

    private String getTestCasesUrl(Long projectId, Long storyId) {
        return "http://localhost:" + port + "/api/v1/projects/" + projectId + "/stories/" + storyId + "/test-cases";
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @BeforeEach
    void cleanUpAndSetup() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        testCaseRepository.deleteAll();
        userStoryRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create User A & User B
        ResponseEntity<AuthResponse> authA = restTemplate.postForEntity(
                getAuthUrl() + "/register",
                new RegisterRequest("User A", "userA@test.com", "Password123!"),
                AuthResponse.class
        );
        userAToken = authA.getBody().getToken();

        ResponseEntity<AuthResponse> authB = restTemplate.postForEntity(
                getAuthUrl() + "/register",
                new RegisterRequest("User B", "userB@test.com", "Password123!"),
                AuthResponse.class
        );
        userBToken = authB.getBody().getToken();

        // 2. Create Admin User
        User adminUser = User.builder()
                .name("Super Admin")
                .email("admin@test.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ROLE_ADMIN)
                .build();
        userRepository.save(adminUser);
        adminToken = jwtService.generateToken(com.smarttestai.security.CustomUserDetails.fromUser(adminUser));

        // 3. User A creates Project
        ResponseEntity<ProjectResponse> projResp = restTemplate.postForEntity(
                getProjectsUrl(),
                new HttpEntity<>(new CreateProjectRequest("Project A", "Desc"), createHeaders(userAToken)),
                ProjectResponse.class
        );
        projectAId = projResp.getBody().getId();

        // 4. User A creates READY User Story
        ResponseEntity<UserStoryResponse> storyResp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/projects/" + projectAId + "/stories",
                new HttpEntity<>(new CreateUserStoryRequest("Story A Title", "Valid description for Story A", "Valid acceptance criteria for Story A", StoryPriority.HIGH, StoryStatus.READY), createHeaders(userAToken)),
                UserStoryResponse.class
        );
        storyAId = storyResp.getBody().getId();

        // 5. User A creates Test Case
        CreateTestCaseRequest tcReq = new CreateTestCaseRequest(
                "Test Case A", "Desc", TestCaseType.POSITIVE, TestPriority.CRITICAL, "Pre",
                Collections.singletonList(new TestStepDto(1, "Action", "Result")), "Expected"
        );
        ResponseEntity<TestCaseResponse> tcResp = restTemplate.postForEntity(
                getTestCasesUrl(projectAId, storyAId),
                new HttpEntity<>(tcReq, createHeaders(userAToken)),
                TestCaseResponse.class
        );
        testCaseAId = tcResp.getBody().getId();
    }

    @Test
    @DisplayName("User B should receive 404 when attempting to generate test cases on User A's story")
    void userBCannotGenerateTestCasesOnUserAStory() {
        HttpEntity<Void> request = new HttpEntity<>(createHeaders(userBToken));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getTestCasesUrl(projectAId, storyAId) + "/generate",
                request,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("PROJECT_NOT_FOUND");
    }

    @Test
    @DisplayName("User B should receive 404 when attempting to read User A's test cases")
    void userBCannotReadUserATestCases() {
        HttpEntity<Void> request = new HttpEntity<>(createHeaders(userBToken));

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                getTestCasesUrl(projectAId, storyAId) + "/" + testCaseAId,
                HttpMethod.GET,
                request,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError()).isEqualTo("PROJECT_NOT_FOUND");
    }

    @Test
    @DisplayName("User B should receive 404 when attempting to delete User A's test case")
    void userBCannotDeleteUserATestCase() {
        HttpEntity<Void> request = new HttpEntity<>(createHeaders(userBToken));

        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                getTestCasesUrl(projectAId, storyAId) + "/" + testCaseAId,
                HttpMethod.DELETE,
                request,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError()).isEqualTo("PROJECT_NOT_FOUND");
    }

    @Test
    @DisplayName("Admin should have supervisory access to read User A's test cases")
    void adminCanReadUserATestCases() {
        HttpEntity<Void> request = new HttpEntity<>(createHeaders(adminToken));

        ResponseEntity<List<TestCaseResponse>> response = restTemplate.exchange(
                getTestCasesUrl(projectAId, storyAId),
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo(testCaseAId);
    }
}
