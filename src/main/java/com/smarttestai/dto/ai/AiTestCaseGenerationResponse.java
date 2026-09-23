package com.smarttestai.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class AiTestCaseGenerationResponse {
    private List<AiGeneratedTestCaseDto> testCases = new ArrayList<>();

    public AiTestCaseGenerationResponse() {
    }

    public AiTestCaseGenerationResponse(List<AiGeneratedTestCaseDto> testCases) {
        if (testCases != null) {
            this.testCases = testCases;
        }
    }

    public List<AiGeneratedTestCaseDto> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<AiGeneratedTestCaseDto> testCases) {
        this.testCases = testCases != null ? testCases : new ArrayList<>();
    }
}
