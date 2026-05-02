package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Factory for creating the appropriate BriefingProcessor for a given category.
 */
@Component
public class BriefingProcessorFactory {

    private final PlaywrightScraperService scraperService;
    private final BriefingSourceFactory sourceFactory;

    public BriefingProcessorFactory(PlaywrightScraperService scraperService, 
                                   BriefingSourceFactory sourceFactory) {
        this.scraperService = scraperService;
        this.sourceFactory = sourceFactory;
    }

    public BriefingProcessor getProcessor(BriefingCategory category, ChatLanguageModel model) {
        BriefingSourceStrategy strategy = sourceFactory.getStrategy(category);
        
        if (isTheaterCategory(category)) {
            return new DeepDiveBriefingProcessor(scraperService, model, strategy, category);
        } else {
            return new StandardBriefingProcessor(scraperService, model, strategy, BriefingPersona.of(category));
        }
    }

    private boolean isTheaterCategory(BriefingCategory c) {
        return c == BriefingCategory.THEATER_UKRAINE || 
               c == BriefingCategory.THEATER_MIDDLE_EAST || 
               c == BriefingCategory.GLOBAL_SITREP;
    }
}
