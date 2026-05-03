package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class DynamicLlmServiceTest {

    @Test
    void shouldGetActiveModelsByDelegatingToProviders() {
        LlmConfigRepository repository = mock(LlmConfigRepository.class);
        ChatModelProvider provider = mock(ChatModelProvider.class);
        ChatLanguageModel model = mock(ChatLanguageModel.class);

        LlmConfig config = new LlmConfig("Local", LlmProvider.OLLAMA, "gemma", true);
        config.setProvider(LlmProvider.OLLAMA);

        when(repository.findByActiveTrue()).thenReturn(List.of(config));
        when(provider.supports(LlmProvider.OLLAMA)).thenReturn(true);
        when(provider.buildModel(config)).thenReturn(model);

        DynamicLlmService service = new DynamicLlmService(repository, List.of(provider));
        List<DynamicLlmService.NamedChatModel> models = service.getActiveModels();

        assertEquals(1, models.size());
        assertEquals("Local", models.get(0).name());
        assertEquals(model, models.get(0).model());
    }
}
