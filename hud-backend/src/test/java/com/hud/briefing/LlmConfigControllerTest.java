package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class LlmConfigControllerTest {

    @Mock private LlmConfigRepository repository;
    @Mock private AutomatedBriefingService briefingService;
    private LlmConfigController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new LlmConfigController(repository, briefingService);
    }

    @Test
    void shouldReturnAllConfigs() {
        LlmConfig c = new LlmConfig("Test", LlmProvider.OLLAMA, "gemma", true);
        when(repository.findAll()).thenReturn(List.of(c));

        List<LlmConfig> result = controller.getAllConfigs();

        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getName());
    }

    @Test
    void shouldSaveConfig() {
        LlmConfig c = new LlmConfig("New", LlmProvider.GEMINI, "pro", true);
        when(repository.save(any())).thenReturn(c);

        LlmConfig result = controller.saveConfig(c);

        assertNotNull(result);
        assertEquals("New", result.getName());
        verify(repository).save(c);
    }

    @Test
    void shouldToggleActiveStatus() {
        LlmConfig c = new LlmConfig("Toggle", LlmProvider.OLLAMA, "gemma", true);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.save(any())).thenReturn(c);

        LlmConfig result = controller.toggleActive(1L);

        assertFalse(result.isActive());
        verify(repository).save(c);
    }

    @Test
    void shouldReturnAllConfigsWithMaskedKeys() {
        LlmConfig c1 = new LlmConfig("Test1", LlmProvider.OLLAMA, "gemma", true);
        c1.setApiKey("short");
        LlmConfig c2 = new LlmConfig("Test2", LlmProvider.GEMINI, "pro", true);
        c2.setApiKey("long-secret-key-1234");
        
        when(repository.findAll()).thenReturn(List.of(c1, c2));

        List<LlmConfig> result = controller.getAllConfigs();

        assertEquals(2, result.size());
        assertEquals("********", result.get(0).getApiKey());
        assertEquals("long...1234", result.get(1).getApiKey());
    }

    @Test
    void shouldSaveConfigAndPreserveKeyIfMasked() {
        LlmConfig existing = new LlmConfig("Existing", LlmProvider.GEMINI, "pro", true);
        existing.setId(1L);
        existing.setApiKey("REAL-SECRET-KEY");
        
        LlmConfig updated = new LlmConfig("Existing", LlmProvider.GEMINI, "pro", true);
        updated.setId(1L);
        updated.setApiKey("REAL...-KEY"); // masked
        
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LlmConfig result = controller.saveConfig(updated);

        assertEquals("REAL-SECRET-KEY", result.getApiKey());
        verify(repository).save(any());
    }

    @Test
    void shouldDeleteConfig() {
        controller.deleteConfig(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void shouldRunModel() {
        String response = controller.runModel(1L);
        assertEquals("Model specific briefing run triggered.", response);
        verify(briefingService).generateForModel(1L);
    }
}
