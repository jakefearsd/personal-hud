package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneralRssSourceStrategyTest {

    @Mock
    private PlaywrightScraperService scraperService;

    private GeneralRssSourceStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new GeneralRssSourceStrategy(scraperService);
    }

    @Test
    void testSupports() {
        assertTrue(strategy.supports(BriefingCategory.WORLD_NEWS));
        assertTrue(strategy.supports(BriefingCategory.US_NEWS));
        assertTrue(strategy.supports(BriefingCategory.FINANCE));
        assertTrue(strategy.supports(BriefingCategory.TECHNOLOGY));
        assertFalse(strategy.supports(BriefingCategory.GLOBAL_SITREP));
    }

    @Test
    void testGetLinksWithEmptyQuery() {
        assertTrue(strategy.getLinks(null, 5).isEmpty());
        assertTrue(strategy.getLinks("", 5).isEmpty());
        assertTrue(strategy.getLinks("   ", 5).isEmpty());
    }

    @Test
    void testGetLinksSingleFeed() {
        String feedUrl = "https://example.com/rss";
        when(scraperService.getLinksFromRss(eq(feedUrl), anyInt()))
                .thenReturn(List.of("link1", "link2", "link3"));

        List<String> links = strategy.getLinks(feedUrl, 2);

        assertEquals(2, links.size());
        verify(scraperService).getLinksFromRss(eq(feedUrl), anyInt());
    }

    @Test
    void testGetLinksMultipleFeeds() {
        String query = "https://rss1.com, https://rss2.com";
        when(scraperService.getLinksFromRss(eq("https://rss1.com"), anyInt()))
                .thenReturn(List.of("link1"));
        when(scraperService.getLinksFromRss(eq("https://rss2.com"), anyInt()))
                .thenReturn(List.of("link2"));

        List<String> links = strategy.getLinks(query, 10);

        assertEquals(2, links.size());
        assertTrue(links.contains("link1"));
        assertTrue(links.contains("link2"));
    }
}
