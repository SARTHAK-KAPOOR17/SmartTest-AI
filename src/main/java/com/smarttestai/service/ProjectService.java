package com.smarttestai.service;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.entity.Project;
import com.smarttestai.entity.User;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.UserRepository;
import com.smarttestai.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, CustomUserDetails currentUser) {
        log.info("Creating new project with name: '{}' for user: {}", request.getName(), currentUser.getEmail());

        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        Project project = Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .owner(owner)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Successfully created project with id: {} for owner: {}", savedProject.getId(), owner.getEmail());

        return ProjectResponse.fromEntity(savedProject);
    }

    public List<ProjectResponse> getAllProjects(CustomUserDetails currentUser) {
        if (currentUser.isAdmin()) {
            log.info("Admin {} fetching all projects across all users", currentUser.getEmail());
            return projectRepository.findAll()
                    .stream()
                    .map(ProjectResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        log.info("User {} fetching their owned projects", currentUser.getEmail());
        return projectRepository.findAllByOwnerId(currentUser.getId())
                .stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long id, CustomUserDetails currentUser) {
        log.info("Fetching project with id: {} for user: {}", id, currentUser.getEmail());

        Project project;
        if (currentUser.isAdmin()) {
            project = projectRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        } else {
            project = projectRepository.findByIdAndOwnerId(id, currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        }

        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    public void deleteProject(Long id, CustomUserDetails currentUser) {
        log.info("Attempting to delete project with id: {} by user: {}", id, currentUser.getEmail());

        if (currentUser.isAdmin()) {
            if (!projectRepository.existsById(id)) {
                throw new ResourceNotFoundException("Project", id);
            }
            projectRepository.deleteById(id);
            log.info("Admin {} successfully deleted project with id: {}", currentUser.getEmail(), id);
        } else {
            if (!projectRepository.existsByIdAndOwnerId(id, currentUser.getId())) {
                throw new ResourceNotFoundException("Project", id);
            }
            projectRepository.deleteById(id);
            log.info("User {} successfully deleted their project with id: {}", currentUser.getEmail(), id);
        }
    }
}
