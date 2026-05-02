package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Handles high-resolution, multi-stage extraction for tactical conflict theaters.
 */
public class DeepDiveBriefingProcessor extends BriefingProcessor {

    private final BriefingCategory category;
    private final BriefingPersona persona;

    public DeepDiveBriefingProcessor(PlaywrightScraperService scraperService, 
                                   ChatLanguageModel chatModel, 
                                   BriefingSourceStrategy sourceStrategy,
                                   BriefingCategory category) {
        super(scraperService, chatModel, sourceStrategy);
        this.category = category;
        this.persona = BriefingPersona.of(category);
    }

    @Override
    protected int getLinkLimit() { 
        return (category == BriefingCategory.GLOBAL_SITREP) ? 3 : 1; 
    }

    @Override
    protected int getMinRequiredChars() { return 1500; }

    @Override
    protected String synthesize(String rawText) {
        // High-density narrative block
        String intelligenceText = rawText.length() > 15000 ? rawText.substring(0, 15000) : rawText;

        if (category == BriefingCategory.GLOBAL_SITREP) {
            String prompt = String.format(
                "COMMAND DIRECTIVE: You are a Global Theater Strategist. " +
                "TASK: Re-write the following raw data into a cross-theater strategic summary. " +
                "RESTRICTION: DO NOT describe the text or sources. Provide a 3-5 paragraph narrative report.\n\nDATA:\n%s\n\nGLOBAL SITREP:",
                intelligenceText
            );
            return chatModel.generate(prompt);
        }

        // 3-Stage Command Fusion for active kinetic theaters
        String tempo = chatModel.generate(
            "COMMAND DIRECTIVE: You are a Tactical Ground Analyst. " +
            "TASK: Re-write the situational momentum into 2 dense narrative paragraphs. " +
            "IGNORE all citation brackets [1], [2] and links. " +
            "OUTPUT: Narrative Ground Truth only.\n\nDATA:\n" + intelligenceText);
        
        String strikes = chatModel.generate(
            "TASK: Extract kinetic strike data (Target, Location, Distance). " +
            "OUTPUT: Markdown Table. Header: '## Kinetic Impact'.\n\nDATA:\n" + intelligenceText);
            
        String innovation = chatModel.generate(
            "TASK: Identify 3 battlefield innovations (Tactics, EW, Drones). " +
            "OUTPUT: Bullet points. Header: '## Innovation & Adaptation'.\n\nDATA:\n" + intelligenceText);
        
        return String.format("# %s THEATER REPORT\n\n## Tactical Momentum\n%s\n\n%s\n\n%s", 
            category.name().replace("THEATER_", ""), tempo, strikes, innovation);
    }
}
