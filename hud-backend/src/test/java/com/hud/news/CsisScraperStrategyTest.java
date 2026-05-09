package com.hud.news;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CsisScraperStrategyTest {

    @Mock private Page page;
    @Mock private Locator locator;
    @Mock private Locator link1;
    @Mock private Locator link2;

    private CsisScraperStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new CsisScraperStrategy(2);
    }

    @Test
    void shouldScrapeCsisLinks() {
        when(page.locator(anyString())).thenReturn(locator);
        when(locator.all()).thenReturn(List.of(link1, link2));
        
        when(link1.getAttribute("href")).thenReturn("/analysis/report1");
        when(link2.getAttribute("href")).thenReturn("https://www.csis.org/analysis/report2");

        List<String> links = strategy.scrape(page);

        assertEquals(2, links.size());
        assertTrue(links.contains("https://www.csis.org/analysis/report1"));
        assertTrue(links.contains("https://www.csis.org/analysis/report2"));
        
        verify(page).navigate(eq("https://www.csis.org/analysis"), any());
    }

    @Test
    void shouldFilterExcludedLinks() {
        when(page.locator(anyString())).thenReturn(locator);
        when(locator.all()).thenReturn(List.of(link1, link2));
        
        when(link1.getAttribute("href")).thenReturn("/about-us");
        when(link2.getAttribute("href")).thenReturn("/analysis/report1");

        List<String> links = strategy.scrape(page);

        assertEquals(1, links.size());
        assertTrue(links.contains("https://www.csis.org/analysis/report1"));
        assertFalse(links.contains("https://www.csis.org/about-us"));
    }

    @Test
    void shouldRespectLimit() {
        strategy = new CsisScraperStrategy(1);
        when(page.locator(anyString())).thenReturn(locator);
        when(locator.all()).thenReturn(List.of(link1, link2));
        
        when(link1.getAttribute("href")).thenReturn("/analysis/1");
        when(link2.getAttribute("href")).thenReturn("/analysis/2");

        List<String> links = strategy.scrape(page);

        assertEquals(1, links.size());
    }
}
