package com.hud.news;

import com.hud.briefing.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * High-resolution correlation engine that links market price action to tactical intelligence.
 */
@Service
public class EventCorrelationService {

    private final MacroMetricsService metricsService;
    private final DailyBriefingRepository briefingRepository;
    private final MarketEventRepository eventRepository;
    private final DynamicLlmService llmService;

    public EventCorrelationService(MacroMetricsService metricsService, 
                                   DailyBriefingRepository briefingRepository,
                                   MarketEventRepository eventRepository,
                                   DynamicLlmService llmService) {
        this.metricsService = metricsService;
        this.briefingRepository = briefingRepository;
        this.eventRepository = eventRepository;
        this.llmService = llmService;
    }

    @Scheduled(cron = "0 30 6 * * *") // Runs 30 mins after the daily briefing
    public void correlateEvents() {
        System.out.println("Initiating Market-Intelligence Correlation...");
        
        List<MacroMetric> metrics = metricsService.getLatestMetrics();
        List<DailyBriefing> briefings = briefingRepository.findLatestToday();
        
        if (briefings.isEmpty()) return;

        // Consolidate all briefings for the day as the intelligence context
        String combinedIntel = briefings.stream()
                .map(b -> "[" + b.getCategory() + "]: " + b.getMarkdownContent())
                .collect(Collectors.joining("\n\n"));

        var models = llmService.getActiveModels();
        if (models.isEmpty()) return;
        ChatLanguageModel model = models.get(0).model();

        for (MacroMetric metric : metrics) {
            // Check for significant movement (> 2%)
            if (Math.abs(metric.getChangePercent()) >= 2.0) {
                analyzeMove(metric, combinedIntel, model);
            }
        }
    }

    private void analyzeMove(MacroMetric metric, String intel, ChatLanguageModel model) {
        String prompt = String.format(
            "COMMAND DIRECTIVE: You are a Market Intelligence Analyst. " +
            "CONTEXT: Asset '%s' (%s) moved %.2f%% today. " +
            "TASK: Analyze the provided tactical field reports and identify the likely catalyst for this move. " +
            "RESTRICTION: If no plausible correlation exists, return 'NONE'. Otherwise, return a short title and 1-sentence rationale. " +
            "FORMAT: [Title] | [Rationale]\n\nDATA:\n%s\n\nCORRELATION:",
            metric.getLabel(), metric.getTicker(), metric.getChangePercent(), intel
        );

        String response = model.generate(prompt);
        if (response != null && !response.contains("NONE") && response.contains("|")) {
            String[] parts = response.split("\\|", 2);
            MarketEvent event = new MarketEvent(
                metric.getTicker(), 
                metric.getUpdatedAt(), 
                parts[0].trim(), 
                parts[1].trim()
            );
            eventRepository.save(event);
            System.out.println("Correlation Found for " + metric.getTicker() + ": " + parts[0].trim());
        }
    }
}
