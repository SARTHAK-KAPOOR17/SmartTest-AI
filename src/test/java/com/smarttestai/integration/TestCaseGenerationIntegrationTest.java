package com.smarttestai.integration;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.request.CreateTestCaseRequest;
import com.smarttestai.dto.request.CreateUserStoryRequest;
import com.smarttestai.dto.request.RegisterRequest;
import com.smarttestai.dto.request.TestStepDto;
import com.smarttestai.dto.request.UpdateTestCaseStatusRequest;
import com.smarttestai.dto.response.AuthResponse;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.dto.response.GenerateTestCasesResponse;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.dto.response.TestCaseResponse;
import com.smarttestai.dto.response.UserStoryResponse;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.TestCaseStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.TestCaseRepository;
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
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TestCaseGenerationIntegrationTest {

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

    private String token;
    private Long projectId;
    private Long readyStoryId;
    private Long draftStoryId;

    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    private String getProjectsUrl() {
        return "http://localhost:" + port + "/api/v1/projects";
    }

    private String getTestCasesUrl(Long storyId) {
        return "http://localhost:" + port + "/api/v1/projects/" + projectId + "/stories/" + storyId + "/test-cases";
    }

    private HttpHeaders createAuthHeaders() {
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

        // 1. Register user & get JWT token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("QA Engineer")
                .email("qa@smarttestai.com")
                .password("Password123!")
                .build();

        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
                getAuthUrl() + "/register",
                registerRequest,
                AuthResponse.class
        );
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.token = authResponse.getBody().getToken();

        // 2. Create Project
        CreateProjectRequest projectRequest = CreateProjectRequest.builder()
                .name("Integration Testing App")
                .description("Project for testing AI test case generation")
                .build();

        HttpEntity<CreateProjectRequest> projectEntity = new HttpEntity<>(projectRequest, createAuthHeaders());
        ResponseEntity<ProjectResponse> projectResponse = restTemplate.postForEntity(
                getProjectsUrl(),
                projectEntity,
                ProjectResponse.class
        );
        assertThat(projectResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.projectId = projectResponse.getBody().getId();

        // 3. Create READY User Story
        CreateUserStoryRequest readyStoryReq = CreateUserStoryRequest.builder()
                .title("Customer Login")
                .description("As a customer, I want to log in using my credentials.")
                .acceptanceCriteria("Scenario: Successful login redirects to home.")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.READY)
                .build();

        HttpEntity<CreateUserStoryRequest> readyStoryEntity = new HttpEntity<>(readyStoryReq, createAuthHeaders());
        ResponseEntity<UserStoryResponse> readyResp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/projects/" + projectId + "/stories",
                readyStoryEntity,
                UserStoryResponse.class
        );
        assertThat(readyResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.readyStoryId = readyResp.getBody().getId();

        // 4. Create DRAFT User Story
        CreateUserStoryRequest draftStoryReq = CreateUserStoryRequest.builder()
                .title("Unfinished Feature")
                .description("Still being drafted by Product Owner.")
                .acceptanceCriteria("Pending product specification and detailed review.")
                .priority(StoryPriority.LOW)
                .status(StoryStatus.DRAFT)
                .build();

        HttpEntity<CreateUserStoryRequest> draftStoryEntity = new HttpEntity<>(draftStoryReq, createAuthHeaders());
        ResponseEntity<UserStoryResponse> draftResp = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/projects/" + projectId + "/stories",
                draftStoryEntity,
                UserStoryResponse.class
        );
        assertThat(draftResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.draftStoryId = draftResp.getBody().getId();
    }

    @Test
    @DisplayName("Should successfully generate and persist test cases for a READY user story")
    void shouldGenerateTestCasesForReadyStory() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<GenerateTestCasesResponse> response = restTemplate.postForEntity(
                getTestCasesUrl(readyStoryId) + "/generate",
                requestEntity,
                GenerateTestCasesResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserStoryId()).isEqualTo(readyStoryId);
        assertThat(response.getBody().getGeneratedCount()).isGreaterThanOrEqualTo(1);

        // Verify test cases exist in database
        assertThat(testCaseRepository.findAllByUserStoryId(readyStoryId)).hasSize(response.getBody().getGeneratedCount());

        Long firstTestCaseId = response.getBody().getTestCases().get(0).getId();

        // Verify human review status transition to APPROVED
        UpdateTestCaseStatusRequest statusRequest = new UpdateTestCaseStatusRequest(TestCaseStatus.APPROVED, "Verified by QA lead");
        HttpEntity<UpdateTestCaseStatusRequest> patchEntity = new HttpEntity<>(statusRequest, createAuthHeaders());

        ResponseEntity<TestCaseResponse> patchResponse = restTemplate.exchange(
                getTestCasesUrl(readyStoryId) + "/" + firstTestCaseId + "/status",
                HttpMethod.PATCH,
                patchEntity,
                TestCaseResponse.class
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody()).isNotNull();
        assertThat(patchResponse.getBody().getStatus()).isEqualTo(TestCaseStatus.APPROVED);
    }

    @Test
    @DisplayName("Should reject test case generation with HTTP 400 when story is in DRAFT status")
    void shouldRejectGenerationForDraftStory() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(createAuthHeaders());

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                getTestCasesUrl(draftStoryId) + "/generate",
                requestEntity,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("USER_STORY_NOT_READY");
    }

    @Test
    @DisplayName("Should create, read, update, and delete manual test cases")
    void shouldPerformManualTestCaseCrud() {
        CreateTestCaseRequest createRequest = new CreateTestCaseRequest(
                "Manual Boundary Scenario",
                "Testing boundaries manually",
                TestCaseType.EDGE_CASE,
                TestPriority.MEDIUM,
                "User is logged in",
                Arrays.asList(
                        new TestStepDto(1, "Go to profile settings", "Profile settings open"),
                        new TestStepDto(2, "Enter maximum length nickname", "Nickname is displayed")
                ),
                "Profile updates correctly"
        );

        HttpEntity<CreateTestCaseRequest> createEntity = new HttpEntity<>(createRequest, createAuthHeaders());
        ResponseEntity<TestCaseResponse> createResp = restTemplate.postForEntity(
                getTestCasesUrl(readyStoryId),
                createEntity,
                TestCaseResponse.class
        );

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getBody()).isNotNull();
        Long testCaseId = createResp.getBody().getId();
        assertThat(createResp.getBody().getTitle()).isEqualTo("Manual Boundary Scenario");

        // List test cases
        HttpEntity<Void> getEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<List<TestCaseResponse>> listResp = restTemplate.exchange(
                getTestCasesUrl(readyStoryId),
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).hasSize(1);

        // Delete test case
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                getTestCasesUrl(readyStoryId) + "/" + testCaseId,
                HttpMethod.DELETE,
                getEntity,
                Void.class
        );
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(testCaseRepository.findById(testCaseId)).isEmpty();
    }
}
