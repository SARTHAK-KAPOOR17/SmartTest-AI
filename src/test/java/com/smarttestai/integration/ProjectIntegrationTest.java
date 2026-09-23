package com.smarttestai.integration;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.request.RegisterRequest;
import com.smarttestai.dto.response.AuthResponse;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.dto.response.ProjectResponse;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProjectIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private com.smarttestai.repository.TestCaseRepository testCaseRepository;

    @Autowired
    private com.smarttestai.repository.UserStoryRepository userStoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private String token;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/projects";
    }

    private String getAuthUrl() {
        return "http://localhost:" + port + "/api/v1/auth";
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @BeforeEach
    void cleanUpAndAuthenticate() {
        restTemplate.getRestTemplate().setRequestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory());
        testCaseRepository.deleteAll();
        userStoryRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest registerRequest = RegisterRequest.builder()
                .name("Integration User")
                .email("integration@smarttestai.com")
                .password("Password123!")
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                getAuthUrl() + "/register",
                registerRequest,
                AuthResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.token = response.getBody().getToken();
    }

    @Test
    @DisplayName("End-to-End lifecycle test: Create -> Get -> List -> Delete -> Not Found")
    void fullProjectLifecycleIntegrationTest() {
        HttpHeaders authHeaders = createAuthHeaders();

        // 1. Create Project
        CreateProjectRequest createRequest = CreateProjectRequest.builder()
                .name("Integration QA Project")
                .description("Automated end-to-end testing pipeline")
                .build();

        HttpEntity<CreateProjectRequest> createEntity = new HttpEntity<>(createRequest, authHeaders);

        ResponseEntity<ProjectResponse> createResponse = restTemplate.postForEntity(
                getBaseUrl(),
                createEntity,
                ProjectResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getHeaders().getLocation()).isNotNull();

        Long createdId = createResponse.getBody().getId();
        assertThat(createdId).isNotNull();
        assertThat(createResponse.getBody().getName()).isEqualTo("Integration QA Project");
        assertThat(createResponse.getBody().getDescription()).isEqualTo("Automated end-to-end testing pipeline");
        assertThat(createResponse.getBody().getOwnerEmail()).isEqualTo("integration@smarttestai.com");

        // 2. Get Project By ID
        HttpEntity<Void> requestEntity = new HttpEntity<>(authHeaders);

        ResponseEntity<ProjectResponse> getResponse = restTemplate.exchange(
                getBaseUrl() + "/" + createdId,
                HttpMethod.GET,
                requestEntity,
                ProjectResponse.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(createdId);
        assertThat(getResponse.getBody().getName()).isEqualTo("Integration QA Project");

        // 3. Get All Projects
        ResponseEntity<List<ProjectResponse>> listResponse = restTemplate.exchange(
                getBaseUrl(),
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody()).hasSize(1);
        assertThat(listResponse.getBody().get(0).getId()).isEqualTo(createdId);

        // 4. Test Validation Error on Invalid Creation
        CreateProjectRequest invalidRequest = CreateProjectRequest.builder()
                .name("")
                .description("Invalid")
                .build();

        HttpEntity<CreateProjectRequest> invalidEntity = new HttpEntity<>(invalidRequest, authHeaders);

        ResponseEntity<ErrorResponse> validationResponse = restTemplate.postForEntity(
                getBaseUrl(),
                invalidEntity,
                ErrorResponse.class
        );

        assertThat(validationResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(validationResponse.getBody()).isNotNull();
        assertThat(validationResponse.getBody().getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(validationResponse.getBody().getErrors()).containsKey("name");

        // 5. Delete Project
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                getBaseUrl() + "/" + createdId,
                HttpMethod.DELETE,
                requestEntity,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 6. Verify Project is Deleted (404 Not Found)
        ResponseEntity<ErrorResponse> getAfterDeleteResponse = restTemplate.exchange(
                getBaseUrl() + "/" + createdId,
                HttpMethod.GET,
                requestEntity,
                ErrorResponse.class
        );

        assertThat(getAfterDeleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getAfterDeleteResponse.getBody()).isNotNull();
        assertThat(getAfterDeleteResponse.getBody().getError()).isEqualTo("PROJECT_NOT_FOUND");
    }
}
