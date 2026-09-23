package com.smarttestai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttestai.dto.request.CreateUserStoryRequest;
import com.smarttestai.dto.request.UpdateUserStoryRequest;
import com.smarttestai.dto.response.UserStoryResponse;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.exception.GlobalExceptionHandler;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.security.CustomUserDetails;
import com.smarttestai.security.CustomUserDetailsService;
import com.smarttestai.security.JwtAuthenticationEntryPoint;
import com.smarttestai.security.JwtAuthenticationFilter;
import com.smarttestai.security.JwtService;
import com.smarttestai.service.UserStoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserStoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserStoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserStoryService userStoryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = new CustomUserDetails(
                1L, "John Doe", "john@example.com", "pass", Role.ROLE_USER,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/stories - 201 Created on valid payload")
    void createStory_Valid_Returns201() throws Exception {
        CreateUserStoryRequest request = CreateUserStoryRequest.builder()
                .title("Customer Login")
                .description("As a customer I want to log in so I can access my dashboard.")
                .acceptanceCriteria("1. Valid password allows login.")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.DRAFT)
                .build();

        UserStoryResponse response = UserStoryResponse.builder()
                .id(1L)
                .projectId(10L)
                .title("Customer Login")
                .description("As a customer I want to log in so I can access my dashboard.")
                .acceptanceCriteria("1. Valid password allows login.")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.DRAFT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userStoryService.createUserStory(eq(10L), any(CreateUserStoryRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects/10/stories")
                        .with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/projects/10/stories/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.projectId", is(10)))
                .andExpect(jsonPath("$.title", is("Customer Login")))
                .andExpect(jsonPath("$.priority", is("HIGH")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/stories - 400 Bad Request when title is blank")
    void createStory_BlankTitle_Returns400() throws Exception {
        CreateUserStoryRequest request = CreateUserStoryRequest.builder()
                .title("")
                .description("As a customer I want to log in.")
                .acceptanceCriteria("Valid criteria.")
                .build();

        mockMvc.perform(post("/api/v1/projects/10/stories")
                        .with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/stories - 200 OK with list of stories")
    void getAllStories_Returns200() throws Exception {
        UserStoryResponse story = UserStoryResponse.builder()
                .id(1L)
                .projectId(10L)
                .title("Customer Login")
                .build();

        when(userStoryService.getAllUserStories(eq(10L), any(), any(), any())).thenReturn(List.of(story));

        mockMvc.perform(get("/api/v1/projects/10/stories")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Customer Login")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/stories/{storyId} - 200 OK when story exists")
    void getStoryById_Exists_Returns200() throws Exception {
        UserStoryResponse story = UserStoryResponse.builder()
                .id(1L)
                .projectId(10L)
                .title("Customer Login")
                .build();

        when(userStoryService.getUserStoryById(eq(10L), eq(1L), any())).thenReturn(story);

        mockMvc.perform(get("/api/v1/projects/10/stories/1")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Customer Login")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/stories/{storyId} - 404 Not Found when story is missing")
    void getStoryById_NotFound_Returns404() throws Exception {
        when(userStoryService.getUserStoryById(eq(10L), eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("User Story", 999L));

        mockMvc.perform(get("/api/v1/projects/10/stories/999")
                        .with(user(mockUserDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("USER_STORY_NOT_FOUND")));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId}/stories/{storyId} - 200 OK on valid update")
    void updateStory_Valid_Returns200() throws Exception {
        UpdateUserStoryRequest request = UpdateUserStoryRequest.builder()
                .title("Updated Login")
                .status(StoryStatus.READY)
                .build();

        UserStoryResponse response = UserStoryResponse.builder()
                .id(1L)
                .projectId(10L)
                .title("Updated Login")
                .status(StoryStatus.READY)
                .build();

        when(userStoryService.updateUserStory(eq(10L), eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/projects/10/stories/1")
                        .with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Login")))
                .andExpect(jsonPath("$.status", is("READY")));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/stories/{storyId} - 204 No Content on delete")
    void deleteStory_Exists_Returns204() throws Exception {
        doNothing().when(userStoryService).deleteUserStory(eq(10L), eq(1L), any());

        mockMvc.perform(delete("/api/v1/projects/10/stories/1")
                        .with(user(mockUserDetails)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/stories/{storyId} - 404 Not Found when story is missing")
    void deleteStory_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("User Story", 999L))
                .when(userStoryService).deleteUserStory(eq(10L), eq(999L), any());

        mockMvc.perform(delete("/api/v1/projects/10/stories/999")
                        .with(user(mockUserDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("USER_STORY_NOT_FOUND")));
    }
}
