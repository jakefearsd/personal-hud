package com.hud.briefing;

import com.hud.briefing.synthesis.SynthesisStrategy;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.data.message.AiMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles the prompt engineering and high-resolution synthesis of raw intelligence.
 * Decoupled from the briefing orchestration to allow for specialized tuning.
 */
@Component
public class IntelligenceSynthesizer {

    private final List<SynthesisStrategy> strategies;

    public IntelligenceSynthesizer(List<SynthesisStrategy> strategies) {
        this.strategies = strategies;
    }

    public SynthesisResult synthesize(ChatLanguageModel model, BriefingCategory category, String rawData) {
        SynthesisStrategy strategy = strategies.stream()
                .filter(s -> s.supports(category))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No synthesis strategy found for category: " + category));
        
        return strategy.synthesize(model, category, rawData);
    }

    private BriefingCategory findCategoryForPersona(BriefingPersona persona) {
        for (BriefingCategory c : BriefingCategory.values()) {
            if (c.getPersona() == persona) return c;
        }
        return BriefingCategory.WORLD_NEWS; // Fallback
    }
}
