package com.smarttestai.controller;

import com.smarttestai.dto.request.CreateUserStoryRequest;
import com.smarttestai.dto.request.UpdateUserStoryRequest;
import com.smarttestai.dto.response.ErrorResponse;
import com.smarttestai.dto.response.UserStoryResponse;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.security.CustomUserDetails;
import com.smarttestai.service.UserStoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/projects/{projectId}/stories")
@Tag(name = "User Stories", description = "Endpoints for managing requirements and user stories within QA automation projects")
@SecurityRequirement(name = "bearerAuth")
public class UserStoryController {

    private final UserStoryService userStoryService;

    public UserStoryController(UserStoryService userStoryService) {
        this.userStoryService = userStoryService;
    }

    @PostMapping
    @Operation(summary = "Create user story", description = "Creates a new user story under the specified project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User story created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserStoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed on user story fields",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found or not owned by user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserStoryResponse> createUserStory(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateUserStoryRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserStoryResponse response = userStoryService.createUserStory(projectId, request, userDetails);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List user stories", description = "Retrieves all user stories under the project, with optional status and priority filters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of user stories retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserStoryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found or not owned by user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserStoryResponse>> getAllUserStories(
            @PathVariable Long projectId,
            @RequestParam(required = false) StoryStatus status,
            @RequestParam(required = false) StoryPriority priority,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<UserStoryResponse> stories = userStoryService.getAllUserStories(projectId, status, priority, userDetails);
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{storyId}")
    @Operation(summary = "Get user story by ID", description = "Retrieves a specific user story under the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User story retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserStoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or User story not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserStoryResponse> getUserStoryById(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserStoryResponse response = userStoryService.getUserStoryById(projectId, storyId, userDetails);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{storyId}")
    @Operation(summary = "Update user story", description = "Updates an existing user story under the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User story updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserStoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed on updated fields",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or User story not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserStoryResponse> updateUserStory(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @Valid @RequestBody UpdateUserStoryRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserStoryResponse response = userStoryService.updateUserStory(projectId, storyId, request, userDetails);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{storyId}")
    @Operation(summary = "Delete user story", description = "Deletes a specific user story under the project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User story deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or User story not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUserStory(
            @PathVariable Long projectId,
            @PathVariable Long storyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userStoryService.deleteUserStory(projectId, storyId, userDetails);
        return ResponseEntity.noContent().build();
    }
}
