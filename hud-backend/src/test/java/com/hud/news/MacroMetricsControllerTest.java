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
}
