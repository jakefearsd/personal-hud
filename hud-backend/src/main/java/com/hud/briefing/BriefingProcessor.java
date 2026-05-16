package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Template method for turning raw signal into intelligence. In Phase 2 this is
 * a transitional form: links now carry source name/tier, but the reduce stage
 * still operates on concatenated text. Task 6 replaces acquireSignal/synthesize
 * with the map-reduce pipeline.
 */
public abstract class BriefingProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(BriefingProcessor.class);

    protected final PlaywrightScraperService scraperService;
    protected final ChatLanguageModel chatModel;
    protected final BriefingSourceStrategy sourceStrategy;
    protected final IntelligenceSynthesizer synthesizer;
    protected final BriefingCategory category;

    protected BriefingProcessor(PlaywrightScraperService scraperService,
                                ChatLanguageModel chatModel,
                                BriefingSourceStrategy sourceStrategy,
                                IntelligenceSynthesizer synthesizer,
                                BriefingCategory category) {
        this.scraperService = scraperService;
        this.chatModel = chatModel;
        this.sourceStrategy = sourceStrategy;
        this.synthesizer = synthesizer;
        this.category = category;
    }

    public final SynthesisResult process() {
        List<SourceLink> links = sourceStrategy.getLinks(category, getLinkLimit());

        if (links.isEmpty()) {
            throw new IllegalStateException("No signal sources found for: " + category);
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

    protected String acquireSignal(List<SourceLink> links) {
        StringBuilder sb = new StringBuilder();
        for (SourceLink link : links) {
            String text = scraperService.extractFullText(link.url(), getScrapeDepth());
            if (isPlausibleContent(text, link.url())) {
                sb.append("\n--- START SOURCE ---\n").append(text).append("\n--- END SOURCE ---\n");
            }
        }
        return sb.toString();
    }

    protected boolean isPlausibleContent(String text, String url) {
        if (text == null || text.length() < 500) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return !lower.contains("before you continue")
                && !lower.contains("accept all cookies")
                && !url.contains("/about");
    }
}
