package com.hud.news;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Template Method Pattern for Scraping Links.
 * Defines the skeleton of the link extraction algorithm, deferring some steps to subclasses.
 */
public abstract class AbstractLinkScraperStrategy implements ScraperStrategy<List<String>> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractLinkScraperStrategy.class);
    protected final int limit;

    public AbstractLinkScraperStrategy(int limit) {
        this.limit = limit;
    }

    @Override
    public final List<String> scrape(Page page) {
        navigate(page);
        
        List<String> links = new ArrayList<>();
        page.locator(getSelector()).all().forEach(l -> {
            try {
                String href = l.getAttribute("href");
                if (href != null && !href.isBlank()) {
                    String abs = getAbsoluteUrl(href);
                    if (isRelevant(abs) && !links.contains(abs)) {
                        links.add(abs);
                    }
                }
            } catch (Exception e) {
                logger.warn("[{}] Failed to extract link attribute: {}", getLogPrefix(), e.getMessage());
            }
        });
        
        postExtractionHook(links);
        
        return links.stream()
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    protected void navigate(Page page) {
        try {
            logger.info("[{}] Navigating to {}...", getLogPrefix(), getUrl());
            page.navigate(getUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(60000));
        } catch (Exception e) {
            logger.error("[{}] Failed to navigate to {}: {}", getLogPrefix(), getUrl(), e.getMessage());
        }
    }

    protected abstract String getSelector();
    
    protected abstract String getAbsoluteUrl(String href);
    
    protected abstract String getLogPrefix();

    /** Hook method for subclasses to filter relevant links */
    protected boolean isRelevant(String url) {
        return true; 
    }
    
    /** Hook method for subclasses to perform actions after extraction */
    protected void postExtractionHook(List<String> links) {
        // Default is no-op
    }
}
