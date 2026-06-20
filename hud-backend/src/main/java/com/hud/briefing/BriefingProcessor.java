package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Orchestrates the intelligence pipeline: acquisition, filtering, and synthesis.
 * Uses Strategy pattern for synthesis and Configuration for behavioral tuning.
 */
public class BriefingProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BriefingProcessor.class);

    private final PlaywrightScraperService scraperService;
    private final ChatLanguageModel chatModel;
    private final BriefingSourceStrategy sourceStrategy;
    private final IntelligenceSynthesizer synthesizer;
    private final BriefingCategory category;
    private final BriefingProcessorConfiguration config;

    public BriefingProcessor(PlaywrightScraperService scraperService,
                             ChatLanguageModel chatModel,
                             BriefingSourceStrategy sourceStrategy,
                             IntelligenceSynthesizer synthesizer,
                             BriefingCategory category,
                             BriefingProcessorConfiguration config) {
        this.scraperService = scraperService;
        this.chatModel = chatModel;
        this.sourceStrategy = sourceStrategy;
        this.synthesizer = synthesizer;
        this.category = category;
        this.config = config;
    }

    public SynthesisResult process() {
        List<SourceLink> links = sourceStrategy.getLinks(category, config.linkLimit());

        if (links.isEmpty()) {
            throw new IllegalStateException("No signal sources found for: " + category);
        }
        
        logger.info("[PIPELINE] Found {} candidate links for category {}", links.size(), category);

        String consolidatedSignal = acquireSignal(links);

        logger.info("[PIPELINE] Acquired {} characters of situational signal for {}", consolidatedSignal.length(), category);

        if (consolidatedSignal.length() < config.minRequiredChars()) {
            logger.error("[PIPELINE] ABORTING: Insufficient situational signal ({} chars) for {}. Required: {}", 
                    consolidatedSignal.length(), category, config.minRequiredChars());
            throw new IllegalStateException("Insufficient situational signal captured.");
        }

        return synthesizer.synthesize(chatModel, category, consolidatedSignal);
    }

    private String acquireSignal(List<SourceLink> links) {
        StringBuilder sb = new StringBuilder();
        int successCount = 0;
        for (SourceLink link : links) {
            logger.info("[PIPELINE] Scraping link: {}", link.url());
            String text = scraperService.extractFullText(link.url(), config.scrapeDepth());
            if (isPlausibleContent(text, link.url())) {
                sb.append("\n--- START SOURCE ---\n").append(text).append("\n--- END SOURCE ---\n");
                successCount++;
            } else {
                logger.debug("[PIPELINE] Link {} rejected: text length = {}", link.url(), (text != null ? text.length() : "null"));
            }
        }
        logger.info("[PIPELINE] Category {} processed {} links, {} passed plausibility check.", category, links.size(), successCount);
        return sb.toString();
    }

    private boolean isPlausibleContent(String text, String url) {
        if (text.length() < 500) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        boolean isAdOrCookie = lower.contains("before you continue")
                || lower.contains("accept all cookies")
                || url.contains("/about");
        
        return !isAdOrCookie;
    }
}
