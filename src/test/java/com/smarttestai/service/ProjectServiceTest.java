package com.smarttestai.service;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.entity.Project;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.User;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.UserRepository;
import com.smarttestai.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collections;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User sampleUser;
    private CustomUserDetails normalUserDetails;
    private CustomUserDetails adminUserDetails;
    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.ROLE_USER)
                .build();

        normalUserDetails = new CustomUserDetails(
                1L, "John Doe", "john@example.com", "pass", Role.ROLE_USER,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        adminUserDetails = new CustomUserDetails(
                99L, "Admin User", "admin@smarttestai.com", "pass", Role.ROLE_ADMIN,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        sampleProject = Project.builder()
                .id(1L)
                .name("E-Commerce Testing")
                .description("Automation testing project")
                .owner(sampleUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new project associated with the authenticated user")
    void createProject_Success() {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("E-Commerce Testing")
                .description("Automation testing project")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        ProjectResponse response = projectService.createProject(request, normalUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("E-Commerce Testing");
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getOwnerEmail()).isEqualTo("john@example.com");

        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should retrieve only owned projects for normal user")
    void getAllProjects_AsUser_ReturnsOnlyOwned() {
        when(projectRepository.findAllByOwnerId(1L)).thenReturn(List.of(sampleProject));

        List<ProjectResponse> result = projectService.getAllProjects(normalUserDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(projectRepository).findAllByOwnerId(1L);
        verify(projectRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should retrieve all projects across users for admin")
    void getAllProjects_AsAdmin_ReturnsAll() {
        Project otherProject = Project.builder()
                .id(2L)
                .name("Other User Project")
                .build();

        when(projectRepository.findAll()).thenReturn(List.of(sampleProject, otherProject));

        List<ProjectResponse> result = projectService.getAllProjects(adminUserDetails);

        assertThat(result).hasSize(2);
        verify(projectRepository).findAll();
    }

    @Test
    @DisplayName("Should retrieve project when owned by normal user")
    void getProjectById_OwnedByUser_Success() {
        when(projectRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(sampleProject));

        ProjectResponse response = projectService.getProjectById(1L, normalUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(projectRepository).findByIdAndOwnerId(1L, 1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when project is not owned by user")
    void getProjectById_NotOwnedByUser_ThrowsNotFound() {
        when(projectRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99L, normalUserDetails))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should retrieve any project for admin")
    void getProjectById_AsAdmin_Success() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        ProjectResponse response = projectService.getProjectById(1L, adminUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        verify(projectRepository).findById(1L);
    }

    @Test
    @DisplayName("Should successfully delete project when owned by user")
    void deleteProject_OwnedByUser_Success() {
        when(projectRepository.existsByIdAndOwnerId(1L, 1L)).thenReturn(true);

        projectService.deleteProject(1L, normalUserDetails);

        verify(projectRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user attempts to delete unowned project")
    void deleteProject_NotOwnedByUser_ThrowsNotFound() {
        when(projectRepository.existsByIdAndOwnerId(99L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> projectService.deleteProject(99L, normalUserDetails))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should allow admin to delete any project")
    void deleteProject_AsAdmin_Success() {
        when(projectRepository.existsById(1L)).thenReturn(true);

        projectService.deleteProject(1L, adminUserDetails);

        verify(projectRepository).deleteById(1L);
    }
}
