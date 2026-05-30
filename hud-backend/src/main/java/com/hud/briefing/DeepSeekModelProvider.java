package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;
import java.time.Duration;

/**
 * Strategy for DeepSeek LLM provider.
 * DeepSeek is OpenAI-compatible, so we use OpenAiChatModel as an adapter.
 */
@Component
public class DeepSeekModelProvider implements ChatModelProvider {

    @Override
    public ChatLanguageModel buildModel(LlmConfig config) {
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .baseUrl(config.getBaseUrl() != null && !config.getBaseUrl().isBlank() 
                        ? config.getBaseUrl() 
                        : "https://api.deepseek.com")
                .temperature(0.0)
                .timeout(Duration.ofMinutes(10))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Override
    public boolean supports(LlmProvider provider) {
        return provider == LlmProvider.DEEPSEEK;
    }
}
