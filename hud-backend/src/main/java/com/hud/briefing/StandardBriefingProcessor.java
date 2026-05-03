package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Handles standard multi-article summaries for general news categories.
 */
public class StandardBriefingProcessor extends BriefingProcessor {

    private final BriefingPersona persona;

    public StandardBriefingProcessor(PlaywrightScraperService scraperService, 
                                   ChatLanguageModel chatModel, 
                                   BriefingSourceStrategy sourceStrategy,
                                   IntelligenceSynthesizer synthesizer,
                                   BriefingPersona persona) {
        super(scraperService, chatModel, sourceStrategy, synthesizer);
        this.persona = persona;
    }

    @Override
    protected int getLinkLimit() { return 3; }

    @Override
    protected int getMinRequiredChars() { return 800; }

    @Override
    protected String synthesize(String rawSignal) {
        return synthesizer.synthesizeStandard(chatModel, persona, rawSignal);
    }
}
