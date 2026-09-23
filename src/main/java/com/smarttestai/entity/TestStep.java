package com.smarttestai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class TestStep {

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "action", nullable = false, length = 500)
    private String action;

    @Column(name = "expected_result", length = 500)
    private String expectedResult;

    public TestStep() {
    }

    public TestStep(Integer stepNumber, String action, String expectedResult) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestStep testStep = (TestStep) o;
        return Objects.equals(stepNumber, testStep.stepNumber) &&
                Objects.equals(action, testStep.action) &&
                Objects.equals(expectedResult, testStep.expectedResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepNumber, action, expectedResult);
    }

    @Override
    public String toString() {
        return "TestStep{" +
                "stepNumber=" + stepNumber +
                ", action='" + action + '\'' +
                ", expectedResult='" + expectedResult + '\'' +
                '}';
    }
}
