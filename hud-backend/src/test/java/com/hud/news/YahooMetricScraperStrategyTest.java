package com.hud.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@Tag("unit")
class YahooMetricScraperStrategyTest {

    private HttpClient mockHttpClient;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mapper = new ObjectMapper();
    }

    @Test
    void getUrl_returnsEncodedUrl() {
        YahooMetricScraperStrategy strategy = new YahooMetricScraperStrategy("^GSPC", "S&P 500");
        assertEquals("https://query1.finance.yahoo.com/v8/finance/chart/%5EGSPC?interval=1d&range=1d", strategy.getUrl());
    }

    @Test
    void scrape_apiSuccess_returnsMetric() throws Exception {
        YahooMetricScraperStrategy strategy = new YahooMetricScraperStrategy("^GSPC", "S&P 500", mockHttpClient, mapper);
        
        String jsonResponse = "{\"chart\":{\"result\":[{\"meta\":{\"regularMarketPrice\":5000.50,\"previousClose\":4900.00}}]}}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonResponse);
        
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Page mockPage = mock(Page.class);
        
        MacroMetric metric = strategy.scrape(mockPage);
        
        assertNotNull(metric);
        assertEquals("^GSPC", metric.getTicker());
        assertEquals("S&P 500", metric.getLabel());
        assertEquals(5000.50, metric.getPrice());
        assertEquals(100.50, metric.getChange());
        assertEquals(100.50 / 4900.00 * 100, metric.getChangePercent());
        
        // Ensure playwright fallback wasn't called
        verify(mockPage, never()).navigate(anyString());
    }

    @Test
    void scrape_apiFailure_fallsBackToPlaywright() throws Exception {
        YahooMetricScraperStrategy strategy = new YahooMetricScraperStrategy("^GSPC", "S&P 500", mockHttpClient, mapper);
        
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("API down"));

        Page mockPage = mock(Page.class);
        Locator mockLocator = mock(Locator.class);
        
        when(mockPage.locator(anyString())).thenReturn(mockLocator);
        when(mockLocator.first()).thenReturn(mockLocator);
        when(mockLocator.count()).thenReturn(1);
        when(mockLocator.innerText()).thenReturn("5000.50");

        MacroMetric metric = strategy.scrape(mockPage);
        
        assertNotNull(metric);
        assertEquals(5000.50, metric.getPrice());
        
        verify(mockPage).navigate("https://finance.yahoo.com/quote/^GSPC");
    }

    @Test
    void scrape_apiAndPlaywrightFailure_returnsNull() throws Exception {
        YahooMetricScraperStrategy strategy = new YahooMetricScraperStrategy("^GSPC", "S&P 500", mockHttpClient, mapper);
        
        when(mockHttpClient.send(ArgumentMatchers.any(HttpRequest.class), ArgumentMatchers.any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("API down"));

        Page mockPage = mock(Page.class);
        doThrow(new RuntimeException("Navigation failed")).when(mockPage).navigate(anyString());

        MacroMetric metric = strategy.scrape(mockPage);
        
        assertNull(metric);
    }
}
