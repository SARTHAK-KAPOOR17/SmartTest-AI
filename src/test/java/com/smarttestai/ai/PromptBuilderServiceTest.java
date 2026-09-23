package com.smarttestai.ai;

import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.UserStory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderServiceTest {

    private PromptBuilderService promptBuilderService;

    @BeforeEach
    void setUp() {
        promptBuilderService = new PromptBuilderService();
    }

    @Test
    @DisplayName("buildSystemPrompt should contain schema instructions, prompt version, and security guards")
    void buildSystemPrompt_shouldContainRequiredDirectives() {
        String systemPrompt = promptBuilderService.buildSystemPrompt();

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("POSITIVE"));
        assertTrue(systemPrompt.contains("NEGATIVE"));
        assertTrue(systemPrompt.contains("EDGE_CASE"));
        assertTrue(systemPrompt.contains("stepNumber"));
        assertTrue(systemPrompt.contains("action"));
        assertTrue(systemPrompt.contains("expectedResult"));
        assertTrue(systemPrompt.contains("SECURITY GUARD"));
        assertTrue(systemPrompt.contains("<user_story_input>"));
    }

    @Test
    @DisplayName("buildUserPrompt should safely encapsulate story fields inside XML boundary tags")
    void buildUserPrompt_shouldWrapStoryInBoundaryTags() {
        UserStory story = UserStory.builder()
                .id(1L)
                .title("Customer Checkout")
                .description("As a shopper, I want to checkout.")
                .acceptanceCriteria("Scenario 1: Card payment succeeds.")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.READY)
                .build();

        String userPrompt = promptBuilderService.buildUserPrompt(story);

        assertNotNull(userPrompt);
        assertTrue(userPrompt.contains("<user_story_input>"));
        assertTrue(userPrompt.contains("</user_story_input>"));
        assertTrue(userPrompt.contains("Title: Customer Checkout"));
        assertTrue(userPrompt.contains("Priority: HIGH"));
        assertTrue(userPrompt.contains("Description:\nAs a shopper, I want to checkout."));
        assertTrue(userPrompt.contains("Acceptance Criteria:\nScenario 1: Card payment succeeds."));
    }

    @Test
    @DisplayName("Prompt version should be v1.0")
    void promptVersion_shouldBeV1() {
        assertEquals("v1.0", PromptBuilderService.PROMPT_VERSION);
    }
}
