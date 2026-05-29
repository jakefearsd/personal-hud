package com.hud.briefing.synthesis;

import com.hud.briefing.BriefingCategory;
import com.hud.briefing.SynthesisResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;

/**
 * High-level strategic overview for the Global SITREP.
 */
@Component
public class GlobalSitrepStrategy implements SynthesisStrategy {

    @Override
    public SynthesisResult synthesize(ChatLanguageModel model, BriefingCategory category, String rawData) {
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

    @Override
    public boolean supports(BriefingCategory category) {
        return category == BriefingCategory.GLOBAL_SITREP;
    }
}
