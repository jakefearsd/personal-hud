package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class DeepDiveBriefingProcessor extends BriefingProcessor {

    public DeepDiveBriefingProcessor(PlaywrightScraperService scraperService,
                                     ChatLanguageModel chatModel,
                                     BriefingSourceStrategy sourceStrategy,
                                     IntelligenceSynthesizer synthesizer,
                                     BriefingCategory category) {
        super(scraperService, chatModel, sourceStrategy, synthesizer, category);
    }

    @Override
    protected int getLinkLimit() {
        return (category == BriefingCategory.GLOBAL_SITREP) ? 25 : 15;
    }

    @Override
    protected int getMinRequiredChars() { return 2500; }

    @Override
    protected int getScrapeDepth() { return 1; }

    @Override
    protected SynthesisResult synthesize(String rawSignal) {
        if (category == BriefingCategory.GLOBAL_SITREP) {
            return synthesizer.synthesizeGlobalSitrep(chatModel, rawSignal);
        }
        return synthesizer.fuseTheaterIntelligence(chatModel, category, rawSignal);
    }
}
