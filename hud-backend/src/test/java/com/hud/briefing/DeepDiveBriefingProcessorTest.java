package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class DeepDiveBriefingProcessorTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private ChatLanguageModel chatModel;
    @Mock private BriefingSourceStrategy sourceStrategy;
    @Mock private IntelligenceSynthesizer synthesizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldProcessUkraineTheaterWithHighSignal() {
        DeepDiveBriefingProcessor processor = new DeepDiveBriefingProcessor(scraperService, chatModel, sourceStrategy, synthesizer, BriefingCategory.THEATER_UKRAINE);
        
        when(sourceStrategy.getLinks(anyString(), anyInt())).thenReturn(List.of("http://test.com/intel"));
        when(scraperService.extractFullText(eq("http://test.com/intel"), anyInt())).thenReturn("A very long piece of tactical field intelligence from the frontline that is definitely longer than 1500 characters so that the deep-dive processor doesn't complain about insufficient signal during its rigorous analytical lifecycle.".repeat(15));
        when(synthesizer.fuseTheaterIntelligence(any(), any(), anyString())).thenReturn(new SynthesisResult("Fused Intel Report", 100, 10));

        SynthesisResult result = processor.process("ukraine");

        assertEquals("Fused Intel Report", result.content());
        verify(sourceStrategy).getLinks("ukraine", 15);
        verify(synthesizer).fuseTheaterIntelligence(eq(chatModel), eq(BriefingCategory.THEATER_UKRAINE), anyString());
    }

    @Test
    void shouldProcessGlobalSitrepWithMultiLinks() {
        DeepDiveBriefingProcessor processor = new DeepDiveBriefingProcessor(scraperService, chatModel, sourceStrategy, synthesizer, BriefingCategory.GLOBAL_SITREP);
        
        when(sourceStrategy.getLinks(anyString(), anyInt())).thenReturn(List.of("http://a.com", "http://b.com"));
        when(scraperService.extractFullText(anyString(), anyInt())).thenReturn("Valid strategic content for the global situational report meeting the character limit requirements.".repeat(20));
        when(synthesizer.synthesizeGlobalSitrep(any(), anyString())).thenReturn(new SynthesisResult("Global Summary", 100, 10));

        SynthesisResult result = processor.process("all");

        assertEquals("Global Summary", result.content());
        verify(sourceStrategy).getLinks("all", 25);
        verify(synthesizer).synthesizeGlobalSitrep(eq(chatModel), anyString());
    }
}
