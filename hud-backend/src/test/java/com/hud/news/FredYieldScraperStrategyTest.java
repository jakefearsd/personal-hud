package com.hud.news;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@Tag("unit")
class FredYieldScraperStrategyTest {

    private final FredYieldScraperStrategy strategy = new FredYieldScraperStrategy();

    @Test
    void getUrl_returnsFredUrl() {
        assertEquals("https://fred.stlouisfed.org/series/T10Y2Y", strategy.getUrl());
    }

    @Test
    void scrape_returnsParsedDouble() {
        Page mockPage = mock(Page.class);
        Locator mockLocator = mock(Locator.class);
        
        when(mockPage.locator(".series-meta-observation-value")).thenReturn(mockLocator);
        when(mockLocator.first()).thenReturn(mockLocator);
        when(mockLocator.innerText(ArgumentMatchers.any())).thenReturn(" -0.45 ");

        Double result = strategy.scrape(mockPage);
        
        assertEquals(-0.45, result);
        verify(mockPage).navigate(eq("https://fred.stlouisfed.org/series/T10Y2Y"), ArgumentMatchers.any());
    }

    @Test
    void scrape_returnsNullOnError() {
        Page mockPage = mock(Page.class);
        
        // Simulating an error during navigation
        doThrow(new RuntimeException("Navigation failed"))
            .when(mockPage).navigate(ArgumentMatchers.anyString(), ArgumentMatchers.any());

        Double result = strategy.scrape(mockPage);
        
        assertNull(result);
    }
}
