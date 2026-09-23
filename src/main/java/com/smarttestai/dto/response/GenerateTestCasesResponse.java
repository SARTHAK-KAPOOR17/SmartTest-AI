package com.smarttestai.dto.response;

import java.util.ArrayList;
import java.util.List;

public class GenerateTestCasesResponse {

    private Long userStoryId;
    private String promptVersion;
    private int generatedCount;
    private List<TestCaseResponse> testCases = new ArrayList<>();

    public GenerateTestCasesResponse() {
    }

    public GenerateTestCasesResponse(Long userStoryId, String promptVersion, int generatedCount, List<TestCaseResponse> testCases) {
        this.userStoryId = userStoryId;
        this.promptVersion = promptVersion;
        this.generatedCount = generatedCount;
        if (testCases != null) {
            this.testCases = testCases;
        }
    }

    public Long getUserStoryId() {
        return userStoryId;
    }

    public void setUserStoryId(Long userStoryId) {
        this.userStoryId = userStoryId;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(int generatedCount) {
        this.generatedCount = generatedCount;
    }

    public List<TestCaseResponse> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCaseResponse> testCases) {
        this.testCases = testCases != null ? testCases : new ArrayList<>();
    }
}
