package com.hud.briefing;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Strategy interface for LLM providers.
 */
public interface ChatModelProvider {
    ChatLanguageModel buildModel(LlmConfig config);
    boolean supports(LlmProvider provider);
}
