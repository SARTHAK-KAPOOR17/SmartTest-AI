package com.smarttestai.service;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.entity.Project;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .name("E-Commerce Testing")
                .description("Automation testing project")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new project")
    void createProject_Success() {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("E-Commerce Testing")
                .description("Automation testing project")
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        ProjectResponse response = projectService.createProject(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("E-Commerce Testing");
        assertThat(response.getDescription()).isEqualTo("Automation testing project");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should retrieve all projects")
    void getAllProjects_Success() {
        Project secondProject = Project.builder()
                .id(2L)
                .name("Mobile App QA")
                .description("Mobile test suite")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(projectRepository.findAll()).thenReturn(List.of(sampleProject, secondProject));

        List<ProjectResponse> result = projectService.getAllProjects();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("E-Commerce Testing");
        assertThat(result.get(1).getName()).isEqualTo("Mobile App QA");
        verify(projectRepository).findAll();
    }

    @Test
    @DisplayName("Should retrieve project by existing ID")
    void getProjectById_Success() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        ProjectResponse response = projectService.getProjectById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("E-Commerce Testing");
        verify(projectRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when project ID does not exist")
    void getProjectById_NotFound() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found with id: 999");

        verify(projectRepository).findById(999L);
    }

    @Test
    @DisplayName("Should successfully delete existing project")
    void deleteProject_Success() {
        when(projectRepository.existsById(1L)).thenReturn(true);

        projectService.deleteProject(1L);

        verify(projectRepository).existsById(1L);
        verify(projectRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent project")
    void deleteProject_NotFound() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.deleteProject(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found with id: 999");

        verify(projectRepository).existsById(999L);
        verify(projectRepository, never()).deleteById(999L);
    }
}
