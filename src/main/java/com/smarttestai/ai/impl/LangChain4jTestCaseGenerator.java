package com.smarttestai.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttestai.ai.PromptBuilderService;
import com.smarttestai.ai.TestCaseGeneratorService;
import com.smarttestai.dto.ai.AiGeneratedTestCaseDto;
import com.smarttestai.dto.ai.AiTestCaseGenerationResponse;
import com.smarttestai.dto.ai.AiTestStepDto;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.entity.UserStory;
import com.smarttestai.exception.AiInvalidResponseException;
import com.smarttestai.exception.AiServiceException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LangChain4jTestCaseGenerator implements TestCaseGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jTestCaseGenerator.class);

    private final ChatLanguageModel chatLanguageModel;
    private final PromptBuilderService promptBuilderService;
    private final ObjectMapper objectMapper;

    public LangChain4jTestCaseGenerator(ChatLanguageModel chatLanguageModel,
                                         PromptBuilderService promptBuilderService,
                                         ObjectMapper objectMapper) {
        this.chatLanguageModel = chatLanguageModel;
        this.promptBuilderService = promptBuilderService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiGeneratedTestCaseDto> generateTestCases(UserStory userStory) {
        log.info("Invoking LangChain4j model for user story id: '{}'", userStory.getId());

        String systemPrompt = promptBuilderService.buildSystemPrompt();
        String userPrompt = promptBuilderService.buildUserPrompt(userStory);

        String rawResponseText;
        try {
            Response<AiMessage> response = chatLanguageModel.generate(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(userPrompt)
            );
            rawResponseText = response.content().text();
        } catch (Exception e) {
            log.error("Error communicating with AI model for story id: {}", userStory.getId(), e);
            throw new AiServiceException("Failed to communicate with AI provider: " + e.getMessage(), e);
        }

        if (rawResponseText == null || rawResponseText.isBlank()) {
            throw new AiInvalidResponseException("AI provider returned empty response");
        }

        AiTestCaseGenerationResponse parsedResponse = parseAndValidate(rawResponseText);
        log.info("Successfully generated and validated {} test cases for user story id: {}",
                parsedResponse.getTestCases().size(), userStory.getId());

        return parsedResponse.getTestCases();
    }

    private AiTestCaseGenerationResponse parseAndValidate(String rawJson) {
        String cleanedJson = cleanJson(rawJson);

        AiTestCaseGenerationResponse response;
        try {
            response = objectMapper.readValue(cleanedJson, AiTestCaseGenerationResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", cleanedJson, e);
            throw new AiInvalidResponseException("AI response is not valid JSON: " + e.getMessage());
        }

        if (response == null || response.getTestCases() == null || response.getTestCases().isEmpty()) {
            throw new AiInvalidResponseException("AI response contains no test cases");
        }

        Set<String> validTypes = new HashSet<>();
        for (TestCaseType type : TestCaseType.values()) {
            validTypes.add(type.name());
        }

        Set<String> validPriorities = new HashSet<>();
        for (TestPriority priority : TestPriority.values()) {
            validPriorities.add(priority.name());
        }

        boolean hasPositive = false;
        for (int i = 0; i < response.getTestCases().size(); i++) {
            AiGeneratedTestCaseDto tc = response.getTestCases().get(i);
            int index = i + 1;

            if (tc.getTitle() == null || tc.getTitle().isBlank()) {
                throw new AiInvalidResponseException("Test case #" + index + " is missing title");
            }
            if (tc.getDescription() == null || tc.getDescription().isBlank()) {
                throw new AiInvalidResponseException("Test case #" + index + " is missing description");
            }

            if (tc.getType() == null || !validTypes.contains(tc.getType().toUpperCase())) {
                tc.setType(TestCaseType.POSITIVE.name());
            } else {
                tc.setType(tc.getType().toUpperCase());
            }

            if (tc.getPriority() == null || !validPriorities.contains(tc.getPriority().toUpperCase())) {
                tc.setPriority(TestPriority.MEDIUM.name());
            } else {
                tc.setPriority(tc.getPriority().toUpperCase());
            }

            if (TestCaseType.POSITIVE.name().equals(tc.getType())) {
                hasPositive = true;
            }

            if (tc.getSteps() == null || tc.getSteps().isEmpty()) {
                throw new AiInvalidResponseException("Test case #" + index + " ('" + tc.getTitle() + "') has no steps");
            }

            for (int s = 0; s < tc.getSteps().size(); s++) {
                AiTestStepDto step = tc.getSteps().get(s);
                if (step.getStepNumber() == null) {
                    step.setStepNumber(s + 1);
                }
                if (step.getAction() == null || step.getAction().isBlank()) {
                    throw new AiInvalidResponseException("Step " + (s + 1) + " of test case #" + index + " has blank action");
                }
            }

            if (tc.getExpectedResult() == null || tc.getExpectedResult().isBlank()) {
                throw new AiInvalidResponseException("Test case #" + index + " is missing expectedResult");
            }
        }

        return response;
    }

    private String cleanJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
