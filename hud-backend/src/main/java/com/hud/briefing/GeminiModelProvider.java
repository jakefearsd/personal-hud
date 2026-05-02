package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class GeminiModelProvider implements ChatModelProvider {
    @Override
    public ChatLanguageModel buildModel(LlmConfig config) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .temperature(0.0)
                .timeout(Duration.ofMinutes(10))
                .build();
    }

    @Override
    public boolean supports(LlmProvider provider) {
        return provider == LlmProvider.GEMINI;
    }
}
