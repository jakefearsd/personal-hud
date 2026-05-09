package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TheaterSourceStrategyTest {

    @Mock private PlaywrightScraperService scraperService;
    private TheaterSourceStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new TheaterSourceStrategy(scraperService);
    }

    @Test
    void shouldFetchRssLinks() {
        when(scraperService.getLinksFromRss(anyString(), anyInt())).thenReturn(List.of("link1", "link2"));
        List<String> links = strategy.getLinks("https://rss.com", 10);
        assertEquals(2, links.size());
        verify(scraperService).getLinksFromRss(eq("https://rss.com"), anyInt());
    }

    @Test
    void shouldFetchIswUkraineLinks() {
        when(scraperService.getIswLinks(anyInt())).thenReturn(List.of(
            "offensive-campaign-assessment-ukraine-1", 
            "middle-east-iran-update",
            "offensive-campaign-assessment-ukraine-2"
        ));
        
        List<String> links = strategy.getLinks("isw-ukraine", 10);
        
        assertEquals(2, links.size());
        assertTrue(links.get(0).contains("ukraine"));
        verify(scraperService).getIswLinks(anyInt());
    }

    @Test
    void shouldFetchIswMideastLinks() {
        when(scraperService.getIswLinks(anyInt())).thenReturn(List.of(
            "offensive-campaign-assessment-ukraine", 
            "iran-update-may-1",
            "israel-hamas-war-update"
        ));
        
        List<String> links = strategy.getLinks("isw-mideast", 10);
        
        assertEquals(2, links.size());
        assertTrue(links.get(0).contains("iran") || links.get(0).contains("israel"));
    }

    @Test
    void shouldFetchCsisLinks() {
        when(scraperService.getCsisLinks(anyInt())).thenReturn(List.of("csis1", "csis2"));
        List<String> links = strategy.getLinks("csis-all", 10);
        assertEquals(2, links.size());
        verify(scraperService).getCsisLinks(anyInt());
    }

    @Test
    void shouldHandleMultipleSources() {
        when(scraperService.getLinksFromRss(anyString(), anyInt())).thenReturn(List.of("rss1"));
        when(scraperService.getCsisLinks(anyInt())).thenReturn(List.of("csis1"));
        
        List<String> links = strategy.getLinks("https://rss.com,csis-all", 10);
        
        assertEquals(2, links.size());
    }

    @Test
    void shouldSupportRelevantCategories() {
        assertTrue(strategy.supports(BriefingCategory.THEATER_UKRAINE));
        assertTrue(strategy.supports(BriefingCategory.THEATER_MIDDLE_EAST));
        assertTrue(strategy.supports(BriefingCategory.GLOBAL_SITREP));
        assertFalse(strategy.supports(BriefingCategory.FINANCE));
    }
}
