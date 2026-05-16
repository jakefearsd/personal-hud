package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class StandardBriefingProcessor extends BriefingProcessor {

    private final BriefingPersona persona;

    public StandardBriefingProcessor(PlaywrightScraperService scraperService,
                                     ChatLanguageModel chatModel,
                                     BriefingSourceStrategy sourceStrategy,
                                     IntelligenceSynthesizer synthesizer,
                                     BriefingCategory category) {
        super(scraperService, chatModel, sourceStrategy, synthesizer, category);
        this.persona = category.getPersona();
    }

    @Override
    protected int getLinkLimit() { return 15; }

    @Override
    protected int getMinRequiredChars() { return 1500; }

    @Override
    protected int getScrapeDepth() { return 0; }

    @Override
    protected SynthesisResult synthesize(String rawSignal) {
        return synthesizer.synthesizeStandard(chatModel, persona, rawSignal);
    }
}
