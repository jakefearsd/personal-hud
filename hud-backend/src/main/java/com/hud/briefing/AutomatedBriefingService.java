package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates the daily automated briefing runs across multiple models and categories.
 */
@Service
public class AutomatedBriefingService {

    private static final Logger logger = LoggerFactory.getLogger(AutomatedBriefingService.class);

    private final DynamicLlmService llmService;
    private final DailyBriefingRepository repository;
    private final BriefingProcessorFactory processorFactory;
    private final TransactionTemplate transactionTemplate;
    private final PipelineRunRepository pipelineRunRepository;
    private final MarkdownService markdownService;
    private final com.hud.news.PredictionService predictionService;

    public AutomatedBriefingService(DynamicLlmService llmService, 
                                    DailyBriefingRepository repository,
                                    BriefingProcessorFactory processorFactory,
                                    TransactionTemplate transactionTemplate,
                                    PipelineRunRepository pipelineRunRepository,
                                    MarkdownService markdownService,
                                    com.hud.news.PredictionService predictionService) {
        this.llmService = llmService;
        this.repository = repository;
        this.processorFactory = processorFactory;
        this.transactionTemplate = transactionTemplate;
        this.pipelineRunRepository = pipelineRunRepository;
        this.markdownService = markdownService;
        this.predictionService = predictionService;
    }

    @Async("briefingExecutor")
    public void generateForModel(Long configId) {
        LlmConfig config = llmService.getConfigRepository().findById(configId)
                .orElseThrow(() -> new RuntimeException("Model config not found: " + configId));
        
        if (!config.isActive()) {
            logger.warn("Refusing to run inactive model: {}", config.getName());
            return;
        }

        DynamicLlmService.NamedChatModel model = llmService.buildSpecificModel(config);
        LocalDate today = LocalDate.now();
        
        logger.info("Starting targeted briefing run for model: {}", model.name());
        executeFullPipeline(today, model);
    }

    @Async("briefingExecutor")
    @Scheduled(cron = "0 0 6 * * *")
    public void generateDailyBriefing() {
        LocalDate today = LocalDate.now();
        List<DynamicLlmService.NamedChatModel> activeModels = llmService.getActiveModels();
        
        if (activeModels.isEmpty()) {
            logger.error("Aborting briefing: No active LLM configurations found.");
            return;
        }

        for (DynamicLlmService.NamedChatModel model : activeModels) {
            logger.info("Starting daily briefing run for model: {}", model.name());
            executeFullPipeline(today, model);
        }

        logger.info("Daily briefings complete. Triggering predictive analytics...");
        predictionService.generateDailyPredictions();
    }

    @Async("briefingExecutor")
    public void generateForCategory(BriefingCategory category) {
        List<DynamicLlmService.NamedChatModel> activeModels = llmService.getActiveModels();
        LocalDate today = LocalDate.now();

        for (DynamicLlmService.NamedChatModel model : activeModels) {
            try {
                generateForCategory(today, category, model);
            } catch (Exception e) {
                logger.error("Async category run failed for {} [{}]: {}", category, model.name(), e.getMessage(), e);
            }
        }
    }

    private void executeFullPipeline(LocalDate today, DynamicLlmService.NamedChatModel model) {
        // News Domain
        for (BriefingCategory category : BriefingCategory.values()) {
            try {
                generateForCategory(today, category, model);
            } catch (Exception e) {
                logger.error("Failed generation for {} [{}]: {}", category, model.name(), e.getMessage(), e);
            }
        }
    }

    public void generateForCategory(LocalDate date, BriefingCategory category, DynamicLlmService.NamedChatModel model) {
        // IDEMPOTENCY CHECK: Do not start if a run is already pending for this category
        boolean alreadyRunning = transactionTemplate.execute(status -> 
                pipelineRunRepository.existsByCategoryAndStatus(category, PipelineStatus.PENDING));
        
        if (Boolean.TRUE.equals(alreadyRunning)) {
            logger.warn("[IDEMPOTENCY] Skipping run for {} because a PENDING run already exists.", category);
            return;
        }

        PipelineRun run = new PipelineRun(category, model.name(), PipelineStatus.PENDING, LocalDateTime.now());
        final PipelineRun savedRun = transactionTemplate.execute(status -> pipelineRunRepository.save(run));

        try {
            BriefingProcessor processor = processorFactory.getProcessor(category, model.model());
            SynthesisResult result = processor.process();
            
            transactionTemplate.execute(status -> {
                // Save the synthesized briefing
                repository.deleteByCategoryAndModelNameAndGeneratedAtAfter(category, model.name(), LocalDate.now().atStartOfDay());
                DailyBriefing briefing = new DailyBriefing(LocalDateTime.now(), category, model.name(), result.content());
                briefing.setHtmlContent(markdownService.renderToHtml(result.content()));
                repository.save(briefing);

                // Update pipeline run status to SUCCESS and save token metrics
                PipelineRun current = pipelineRunRepository.findById(savedRun.getId()).orElseThrow();
                current.setStatus(PipelineStatus.SUCCESS);
                current.setEndTime(LocalDateTime.now());
                current.setInputTokens(result.inputTokens());
                current.setOutputTokens(result.outputTokens());
                pipelineRunRepository.save(current);
                return null;
            });
        } catch (Exception e) {
            transactionTemplate.execute(status -> {
                PipelineRun current = pipelineRunRepository.findById(savedRun.getId()).orElseThrow();
                current.setStatus(PipelineStatus.FAILED);
                current.setEndTime(LocalDateTime.now());
                current.setErrorMessage(e.getMessage());

                // Capture cause chain for better diagnostics
                StringBuilder detail = new StringBuilder();
                Throwable cause = e;
                while (cause != null) {
                    detail.append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append("\n");
                    cause = cause.getCause();
                }
                current.setErrorDetail(detail.toString());

                pipelineRunRepository.save(current);
                return null;
            });
            throw e; // Rethrow for the main loop to log
        }
    }
}
