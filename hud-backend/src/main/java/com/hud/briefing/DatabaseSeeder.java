package com.hud.briefing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder {

    private final LlmConfigRepository repository;

    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String defaultBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name}")
    private String defaultModelName;

    @Value("${langchain4j.ollama.chat-model.num-ctx}")
    private Integer defaultNumCtx;

    public DatabaseSeeder(LlmConfigRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedDefaultConfig() {
        if (repository.count() == 0) {
            System.out.println("Seeding default LLM configuration...");
            LlmConfig config = new LlmConfig("Local Gemma", LlmProvider.OLLAMA, defaultModelName, true);
            config.setBaseUrl(defaultBaseUrl);
            config.setNumCtx(defaultNumCtx);
            repository.save(config);
        }
    }
}
