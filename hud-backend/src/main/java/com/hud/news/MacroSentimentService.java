package com.hud.news;

import com.hud.briefing.DailyBriefing;
import com.hud.briefing.DailyBriefingRepository;
import com.hud.briefing.DynamicLlmService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MacroSentimentService {

    private final DynamicLlmService llmService;
    private final DailyBriefingRepository briefingRepository;

    public MacroSentimentService(DynamicLlmService llmService, DailyBriefingRepository briefingRepository) {
        this.llmService = llmService;
        this.briefingRepository = briefingRepository;
    }

    public String generatePodSentiment(String podTheme) {
        List<DynamicLlmService.NamedChatModel> activeModels = llmService.getActiveModels();
        if (activeModels.isEmpty()) {
            return "Sentiment analysis temporarily unavailable due to no active LLM provider.";
        }

        ChatLanguageModel chatModel = activeModels.get(0).model();

        List<DailyBriefing> recentBriefings = briefingRepository.findLatestGlobal();
        String newsContext = recentBriefings.stream()
                .map(DailyBriefing::getMarkdownContent)
                .collect(Collectors.joining("\n---\n"));

        String prompt = "You are an objective financial analyst. Analyze the following recent news and provide a brief (3-4 sentences), highly focused narrative on the current market sentiment specifically regarding: " + podTheme + ".\n\n" +
                "Do NOT provide portfolio tilt or investment advice. Focus purely on explaining the underlying narrative driving the data.\n\n" +
                "Recent News:\n" + newsContext;

        try {
            return chatModel.generate(prompt);
        } catch (Exception e) {
            return "Sentiment analysis temporarily unavailable due to LLM provider error.";
        }
    }
}
