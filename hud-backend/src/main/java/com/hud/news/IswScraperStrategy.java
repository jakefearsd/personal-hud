package com.hud.news;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IswScraperStrategy implements ScraperStrategy<List<String>> {
    private static final String URL = "https://www.understandingwar.org/publications";
    private static final String SELECTOR = "a[href*='offensive-campaign-assessment'], a[href*='iran-update'], a[href*='israel-hamas-war-update'], a[href*='conflict-update']";
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
        page.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        
        List<String> links = new ArrayList<>();
        page.locator(SELECTOR).all().forEach(l -> {
            try {
                String href = l.getAttribute("href");
                if (href != null && !href.isBlank()) {
                    String abs = href.startsWith("/") ? "https://www.understandingwar.org" + href : href;
                    if (!links.contains(abs)) links.add(abs);
                }
            } catch (Exception e) {}
        });
        return links.stream()
                .filter(l -> !l.contains("/about") && !l.contains("/terms") && !l.contains("/privacy") && !l.endsWith("/publications"))
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
