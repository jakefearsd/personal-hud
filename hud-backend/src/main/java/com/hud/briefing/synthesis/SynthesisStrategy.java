package com.hud.briefing.synthesis;

import com.hud.briefing.SynthesisResult;
import com.hud.briefing.BriefingCategory;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Strategy for synthesizing raw data into structured intelligence.
 */
public interface SynthesisStrategy {
    SynthesisResult synthesize(ChatLanguageModel model, BriefingCategory category, String rawData);
    boolean supports(BriefingCategory category);
}
