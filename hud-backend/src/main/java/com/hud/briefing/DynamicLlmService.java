package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DynamicLlmService {

    private final LlmConfigRepository repository;

    public DynamicLlmService(LlmConfigRepository repository) {
        this.repository = repository;
    }

    public record NamedChatModel(String name, ChatLanguageModel model) {}

    public List<NamedChatModel> getActiveModels() {
        List<LlmConfig> configs = repository.findByActiveTrue();
        return configs.stream()
                .map(this::buildModel)
                .collect(Collectors.toList());
    }

    private NamedChatModel buildModel(LlmConfig config) {
        ChatLanguageModel model;
        if (config.getProvider() == LlmProvider.GEMINI) {
            model = GoogleAiGeminiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .temperature(0.0)
                    .timeout(Duration.ofMinutes(10))
                    .build();
        } else {
            model = OllamaChatModel.builder()
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModelName())
                    .temperature(0.0)
                    .numCtx(config.getNumCtx() != null ? config.getNumCtx() : 32768)
                    .timeout(Duration.ofMinutes(10))
                    .build();
        }
        return new NamedChatModel(config.getName(), model);
    }
}
