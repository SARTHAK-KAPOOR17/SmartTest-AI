package com.smarttestai.service;

import com.smarttestai.dto.request.CreateUserStoryRequest;
import com.smarttestai.dto.request.UpdateUserStoryRequest;
import com.smarttestai.dto.response.UserStoryResponse;
import com.smarttestai.entity.Project;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.UserStory;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.UserStoryRepository;
import com.smarttestai.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserStoryService {

    private static final Logger log = LoggerFactory.getLogger(UserStoryService.class);

    private final UserStoryRepository userStoryRepository;
    private final ProjectRepository projectRepository;

    public UserStoryService(UserStoryRepository userStoryRepository, ProjectRepository projectRepository) {
        this.userStoryRepository = userStoryRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public UserStoryResponse createUserStory(Long projectId, CreateUserStoryRequest request, CustomUserDetails currentUser) {
        log.info("Creating user story '{}' under project id: {} by user: {}", request.getTitle(), projectId, currentUser.getEmail());
        Project project = getProjectOrThrow(projectId, currentUser);

        UserStory userStory = UserStory.builder()
                .project(project)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .acceptanceCriteria(request.getAcceptanceCriteria().trim())
                .priority(request.getPriority() != null ? request.getPriority() : StoryPriority.MEDIUM)
                .status(request.getStatus() != null ? request.getStatus() : StoryStatus.DRAFT)
                .build();

        UserStory savedStory = userStoryRepository.save(userStory);
        log.info("Successfully created user story id: {} under project id: {}", savedStory.getId(), projectId);

        return UserStoryResponse.fromEntity(savedStory);
    }

    public List<UserStoryResponse> getAllUserStories(Long projectId, StoryStatus status, StoryPriority priority, CustomUserDetails currentUser) {
        log.info("Listing user stories for project id: {} (status: {}, priority: {}) by user: {}", projectId, status, priority, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);

        List<UserStory> stories;
        if (status != null && priority != null) {
            stories = userStoryRepository.findAllByProjectIdAndStatusAndPriority(projectId, status, priority);
        } else if (status != null) {
            stories = userStoryRepository.findAllByProjectIdAndStatus(projectId, status);
        } else if (priority != null) {
            stories = userStoryRepository.findAllByProjectIdAndPriority(projectId, priority);
        } else {
            stories = userStoryRepository.findAllByProjectId(projectId);
        }

        return stories.stream()
                .map(UserStoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public UserStoryResponse getUserStoryById(Long projectId, Long storyId, CustomUserDetails currentUser) {
        log.info("Fetching user story id: {} under project id: {} by user: {}", storyId, projectId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);

        UserStory story = userStoryRepository.findByIdAndProjectId(storyId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("User Story", storyId));

        return UserStoryResponse.fromEntity(story);
    }

    @Transactional
    public UserStoryResponse updateUserStory(Long projectId, Long storyId, UpdateUserStoryRequest request, CustomUserDetails currentUser) {
        log.info("Updating user story id: {} under project id: {} by user: {}", storyId, projectId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);

        UserStory story = userStoryRepository.findByIdAndProjectId(storyId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("User Story", storyId));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            story.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            story.setDescription(request.getDescription().trim());
        }
        if (request.getAcceptanceCriteria() != null && !request.getAcceptanceCriteria().isBlank()) {
            story.setAcceptanceCriteria(request.getAcceptanceCriteria().trim());
        }
        if (request.getPriority() != null) {
            story.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            story.setStatus(request.getStatus());
        }

        UserStory updatedStory = userStoryRepository.save(story);
        log.info("Successfully updated user story id: {}", storyId);

        return UserStoryResponse.fromEntity(updatedStory);
    }

    @Transactional
    public void deleteUserStory(Long projectId, Long storyId, CustomUserDetails currentUser) {
        log.info("Deleting user story id: {} under project id: {} by user: {}", storyId, projectId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);

        UserStory story = userStoryRepository.findByIdAndProjectId(storyId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("User Story", storyId));

        userStoryRepository.delete(story);
        log.info("Successfully deleted user story id: {} from project id: {}", storyId, projectId);
    }

    private Project getProjectOrThrow(Long projectId, CustomUserDetails currentUser) {
        if (currentUser.isAdmin()) {
            return projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        } else {
            return projectRepository.findByIdAndOwnerId(projectId, currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        }
    }
}
