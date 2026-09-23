package com.smarttestai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttestai.dto.request.CreateTestCaseRequest;
import com.smarttestai.dto.request.TestStepDto;
import com.smarttestai.dto.request.UpdateTestCaseRequest;
import com.smarttestai.dto.request.UpdateTestCaseStatusRequest;
import com.smarttestai.dto.response.GenerateTestCasesResponse;
import com.smarttestai.dto.response.TestCaseResponse;
import com.smarttestai.dto.response.TestStepResponse;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.TestCaseStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.exception.GlobalExceptionHandler;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.exception.UserStoryNotReadyException;
import com.smarttestai.security.CustomUserDetails;
import com.smarttestai.security.CustomUserDetailsService;
import com.smarttestai.security.JwtService;
import com.smarttestai.service.TestCaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestCaseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TestCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestCaseService testCaseService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private CustomUserDetails mockUserDetails;
    private TestCaseResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockUserDetails = new CustomUserDetails(
                1L,
                "John QA",
                "john@example.com",
                "password",
                Role.ROLE_USER,
                Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        sampleResponse = new TestCaseResponse(
                500L,
                100L,
                "Valid User Login",
                "Verifies valid authentication",
                TestCaseType.POSITIVE,
                TestPriority.CRITICAL,
                TestCaseStatus.GENERATED,
                "Active account",
                Arrays.asList(new TestStepResponse(1, "Enter username", "Username accepted")),
                "Redirect to dashboard",
                "v1.0",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("POST /generate should return 201 Created with generated test cases")
    void generateTestCases_shouldReturn201() throws Exception {
        GenerateTestCasesResponse genResponse = new GenerateTestCasesResponse(
                100L, "v1.0", 1, Collections.singletonList(sampleResponse));

        when(testCaseService.generateTestCases(eq(10L), eq(100L), any()))
                .thenReturn(genResponse);

        mockMvc.perform(post("/api/v1/projects/10/stories/100/test-cases/generate")
                        .with(user(mockUserDetails)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userStoryId", is(100)))
                .andExpect(jsonPath("$.promptVersion", is("v1.0")))
                .andExpect(jsonPath("$.generatedCount", is(1)))
                .andExpect(jsonPath("$.testCases", hasSize(1)))
                .andExpect(jsonPath("$.testCases[0].title", is("Valid User Login")));
    }

    @Test
    @DisplayName("POST /generate should return 400 Bad Request when story is not READY")
    void generateTestCases_notReady_shouldReturn400() throws Exception {
        when(testCaseService.generateTestCases(eq(10L), eq(100L), any()))
                .thenThrow(new UserStoryNotReadyException("User Story id: 100 is in 'DRAFT' status. Only stories in 'READY' status can generate test cases."));

        mockMvc.perform(post("/api/v1/projects/10/stories/100/test-cases/generate")
                        .with(user(mockUserDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("USER_STORY_NOT_READY")))
                .andExpect(jsonPath("$.message", is("User Story id: 100 is in 'DRAFT' status. Only stories in 'READY' status can generate test cases.")));
    }

    @Test
    @DisplayName("POST should create manual test case and return 201 Created")
    void createTestCase_shouldReturn201() throws Exception {
        CreateTestCaseRequest request = new CreateTestCaseRequest(
                "Manual Test Scenario",
                "Testing manual creation",
                TestCaseType.POSITIVE,
                TestPriority.HIGH,
                "Precondition",
                Arrays.asList(new TestStepDto(1, "Action 1", "Result 1")),
                "Final Result"
        );

        when(testCaseService.createTestCase(eq(10L), eq(100L), any(CreateTestCaseRequest.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/projects/10/stories/100/test-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(mockUserDetails)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.title", is("Valid User Login")));
    }

    @Test
    @DisplayName("GET should list test cases and return 200 OK")
    void getAllTestCases_shouldReturn200() throws Exception {
        when(testCaseService.getAllTestCases(eq(10L), eq(100L), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(sampleResponse));

        mockMvc.perform(get("/api/v1/projects/10/stories/100/test-cases")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Valid User Login")));
    }

    @Test
    @DisplayName("GET /{testCaseId} should return test case by ID and return 200 OK")
    void getTestCaseById_shouldReturn200() throws Exception {
        when(testCaseService.getTestCaseById(eq(10L), eq(100L), eq(500L), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/projects/10/stories/100/test-cases/500")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(500)))
                .andExpect(jsonPath("$.title", is("Valid User Login")));
    }

    @Test
    @DisplayName("GET /{testCaseId} should return 404 Not Found when test case does not exist")
    void getTestCaseById_notFound_shouldReturn404() throws Exception {
        when(testCaseService.getTestCaseById(eq(10L), eq(100L), eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Test Case", 999L));

        mockMvc.perform(get("/api/v1/projects/10/stories/100/test-cases/999")
                        .with(user(mockUserDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("TEST_CASE_NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /{testCaseId} should update test case and return 200 OK")
    void updateTestCase_shouldReturn200() throws Exception {
        UpdateTestCaseRequest request = new UpdateTestCaseRequest();
        request.setTitle("Updated Title");

        when(testCaseService.updateTestCase(eq(10L), eq(100L), eq(500L), any(UpdateTestCaseRequest.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/projects/10/stories/100/test-cases/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /{testCaseId}/status should update status and return 200 OK")
    void updateTestCaseStatus_shouldReturn200() throws Exception {
        UpdateTestCaseStatusRequest request = new UpdateTestCaseStatusRequest(TestCaseStatus.APPROVED, "Verified by QA");

        when(testCaseService.updateTestCaseStatus(eq(10L), eq(100L), eq(500L), any(UpdateTestCaseStatusRequest.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/projects/10/stories/100/test-cases/500/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /{testCaseId} should return 204 No Content")
    void deleteTestCase_shouldReturn204() throws Exception {
        doNothing().when(testCaseService).deleteTestCase(eq(10L), eq(100L), eq(500L), any());

        mockMvc.perform(delete("/api/v1/projects/10/stories/100/test-cases/500")
                        .with(user(mockUserDetails)))
                .andExpect(status().isNoContent());
    }
}
