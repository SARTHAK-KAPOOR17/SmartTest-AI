package com.smarttestai.dto.request;

import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating an existing user story")
public class UpdateUserStoryRequest {

    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    @Schema(description = "Updated title of the user story", example = "User Authentication & OAuth2 Login")
    private String title;

    @Size(min = 5, max = 2000, message = "Description must be between 5 and 2000 characters")
    @Schema(description = "Updated narrative of the user story", example = "As a user, I want to authenticate via standard credentials or OAuth.")
    private String description;

    @Size(min = 5, max = 4000, message = "Acceptance criteria must be between 5 and 4000 characters")
    @Schema(description = "Updated acceptance criteria and scenarios", example = "1. Support Bearer JWT.\n2. Invalidate expired tokens.")
    private String acceptanceCriteria;

    @Schema(description = "Updated priority level", example = "CRITICAL")
    private StoryPriority priority;

    @Schema(description = "Updated status (e.g., READY for AI test generation)", example = "READY")
    private StoryStatus status;

    public UpdateUserStoryRequest() {
    }

    public UpdateUserStoryRequest(String title, String description, String acceptanceCriteria, StoryPriority priority, StoryStatus status) {
        this.title = title;
        this.description = description;
        this.acceptanceCriteria = acceptanceCriteria;
        this.priority = priority;
        this.status = status;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String description;
        private String acceptanceCriteria;
        private StoryPriority priority;
        private StoryStatus status;

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

        public UpdateUserStoryRequest build() {
            return new UpdateUserStoryRequest(title, description, acceptanceCriteria, priority, status);
        }
    }
}
