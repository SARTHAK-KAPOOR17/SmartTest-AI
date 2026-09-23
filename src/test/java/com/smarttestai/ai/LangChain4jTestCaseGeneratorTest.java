package com.smarttestai.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttestai.ai.impl.LangChain4jTestCaseGenerator;
import com.smarttestai.dto.ai.AiGeneratedTestCaseDto;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.entity.UserStory;
import com.smarttestai.exception.AiInvalidResponseException;
import com.smarttestai.exception.AiServiceException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LangChain4jTestCaseGeneratorTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private LangChain4jTestCaseGenerator generator;
    private UserStory testStory;

    @BeforeEach
    void setUp() {
        PromptBuilderService promptBuilderService = new PromptBuilderService();
        ObjectMapper objectMapper = new ObjectMapper();
        generator = new LangChain4jTestCaseGenerator(chatLanguageModel, promptBuilderService, objectMapper);

        testStory = UserStory.builder()
                .id(10L)
                .title("User Login")
                .description("As a registered user, I want to log in.")
                .acceptanceCriteria("Given credentials, when valid, then dashboard.")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.READY)
                .build();
    }

    @Test
    @DisplayName("Should successfully parse valid structured JSON from AI model")
    void generateTestCases_validJson_shouldReturnTestCases() {
        String validJson = """
                ```json
                {
                  "testCases": [
                    {
                      "title": "Valid Login",
                      "description": "User logs in with valid credentials",
                      "type": "POSITIVE",
                      "priority": "CRITICAL",
                      "preconditions": "Account exists",
                      "steps": [
                        { "stepNumber": 1, "action": "Go to login", "expectedResult": "Login page shown" },
                        { "stepNumber": 2, "action": "Click submit", "expectedResult": "Dashboard displayed" }
                      ],
                      "expectedResult": "Authenticated"
                    }
                  ]
                }
                ```
                """;

        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(Response.from(AiMessage.from(validJson)));

        List<AiGeneratedTestCaseDto> result = generator.generateTestCases(testStory);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Valid Login", result.get(0).getTitle());
        assertEquals(TestCaseType.POSITIVE.name(), result.get(0).getType());
        assertEquals(TestPriority.CRITICAL.name(), result.get(0).getPriority());
        assertEquals(2, result.get(0).getSteps().size());
        assertEquals("Go to login", result.get(0).getSteps().get(0).getAction());
    }

    @Test
    @DisplayName("Should throw AiInvalidResponseException when response is empty")
    void generateTestCases_emptyResponse_shouldThrowException() {
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(Response.from(AiMessage.from("   ")));

        assertThrows(AiInvalidResponseException.class, () -> generator.generateTestCases(testStory));
    }

    @Test
    @DisplayName("Should throw AiInvalidResponseException when response is not valid JSON")
    void generateTestCases_malformedJson_shouldThrowException() {
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(Response.from(AiMessage.from("This is not JSON at all")));

        assertThrows(AiInvalidResponseException.class, () -> generator.generateTestCases(testStory));
    }

    @Test
    @DisplayName("Should throw AiInvalidResponseException when test case has no steps")
    void generateTestCases_missingSteps_shouldThrowException() {
        String invalidJson = """
                {
                  "testCases": [
                    {
                      "title": "Login without steps",
                      "description": "Missing steps",
                      "type": "POSITIVE",
                      "priority": "LOW",
                      "steps": [],
                      "expectedResult": "Should fail"
                    }
                  ]
                }
                """;

        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenReturn(Response.from(AiMessage.from(invalidJson)));

        assertThrows(AiInvalidResponseException.class, () -> generator.generateTestCases(testStory));
    }

    @Test
    @DisplayName("Should throw AiServiceException when chat language model throws exception")
    void generateTestCases_modelFails_shouldThrowAiServiceException() {
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        assertThrows(AiServiceException.class, () -> generator.generateTestCases(testStory));
    }
}
