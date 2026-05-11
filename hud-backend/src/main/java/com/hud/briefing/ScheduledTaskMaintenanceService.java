package com.hud.briefing;

import com.hud.news.MacroMetricsService;
import com.hud.news.MarketPredictionRepository;
import com.hud.news.PredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ScheduledTaskMaintenanceService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskMaintenanceService.class);

    private final AutomatedBriefingService briefingService;
    private final PredictionService predictionService;
    private final MacroMetricsService macroMetricsService;
    private final DailyBriefingRepository briefingRepository;
    private final MarketPredictionRepository predictionRepository;

    public ScheduledTaskMaintenanceService(AutomatedBriefingService briefingService,
                                           PredictionService predictionService,
                                           MacroMetricsService macroMetricsService,
                                           DailyBriefingRepository briefingRepository,
                                           MarketPredictionRepository predictionRepository) {
        this.briefingService = briefingService;
        this.predictionService = predictionService;
        this.macroMetricsService = macroMetricsService;
        this.briefingRepository = briefingRepository;
        this.predictionRepository = predictionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkAndCatchUp() {
        logger.info("Verifying daily scheduled tasks...");

        // 1. Sync historical gaps always to ensure data integrity
        logger.info("Initiating background historical data sync...");
        macroMetricsService.syncHistoricalGaps();

        // 2. Check for missing today's briefings
        long briefingCount = briefingRepository.countByGeneratedAtAfter(LocalDate.now().atStartOfDay());
        if (briefingCount == 0) {
            logger.info("No briefings found for today. Catching up...");
            briefingService.generateDailyBriefing();
            // Note: generateDailyBriefing() will trigger generateDailyPredictions() once it finishes
        } else {
            logger.info("Found {} briefings for today.", briefingCount);
            
            // 3. If briefings exist, check if predictions are still missing
            long predictionCount = predictionRepository.countByGenerationDate(LocalDate.now());
            if (predictionCount == 0) {
                logger.info("No market predictions found for today. Catching up...");
                predictionService.generateDailyPredictions();
            } else {
                logger.info("Found {} market predictions for today.", predictionCount);
            }
        }
    }
}
