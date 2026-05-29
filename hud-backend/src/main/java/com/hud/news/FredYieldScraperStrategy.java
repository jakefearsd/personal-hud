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
            logger.debug("[FRED] Navigating to {}...", URL);
            page.navigate(URL, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60000));
            
            logger.debug("[FRED] Waiting for observation value...");
            String val = page.locator(".series-meta-observation-value").first()
                    .innerText(new com.microsoft.playwright.Locator.InnerTextOptions().setTimeout(30000));
            
            logger.info("[FRED] Successfully scraped yield spread: {}", val);
            return Double.parseDouble(val.trim());
        } catch (Exception e) {
            logger.error("[FRED] Failed to scrape yield spread from {}: {}", URL, e.getMessage());
            return null;
        }
    }
}
