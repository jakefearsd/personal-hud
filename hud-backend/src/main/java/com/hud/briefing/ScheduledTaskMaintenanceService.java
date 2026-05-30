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
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledTaskMaintenanceService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskMaintenanceService.class);

    private final AutomatedBriefingService briefingService;
    private final PredictionService predictionService;
    private final MacroMetricsService macroMetricsService;
    private final DailyBriefingRepository briefingRepository;
    private final MarketPredictionRepository predictionRepository;
    private final PipelineRunRepository pipelineRunRepository;

    public ScheduledTaskMaintenanceService(AutomatedBriefingService briefingService,
                                           PredictionService predictionService,
                                           MacroMetricsService macroMetricsService,
                                           DailyBriefingRepository briefingRepository,
                                           MarketPredictionRepository predictionRepository,
                                           PipelineRunRepository pipelineRunRepository) {
        this.briefingService = briefingService;
        this.predictionService = predictionService;
        this.macroMetricsService = macroMetricsService;
        this.briefingRepository = briefingRepository;
        this.predictionRepository = predictionRepository;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOrphanedRunsOnStartup() {
        logger.info("[MAINTENANCE] Cleaning up any orphaned pipeline runs from previous termination...");
        List<PipelineRun> pendingRuns = pipelineRunRepository.findByStatus(PipelineStatus.PENDING);
        for (PipelineRun run : pendingRuns) {
            run.setStatus(PipelineStatus.FAILED);
            run.setEndTime(LocalDateTime.now());
            run.setErrorMessage("Server restarted during execution");
            run.setErrorDetail("The application container was stopped or restarted while this pipeline run was processing.");
            pipelineRunRepository.save(run);
        }
        if (!pendingRuns.isEmpty()) {
            logger.info("[MAINTENANCE] Cleaned up {} orphaned runs.", pendingRuns.size());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1800000) // Also check every 30 minutes
    public void checkAndCatchUp() {
        logger.info("[MAINTENANCE] Verifying daily scheduled tasks...");

        // 1. Sync historical gaps
        macroMetricsService.syncHistoricalGaps();

        // 2. Check for missing today's briefings
        long briefingCount = briefingRepository.countByGeneratedAtAfter(LocalDate.now().atStartOfDay());
        if (briefingCount == 0) {
            // Only trigger if we are past the expected early morning window (e.g. 1 AM) to avoid pre-empting the actual schedule
            // But actually, catch-up is safest if it just ensures something exists.
            logger.info("[MAINTENANCE] No briefings found for today ({}). Triggering recovery run.", LocalDate.now());
            briefingService.generateDailyBriefing();
        } else {
            logger.info("[MAINTENANCE] Found {} briefings for today.", briefingCount);
            
            // 3. Check for missing predictions
            long predictionCount = predictionRepository.countByGenerationDate(LocalDate.now());
            if (predictionCount == 0) {
                logger.info("[MAINTENANCE] No market predictions found for today. Recovering...");
                predictionService.generateDailyPredictions();
            }
        }
    }
}
