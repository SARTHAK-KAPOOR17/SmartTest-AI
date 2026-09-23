package com.smarttestai.dto.request;

import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating a new user story")
public class CreateUserStoryRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    @Schema(description = "Short, descriptive title of the user story", example = "User Authentication & Login", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 2000, message = "Description must be between 5 and 2000 characters")
    @Schema(description = "Standard user story narrative (As a... I want... So that...)", example = "As a registered user, I want to login with email and password so that I can access my dashboard.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @NotBlank(message = "Acceptance criteria is required")
    @Size(min = 5, max = 4000, message = "Acceptance criteria must be between 5 and 4000 characters")
    @Schema(description = "Specific conditions of satisfaction and scenarios for test generation", example = "1. Valid email and password returns JWT.\n2. Invalid password returns 401.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String acceptanceCriteria;

    @Schema(description = "Priority level of the user story", example = "HIGH", defaultValue = "MEDIUM")
    private StoryPriority priority = StoryPriority.MEDIUM;

    @Schema(description = "Lifecycle status of the user story", example = "DRAFT", defaultValue = "DRAFT")
    private StoryStatus status = StoryStatus.DRAFT;

    public CreateUserStoryRequest() {
    }

    public CreateUserStoryRequest(String title, String description, String acceptanceCriteria, StoryPriority priority, StoryStatus status) {
        this.title = title;
        this.description = description;
        this.acceptanceCriteria = acceptanceCriteria;
        this.priority = priority != null ? priority : StoryPriority.MEDIUM;
        this.status = status != null ? status : StoryStatus.DRAFT;
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
        return priority != null ? priority : StoryPriority.MEDIUM;
    }

    public void setPriority(StoryPriority priority) {
        this.priority = priority != null ? priority : StoryPriority.MEDIUM;
    }

    public StoryStatus getStatus() {
        return status != null ? status : StoryStatus.DRAFT;
    }

    public void setStatus(StoryStatus status) {
        this.status = status != null ? status : StoryStatus.DRAFT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String description;
        private String acceptanceCriteria;
        private StoryPriority priority = StoryPriority.MEDIUM;
        private StoryStatus status = StoryStatus.DRAFT;

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

        public CreateUserStoryRequest build() {
            return new CreateUserStoryRequest(title, description, acceptanceCriteria, priority, status);
        }
    }
}
