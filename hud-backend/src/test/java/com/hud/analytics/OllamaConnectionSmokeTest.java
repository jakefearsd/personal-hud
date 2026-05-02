package com.hud.analytics;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class OllamaConnectionSmokeTest {

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    @Test
    void chatModelShouldBeInjected() {
        assertNotNull(chatLanguageModel, "ChatLanguageModel should be auto-configured by the starter");
    }

    @Test
    void shouldConnectToOllama() {
        // Ping prompt to verify network and model loading
        String response = chatLanguageModel.generate("Say 'Hello HUD'");
        assertNotNull(response);
        assertTrue(response.toLowerCase().contains("hello") || response.toLowerCase().contains("hud"), 
                "Response should contain greeting: " + response);
        System.out.println("Ollama response: " + response);
    }
}
