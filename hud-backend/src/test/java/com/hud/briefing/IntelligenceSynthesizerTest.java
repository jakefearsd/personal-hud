package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("unit")
class IntelligenceSynthesizerTest {

    @Mock private ChatLanguageModel model;
    private IntelligenceSynthesizer synthesizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        synthesizer = new IntelligenceSynthesizer();
    }

    @Test
    void shouldSynthesizeStandardBriefing() {
        when(model.generate(anyString())).thenReturn("Narrative Output");
        
        String result = synthesizer.synthesizeStandard(model, BriefingPersona.of(BriefingCategory.WORLD_NEWS), "Raw Data");

        assertEquals("Narrative Output", result);
        verify(model).generate(contains("Geopolitical Strategist"));
        verify(model).generate(contains("Raw Data"));
    }

    @Test
    void shouldFuseTheaterIntelligence() {
        when(model.generate(anyString())).thenReturn("Tempo Analysis", "Kinetic Table", "Innovations");
        
        String result = synthesizer.fuseTheaterIntelligence(model, BriefingCategory.THEATER_UKRAINE, "Frontline Intelligence Data");

        assertTrue(result.contains("# UKRAINE THEATER REPORT"));
        assertTrue(result.contains("## Tactical Momentum"));
        assertTrue(result.contains("Tempo Analysis"));
        assertTrue(result.contains("Kinetic Table"));
        assertTrue(result.contains("Innovations"));
        
        verify(model, times(3)).generate(anyString());
        verify(model).generate(contains("Tactical Ground Analyst"));
        verify(model).generate(contains("kinetic strike data"));
        verify(model).generate(contains("battlefield innovations"));
    }

    @Test
    void shouldSynthesizeGlobalSitrep() {
        when(model.generate(anyString())).thenReturn("Global Overview");

        String result = synthesizer.synthesizeGlobalSitrep(model, "Global Data");

        assertEquals("Global Overview", result);
        verify(model).generate(contains("Global Theater Strategist"));
        verify(model).generate(contains("Global Data"));
    }

    private void assertEquals(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
