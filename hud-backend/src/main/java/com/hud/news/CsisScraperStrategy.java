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
public class CsisScraperStrategy extends AbstractLinkScraperStrategy {
    private static final String URL = "https://www.csis.org/analysis";
    private static final String SELECTOR = ".view-content h3 a";

    public CsisScraperStrategy(int limit) {
        super(limit);
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    protected String getSelector() {
        return SELECTOR;
    }

    @Override
    protected String getAbsoluteUrl(String href) {
        return href.startsWith("/") ? "https://www.csis.org" + href : href;
    }

    @Override
    protected String getLogPrefix() {
        return "CSIS";
    }

    @Override
    protected boolean isRelevant(String url) {
        return !url.contains("/about") && !url.contains("/events") && !url.contains("/programs");
    }
}
