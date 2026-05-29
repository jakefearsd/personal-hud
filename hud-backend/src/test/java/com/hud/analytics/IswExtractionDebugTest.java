package com.hud.analytics;

import com.hud.news.PlaywrightScraperService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class IswExtractionDebugTest {

    @Autowired
    private PlaywrightScraperService scraperService;

    @Test
    void debugIswExtraction() {
        System.out.println("--- ISW EXTRACTION DEBUG START ---");
        List<String> links = scraperService.getIswLinks(1, com.hud.briefing.BriefingCategory.THEATER_UKRAINE);
        if (links.isEmpty()) {
            System.out.println("No ISW links found.");
            return;
        }
        
        String url = links.get(0);
        System.out.println("Target URL: " + url);
        
        String text = scraperService.extractFullText(url, 0);
        System.out.println("Extracted text length: " + text.length());
        System.out.println("--- TEXT SNIPPET (First 2000 chars) ---");
        System.out.println(text.length() > 2000 ? text.substring(0, 2000) : text);
        System.out.println("--- TEXT SNIPPET END ---");
        
        System.out.println("--- ISW EXTRACTION DEBUG END ---");
    }
}
