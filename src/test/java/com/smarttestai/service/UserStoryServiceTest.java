package com.smarttestai.service;

import com.smarttestai.dto.request.CreateUserStoryRequest;
import com.smarttestai.dto.request.UpdateUserStoryRequest;
import com.smarttestai.dto.response.UserStoryResponse;
import com.smarttestai.entity.Project;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.User;
import com.smarttestai.entity.UserStory;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.UserStoryRepository;
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
class UserStoryServiceTest {

    @Mock
    private UserStoryRepository userStoryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private UserStoryService userStoryService;

    private User sampleUser;
    private Project sampleProject;
    private UserStory sampleStory;
    private CustomUserDetails normalUserDetails;
    private CustomUserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.ROLE_USER)
                .build();

        sampleProject = Project.builder()
                .id(10L)
                .name("E-Commerce Testing")
                .owner(sampleUser)
                .build();

        sampleStory = UserStory.builder()
                .id(100L)
                .project(sampleProject)
                .title("Customer Login")
                .description("As a user I want to log in")
                .acceptanceCriteria("Valid credentials return JWT")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.DRAFT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        normalUserDetails = new CustomUserDetails(
                1L, "John Doe", "john@example.com", "pass", Role.ROLE_USER,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        adminUserDetails = new CustomUserDetails(
                99L, "Admin", "admin@smarttestai.com", "pass", Role.ROLE_ADMIN,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("Should successfully create user story under owned project")
    void createUserStory_Success() {
        CreateUserStoryRequest request = CreateUserStoryRequest.builder()
                .title("Customer Login")
                .description("As a user I want to log in")
                .acceptanceCriteria("Valid credentials return JWT")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.DRAFT)
                .build();

        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userStoryRepository.save(any(UserStory.class))).thenReturn(sampleStory);

        UserStoryResponse response = userStoryService.createUserStory(10L, request, normalUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Customer Login");
        assertThat(response.getPriority()).isEqualTo(StoryPriority.HIGH);
        assertThat(response.getStatus()).isEqualTo(StoryStatus.DRAFT);
        verify(userStoryRepository).save(any(UserStory.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating story under unowned project")
    void createUserStory_UnownedProject_ThrowsNotFound() {
        CreateUserStoryRequest request = CreateUserStoryRequest.builder()
                .title("Customer Login")
                .description("As a user I want to log in")
                .acceptanceCriteria("Valid credentials return JWT")
                .build();

        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userStoryService.createUserStory(10L, request, normalUserDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");

        verify(userStoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should list all stories for project without filters")
    void getAllUserStories_NoFilters_Success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userStoryRepository.findAllByProjectId(10L)).thenReturn(List.of(sampleStory));

        List<UserStoryResponse> result = userStoryService.getAllUserStories(10L, null, null, normalUserDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should filter stories by status and priority")
    void getAllUserStories_WithStatusAndPriority_Success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userStoryRepository.findAllByProjectIdAndStatusAndPriority(10L, StoryStatus.DRAFT, StoryPriority.HIGH))
                .thenReturn(List.of(sampleStory));

        List<UserStoryResponse> result = userStoryService.getAllUserStories(10L, StoryStatus.DRAFT, StoryPriority.HIGH, normalUserDetails);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Customer Login");
    }

    @Test
    @DisplayName("Should retrieve user story by ID")
    void getUserStoryById_Success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(sampleStory));

        UserStoryResponse response = userStoryService.getUserStoryById(10L, 100L, normalUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when story does not exist in project")
    void getUserStoryById_NotFound_ThrowsException() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userStoryRepository.findByIdAndProjectId(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userStoryService.getUserStoryById(10L, 999L, normalUserDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User Story not found");
    }

    @Test
    @DisplayName("Should update user story fields successfully")
    void updateUserStory_Success() {
        UpdateUserStoryRequest updateRequest = UpdateUserStoryRequest.builder()
                .title("Updated Title")
                .status(StoryStatus.READY)
                .priority(StoryPriority.CRITICAL)
                .build();

        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(sampleStory));
        when(userStoryRepository.save(any(UserStory.class))).thenReturn(sampleStory);

        UserStoryResponse response = userStoryService.updateUserStory(10L, 100L, updateRequest, normalUserDetails);

        assertThat(response).isNotNull();
        assertThat(sampleStory.getTitle()).isEqualTo("Updated Title");
        assertThat(sampleStory.getStatus()).isEqualTo(StoryStatus.READY);
        assertThat(sampleStory.getPriority()).isEqualTo(StoryPriority.CRITICAL);
    }

    @Test
    @DisplayName("Should delete user story successfully")
    void deleteUserStory_Success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(sampleStory));

        userStoryService.deleteUserStory(10L, 100L, normalUserDetails);

        verify(userStoryRepository).delete(sampleStory);
    }

    @Test
    @DisplayName("Should allow admin to access stories under any project")
    void admin_CanAccessAnyProjectStories_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(sampleProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(sampleStory));

        UserStoryResponse response = userStoryService.getUserStoryById(10L, 100L, adminUserDetails);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
    }
}
