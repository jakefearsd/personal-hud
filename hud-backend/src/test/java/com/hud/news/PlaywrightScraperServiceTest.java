package com.hud.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class PlaywrightScraperServiceTest {

    @Test
    void shouldScrapeYahooFinance() {
        PlaywrightScraperService scraperService = new PlaywrightScraperService();
        List<NewsArticle> articles = scraperService.scrapeYahooFinance();
        
        assertNotNull(articles);
        assertFalse(articles.isEmpty(), "Should find some news articles on Yahoo Finance");
        assertTrue(articles.size() <= 15);
    }
}
