package com.hud.news;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WeeklyInsightsPipeline {
    private final NewsDiscoveryService discovery;
    private final InsightsGenerationService generation;
    private final WeeklyInsightRepository repository;

    public WeeklyInsightsPipeline(NewsDiscoveryService discovery, InsightsGenerationService generation, WeeklyInsightRepository repository) {
        this.discovery = discovery;
        this.generation = generation;
        this.repository = repository;
    }
    
    @Scheduled(cron = "0 0 0 * * SAT")
    public void runPipeline() {
        List<String> urls = discovery.discoverRecentEvents();
        WeeklyInsight insight = generation.generateInsight(urls);
        repository.save(insight);
    }
}
