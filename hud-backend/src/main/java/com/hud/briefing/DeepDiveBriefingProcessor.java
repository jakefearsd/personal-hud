package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Handles high-resolution situational synthesis for tactical conflict theaters.
 */
public class DeepDiveBriefingProcessor extends BriefingProcessor {

    private final BriefingCategory category;

    public DeepDiveBriefingProcessor(PlaywrightScraperService scraperService, 
                                   ChatLanguageModel chatModel, 
                                   BriefingSourceStrategy sourceStrategy,
                                   IntelligenceSynthesizer synthesizer,
                                   BriefingCategory category) {
        super(scraperService, chatModel, sourceStrategy, synthesizer);
        this.category = category;
    }

    @Override
    protected int getLinkLimit() { 
        return (category == BriefingCategory.GLOBAL_SITREP) ? 6 : 4; 
    }

    @Override
    protected int getMinRequiredChars() { return 2500; }

    @Override
    protected String synthesize(String rawSignal) {
        if (category == BriefingCategory.GLOBAL_SITREP) {
            return synthesizer.synthesizeGlobalSitrep(chatModel, rawSignal);
        }
        return synthesizer.fuseTheaterIntelligence(chatModel, category, rawSignal);
    }
}
