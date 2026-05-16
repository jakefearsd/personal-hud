package com.hud.analytics;

import com.hud.briefing.DynamicLlmService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class OllamaConnectionSmokeTest {

    @Autowired
    private DynamicLlmService dynamicLlmService;

    @Test
    void chatModelShouldBeAvailable() {
        List<DynamicLlmService.NamedChatModel> models = dynamicLlmService.getActiveModels();
        assertNotNull(models);
        assertFalse(models.isEmpty(), "At least one active model should be available (seeded)");
    }

    @Test
    void shouldConnectToOllama() {
        List<DynamicLlmService.NamedChatModel> models = dynamicLlmService.getActiveModels();
        if (models.isEmpty()) return;
        
        var chatLanguageModel = models.get(0).model();
        // Ping prompt to verify network and model loading
        String response = chatLanguageModel.generate("Say 'Hello HUD'");
        assertNotNull(response);
        assertTrue(response.toLowerCase().contains("hello") || response.toLowerCase().contains("hud"), 
                "Response should contain greeting: " + response);
    }
}
