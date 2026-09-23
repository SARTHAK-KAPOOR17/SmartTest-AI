package com.smarttestai.integration;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.request.CreateUserStoryRequest;
import com.smarttestai.dto.request.RegisterRequest;
import com.smarttestai.dto.request.UpdateUserStoryRequest;
import com.smarttestai.dto.response.AuthResponse;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.dto.response.UserStoryResponse;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserStoryIntegrationTest {

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

    private String token;
    private Long projectId;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/projects/" + projectId + "/stories";
    }

    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    private String getProjectsUrl() {
        return "http://localhost:" + port + "/api/v1/projects";
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @BeforeEach
    void cleanUpAndSetup() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        userStoryRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Register user and get JWT
        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Story Owner")
                .email("storyowner@test.com")
                .password("Password123!")
                .build();

        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
                getAuthUrl() + "/register",
                registerRequest,
                AuthResponse.class
        );
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.token = authResponse.getBody().getToken();

        // Create Project
        CreateProjectRequest projectRequest = CreateProjectRequest.builder()
                .name("Story QA Project")
                .description("Project for user stories")
                .build();

        HttpEntity<CreateProjectRequest> projectEntity = new HttpEntity<>(projectRequest, createAuthHeaders());
        ResponseEntity<ProjectResponse> projectResponse = restTemplate.postForEntity(
                getProjectsUrl(),
                projectEntity,
                ProjectResponse.class
        );
        assertThat(projectResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.projectId = projectResponse.getBody().getId();
    }

    @Test
    @DisplayName("Full User Story Lifecycle: Create -> Get -> List -> Filter -> Update -> Delete -> 404")
    void fullUserStoryLifecycleTest() {
        HttpHeaders headers = createAuthHeaders();

        // 1. Create Story
        CreateUserStoryRequest createRequest = CreateUserStoryRequest.builder()
                .title("Customer Login Feature")
                .description("As a registered customer I want to log in so I can see my profile.")
                .acceptanceCriteria("1. Valid email and password returns 200 and JWT.\n2. Bad password returns 401.")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.DRAFT)
                .build();

        HttpEntity<CreateUserStoryRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<UserStoryResponse> createResponse = restTemplate.postForEntity(
                getBaseUrl(),
                createEntity,
                UserStoryResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        Long storyId = createResponse.getBody().getId();
        assertThat(storyId).isNotNull();
        assertThat(createResponse.getBody().getTitle()).isEqualTo("Customer Login Feature");
        assertThat(createResponse.getBody().getProjectId()).isEqualTo(projectId);
        assertThat(createResponse.getBody().getPriority()).isEqualTo(StoryPriority.HIGH);
        assertThat(createResponse.getBody().getStatus()).isEqualTo(StoryStatus.DRAFT);

        // 2. Get Story By ID
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<UserStoryResponse> getResponse = restTemplate.exchange(
                getBaseUrl() + "/" + storyId,
                HttpMethod.GET,
                getEntity,
                UserStoryResponse.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(storyId);
        assertThat(getResponse.getBody().getTitle()).isEqualTo("Customer Login Feature");

        // 3. List All Stories
        ResponseEntity<List<UserStoryResponse>> listResponse = restTemplate.exchange(
                getBaseUrl(),
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSize(1);
        assertThat(listResponse.getBody().get(0).getId()).isEqualTo(storyId);

        // 4. Filter Stories by status=DRAFT
        ResponseEntity<List<UserStoryResponse>> filteredResponse = restTemplate.exchange(
                getBaseUrl() + "?status=DRAFT",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(filteredResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filteredResponse.getBody()).hasSize(1);

        // Filter by status=READY (should be empty initially)
        ResponseEntity<List<UserStoryResponse>> readyResponse = restTemplate.exchange(
                getBaseUrl() + "?status=READY",
                HttpMethod.GET,
                getEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(readyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readyResponse.getBody()).isEmpty();

        // 5. Update Story to READY for AI test generation
        UpdateUserStoryRequest updateRequest = UpdateUserStoryRequest.builder()
                .title("Customer Login Feature (Refined)")
                .status(StoryStatus.READY)
                .priority(StoryPriority.CRITICAL)
                .build();

        HttpEntity<UpdateUserStoryRequest> updateEntity = new HttpEntity<>(updateRequest, headers);
        ResponseEntity<UserStoryResponse> updateResponse = restTemplate.exchange(
                getBaseUrl() + "/" + storyId,
                HttpMethod.PUT,
                updateEntity,
                UserStoryResponse.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getTitle()).isEqualTo("Customer Login Feature (Refined)");
        assertThat(updateResponse.getBody().getStatus()).isEqualTo(StoryStatus.READY);
        assertThat(updateResponse.getBody().getPriority()).isEqualTo(StoryPriority.CRITICAL);

        // 6. Delete Story
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                getBaseUrl() + "/" + storyId,
                HttpMethod.DELETE,
                getEntity,
                Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 7. Verify Story is Gone (404)
        ResponseEntity<ErrorResponse> getDeletedResponse = restTemplate.exchange(
                getBaseUrl() + "/" + storyId,
                HttpMethod.GET,
                getEntity,
                ErrorResponse.class
        );
        assertThat(getDeletedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getDeletedResponse.getBody()).isNotNull();
        assertThat(getDeletedResponse.getBody().getError()).isEqualTo("USER_STORY_NOT_FOUND");
    }
}
