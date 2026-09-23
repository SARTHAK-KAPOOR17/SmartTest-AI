package com.smarttestai.dto.response;

import com.smarttestai.entity.TestCase;
import com.smarttestai.entity.TestCaseStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestCaseResponse {

    private Long id;
    private Long userStoryId;
    private String title;
    private String description;
    private TestCaseType type;
    private TestPriority priority;
    private TestCaseStatus status;
    private String preconditions;
    private List<TestStepResponse> steps = new ArrayList<>();
    private String expectedResult;
    private String promptVersion;
    private Instant createdAt;
    private Instant updatedAt;

    public TestCaseResponse() {
    }

    public TestCaseResponse(Long id, Long userStoryId, String title, String description,
                            TestCaseType type, TestPriority priority, TestCaseStatus status,
                            String preconditions, List<TestStepResponse> steps,
                            String expectedResult, String promptVersion,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userStoryId = userStoryId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.preconditions = preconditions;
        if (steps != null) {
            this.steps = steps;
        }
        this.expectedResult = expectedResult;
        this.promptVersion = promptVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TestCaseResponse fromEntity(TestCase entity) {
        if (entity == null) return null;

        List<TestStepResponse> stepResponses = entity.getSteps() != null
                ? entity.getSteps().stream().map(TestStepResponse::fromEntity).collect(Collectors.toList())
                : new ArrayList<>();

        return new TestCaseResponse(
                entity.getId(),
                entity.getUserStory() != null ? entity.getUserStory().getId() : null,
                entity.getTitle(),
                entity.getDescription(),
                entity.getType(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getPreconditions(),
                stepResponses,
                entity.getExpectedResult(),
                entity.getPromptVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserStoryId() {
        return userStoryId;
    }

    public void setUserStoryId(Long userStoryId) {
        this.userStoryId = userStoryId;
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

    public TestCaseType getType() {
        return type;
    }

    public void setType(TestCaseType type) {
        this.type = type;
    }

    public TestPriority getPriority() {
        return priority;
    }

    public void setPriority(TestPriority priority) {
        this.priority = priority;
    }

    public TestCaseStatus getStatus() {
        return status;
    }

    public void setStatus(TestCaseStatus status) {
        this.status = status;
    }

    public String getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(String preconditions) {
        this.preconditions = preconditions;
    }

    public List<TestStepResponse> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStepResponse> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
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
}
