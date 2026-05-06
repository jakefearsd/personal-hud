package com.hud.briefing;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.data.message.AiMessage;
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
    public SynthesisResult synthesizeStandard(ChatLanguageModel model, BriefingPersona persona, String rawData) {
        String prompt = String.format(
            "You are the %s. %s\nSTRICT RULES: NO META-COMMENTARY. FOCUS on %s. Use Markdown. 2-5 dense paragraphs.\n\nINTELLIGENCE DATA:\n%s\n\nTACTICAL BRIEFING:",
            persona.name(), persona.instruction(), persona.focus(), rawData
        );
        Response<AiMessage> response = model.generate(UserMessage.from(prompt));
        TokenUsage usage = response.tokenUsage();
        return new SynthesisResult(
            response.content().text(),
            usage != null ? usage.inputTokenCount() : 0,
            usage != null ? usage.outputTokenCount() : 0
        );
    }

    /**
     * Performs a 3-stage intelligence fusion for kinetic conflict theaters.
     */
    public SynthesisResult fuseTheaterIntelligence(ChatLanguageModel model, BriefingCategory category, String rawData) {
        String data = rawData.length() > 2000000 ? rawData.substring(0, 2000000) : rawData;

        // Stage 1: Tactical Momentum
        Response<AiMessage> tempoRes = model.generate(UserMessage.from(
            "COMMAND DIRECTIVE: You are a Tactical Ground Analyst. " +
            "TASK: Re-write the situational momentum into 2 dense narrative paragraphs. " +
            "IGNORE all citation brackets [1], [2] and links. " +
            "OUTPUT: Narrative Ground Truth only.\n\nDATA:\n" + data));
        
        // Stage 2: Kinetic Impact
        Response<AiMessage> strikesRes = model.generate(UserMessage.from(
            "TASK: Extract kinetic strike data (Target, Location, Distance). " +
            "STRICT RULE: Output MUST be a valid GFM Markdown Table. " +
            "STRICT RULE: Every Markdown row MUST be on its own line. " +
            "STRICT RULE: Ensure the separator row (e.g., | --- | --- |) is present. " +
            "HEADER: '## Kinetic Impact'\n\nDATA:\n" + data));
            
        // Stage 3: Innovation & Adaptation
        Response<AiMessage> innovationRes = model.generate(UserMessage.from(
            "TASK: Identify battlefield innovations (Tactics, EW, Drones). " +
            "OUTPUT: Clean bullet points. " +
            "HEADER: '## Innovation & Adaptation'\n\nDATA:\n" + data));
        
        String combinedContent = String.format("# %s THEATER REPORT\n\n## Tactical Momentum\n%s\n\n%s\n\n%s", 
            category.name().replace("THEATER_", ""), 
            tempoRes.content().text(), 
            strikesRes.content().text(), 
            innovationRes.content().text());

        int inputTokens = 0;
        int outputTokens = 0;

        if (tempoRes.tokenUsage() != null) {
            inputTokens += tempoRes.tokenUsage().inputTokenCount();
            outputTokens += tempoRes.tokenUsage().outputTokenCount();
        }
        if (strikesRes.tokenUsage() != null) {
            inputTokens += strikesRes.tokenUsage().inputTokenCount();
            outputTokens += strikesRes.tokenUsage().outputTokenCount();
        }
        if (innovationRes.tokenUsage() != null) {
            inputTokens += innovationRes.tokenUsage().inputTokenCount();
            outputTokens += innovationRes.tokenUsage().outputTokenCount();
        }

        return new SynthesisResult(combinedContent, inputTokens, outputTokens);
    }

    /**
     * Synthesizes a high-level strategic overview for the Global SITREP.
     */
    public SynthesisResult synthesizeGlobalSitrep(ChatLanguageModel model, String rawData) {
        String data = rawData.length() > 2000000 ? rawData.substring(0, 2000000) : rawData;
        String prompt = String.format(
            "COMMAND DIRECTIVE: You are a Global Theater Strategist. " +
            "TASK: Re-write the following raw data into a cross-theater strategic summary. " +
            "RESTRICTION: DO NOT describe the text or sources. Provide a 3-5 paragraph narrative report.\n\nDATA:\n%s\n\nGLOBAL SITREP:",
            data
        );
        Response<AiMessage> response = model.generate(UserMessage.from(prompt));
        TokenUsage usage = response.tokenUsage();
        return new SynthesisResult(
            response.content().text(),
            usage != null ? usage.inputTokenCount() : 0,
            usage != null ? usage.outputTokenCount() : 0
        );
    }
}
