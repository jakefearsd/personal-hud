package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("unit")
class DeepSeekModelProviderTest {

    private final DeepSeekModelProvider provider = new DeepSeekModelProvider();

    @Test
    void supports_returnsTrueForDeepSeek() {
        assertTrue(provider.supports(LlmProvider.DEEPSEEK));
    }

    @Test
    void supports_returnsFalseForOthers() {
        assertFalse(provider.supports(LlmProvider.OLLAMA));
        assertFalse(provider.supports(LlmProvider.GEMINI));
    }

    @Test
    void buildModel_withBaseUrl_createsModel() {
        LlmConfig config = new LlmConfig("DeepSeek Config", LlmProvider.DEEPSEEK, "deepseek-chat", true);
        config.setApiKey("test-key");
        config.setBaseUrl("https://custom.deepseek.url");

        ChatLanguageModel model = provider.buildModel(config);
        
        assertNotNull(model);
    }

    @Test
    void buildModel_withoutBaseUrl_createsModelWithDefaultUrl() {
        LlmConfig config = new LlmConfig("DeepSeek Config", LlmProvider.DEEPSEEK, "deepseek-chat", true);
        config.setApiKey("test-key");

        ChatLanguageModel model = provider.buildModel(config);
        
        assertNotNull(model);
    }
}
