package com.hud.news;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class PlaywrightScraperServiceUnitTest {

    @Test
    void shouldCleanExtractedText() {
        PlaywrightScraperService service = new PlaywrightScraperService();
        String input = "Valid content. Endnotes. Extra stuff.";
        // Index of Endnotes is 15. The logic says if idx > 1000, truncate. 
        // So this should NOT truncate.
        assertEquals(input.trim(), service.cleanExtractedText(input));
        
        StringBuilder sb = new StringBuilder("Valid content.");
        for (int i = 0; i < 1100; i++) sb.append("a");
        sb.append("Endnotes. Extra stuff.");
        String longInput = sb.toString();
        
        String cleaned = service.cleanExtractedText(longInput);
        assertFalse(cleaned.contains("Extra stuff."));
        assertTrue(cleaned.contains("Valid content."));
    }

    @Test
    void shouldValidateForDeepCrawl() {
        PlaywrightScraperService service = new PlaywrightScraperService();
        String host = "example.com";
        
        assertTrue(service.isValidForDeepCrawl("https://example.com/news/1", host));
        assertFalse(service.isValidForDeepCrawl("https://other.com/news/1", host));
        assertFalse(service.isValidForDeepCrawl("https://example.com/about", host));
        assertFalse(service.isValidForDeepCrawl("https://example.com/page#anchor", host));
        assertFalse(service.isValidForDeepCrawl("https://example.com/doc.pdf", host));
    }

    @Test
    void shouldTruncateContent() {
        PlaywrightScraperService service = new PlaywrightScraperService();
        String shortText = "Hello";
        assertEquals(shortText, service.truncateContent(shortText));
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000005; i++) sb.append("x");
        String longText = sb.toString();
        
        assertEquals(2000000, service.truncateContent(longText).length());
    }
}
