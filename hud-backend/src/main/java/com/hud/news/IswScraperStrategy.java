package com.hud.news;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IswScraperStrategy implements ScraperStrategy<List<String>> {
    private static final Logger logger = LoggerFactory.getLogger(IswScraperStrategy.class);
    private static final String URL = "https://understandingwar.org/research/";
    private static final String SELECTOR = ".research-card-title a";
    private final int limit;

    public IswScraperStrategy(int limit) {
        this.limit = limit;
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public List<String> scrape(Page page) {
        try {
            page.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
            // Wait for the research library to load (it might have dynamic content)
            page.waitForSelector(SELECTOR, new Page.WaitForSelectorOptions().setTimeout(10000));
        } catch (Exception e) {
            logger.error("[ISW] Failed to navigate to {}: {}", URL, e.getMessage());
            return List.of();
        }
        
        List<String> links = new ArrayList<>();
        page.locator(SELECTOR).all().forEach(l -> {
            try {
                String href = l.getAttribute("href");
                if (href != null && !href.isBlank()) {
                    String abs = href.startsWith("/") ? "https://understandingwar.org" + href : href;
                    if (!links.contains(abs)) links.add(abs);
                }
            } catch (Exception e) {
                logger.warn("[ISW] Failed to extract link attribute: {}", e.getMessage());
            }
        });
        return links.stream()
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
