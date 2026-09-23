package com.smarttestai.ai.impl;

import com.smarttestai.ai.TestCaseGeneratorService;
import com.smarttestai.dto.ai.AiGeneratedTestCaseDto;
import com.smarttestai.dto.ai.AiTestStepDto;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.entity.UserStory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockTestCaseGenerator implements TestCaseGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(MockTestCaseGenerator.class);

    @Override
    public List<AiGeneratedTestCaseDto> generateTestCases(UserStory userStory) {
        log.info("Generating mock test cases for user story id: {}", userStory.getId());

        String storyTitle = userStory.getTitle() != null ? userStory.getTitle() : "User Story";

        List<AiGeneratedTestCaseDto> testCases = new ArrayList<>();

        // 1. Positive Scenario
        AiGeneratedTestCaseDto positive = new AiGeneratedTestCaseDto(
                "Successful execution: " + storyTitle,
                "Verifies that the happy path for " + storyTitle + " executes successfully with valid parameters.",
                TestCaseType.POSITIVE.name(),
                TestPriority.CRITICAL.name(),
                "System is active and prerequisite test data is provisioned",
                Arrays.asList(
                        new AiTestStepDto(1, "Navigate to the feature page for " + storyTitle, "Page loads successfully"),
                        new AiTestStepDto(2, "Provide valid standard inputs as specified in acceptance criteria", "Inputs are accepted"),
                        new AiTestStepDto(3, "Submit the action", "Request is processed without errors")
                ),
                "Feature executes successfully and expected confirmation is displayed."
        );
        testCases.add(positive);

        // 2. Negative Scenario
        AiGeneratedTestCaseDto negative = new AiGeneratedTestCaseDto(
                "Validation failure with invalid inputs: " + storyTitle,
                "Verifies that invalid or missing inputs trigger expected validation messages.",
                TestCaseType.NEGATIVE.name(),
                TestPriority.HIGH.name(),
                "User is on the entry page",
                Arrays.asList(
                        new AiTestStepDto(1, "Navigate to the feature page for " + storyTitle, "Page loads successfully"),
                        new AiTestStepDto(2, "Submit form with empty required fields", "Validation errors trigger"),
                        new AiTestStepDto(3, "Verify validation banner", "Field-specific error messages are clearly visible")
                ),
                "System rejects invalid request with HTTP 400 or inline validation banner."
        );
        testCases.add(negative);

        // 3. Edge Case Scenario
        AiGeneratedTestCaseDto edgeCase = new AiGeneratedTestCaseDto(
                "Boundary and edge condition handling: " + storyTitle,
                "Verifies behavior when input lengths hit maximum boundaries or special characters are supplied.",
                TestCaseType.EDGE_CASE.name(),
                TestPriority.MEDIUM.name(),
                "User account is active",
                Arrays.asList(
                        new AiTestStepDto(1, "Navigate to the feature input", "Input field is ready"),
                        new AiTestStepDto(2, "Enter maximum boundary length string with UTF-8 characters", "Characters entered cleanly"),
                        new AiTestStepDto(3, "Submit action and verify response", "System handles input gracefully without crashing")
                ),
                "System safely processes or truncates boundary input without internal server errors."
        );
        testCases.add(edgeCase);

        return testCases;
    }
}
