package com.hud.briefing.synthesis;

import com.hud.briefing.BriefingCategory;
import com.hud.briefing.BriefingPersona;
import com.hud.briefing.SynthesisResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;

/**
 * Standard narrative synthesis for news categories.
 */
@Component
public class StandardSynthesisStrategy implements SynthesisStrategy {

    @Override
    public SynthesisResult synthesize(ChatLanguageModel model, BriefingCategory category, String rawData) {
        BriefingPersona persona = category.getPersona();
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

    @Override
    public boolean supports(BriefingCategory category) {
        return category != BriefingCategory.GLOBAL_SITREP && 
               !category.name().startsWith("THEATER_");
    }
}
