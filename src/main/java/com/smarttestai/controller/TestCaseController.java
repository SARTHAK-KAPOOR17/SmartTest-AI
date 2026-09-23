package com.smarttestai.controller;

import com.smarttestai.dto.request.CreateTestCaseRequest;
import com.smarttestai.dto.request.UpdateTestCaseRequest;
import com.smarttestai.dto.request.UpdateTestCaseStatusRequest;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.dto.response.GenerateTestCasesResponse;
import com.smarttestai.dto.response.TestCaseResponse;
import com.smarttestai.entity.TestCaseStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.security.CustomUserDetails;
import com.smarttestai.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/stories/{storyId}/test-cases")
@Tag(name = "Test Cases", description = "Endpoints for generating, reviewing, and managing QA test cases within user stories")
@SecurityRequirement(name = "bearerAuth")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping("/generate")
    @Operation(summary = "AI generate test cases",
               description = "Decomposes a READY user story into structured QA test cases using LangChain4j and LLM.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Test cases generated and persisted successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GenerateTestCasesResponse.class))),
            @ApiResponse(responseCode = "400", description = "User Story is not in READY status or validation error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or User story not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "AI model produced malformed or invalid response",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "AI provider service unavailable or connection failure",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GenerateTestCasesResponse> generateTestCases(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        GenerateTestCasesResponse response = testCaseService.generateTestCases(projectId, storyId, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping
    @Operation(summary = "Create test case manually", description = "Manually creates a new test case under the user story.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Test case created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed on test case fields",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or User story not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TestCaseResponse> createTestCase(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @Valid @RequestBody CreateTestCaseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TestCaseResponse response = testCaseService.createTestCase(projectId, storyId, request, userDetails);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List test cases", description = "Retrieves all test cases under the user story with optional filtering.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test cases retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TestCaseResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or User story not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<TestCaseResponse>> getAllTestCases(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @RequestParam(required = false) TestCaseType type,
            @RequestParam(required = false) TestPriority priority,
            @RequestParam(required = false) TestCaseStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TestCaseResponse> testCases = testCaseService.getAllTestCases(projectId, storyId, type, priority, status, userDetails);
        return ResponseEntity.ok(testCases);
    }

    @GetMapping("/{testCaseId}")
    @Operation(summary = "Get test case by ID", description = "Retrieves a specific test case under the user story.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test case retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestCaseResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project, User story, or Test case not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TestCaseResponse> getTestCaseById(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @PathVariable Long testCaseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TestCaseResponse response = testCaseService.getTestCaseById(projectId, storyId, testCaseId, userDetails);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{testCaseId}")
    @Operation(summary = "Update test case", description = "Updates fields or steps of an existing test case.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test case updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed on updated fields",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project, User story, or Test case not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TestCaseResponse> updateTestCase(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @PathVariable Long testCaseId,
            @Valid @RequestBody UpdateTestCaseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TestCaseResponse response = testCaseService.updateTestCase(projectId, storyId, testCaseId, request, userDetails);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{testCaseId}/status")
    @Operation(summary = "Update test case review status",
               description = "Human-in-the-loop transition of review status (e.g. GENERATED -> REVIEWED -> APPROVED).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status specified",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project, User story, or Test case not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TestCaseResponse> updateTestCaseStatus(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @PathVariable Long testCaseId,
            @Valid @RequestBody UpdateTestCaseStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TestCaseResponse response = testCaseService.updateTestCaseStatus(projectId, storyId, testCaseId, request, userDetails);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{testCaseId}")
    @Operation(summary = "Delete test case", description = "Deletes a specific test case under the user story.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Test case deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project, User story, or Test case not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteTestCase(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @PathVariable Long testCaseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        testCaseService.deleteTestCase(projectId, storyId, testCaseId, userDetails);
        return ResponseEntity.noContent().build();
    }
}
