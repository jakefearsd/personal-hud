package com.hud.briefing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class DatabaseSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);
    private final LlmConfigRepository llmRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${langchain4j.ollama.chat-model.base-url}")
    private String defaultBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name}")
    private String defaultModelName;

    @Value("${langchain4j.ollama.chat-model.num-ctx}")
    private Integer defaultNumCtx;

    @Value("${HUD_ADMIN_PASSWORD:}")
    private String adminPassword;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PW_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    public DatabaseSeeder(LlmConfigRepository llmRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.llmRepository = llmRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Package-private for unit testing (DatabaseSeederTest).
    void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        seedDefaultConfig();
        seedDefaultUser();
    }

    private void seedDefaultConfig() {
        if (llmRepository.count() == 0) {
            logger.info("Seeding default LLM configuration...");
            LlmConfig config = new LlmConfig("Local Gemma", LlmProvider.OLLAMA, defaultModelName, true);
            config.setBaseUrl(defaultBaseUrl);
            config.setNumCtx(defaultNumCtx);
            llmRepository.save(config);
        }
    }

    // Package-private for unit testing (DatabaseSeederTest).
    void seedDefaultUser() {
        if (userRepository.count() == 0) {
            String password = (adminPassword == null || adminPassword.isBlank())
                    ? generatePassword()
                    : adminPassword;
            boolean generated = (adminPassword == null || adminPassword.isBlank());

            AppUser admin = new AppUser("admin", passwordEncoder.encode(password), "ROLE_ADMIN");
            admin.setPasswordChangeRequired(true);
            userRepository.save(admin);

            if (generated) {
                logger.warn("Seeded admin user with a GENERATED password: {} "
                        + "-- change it on first login.", password);
            } else {
                logger.info("Seeded admin user from HUD_ADMIN_PASSWORD; "
                        + "password change is required on first login.");
            }
        }
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(PW_CHARS.charAt(RANDOM.nextInt(PW_CHARS.length())));
        }
        return sb.toString();
    }
}
