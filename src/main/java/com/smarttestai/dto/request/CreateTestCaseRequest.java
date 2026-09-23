package com.smarttestai.dto.request;

import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class CreateTestCaseRequest {

    @NotBlank(message = "Test case title is required")
    @Size(max = 200, message = "Test case title must not exceed 200 characters")
    @Schema(description = "Concise title of the test scenario", example = "Successful Login with Valid Credentials")
    private String title;

    @NotBlank(message = "Test case description is required")
    @Schema(description = "Detailed description of what is being tested", example = "Verifies user can authenticate with valid credentials")
    private String description;

    @NotNull(message = "Test case type is required (POSITIVE, NEGATIVE, EDGE_CASE, SECURITY)")
    @Schema(description = "Category of the test case", example = "POSITIVE")
    private TestCaseType type;

    @Schema(description = "Priority of the test case", example = "CRITICAL")
    private TestPriority priority = TestPriority.MEDIUM;

    @Schema(description = "Prerequisite system state or data required", example = "User account exists and is active")
    private String preconditions;

    @NotEmpty(message = "At least one test step is required")
    @Valid
    @Schema(description = "Ordered sequence of steps")
    private List<TestStepDto> steps = new ArrayList<>();

    @NotBlank(message = "Expected result is required")
    @Schema(description = "Ultimate expected end-to-end outcome", example = "User is logged in and redirected to /dashboard")
    private String expectedResult;

    public CreateTestCaseRequest() {
    }

    public CreateTestCaseRequest(String title, String description, TestCaseType type, TestPriority priority,
                                 String preconditions, List<TestStepDto> steps, String expectedResult) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority != null ? priority : TestPriority.MEDIUM;
        this.preconditions = preconditions;
        if (steps != null) {
            this.steps = steps;
        }
        this.expectedResult = expectedResult;
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

    public String getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(String preconditions) {
        this.preconditions = preconditions;
    }

    public List<TestStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStepDto> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}
