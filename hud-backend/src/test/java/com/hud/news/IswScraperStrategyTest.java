package com.hud.news;

import com.hud.briefing.BriefingCategory;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Tag("unit")
class IswScraperStrategyTest {

    @Test
    void getUrl_returnsIswUrl() {
        IswScraperStrategy strategy = new IswScraperStrategy(5, BriefingCategory.THEATER_UKRAINE);
        assertEquals("https://understandingwar.org/research/", strategy.getUrl());
    }

    @Test
    void scrape_handlesNavigationErrorAndReturnsEmptyList() {
        IswScraperStrategy strategy = new IswScraperStrategy(5, BriefingCategory.THEATER_UKRAINE);
        Page mockPage = mock(Page.class);
        
        doThrow(new RuntimeException("Navigation failed")).when(mockPage)
            .navigate(ArgumentMatchers.anyString(), ArgumentMatchers.any());
            
        List<String> result = strategy.scrape(mockPage);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void extractLinks_filtersByCategory() {
        IswScraperStrategy strategy = new IswScraperStrategy(2, BriefingCategory.THEATER_UKRAINE);
        Page mockPage = mock(Page.class);
        Locator mockLocator = mock(Locator.class);
        
        when(mockPage.locator(".research-card-title a, .research-card-loop-item a")).thenReturn(mockLocator);
        
        Locator link1 = mock(Locator.class);
        when(link1.getAttribute("href")).thenReturn("/research/russia-ukraine-update-1");
        
        Locator link2 = mock(Locator.class);
        when(link2.getAttribute("href")).thenReturn("/research/iran-update-1"); // Should be filtered out
        
        Locator link3 = mock(Locator.class);
        when(link3.getAttribute("href")).thenReturn("https://understandingwar.org/research/ukraine-update-2");

        when(mockLocator.all()).thenReturn(List.of(link1, link2, link3));

        List<String> result = strategy.extractLinks(mockPage);
        
        assertEquals(2, result.size());
        assertTrue(result.contains("https://understandingwar.org/research/russia-ukraine-update-1"));
        assertTrue(result.contains("https://understandingwar.org/research/ukraine-update-2"));
    }
}
