package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TheaterSourceStrategyTest {

    @Mock
    private PlaywrightScraperService scraperService;

    private TheaterSourceStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new TheaterSourceStrategy(scraperService);
    }

    @Test
    void testSupports() {
        assertTrue(strategy.supports(BriefingCategory.THEATER_UKRAINE));
        assertTrue(strategy.supports(BriefingCategory.THEATER_MIDDLE_EAST));
        assertTrue(strategy.supports(BriefingCategory.GLOBAL_SITREP));
        assertFalse(strategy.supports(BriefingCategory.FINANCE));
    }

    @Test
    void testGetLinksWithEmptyQuery() {
        assertTrue(strategy.getLinks(null, 5).isEmpty());
        assertTrue(strategy.getLinks("", 5).isEmpty());
        assertTrue(strategy.getLinks("   ", 5).isEmpty());
    }

    @Test
    void testGetLinksRss() {
        String rssUrl = "https://example.com/rss";
        when(scraperService.getLinksFromRss(eq(rssUrl), anyInt()))
                .thenReturn(List.of("link1", "link2"));

        List<String> links = strategy.getLinks(rssUrl, 5);

        assertEquals(2, links.size());
        verify(scraperService).getLinksFromRss(eq(rssUrl), anyInt());
    }

    @Test
    void testGetLinksIswUkraine() {
        when(scraperService.getIswLinks(anyInt()))
                .thenReturn(List.of(
                        "https://isw.org/offensive-campaign-assessment-1",
                        "https://isw.org/offensive-campaign-assessment-2",
                        "https://isw.org/ukraine-update-1",
                        "https://isw.org/something-else"
                ));

        List<String> links = strategy.getLinks("isw-ukraine", 2);

        assertEquals(2, links.size());
        assertTrue(links.get(0).contains("offensive-campaign-assessment"));
    }

    @Test
    void testGetLinksIswMideast() {
        when(scraperService.getIswLinks(anyInt()))
                .thenReturn(List.of(
                        "https://isw.org/iran-update-1",
                        "https://isw.org/israel-hamas-war-1",
                        "https://isw.org/middle-east-1",
                        "https://isw.org/ukraine-1"
                ));

        List<String> links = strategy.getLinks("isw-mideast", 3);

        assertEquals(3, links.size());
        assertTrue(links.stream().anyMatch(l -> l.contains("iran-update")));
        assertTrue(links.stream().anyMatch(l -> l.contains("israel-hamas-war")));
    }

    @Test
    void testGetLinksMultipleSources() {
        String query = "https://rss1.com, isw-ukraine";
        when(scraperService.getLinksFromRss(eq("https://rss1.com"), anyInt()))
                .thenReturn(List.of("rss-link"));
        when(scraperService.getIswLinks(anyInt()))
                .thenReturn(List.of("https://isw.org/offensive-campaign-assessment-1"));

        List<String> links = strategy.getLinks(query, 10);

        assertEquals(2, links.size());
        assertTrue(links.contains("rss-link"));
        assertTrue(links.contains("https://isw.org/offensive-campaign-assessment-1"));
    }
}
