package com.hud.news;

import com.hud.briefing.BriefingCategory;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IswScraperStrategy extends AbstractLinkScraperStrategy {
    private static final Logger logger = LoggerFactory.getLogger(IswScraperStrategy.class);
    private static final String URL = "https://understandingwar.org/research/";
    private final BriefingCategory category;

    public IswScraperStrategy(int limit, BriefingCategory category) {
        super(limit);
        this.category = category;
    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    protected void navigate(Page page) {
        try {
            logger.info("[ISW] Navigating to research library for {}...", category);
            page.navigate(URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(90000));
            page.waitForTimeout(5000); 
        } catch (Exception e) {
            logger.error("[ISW] Failed to navigate to {}: {}", URL, e.getMessage());
        }
    }

    @Override
    protected String getSelector() {
        return ".research-card-title a, .research-card-loop-item a";
    }

    @Override
    protected String getAbsoluteUrl(String href) {
        return href.startsWith("/") ? "https://understandingwar.org" + href : href;
    }

    @Override
    protected String getLogPrefix() {
        return "ISW";
    }

    @Override
    protected void postExtractionHook(List<String> links) {
        logger.info("[ISW] Discovered {} relevant links for {}", links.size(), category);
    }

    @Override
    protected boolean isRelevant(String url) {
        if (!url.contains("/research/")) return false;
        if (url.endsWith("/research/")) return false;

        if (category == BriefingCategory.THEATER_UKRAINE) {
            return url.contains("russia-ukraine") || url.contains("ukraine-update");
        }
        if (category == BriefingCategory.THEATER_MIDDLE_EAST) {
            return url.contains("middle-east") || url.contains("iran-update");
        }
        // For other categories (like GLOBAL_SITREP), take anything from research
        return true;
    }
}
