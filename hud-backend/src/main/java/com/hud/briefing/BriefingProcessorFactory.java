package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Factory for creating the appropriate BriefingProcessor for a given category.
 * Encapsulates the wiring of Scrapers, Models, and Synthesizers.
 */
@Component
public class BriefingProcessorFactory {

    private final PlaywrightScraperService scraperService;
    private final BriefingSourceFactory sourceFactory;
    private final IntelligenceSynthesizer synthesizer;

    public BriefingProcessorFactory(PlaywrightScraperService scraperService, 
                                   BriefingSourceFactory sourceFactory,
                                   IntelligenceSynthesizer synthesizer) {
        this.scraperService = scraperService;
        this.sourceFactory = sourceFactory;
        this.synthesizer = synthesizer;
    }

    public BriefingProcessor getProcessor(BriefingCategory category, ChatLanguageModel model) {
        BriefingSourceStrategy strategy = sourceFactory.getStrategy(category);
        BriefingProcessorConfiguration config = getConfiguration(category);

        return new BriefingProcessor(scraperService, model, strategy, synthesizer, category, config);
    }

    private BriefingProcessorConfiguration getConfiguration(BriefingCategory category) {
        if (category == BriefingCategory.GLOBAL_SITREP) {
            return BriefingProcessorConfiguration.GLOBAL_SITREP;
        }
        if (category.name().startsWith("THEATER_")) {
            return BriefingProcessorConfiguration.DEEP_DIVE;
        }
        return BriefingProcessorConfiguration.STANDARD;
    }
}
