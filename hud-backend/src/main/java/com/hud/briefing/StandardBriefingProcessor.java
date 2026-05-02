package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Handles standard multi-article summaries for news categories.
 */
public class StandardBriefingProcessor extends BriefingProcessor {

    private final BriefingPersona persona;

    public StandardBriefingProcessor(PlaywrightScraperService scraperService, 
                                   ChatLanguageModel chatModel, 
                                   BriefingSourceStrategy sourceStrategy,
                                   BriefingPersona persona) {
        super(scraperService, chatModel, sourceStrategy);
        this.persona = persona;
    }

    @Override
    protected int getLinkLimit() { return 3; }

    @Override
    protected int getMinRequiredChars() { return 800; }

    @Override
    protected String synthesize(String rawText) {
        String prompt = String.format(
            "You are the %s. %s\nSTRICT RULES: NO META-COMMENTARY. FOCUS on %s. Use Markdown. 2-5 dense paragraphs.\n\nINTELLIGENCE DATA:\n%s\n\nTACTICAL BRIEFING:",
            persona.name(), persona.instruction(), persona.focus(), rawText
        );
        return chatModel.generate(prompt);
    }
}
