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
        
        if (isTheaterCategory(category)) {
            return new DeepDiveBriefingProcessor(scraperService, model, strategy, synthesizer, category);
        } else {
            return new StandardBriefingProcessor(scraperService, model, strategy, synthesizer, category.getPersona());
        }
    }

    private boolean isTheaterCategory(BriefingCategory c) {
        return c == BriefingCategory.THEATER_UKRAINE || 
               c == BriefingCategory.THEATER_MIDDLE_EAST || 
               c == BriefingCategory.GLOBAL_SITREP;
    }
}
