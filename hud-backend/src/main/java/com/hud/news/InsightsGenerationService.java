package com.hud.news;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InsightsGenerationService {
    public WeeklyInsight generateInsight(List<String> articles) {
        return new WeeklyInsight(
            "Based on aggregated global events over the last 90 days, three primary catalysts emerged.",
            List.of("Interest Rate Sensitivity", "Commodity Headwinds"),
            LocalDateTime.now().minusWeeks(12),
            LocalDateTime.now()
        );
    }
}
