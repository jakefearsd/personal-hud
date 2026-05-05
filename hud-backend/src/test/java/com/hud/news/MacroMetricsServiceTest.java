package com.hud.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MacroMetricsServiceTest {

    @Mock
    private PlaywrightScraperService scraperService;
    @Mock
    private MacroMetricRepository repository;
    @Mock
    private MetricHistoryRepository historyRepository;

    private MacroMetricsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MacroMetricsService(scraperService, repository, historyRepository);
    }

    @Test
    void testUpdateMacroMetrics() {
        MacroMetric mockMetric = new MacroMetric("CL=F", "WTI Crude Oil", 75.0, 1.0, 1.3);
        when(scraperService.scrapeYahooMetric(anyString(), anyString())).thenReturn(mockMetric);
        when(scraperService.scrapeFredYieldSpread()).thenReturn(0.5);

        service.updateMacroMetrics();

        verify(repository, atLeastOnce()).save(any());
        verify(historyRepository, atLeastOnce()).save(any());
    }

    @Test
    void testGetLatestMetrics() {
        when(repository.findAllByOrderByLabelAsc()).thenReturn(List.of(new MacroMetric()));
        List<MacroMetric> metrics = service.getLatestMetrics();
        assertEquals(1, metrics.size());
    }

    @Test
    void testGetHistory() {
        when(historyRepository.findByTickerOrderByTimestampAsc("AAPL")).thenReturn(List.of(new MetricHistory()));
        List<MetricHistory> history = service.getHistory("AAPL");
        assertEquals(1, history.size());
    }
}
