package com.hud.news;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class PlaywrightScraperServiceUnitTest {

    private final PlaywrightScraperService service = new PlaywrightScraperService(null, null, null);

    @Test
    void deepCrawlValidationRules() {
        assertTrue(service.isValidForDeepCrawl("https://isw.org/article-1", "isw.org"));
        assertFalse(service.isValidForDeepCrawl("https://other.com/article", "isw.org"));
        assertFalse(service.isValidForDeepCrawl("https://isw.org/about", "isw.org"));
        assertFalse(service.isValidForDeepCrawl("https://isw.org/search?q=test", "isw.org"));
        assertFalse(service.isValidForDeepCrawl("https://isw.org/doc.pdf", "isw.org"));
    }

    @Test
    void truncatesExcessiveContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000005; i++) sb.append("x");
        String longText = sb.toString();
        
        assertEquals(2000000, service.truncateContent(longText).length());
    }
}
