package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ChatModelProviderTest {

    @Test
    void ollamaProviderShouldSupportOllama() {
        OllamaModelProvider provider = new OllamaModelProvider();
        assertTrue(provider.supports(LlmProvider.OLLAMA));
        assertFalse(provider.supports(LlmProvider.GEMINI));
    }

    @Test
    void geminiProviderShouldSupportGemini() {
        GeminiModelProvider provider = new GeminiModelProvider();
        assertTrue(provider.supports(LlmProvider.GEMINI));
        assertFalse(provider.supports(LlmProvider.OLLAMA));
    }

    @Test
    void ollamaProviderShouldBuildModel() {
        OllamaModelProvider provider = new OllamaModelProvider();
        LlmConfig config = new LlmConfig("Test", LlmProvider.OLLAMA, "gemma", true);
        config.setBaseUrl("http://localhost:11434");
        
        ChatLanguageModel model = provider.buildModel(config);
        assertNotNull(model);
    }
}
