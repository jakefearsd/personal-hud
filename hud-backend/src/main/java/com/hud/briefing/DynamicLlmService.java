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

    public record NamedChatModel(String name, ChatLanguageModel model) {}

    public List<NamedChatModel> getActiveModels() {
        return repository.findByActiveTrue().stream()
                .map(config -> new NamedChatModel(
                        config.getName(), 
                        findProvider(config.getProvider()).buildModel(config))
                )
                .collect(Collectors.toList());
    }

    private ChatModelProvider findProvider(LlmProvider provider) {
        return providers.stream()
                .filter(p -> p.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported LLM Provider: " + provider));
    }
}
