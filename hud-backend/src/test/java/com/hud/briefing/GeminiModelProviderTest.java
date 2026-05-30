package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("unit")
class GeminiModelProviderTest {

    private final GeminiModelProvider provider = new GeminiModelProvider();

    @Test
    void supports_returnsTrueForGemini() {
        assertTrue(provider.supports(LlmProvider.GEMINI));
    }

    @Test
    void supports_returnsFalseForOthers() {
        assertFalse(provider.supports(LlmProvider.OLLAMA));
        assertFalse(provider.supports(LlmProvider.DEEPSEEK));
    }

    @Test
    void buildModel_createsModel() {
        LlmConfig config = new LlmConfig("Gemini Config", LlmProvider.GEMINI, "gemini-1.5-flash", true);
        config.setApiKey("test-key");

        ChatLanguageModel model = provider.buildModel(config);
        
        assertNotNull(model);
    }
}
