package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

/**
 * Base class for processing briefings using the GoF Template Method pattern.
 */
public abstract class BriefingProcessor {

    protected final PlaywrightScraperService scraperService;
    protected final ChatLanguageModel chatModel;
    protected final BriefingSourceStrategy sourceStrategy;

    protected BriefingProcessor(PlaywrightScraperService scraperService, 
                                ChatLanguageModel chatModel, 
                                BriefingSourceStrategy sourceStrategy) {
        this.scraperService = scraperService;
        this.chatModel = chatModel;
        this.sourceStrategy = sourceStrategy;
    }

    /**
     * The Template Method: Defines the analytical lifecycle.
     */
    public final String process(String query) {
        List<String> links = sourceStrategy.getLinks(query, getLinkLimit());
        
        if (links.isEmpty()) {
            throw new RuntimeException("No sources found for query: " + query);
        }

        String consolidatedText = acquireConsolidatedText(links);
        
        if (consolidatedText.length() < getMinRequiredChars()) {
            throw new RuntimeException("Insufficient situational data captured.");
        }

        return synthesize(consolidatedText);
    }

    protected abstract int getLinkLimit();
    protected abstract int getMinRequiredChars();
    protected abstract String synthesize(String rawText);

    protected String acquireConsolidatedText(List<String> links) {
        StringBuilder sb = new StringBuilder();
        for (String url : links) {
            String text = scraperService.extractFullText(url);
            if (isValid(text, url)) {
                sb.append("\n--- START SOURCE ---\n").append(text).append("\n--- END SOURCE ---\n");
            }
        }
        return sb.toString();
    }

    private boolean isValid(String text, String url) {
        if (text == null || text.length() < 500) return false;
        String lower = text.toLowerCase();
        return !lower.contains("before you continue") && !lower.contains("accept all cookies") && !url.contains("/about");
    }
}
