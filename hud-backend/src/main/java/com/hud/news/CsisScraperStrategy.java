package com.hud.news;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scraper strategy for the Center for Strategic and International Studies (CSIS).
 * Fetches high-density analytical reports and articles.
 */
public class CsisScraperStrategy implements ScraperStrategy<List<String>> {
    private static final Logger logger = LoggerFactory.getLogger(CsisScraperStrategy.class);
    private static final String URL = "https://www.csis.org/analysis";
    private static final String SELECTOR = ".view-content h3 a";
    private final int limit;

    public CsisScraperStrategy(int limit) {
        this.limit = limit;
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public List<String> scrape(Page page) {
        page.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        
        List<String> links = new ArrayList<>();
        page.locator(SELECTOR).all().forEach(l -> {
            try {
                String href = l.getAttribute("href");
                if (href != null && !href.isBlank()) {
                    String abs = href.startsWith("/") ? "https://www.csis.org" + href : href;
                    if (!links.contains(abs)) links.add(abs);
                }
            } catch (Exception e) {
                logger.warn("[CSIS] Failed to extract link attribute: {}", e.getMessage());
            }
        });
        
        return links.stream()
                .filter(l -> !l.contains("/about") && !l.contains("/events") && !l.contains("/programs"))
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
