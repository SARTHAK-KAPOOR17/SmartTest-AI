package com.smarttestai.dto.response;

import com.smarttestai.entity.TestStep;

public class TestStepResponse {
    private Integer stepNumber;
    private String action;
    private String expectedResult;

    public TestStepResponse() {
    }

    public TestStepResponse(Integer stepNumber, String action, String expectedResult) {
        this.stepNumber = stepNumber;
        this.action = action;
        this.expectedResult = expectedResult;
    }

    public static TestStepResponse fromEntity(TestStep entity) {
        if (entity == null) return null;
        return new TestStepResponse(
                entity.getStepNumber(),
                entity.getAction(),
                entity.getExpectedResult()
        );
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
