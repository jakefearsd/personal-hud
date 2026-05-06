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

    private StandardBriefingProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new StandardBriefingProcessor(scraperService, chatModel, sourceStrategy, synthesizer, BriefingCategory.WORLD_NEWS.getPersona());
    }

    @Test
    void shouldProcessSuccessfully() {
        // Arrange
        when(sourceStrategy.getLinks(anyString(), anyInt())).thenReturn(List.of("http://test.com/1"));
        String longContent = "Valid situational intelligence report that provides enough textual density to satisfy the high-resolution requirements of the analytic heads-up display system.".repeat(15);
        when(scraperService.extractFullText(eq("http://test.com/1"), anyInt())).thenReturn(longContent);
        when(synthesizer.synthesizeStandard(any(), any(), anyString())).thenReturn(new SynthesisResult("Synthesized Intelligence", 100, 10));

        // Act
        SynthesisResult result = processor.process("test query");

        // Assert
        assertEquals("Synthesized Intelligence", result.content());
        verify(sourceStrategy).getLinks("test query", 15);
        verify(scraperService).extractFullText(eq("http://test.com/1"), anyInt());
        verify(synthesizer).synthesizeStandard(eq(chatModel), any(), contains("Valid situational intelligence"));
    }

    @Test
    void shouldThrowExceptionWhenNoLinksFound() {
        when(sourceStrategy.getLinks(anyString(), anyInt())).thenReturn(List.of());
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> processor.process("test"));
        assertTrue(ex.getMessage().contains("No signal sources found"));
    }

    @Test
    void shouldThrowExceptionWhenInsufficientSignal() {
        when(sourceStrategy.getLinks(anyString(), anyInt())).thenReturn(List.of("http://test.com"));
        when(scraperService.extractFullText(anyString(), anyInt())).thenReturn("Too short signal");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> processor.process("test"));
        assertTrue(ex.getMessage().contains("Insufficient situational signal"));
    }

    @Test
    void shouldFilterNonPlausibleContent() {
        when(sourceStrategy.getLinks(anyString(), anyInt())).thenReturn(List.of("http://test.com/cookie", "http://test.com/valid"));
        when(scraperService.extractFullText(eq("http://test.com/cookie"), anyInt())).thenReturn("Before you continue... Accept all cookies");
        String longContent = "A long piece of valid situational content that meets the length requirements for processing and provides the necessary analytical depth.".repeat(15);
        when(scraperService.extractFullText(eq("http://test.com/valid"), anyInt())).thenReturn(longContent);
        when(synthesizer.synthesizeStandard(any(), any(), anyString())).thenReturn(new SynthesisResult("Success", 100, 10));

        processor.process("test");

        // Verify only the valid one was passed to synthesizer
        verify(synthesizer).synthesizeStandard(any(), any(), argThat(s -> !s.contains("Accept all cookies")));
    }
}
