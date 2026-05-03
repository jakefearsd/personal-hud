package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Handles the prompt engineering and high-resolution synthesis of raw intelligence.
 * Decoupled from the briefing orchestration to allow for specialized tuning.
 */
@Component
public class IntelligenceSynthesizer {

    /**
     * Synthesizes a standard narrative briefing for news categories.
     */
    public String synthesizeStandard(ChatLanguageModel model, BriefingPersona persona, String rawData) {
        String prompt = String.format(
            "You are the %s. %s\nSTRICT RULES: NO META-COMMENTARY. FOCUS on %s. Use Markdown. 2-5 dense paragraphs.\n\nINTELLIGENCE DATA:\n%s\n\nTACTICAL BRIEFING:",
            persona.name(), persona.instruction(), persona.focus(), rawData
        );
        return model.generate(prompt);
    }

    /**
     * Performs a 3-stage intelligence fusion for kinetic conflict theaters.
     */
    public String fuseTheaterIntelligence(ChatLanguageModel model, BriefingCategory category, String rawData) {
        String data = rawData.length() > 500000 ? rawData.substring(0, 500000) : rawData;

        // Stage 1: Tactical Momentum
        String tempo = model.generate(
            "COMMAND DIRECTIVE: You are a Tactical Ground Analyst. " +
            "TASK: Re-write the situational momentum into 2 dense narrative paragraphs. " +
            "IGNORE all citation brackets [1], [2] and links. " +
            "OUTPUT: Narrative Ground Truth only.\n\nDATA:\n" + data);
        
        // Stage 2: Kinetic Impact
        String strikes = model.generate(
            "TASK: Extract kinetic strike data (Target, Location, Distance). " +
            "OUTPUT: Markdown Table. Header: '## Kinetic Impact'.\n\nDATA:\n" + data);
            
        // Stage 3: Innovation & Adaptation
        String innovation = model.generate(
            "TASK: Identify 3 battlefield innovations (Tactics, EW, Drones). " +
            "OUTPUT: Bullet points. Header: '## Innovation & Adaptation'.\n\nDATA:\n" + data);
        
        return String.format("# %s THEATER REPORT\n\n## Tactical Momentum\n%s\n\n%s\n\n%s", 
            category.name().replace("THEATER_", ""), tempo, strikes, innovation);
    }

    /**
     * Synthesizes a high-level strategic overview for the Global SITREP.
     */
    public String synthesizeGlobalSitrep(ChatLanguageModel model, String rawData) {
        String data = rawData.length() > 500000 ? rawData.substring(0, 500000) : rawData;
        String prompt = String.format(
            "COMMAND DIRECTIVE: You are a Global Theater Strategist. " +
            "TASK: Re-write the following raw data into a cross-theater strategic summary. " +
            "RESTRICTION: DO NOT describe the text or sources. Provide a 3-5 paragraph narrative report.\n\nDATA:\n%s\n\nGLOBAL SITREP:",
            data
        );
        return model.generate(prompt);
    }
}
