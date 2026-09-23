package com.smarttestai.selenium.model;

import java.util.Objects;

public class SeleniumStepModel {
    private Integer stepNumber;
    private SeleniumActionType actionType;
    private Locator locator;
    private String value;
    private String expectedResult;
    private String description;

    public SeleniumStepModel() {
    }

    public SeleniumStepModel(Integer stepNumber, SeleniumActionType actionType, Locator locator, String value, String expectedResult, String description) {
        this.stepNumber = stepNumber;
        this.actionType = actionType;
        this.locator = locator;
        this.value = value;
        this.expectedResult = expectedResult;
        this.description = description;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public SeleniumActionType getActionType() {
        return actionType;
    }

    public void setActionType(SeleniumActionType actionType) {
        this.actionType = actionType;
    }

    public Locator getLocator() {
        return locator;
    }

    public void setLocator(Locator locator) {
        this.locator = locator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeleniumStepModel that = (SeleniumStepModel) o;
        return Objects.equals(stepNumber, that.stepNumber) &&
                actionType == that.actionType &&
                Objects.equals(locator, that.locator) &&
                Objects.equals(value, that.value) &&
                Objects.equals(expectedResult, that.expectedResult) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepNumber, actionType, locator, value, expectedResult, description);
    }

    @Override
    public String toString() {
        return "SeleniumStepModel{" +
                "stepNumber=" + stepNumber +
                ", actionType=" + actionType +
                ", locator=" + locator +
                ", value='" + value + '\'' +
                ", expectedResult='" + expectedResult + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
