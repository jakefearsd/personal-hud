package com.hud.news;

import com.hud.briefing.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventCorrelationServiceTest {

    @Mock
    private MacroMetricsService metricsService;
    @Mock
    private DailyBriefingRepository briefingRepository;
    @Mock
    private MarketEventRepository eventRepository;
    @Mock
    private DynamicLlmService llmService;
    @Mock
    private ChatLanguageModel chatModel;

    private EventCorrelationService correlationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        correlationService = new EventCorrelationService(metricsService, briefingRepository, eventRepository, llmService);
    }

    @Test
    void testCorrelateEventsNoBriefings() {
        when(briefingRepository.findLatestToday()).thenReturn(Collections.emptyList());

        correlationService.correlateEvents();

        verifyNoInteractions(llmService);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void testCorrelateEventsNoModels() {
        DailyBriefing briefing = new DailyBriefing();
        briefing.setCategory(BriefingCategory.WORLD_NEWS);
        briefing.setMarkdownContent("Significant world events.");
        
        when(briefingRepository.findLatestToday()).thenReturn(List.of(briefing));
        when(llmService.getActiveModels()).thenReturn(Collections.emptyList());

        correlationService.correlateEvents();

        verify(metricsService).getLatestMetrics();
        verifyNoInteractions(eventRepository);
    }

    @Test
    void testCorrelateEventsWithCorrelation() {
        DailyBriefing briefing = new DailyBriefing();
        briefing.setCategory(BriefingCategory.WORLD_NEWS);
        briefing.setMarkdownContent("Significant world events.");
        
        MacroMetric metric = new MacroMetric();
        metric.setTicker("AAPL");
        metric.setLabel("Apple");
        metric.setChangePercent(2.5);
        metric.setUpdatedAt(LocalDateTime.now());

        DynamicLlmService.NamedChatModel namedModel = new DynamicLlmService.NamedChatModel("Gemma", chatModel);

        when(briefingRepository.findLatestToday()).thenReturn(List.of(briefing));
        when(llmService.getActiveModels()).thenReturn(List.of(namedModel));
        when(metricsService.getLatestMetrics()).thenReturn(List.of(metric));
        when(chatModel.generate(anyString())).thenReturn("Major Catalyst | Apple stock moved because of world events.");

        correlationService.correlateEvents();

        ArgumentCaptor<MarketEvent> eventCaptor = ArgumentCaptor.forClass(MarketEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        
        MarketEvent savedEvent = eventCaptor.getValue();
        assertEquals("AAPL", savedEvent.getTicker());
        assertEquals("Major Catalyst", savedEvent.getTitle());
        assertEquals("Apple stock moved because of world events.", savedEvent.getRationale());
    }

    @Test
    void testCorrelateEventsNoSignificantMove() {
        DailyBriefing briefing = new DailyBriefing();
        briefing.setCategory(BriefingCategory.WORLD_NEWS);
        briefing.setMarkdownContent("Significant world events.");
        
        MacroMetric metric = new MacroMetric();
        metric.setTicker("AAPL");
        metric.setChangePercent(0.5); // Less than 2%

        DynamicLlmService.NamedChatModel namedModel = new DynamicLlmService.NamedChatModel("Gemma", chatModel);

        when(briefingRepository.findLatestToday()).thenReturn(List.of(briefing));
        when(llmService.getActiveModels()).thenReturn(List.of(namedModel));
        when(metricsService.getLatestMetrics()).thenReturn(List.of(metric));

        correlationService.correlateEvents();

        verify(eventRepository, never()).save(any());
    }

    @Test
    void testCorrelateEventsNoneResponse() {
        DailyBriefing briefing = new DailyBriefing();
        briefing.setCategory(BriefingCategory.WORLD_NEWS);
        briefing.setMarkdownContent("Significant world events.");
        
        MacroMetric metric = new MacroMetric();
        metric.setTicker("AAPL");
        metric.setChangePercent(3.0);

        DynamicLlmService.NamedChatModel namedModel = new DynamicLlmService.NamedChatModel("Gemma", chatModel);

        when(briefingRepository.findLatestToday()).thenReturn(List.of(briefing));
        when(llmService.getActiveModels()).thenReturn(List.of(namedModel));
        when(metricsService.getLatestMetrics()).thenReturn(List.of(metric));
        when(chatModel.generate(anyString())).thenReturn("NONE");

        correlationService.correlateEvents();

        verify(eventRepository, never()).save(any());
    }
}
