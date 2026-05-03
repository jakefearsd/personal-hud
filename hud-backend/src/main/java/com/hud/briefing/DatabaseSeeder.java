package com.hud.briefing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder {

    private final LlmConfigRepository llmRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String defaultBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name}")
    private String defaultModelName;

    @Value("${langchain4j.ollama.chat-model.num-ctx}")
    private Integer defaultNumCtx;

    public DatabaseSeeder(LlmConfigRepository llmRepository, 
                          UserRepository userRepository, 
                          PasswordEncoder passwordEncoder) {
        this.llmRepository = llmRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        seedDefaultConfig();
        seedDefaultUser();
    }

    private void seedDefaultConfig() {
        if (llmRepository.count() == 0) {
            System.out.println("Seeding default LLM configuration...");
            LlmConfig config = new LlmConfig("Local Gemma", LlmProvider.OLLAMA, defaultModelName, true);
            config.setBaseUrl(defaultBaseUrl);
            config.setNumCtx(defaultNumCtx);
            llmRepository.save(config);
        }
    }

    private void seedDefaultUser() {
        if (userRepository.count() == 0) {
            System.out.println("Seeding default admin user...");
            AppUser admin = new AppUser("admin", passwordEncoder.encode("admin"), "ROLE_ADMIN");
            userRepository.save(admin);
        }
    }
}
