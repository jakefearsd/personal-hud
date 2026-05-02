package com.hud.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlaywrightScraperServiceTest {

    @Test
    void shouldScrapeYahooFinance() {
        PlaywrightScraperService scraperService = new PlaywrightScraperService();
        List<NewsArticle> articles = scraperService.scrapeYahooFinance();
        
        assertNotNull(articles);
        assertFalse(articles.isEmpty(), "Should find some news articles on Yahoo Finance");
        
        NewsArticle first = articles.get(0);
        assertNotNull(first.title());
        assertFalse(first.title().isBlank());
        assertNotNull(first.url());
        assertTrue(first.url().startsWith("http"));
    }
}
