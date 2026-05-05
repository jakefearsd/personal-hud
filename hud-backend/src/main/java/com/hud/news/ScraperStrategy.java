package com.hud.news;

import com.microsoft.playwright.Page;

/**
 * Strategy interface for scraping specific data sources.
 */
public interface ScraperStrategy<T> {
    T scrape(Page page);
    String getUrl();
}
