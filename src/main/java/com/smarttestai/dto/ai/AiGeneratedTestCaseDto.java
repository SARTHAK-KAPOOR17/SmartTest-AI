package com.smarttestai.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class AiGeneratedTestCaseDto {
    private String title;
    private String description;
    private String type;
    private String priority;
    private String preconditions;
    private List<AiTestStepDto> steps = new ArrayList<>();
    private String expectedResult;

    public AiGeneratedTestCaseDto() {
    }

    public AiGeneratedTestCaseDto(String title, String description, String type, String priority,
                                  String preconditions, List<AiTestStepDto> steps, String expectedResult) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(String preconditions) {
        this.preconditions = preconditions;
    }

    public List<AiTestStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<AiTestStepDto> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}
