package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class BriefingProcessorTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private ChatLanguageModel chatModel;
    @Mock private BriefingSourceStrategy sourceStrategy;
    @Mock private IntelligenceSynthesizer synthesizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private SourceLink link(String url) {
        return new SourceLink(url, "Test Feed", SourceTier.TIER_1);
    }

    @Test
    void shouldProcessStandardCategorySuccessfully() {
        BriefingProcessor processor = new BriefingProcessor(scraperService, chatModel, sourceStrategy,
                synthesizer, BriefingCategory.WORLD_NEWS, BriefingProcessorConfiguration.STANDARD);

        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://test.com/1")));
        String longContent = "Valid situational intelligence report".repeat(30);
        when(scraperService.extractFullText(eq("http://test.com/1"), anyInt())).thenReturn(longContent);
        when(synthesizer.synthesize(any(), any(), anyString()))
                .thenReturn(new SynthesisResult("Synthesized Intel", 100, 10));

        SynthesisResult result = processor.process();

        assertEquals("Synthesized Intel", result.content());
        verify(sourceStrategy).getLinks(BriefingCategory.WORLD_NEWS, 15);
        verify(scraperService).extractFullText(eq("http://test.com/1"), eq(0));
        verify(synthesizer).synthesize(eq(chatModel), eq(BriefingCategory.WORLD_NEWS), contains("Valid situational intelligence"));
    }

    @Test
    void shouldProcessTheaterCategoryWithDeepCrawl() {
        BriefingProcessor processor = new BriefingProcessor(scraperService, chatModel, sourceStrategy,
                synthesizer, BriefingCategory.THEATER_UKRAINE, BriefingProcessorConfiguration.DEEP_DIVE);

        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://test.com/intel")));
        String deepContent = "Detailed battlefield analysis".repeat(100);
        when(scraperService.extractFullText(eq("http://test.com/intel"), anyInt())).thenReturn(deepContent);
        when(synthesizer.synthesize(any(), any(), anyString()))
                .thenReturn(new SynthesisResult("Theater Report", 200, 20));

        SynthesisResult result = processor.process();

        assertEquals("Theater Report", result.content());
        verify(scraperService).extractFullText(eq("http://test.com/intel"), eq(1)); // Depth 1 for deep dive
    }

    @Test
    void shouldThrowExceptionWhenNoLinksFound() {
        BriefingProcessor processor = new BriefingProcessor(scraperService, chatModel, sourceStrategy,
                synthesizer, BriefingCategory.WORLD_NEWS, BriefingProcessorConfiguration.STANDARD);
        
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt())).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> processor.process());
        assertTrue(ex.getMessage().contains("No signal sources found"));
    }

    @Test
    void shouldThrowExceptionWhenInsufficientSignal() {
        BriefingProcessor processor = new BriefingProcessor(scraperService, chatModel, sourceStrategy,
                synthesizer, BriefingCategory.WORLD_NEWS, BriefingProcessorConfiguration.STANDARD);

        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://test.com")));
        when(scraperService.extractFullText(anyString(), anyInt())).thenReturn("Too short");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> processor.process());
        assertTrue(ex.getMessage().contains("Insufficient situational signal"));
    }

    @Test
    void shouldFilterNonPlausibleContent() {
        BriefingProcessor processor = new BriefingProcessor(scraperService, chatModel, sourceStrategy,
                synthesizer, BriefingCategory.WORLD_NEWS, BriefingProcessorConfiguration.STANDARD);

        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://test.com/cookie"), link("http://test.com/valid")));
        when(scraperService.extractFullText(eq("http://test.com/cookie"), anyInt()))
                .thenReturn("Before you continue... Accept all cookies");
        String longContent = "A long piece of valid situational content".repeat(20);
        when(scraperService.extractFullText(eq("http://test.com/valid"), anyInt())).thenReturn(longContent);
        when(synthesizer.synthesize(any(), any(), anyString()))
                .thenReturn(new SynthesisResult("Success", 100, 10));

        processor.process();

        verify(synthesizer).synthesize(any(), any(), argThat(s -> !s.contains("Accept all cookies")));
    }
}
