package com.smarttestai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttestai.ai.PromptBuilderService;
import com.smarttestai.ai.TestCaseGeneratorService;
import com.smarttestai.ai.impl.LangChain4jTestCaseGenerator;
import com.smarttestai.ai.impl.MockTestCaseGenerator;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${llm.provider:openai}")
    private String provider;

    @Value("${llm.api-key:demo-api-key}")
    private String apiKey;

    @Value("${llm.model-name:gpt-4o-mini}")
    private String modelName;

    @Value("${llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${llm.temperature:0.2}")
    private Double temperature;

    @Value("${llm.timeout-seconds:30}")
    private Integer timeoutSeconds;

    @Value("${llm.max-tokens:3000}")
    private Integer maxTokens;

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
    public ChatLanguageModel openAiChatModel() {
        log.info("Configuring OpenAI ChatLanguageModel with model: '{}', baseUrl: '{}'", modelName, baseUrl);
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxTokens(maxTokens)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
    public TestCaseGeneratorService langChain4jTestCaseGenerator(ChatLanguageModel chatLanguageModel,
                                                                 PromptBuilderService promptBuilderService,
                                                                 ObjectMapper objectMapper) {
        log.info("Registering LangChain4jTestCaseGenerator with active ChatLanguageModel");
        return new LangChain4jTestCaseGenerator(chatLanguageModel, promptBuilderService, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "mock")
    public TestCaseGeneratorService mockTestCaseGenerator() {
        log.info("Registering MockTestCaseGenerator for offline/test mode");
        return new MockTestCaseGenerator();
    }
}
