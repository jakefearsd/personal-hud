package com.hud.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MacroMetricsControllerTest {

    @Mock
    private MacroMetricsService service;
    @Mock
    private PredictionService predictionService;
    @Mock
    private MacroSentimentService sentimentService;
    @Mock
    private WeeklyInsightRepository insightRepository;
    @Mock
    private WeeklyInsightsPipeline pipeline;

    private MacroMetricsController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new MacroMetricsController(service, predictionService, sentimentService, insightRepository, pipeline);
    }

    @Test
    void testGetVitals() {
        when(service.getLatestMetrics()).thenReturn(List.of(new MacroMetric()));
        List<MacroMetric> vitals = controller.getVitals();
        assertEquals(1, vitals.size());
    }

    @Test
    void testGetHistory() {
        when(service.getHistory("AAPL")).thenReturn(List.of(new MetricHistory()));
        List<MetricHistory> history = controller.getHistory("AAPL");
        assertEquals(1, history.size());
    }

    @Test
    void testTriggerUpdate() {
        String response = controller.triggerUpdate();
        assertEquals("Macro update triggered.", response);
        verify(service).updateMacroMetrics();
    }

    @Test
    void testTriggerSync() {
        String response = controller.triggerSync();
        assertEquals("Historical data sync triggered.", response);
        verify(service).syncHistoricalGaps();
    }

    @Test
    void testPredictions() {
        when(predictionService.getLatestPredictions()).thenReturn(List.of(new MarketPrediction()));
        assertEquals(1, controller.getLatestPredictions().size());

        when(predictionService.getHistory("AAPL")).thenReturn(List.of(new MarketPrediction()));
        assertEquals(1, controller.getPredictionHistory("AAPL").size());

        assertEquals("Market predictions triggered.", controller.triggerPredictions());
        verify(predictionService).generateDailyPredictions();
    }

    @Test
    void testInsights() {
        when(insightRepository.findTopByOrderByGeneratedAtDesc()).thenReturn(java.util.Optional.of(new WeeklyInsight()));
        assertNotNull(controller.getLatestInsight());

        assertEquals("Insight pipeline triggered.", controller.triggerInsights());
        verify(pipeline).runPipeline();
    }

    @Test
    void testGetMacroPods() {
        when(sentimentService.generatePodSentiment(anyString())).thenReturn("sentiment");
        List<MacroPod> pods = controller.getMacroPods();
        assertFalse(pods.isEmpty());
        assertEquals(4, pods.size());
        assertEquals("Economic Health", pods.get(0).getTitle());
    }
}
