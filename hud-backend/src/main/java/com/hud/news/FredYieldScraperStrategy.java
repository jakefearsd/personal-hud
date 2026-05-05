package com.hud.news;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

public class FredYieldScraperStrategy implements ScraperStrategy<Double> {
    private static final String URL = "https://fred.stlouisfed.org/series/T10Y2Y";

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public Double scrape(Page page) {
        try {
            page.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            String value = page.locator(".series-meta-observation-value").innerText();
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            System.err.println("[FRED] Failed to scrape yield spread: " + e.getMessage());
            return null;
        }
    }
}
