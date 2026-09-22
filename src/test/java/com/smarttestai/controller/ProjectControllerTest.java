package com.smarttestai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.exception.GlobalExceptionHandler;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.service.ProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(GlobalExceptionHandler.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

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
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects")
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
        ProjectResponse p1 = ProjectResponse.builder()
                .id(1L)
                .name("Project Alpha")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ProjectResponse p2 = ProjectResponse.builder()
                .id(2L)
                .name("Project Beta")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectService.getAllProjects()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Project Alpha")))
                .andExpect(jsonPath("$[1].name", is("Project Beta")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} - 200 OK when project exists")
    void getProjectById_Existing_Returns200() throws Exception {
        ProjectResponse response = ProjectResponse.builder()
                .id(1L)
                .name("Project Alpha")
                .description("Sample description")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectService.getProjectById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Project Alpha")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} - 404 Not Found when project does not exist")
    void getProjectById_Missing_Returns404() throws Exception {
        when(projectService.getProjectById(999L))
                .thenThrow(new ResourceNotFoundException("Project", 999L));

        mockMvc.perform(get("/api/v1/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("PROJECT_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Project not found with id: 999")));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id} - 204 No Content when project exists")
    void deleteProject_Existing_Returns204() throws Exception {
        doNothing().when(projectService).deleteProject(1L);

        mockMvc.perform(delete("/api/v1/projects/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id} - 404 Not Found when project does not exist")
    void deleteProject_Missing_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Project", 999L))
                .when(projectService).deleteProject(999L);

        mockMvc.perform(delete("/api/v1/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("PROJECT_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Project not found with id: 999")));
    }
}
