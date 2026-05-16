package com.hud.analytics;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class GeminiIntegrationTest {

    private String getApiKey() throws Exception {
        java.nio.file.Path keyPath = Paths.get("../.key");
        if (!Files.exists(keyPath)) {
            keyPath = Paths.get(".key");
        }
        String content = Files.readString(keyPath).trim();
        if (content.contains(":")) {
            return content.split(":")[1].trim();
        }
        return content;
    }

    /**
     * Stable persistent smoke test.
     */
    @Test
    void smokeTestStableModel() throws Exception {
        testModel("gemini-2.5-flash");
    }

    /**
     * Discovery test to verify which frontier/stable models are active.
     */
    @Test
    void discoverAvailableModels() throws Exception {
        String[] modelsToTry = {
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-pro",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite"
        };
        
        for (String model : modelsToTry) {
            try {
                testModel(model);
                System.out.println("SUCCESS: Model '" + model + "' is available.");
            } catch (Exception e) {
                System.out.println("SKIPPED: Model '" + model + "' returned error (likely tier restriction/invalid ID).");
            }
        }
    }

    private void testModel(String modelName) throws Exception {
        String apiKey = getApiKey();
        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0)
                .timeout(Duration.ofMinutes(1))
                .build();

        String response = model.generate("Say 'HUD-VERIFY'");
        assertNotNull(response);
        assertTrue(response.contains("HUD-VERIFY"), "Response should contain HUD-VERIFY but was: " + response);
    }
}
