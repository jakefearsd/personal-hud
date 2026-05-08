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
    private MarketEventRepository eventRepository;
    @Mock
    private EventCorrelationService correlationService;
    @Mock
    private PredictionService predictionService;

    private MacroMetricsController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new MacroMetricsController(service, eventRepository, correlationService, predictionService);
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
    void testGetEvents() {
        when(eventRepository.findByTickerOrderByTimestampDesc("AAPL")).thenReturn(List.of(new MarketEvent()));
        List<MarketEvent> events = controller.getEvents("AAPL");
        assertEquals(1, events.size());
    }

    @Test
    void testTriggerUpdate() {
        String response = controller.triggerUpdate();
        assertEquals("Macro update triggered.", response);
        verify(service).updateMacroMetrics();
    }

    @Test
    void testTriggerCorrelation() {
        String response = controller.triggerCorrelation();
        assertEquals("Market correlation analysis triggered.", response);
        verify(correlationService).correlateEvents();
    }
}
