package com.hud.news;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FredYieldScraperStrategy implements ScraperStrategy<Double> {
    private static final Logger logger = LoggerFactory.getLogger(FredYieldScraperStrategy.class);
    private static final String URL = "https://fred.stlouisfed.org/series/T10Y2Y";

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public Double scrape(Page page) {
        try {
            page.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            String val = page.locator(".series-meta-observation-value").first().innerText();
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            logger.error("[FRED] Failed to scrape yield spread from {}: {}", URL, e.getMessage(), e);
            return null;
        }
    }
}
