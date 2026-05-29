package com.hud.briefing;

import com.hud.briefing.synthesis.GlobalSitrepStrategy;
import com.hud.briefing.synthesis.StandardSynthesisStrategy;
import com.hud.briefing.synthesis.TheaterFusionStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
class IntelligenceSynthesizerTest {

    @Mock private ChatLanguageModel model;
    private IntelligenceSynthesizer synthesizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        synthesizer = new IntelligenceSynthesizer(List.of(
            new StandardSynthesisStrategy(),
            new TheaterFusionStrategy(),
            new GlobalSitrepStrategy()
        ));
    }

    @Test
    void shouldSynthesizeStandardBriefing() {
        Response<AiMessage> response = Response.from(AiMessage.from("Narrative Output"), new TokenUsage(10, 5));
        when(model.generate(any(UserMessage.class))).thenReturn(response);
        
        SynthesisResult result = synthesizer.synthesize(model, BriefingCategory.WORLD_NEWS, "Raw Data");

        assertEquals("Narrative Output", result.content());
        assertEquals(10, result.inputTokens());
        assertEquals(5, result.outputTokens());
        verify(model).generate(argThat((UserMessage m) -> m.text().contains("Geopolitical Strategist")));
    }

    @Test
    void shouldFuseTheaterIntelligence() {
        Response<AiMessage> tempo = Response.from(AiMessage.from("Tempo Analysis"), new TokenUsage(10, 5));
        Response<AiMessage> kinetic = Response.from(AiMessage.from("Kinetic Table"), new TokenUsage(10, 5));
        Response<AiMessage> innovations = Response.from(AiMessage.from("Innovations"), new TokenUsage(10, 5));
        
        when(model.generate(any(UserMessage.class))).thenReturn(tempo, kinetic, innovations);
        
        SynthesisResult result = synthesizer.synthesize(model, BriefingCategory.THEATER_UKRAINE, "Frontline Intelligence Data");

        assertTrue(result.content().contains("# UKRAINE THEATER REPORT"));
        assertTrue(result.content().contains("## Tactical Momentum"));
        assertTrue(result.content().contains("Tempo Analysis"));
        assertEquals(30, result.inputTokens());
        assertEquals(15, result.outputTokens());
        
        verify(model, times(3)).generate(any(UserMessage.class));
    }

    @Test
    void shouldSynthesizeGlobalSitrep() {
        Response<AiMessage> response = Response.from(AiMessage.from("Global Overview"), new TokenUsage(20, 10));
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        SynthesisResult result = synthesizer.synthesize(model, BriefingCategory.GLOBAL_SITREP, "Global Data");

        assertEquals("Global Overview", result.content());
        assertEquals(20, result.inputTokens());
        assertEquals(10, result.outputTokens());
        verify(model).generate(argThat((UserMessage m) -> m.text().contains("Global Theater Strategist")));
    }
}
