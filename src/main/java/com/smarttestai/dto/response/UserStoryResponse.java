package com.smarttestai.dto.response;

import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.UserStory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response representation of a user story")
public class UserStoryResponse {

    @Schema(description = "Unique identifier of the user story", example = "1")
    private Long id;

    @Schema(description = "ID of the project to which this story belongs", example = "10")
    private Long projectId;

    @Schema(description = "Title of the user story", example = "User Authentication & Login")
    private String title;

    @Schema(description = "Description narrative of the user story", example = "As a user, I want to log in...")
    private String description;

    @Schema(description = "Acceptance criteria and scenarios", example = "1. Valid login returns JWT.")
    private String acceptanceCriteria;

    @Schema(description = "Priority level", example = "HIGH")
    private StoryPriority priority;

    @Schema(description = "Current lifecycle status", example = "DRAFT")
    private StoryStatus status;

    @Schema(description = "Timestamp when the user story was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when the user story was last updated")
    private Instant updatedAt;

    public UserStoryResponse() {
    }

    public UserStoryResponse(Long id, Long projectId, String title, String description,
                             String acceptanceCriteria, StoryPriority priority, StoryStatus status,
                             Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.acceptanceCriteria = acceptanceCriteria;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public StoryPriority getPriority() {
        return priority;
    }

    public void setPriority(StoryPriority priority) {
        this.priority = priority;
    }

    public StoryStatus getStatus() {
        return status;
    }

    public void setStatus(StoryStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static UserStoryResponse fromEntity(UserStory story) {
        if (story == null) {
            return null;
        }
        Long projId = story.getProject() != null ? story.getProject().getId() : null;
        return UserStoryResponse.builder()
                .id(story.getId())
                .projectId(projId)
                .title(story.getTitle())
                .description(story.getDescription())
                .acceptanceCriteria(story.getAcceptanceCriteria())
                .priority(story.getPriority())
                .status(story.getStatus())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long projectId;
        private String title;
        private String description;
        private String acceptanceCriteria;
        private StoryPriority priority;
        private StoryStatus status;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder acceptanceCriteria(String acceptanceCriteria) {
            this.acceptanceCriteria = acceptanceCriteria;
            return this;
        }

        public Builder priority(StoryPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder status(StoryStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserStoryResponse build() {
            return new UserStoryResponse(id, projectId, title, description, acceptanceCriteria, priority, status, createdAt, updatedAt);
        }
    }
}
