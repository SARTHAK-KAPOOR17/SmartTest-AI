package com.smarttestai.dto.response;

import com.smarttestai.entity.Project;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response payload representing a project")
public class ProjectResponse {

    @Schema(description = "Unique identifier of the project", example = "1")
    private Long id;

    @Schema(description = "Name of the project", example = "E-Commerce Testing")
    private String name;

    @Schema(description = "Description of the project", example = "Automation testing project for retail web app")
    private String description;

    @Schema(description = "User ID of the project owner", example = "1")
    private Long ownerId;

    @Schema(description = "Email of the project owner", example = "user@smarttestai.com")
    private String ownerEmail;

    @Schema(description = "Timestamp when the project was created")
    private Instant createdAt;

    @Schema(description = "Timestamp when the project was last updated")
    private Instant updatedAt;

    public ProjectResponse() {
    }

    public ProjectResponse(Long id, String name, String description, Long ownerId, String ownerEmail, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerEmail = ownerEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
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

    public static ProjectResponse fromEntity(Project project) {
        if (project == null) {
            return null;
        }
        Long ownerId = project.getOwner() != null ? project.getOwner().getId() : null;
        String ownerEmail = project.getOwner() != null ? project.getOwner().getEmail() : null;

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(ownerId)
                .ownerEmail(ownerEmail)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private Long ownerId;
        private String ownerEmail;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder ownerId(Long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder ownerEmail(String ownerEmail) {
            this.ownerEmail = ownerEmail;
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

        public ProjectResponse build() {
            return new ProjectResponse(id, name, description, ownerId, ownerEmail, createdAt, updatedAt);
        }
    }
}
