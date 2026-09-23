package com.smarttestai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TestStepDto {

    @NotNull(message = "Step number is required")
    @Schema(description = "1-based step order number", example = "1")
    private Integer stepNumber;

    @NotBlank(message = "Step action is required")
    @Size(max = 500, message = "Step action must not exceed 500 characters")
    @Schema(description = "Action to perform in the step", example = "Navigate to /login")
    private String action;

    @Size(max = 500, message = "Expected result must not exceed 500 characters")
    @Schema(description = "Intermediate verification or outcome for this step", example = "Login form is displayed")
    private String expectedResult;

    public TestStepDto() {
    }

    public TestStepDto(Integer stepNumber, String action, String expectedResult) {
        this.stepNumber = stepNumber;
        this.action = action;
        this.expectedResult = expectedResult;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}
