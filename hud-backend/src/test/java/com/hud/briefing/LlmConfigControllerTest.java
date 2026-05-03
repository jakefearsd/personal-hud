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
}
