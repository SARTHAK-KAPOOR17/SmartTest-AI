package com.smarttestai.dto.request;

import com.smarttestai.entity.TestCaseStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UpdateTestCaseRequest {

    @Size(max = 200, message = "Test case title must not exceed 200 characters")
    @Schema(description = "Updated title of the test scenario", example = "Updated: Successful Login with Valid Credentials")
    private String title;

    @Schema(description = "Updated description of what is being tested")
    private String description;

    @Schema(description = "Updated category of the test case")
    private TestCaseType type;

    @Schema(description = "Updated priority of the test case")
    private TestPriority priority;

    @Schema(description = "Updated review status of the test case")
    private TestCaseStatus status;

    @Schema(description = "Updated preconditions")
    private String preconditions;

    @Valid
    @Schema(description = "Updated list of ordered steps")
    private List<TestStepDto> steps;

    @Schema(description = "Updated expected outcome")
    private String expectedResult;

    public UpdateTestCaseRequest() {
    }

    public UpdateTestCaseRequest(String title, String description, TestCaseType type, TestPriority priority,
                                 TestCaseStatus status, String preconditions, List<TestStepDto> steps,
                                 String expectedResult) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.preconditions = preconditions;
        this.steps = steps;
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

    public List<TestStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStepDto> steps) {
        this.steps = steps;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}
