package com.hud.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MacroMetricsServiceTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private MacroMetricRepository repository;
    @Mock private MetricHistoryRepository historyRepository;
    @Mock private HttpClient httpClient;
    @Mock private HttpResponse<String> httpResponse;

    private MacroMetricsService metricsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        metricsService = new MacroMetricsService(scraperService, repository, historyRepository, httpClient);
    }

    @Test
    void shouldUpdateMacroMetrics() {
        MacroMetric metric = new MacroMetric("^GSPC", "S&P 500", 4500.0, 10.0, 0.2);
        when(scraperService.scrapeYahooMetric(anyString(), anyString())).thenReturn(metric);
        when(repository.findById(anyString())).thenReturn(Optional.empty());

        metricsService.updateMacroMetrics();

        verify(repository, atLeastOnce()).save(any(MacroMetric.class));
        verify(historyRepository, atLeastOnce()).save(any(MetricHistory.class));
    }

    @Test
    void shouldHandleScraperFailure() {
        when(scraperService.scrapeYahooMetric(anyString(), anyString())).thenReturn(null);
        when(scraperService.scrapeFredYieldSpread()).thenReturn(null);
        metricsService.updateMacroMetrics();
        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateYieldSpread() {
        when(scraperService.scrapeFredYieldSpread()).thenReturn(0.5);
        metricsService.updateMacroMetrics();
        verify(repository).save(argThat(m -> m.getTicker().equals("T10Y2Y")));
    }

    @Test
    void shouldSyncHistoricalGaps() throws Exception {
        MetricHistory last = new MetricHistory("^GSPC", 4400.0, 0.1);
        last.setTimestamp(LocalDateTime.now().minusDays(14));
        when(historyRepository.findTopByTickerOrderByTimestampDesc(anyString())).thenReturn(Optional.of(last));
        
        String json = "{\"chart\":{\"result\":[{\"meta\":{\"currency\":\"USD\"},\"timestamp\":[1715000000],\"indicators\":{\"quote\":[{\"close\":[4500.0]}]}}]}}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        metricsService.syncHistoricalGaps();

        verify(historyRepository, atLeastOnce()).save(any(MetricHistory.class));
    }

    @Test
    void shouldReturnLatestMetrics() {
        metricsService.getLatestMetrics();
        verify(repository).findAllByOrderByLabelAsc();
    }

    @Test
    void shouldReturnHistory() {
        metricsService.getHistory("AAPL");
        verify(historyRepository).findByTickerOrderByTimestampAsc("AAPL");
    }
}
