package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class OllamaModelProvider implements ChatModelProvider {
    @Override
    public ChatLanguageModel buildModel(LlmConfig config) {
        return OllamaChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .temperature(0.0)
                .numCtx(config.getNumCtx() != null ? config.getNumCtx() : 131072)
                .timeout(Duration.ofMinutes(10))
                .build();
    }

    @Override
    public boolean supports(LlmProvider provider) {
        return provider == LlmProvider.OLLAMA;
    }
}
