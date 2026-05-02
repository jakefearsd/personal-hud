package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
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

    private final DynamicLlmService llmService;
    private final DailyBriefingRepository repository;
    private final BriefingProcessorFactory processorFactory;
    private final TransactionTemplate transactionTemplate;
    private final PipelineRunRepository pipelineRunRepository;

    public AutomatedBriefingService(DynamicLlmService llmService, 
                                    DailyBriefingRepository repository,
                                    BriefingProcessorFactory processorFactory,
                                    TransactionTemplate transactionTemplate,
                                    PipelineRunRepository pipelineRunRepository) {
        this.llmService = llmService;
        this.repository = repository;
        this.processorFactory = processorFactory;
        this.transactionTemplate = transactionTemplate;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void generateDailyBriefing() {
        LocalDate today = LocalDate.now();
        List<DynamicLlmService.NamedChatModel> activeModels = llmService.getActiveModels();
        
        if (activeModels.isEmpty()) {
            System.err.println("Aborting briefing: No active LLM configurations found.");
            return;
        }

        for (DynamicLlmService.NamedChatModel model : activeModels) {
            System.out.println("Starting daily briefing run for model: " + model.name());
            
            // Execute all categories
            for (BriefingCategory category : BriefingCategory.values()) {
                String query = getQueryForCategory(category);
                try { 
                    generateForCategory(today, category, query, model); 
                } catch (Exception e) { 
                    System.err.println("Failed generation for " + category + " [" + model.name() + "]: " + e.getMessage());
                }
            }
        }
    }

    public void generateForCategory(LocalDate date, BriefingCategory category, String query, DynamicLlmService.NamedChatModel model) {
        PipelineRun run = new PipelineRun(category, model.name(), PipelineStatus.PENDING, LocalDateTime.now());
        final PipelineRun savedRun = transactionTemplate.execute(status -> pipelineRunRepository.save(run));

        try {
            BriefingProcessor processor = processorFactory.getProcessor(category, model.model());
            String content = processor.process(query);
            
            transactionTemplate.execute(status -> {
                // Save the synthesized briefing
                repository.deleteByCategoryAndModelNameAndGeneratedAtAfter(category, model.name(), LocalDate.now().atStartOfDay());
                DailyBriefing briefing = new DailyBriefing(LocalDateTime.now(), category, model.name(), content);
                repository.save(briefing);

                // Update pipeline run status to SUCCESS
                PipelineRun current = pipelineRunRepository.findById(savedRun.getId()).orElseThrow();
                current.setStatus(PipelineStatus.SUCCESS);
                current.setEndTime(LocalDateTime.now());
                pipelineRunRepository.save(current);
                return null;
            });
        } catch (Exception e) {
            transactionTemplate.execute(status -> {
                PipelineRun current = pipelineRunRepository.findById(savedRun.getId()).orElseThrow();
                current.setStatus(PipelineStatus.FAILED);
                current.setEndTime(LocalDateTime.now());
                current.setErrorMessage(e.getMessage());
                pipelineRunRepository.save(current);
                return null;
            });
            throw e; // Rethrow for the main loop to log
        }
    }

    private String getQueryForCategory(BriefingCategory category) {
        return switch (category) {
            case WORLD_NEWS -> "https://feeds.bbci.co.uk/news/world/rss.xml";
            case US_NEWS -> "https://feeds.bbci.co.uk/news/world/us_and_canada/rss.xml";
            case FINANCE -> "https://feeds.bbci.co.uk/news/business/rss.xml";
            case TECHNOLOGY -> "hn";
            case THEATER_UKRAINE -> "ukraine";
            case THEATER_MIDDLE_EAST -> "mideast";
            case GLOBAL_SITREP -> "all";
        };
    }
}
