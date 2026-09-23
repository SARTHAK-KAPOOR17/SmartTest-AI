package com.smarttestai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.entity.Role;
import com.smarttestai.exception.GlobalExceptionHandler;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.security.CustomUserDetails;
import com.smarttestai.security.CustomUserDetailsService;
import com.smarttestai.security.JwtAuthenticationEntryPoint;
import com.smarttestai.security.JwtAuthenticationFilter;
import com.smarttestai.security.JwtService;
import com.smarttestai.service.ProjectService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

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
    @DisplayName("POST /api/v1/projects - 201 Created when payload is valid")
    void createProject_Valid_Returns201() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("E-Commerce Testing")
                .description("Valid description")
                .build();

        ProjectResponse response = ProjectResponse.builder()
                .id(1L)
                .name("E-Commerce Testing")
                .description("Valid description")
                .ownerId(1L)
                .ownerEmail("john@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectService.createProject(any(CreateProjectRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects")
                        .with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/projects/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("E-Commerce Testing")))
                .andExpect(jsonPath("$.description", is("Valid description")));
    }

    @Test
    @DisplayName("POST /api/v1/projects - 400 Bad Request when project name is blank")
    void createProject_BlankName_Returns400() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("")
                .description("Valid description")
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("POST /api/v1/projects - 400 Bad Request when project name is too short")
    void createProject_ShortName_Returns400() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("A")
                .description("Valid description")
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("GET /api/v1/projects - 200 OK with list of projects")
    void getAllProjects_Returns200() throws Exception {
        ProjectResponse project = ProjectResponse.builder()
                .id(1L)
                .name("E-Commerce Testing")
                .description("Desc")
                .ownerId(1L)
                .ownerEmail("john@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectService.getAllProjects(any())).thenReturn(List.of(project));

        mockMvc.perform(get("/api/v1/projects")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("E-Commerce Testing")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} - 200 OK when project exists")
    void getProjectById_Exists_Returns200() throws Exception {
        ProjectResponse response = ProjectResponse.builder()
                .id(1L)
                .name("E-Commerce Testing")
                .description("Desc")
                .ownerId(1L)
                .ownerEmail("john@example.com")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectService.getProjectById(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/1")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("E-Commerce Testing")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} - 404 Not Found when project does not exist")
    void getProjectById_NotFound_Returns404() throws Exception {
        when(projectService.getProjectById(any(), any()))
                .thenThrow(new ResourceNotFoundException("Project", 99L));

        mockMvc.perform(get("/api/v1/projects/99")
                        .with(user(mockUserDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("PROJECT_NOT_FOUND")));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id} - 204 No Content when project deleted")
    void deleteProject_Exists_Returns204() throws Exception {
        doNothing().when(projectService).deleteProject(any(), any());

        mockMvc.perform(delete("/api/v1/projects/1")
                        .with(user(mockUserDetails)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id} - 404 Not Found when project does not exist")
    void deleteProject_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Project", 99L))
                .when(projectService).deleteProject(any(), any());

        mockMvc.perform(delete("/api/v1/projects/99")
                        .with(user(mockUserDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("PROJECT_NOT_FOUND")));
    }
}
