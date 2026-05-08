package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;
import java.util.Locale;

/**
 * Base class for processing briefings using the GoF Template Method pattern.
 * Coordinates the Acquisition and Synthesis phases of the intelligence lifecycle.
 */
public abstract class BriefingProcessor {

    protected final PlaywrightScraperService scraperService;
    protected final ChatLanguageModel chatModel;
    protected final BriefingSourceStrategy sourceStrategy;
    protected final IntelligenceSynthesizer synthesizer;

    protected BriefingProcessor(PlaywrightScraperService scraperService, 
                                ChatLanguageModel chatModel, 
                                BriefingSourceStrategy sourceStrategy,
                                IntelligenceSynthesizer synthesizer) {
        this.scraperService = scraperService;
        this.chatModel = chatModel;
        this.sourceStrategy = sourceStrategy;
        this.synthesizer = synthesizer;
    }

    /**
     * The Template Method: Orchestrates the transition from Raw Signal to Intelligence.
     */
    public final SynthesisResult process(String query) {
        List<String> links = sourceStrategy.getLinks(query, getLinkLimit());
        
        if (links.isEmpty()) {
            throw new IllegalStateException("No signal sources found for: " + query);
        }

        String consolidatedSignal = acquireSignal(links);
        
        if (consolidatedSignal.length() < getMinRequiredChars()) {
            throw new IllegalStateException("Insufficient situational signal captured.");
        }

        return synthesize(consolidatedSignal);
    }

    protected abstract int getLinkLimit();
    protected abstract int getMinRequiredChars();
    protected abstract int getScrapeDepth();
    protected abstract SynthesisResult synthesize(String rawSignal);

    protected String acquireSignal(List<String> links) {
        StringBuilder sb = new StringBuilder();
        for (String url : links) {
            String text = scraperService.extractFullText(url, getScrapeDepth());
            if (isPlausibleContent(text, url)) {
                sb.append("\n--- START SOURCE ---\n").append(text).append("\n--- END SOURCE ---\n");
            }
        }
        return sb.toString();
    }

    private boolean isPlausibleContent(String text, String url) {
        if (text == null || text.length() < 500) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        // Filter out common cookie walls and non-narrative pages
        return !lower.contains("before you continue") && !lower.contains("accept all cookies") && !url.contains("/about");
    }
}
