package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to manage active LLM models based on database configuration.
 */
@Service
public class DynamicLlmService {

    private final LlmConfigRepository repository;
    private final List<ChatModelProvider> providers;

    public DynamicLlmService(LlmConfigRepository repository, List<ChatModelProvider> providers) {
        this.repository = repository;
        this.providers = providers;
    }

    public LlmConfigRepository getConfigRepository() {
        return repository;
    }

    public record NamedChatModel(String name, ChatLanguageModel model) {}

    public List<NamedChatModel> getActiveModels() {
        return repository.findByActiveTrue().stream()
                .map(this::buildSpecificModel)
                .collect(Collectors.toList());
    }

    public NamedChatModel buildSpecificModel(LlmConfig config) {
        String providerType = config.getProvider() == LlmProvider.GEMINI ? "Cloud" : "Local";
        String descriptiveName = providerType + ": " + config.getName() + " [" + config.getModelName() + "]";
        return new NamedChatModel(
                descriptiveName, 
                findProvider(config.getProvider()).buildModel(config)
        );
    }

    private ChatModelProvider findProvider(LlmProvider provider) {
        return providers.stream()
                .filter(p -> p.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported LLM Provider: " + provider));
    }
}
