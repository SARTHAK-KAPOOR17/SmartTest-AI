package com.smarttestai.service;

import com.smarttestai.dto.request.CreateProjectRequest;
import com.smarttestai.dto.response.ProjectResponse;
import com.smarttestai.entity.Project;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.repository.ProjectRepository;
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

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.info("Creating new project with name: {}", request.getName());

        Project project = Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Successfully created project with id: {}", savedProject.getId());

        return ProjectResponse.fromEntity(savedProject);
    }

    public List<ProjectResponse> getAllProjects() {
        log.info("Fetching all projects");
        return projectRepository.findAll()
                .stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long id) {
        log.info("Fetching project with id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        log.info("Deleting project with id: {}", id);
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        }
        projectRepository.deleteById(id);
        log.info("Successfully deleted project with id: {}", id);
    }
}
