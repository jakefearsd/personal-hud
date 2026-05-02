package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutomatedBriefingService {

    private final PlaywrightScraperService scraperService;
    private final DynamicLlmService llmService;
    private final DailyBriefingRepository repository;
    private final BriefingSourceFactory sourceFactory;
    private final TransactionTemplate transactionTemplate;
    private final PipelineRunRepository pipelineRunRepository;

    public AutomatedBriefingService(PlaywrightScraperService scraperService, 
                                    DynamicLlmService llmService, 
                                    DailyBriefingRepository repository,
                                    BriefingSourceFactory sourceFactory,
                                    TransactionTemplate transactionTemplate,
                                    PipelineRunRepository pipelineRunRepository) {
        this.scraperService = scraperService;
        this.llmService = llmService;
        this.repository = repository;
        this.sourceFactory = sourceFactory;
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
            
            // News Domain
            try { generateForCategory(today, BriefingCategory.WORLD_NEWS, "https://feeds.bbci.co.uk/news/world/rss.xml", model); } catch (Exception e) { e.printStackTrace(); }
            try { generateForCategory(today, BriefingCategory.US_NEWS, "https://feeds.bbci.co.uk/news/world/us_and_canada/rss.xml", model); } catch (Exception e) { e.printStackTrace(); }
            try { generateForCategory(today, BriefingCategory.FINANCE, "https://feeds.bbci.co.uk/news/business/rss.xml", model); } catch (Exception e) { e.printStackTrace(); }
            try { generateForCategory(today, BriefingCategory.TECHNOLOGY, "hn", model); } catch (Exception e) { e.printStackTrace(); }
            
            // Intelligence Domain (Theaters)
            try { generateForCategory(today, BriefingCategory.THEATER_UKRAINE, "ukraine", model); } catch (Exception e) { e.printStackTrace(); }
            try { generateForCategory(today, BriefingCategory.THEATER_MIDDLE_EAST, "mideast", model); } catch (Exception e) { e.printStackTrace(); }
            try { generateForCategory(today, BriefingCategory.GLOBAL_SITREP, "all", model); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void generateForCategory(LocalDate date, BriefingCategory category, String query, DynamicLlmService.NamedChatModel model) {
        PipelineRun run = new PipelineRun(category, model.name(), PipelineStatus.PENDING, LocalDateTime.now());
        final PipelineRun savedRun = transactionTemplate.execute(status -> pipelineRunRepository.save(run));

        try {
            LocalDateTime now = LocalDateTime.now();
            String content;
            if (isTheaterCategory(category)) {
                content = generateDeepDiveContent(category, query, model.model());
            } else {
                content = generateStandardContent(category, query, model.model());
            }
            
            final String finalContent = content;
            transactionTemplate.execute(status -> {
                // 1. Save Briefing
                repository.deleteByCategoryAndModelNameAndGeneratedAtAfter(category, model.name(), LocalDate.now().atStartOfDay());
                DailyBriefing briefing = new DailyBriefing(now, category, model.name(), finalContent);
                repository.save(briefing);

                // 2. Update Run Status
                PipelineRun current = pipelineRunRepository.findById(savedRun.getId()).orElseThrow();
                current.setStatus(PipelineStatus.SUCCESS);
                current.setEndTime(LocalDateTime.now());
                pipelineRunRepository.save(current);
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
            transactionTemplate.execute(status -> {
                PipelineRun current = pipelineRunRepository.findById(savedRun.getId()).orElseThrow();
                current.setStatus(PipelineStatus.FAILED);
                current.setEndTime(LocalDateTime.now());
                current.setErrorMessage(e.getMessage());
                pipelineRunRepository.save(current);
                return null;
            });
        }
    }

    private String generateStandardContent(BriefingCategory category, String query, ChatLanguageModel model) {
        BriefingPersona persona = BriefingPersona.of(category);
        BriefingSourceStrategy strategy = sourceFactory.getStrategy(category);
        List<String> links = strategy.getLinks(query, 3);
        
        StringBuilder combinedText = new StringBuilder();
        for (String url : links) {
            String text = scraperService.extractFullText(url);
            if (isValidSituationalContent(text, url)) {
                combinedText.append("\n--- START SOURCE ---\n").append(text).append("\n--- END SOURCE ---\n");
            }
        }

        if (combinedText.length() < 800) {
            throw new RuntimeException("Insufficient signal for " + category);
        }

        return model.generate(buildStandardPrompt(persona, category, combinedText.toString()));
    }

    private String generateDeepDiveContent(BriefingCategory category, String query, ChatLanguageModel model) {
        BriefingPersona persona = BriefingPersona.of(category);
        BriefingSourceStrategy strategy = sourceFactory.getStrategy(category);
        List<String> links = strategy.getLinks(query, (category == BriefingCategory.GLOBAL_SITREP) ? 3 : 1);
        
        if (links.isEmpty()) {
            throw new RuntimeException("No deep-dive sources found for " + category);
        }

        StringBuilder combinedRaw = new StringBuilder();
        for (String url : links) {
            String text = scraperService.extractFullText(url);
            if (text != null && text.length() > 1500) {
                combinedRaw.append(text).append("\n\n");
            }
        }

        if (combinedRaw.length() < 1500) {
            throw new RuntimeException("Insufficient raw intelligence for deep-dive: " + category);
        }

        String intelligenceText = combinedRaw.length() > 15000 ? combinedRaw.substring(0, 15000) : combinedRaw.toString();

        if (category == BriefingCategory.GLOBAL_SITREP) {
            return model.generate(buildSitrepPrompt(persona, intelligenceText));
        } else {
            String tempo = model.generate(
                "COMMAND DIRECTIVE: You are a Lead Intelligence Officer. " +
                "INPUT: Raw field reports with citations. " +
                "TASK: Re-write the situational analysis into 2 dense narrative paragraphs. " +
                "RESTRICTION: DO NOT mention citations, links, or report nature. Focus on ground truth.\n\nDATA:\n" + intelligenceText);
            
            String strikes = model.generate(
                "TASK: Extract kinetic strike data. Include Target, Location, and Border Distance. " +
                "OUTPUT: Markdown Table. Header: '## Kinetic Impact'.\n\nDATA:\n" + intelligenceText);
                
            String innovation = model.generate(
                "TASK: Identify 3 battlefield innovations (Tactics, EW, Drones). " +
                "OUTPUT: Bullet points. Header: '## Innovation & Adaptation'.\n\nDATA:\n" + intelligenceText);
            
            return String.format("# %s THEATER REPORT\n\n%s\n\n%s\n\n%s", 
                category.name().replace("THEATER_", ""), tempo, strikes, innovation);
        }
    }

    private String buildStandardPrompt(BriefingPersona persona, BriefingCategory category, String data) {
        return String.format(
            "You are the %s. %s\nSTRICT RULES: NO META-COMMENTARY. FOCUS on %s. Use Markdown. 2-5 dense paragraphs.\n\nINTELLIGENCE DATA:\n%s\n\nTACTICAL BRIEFING:",
            persona.name(), persona.instruction(), persona.focus(), data
        );
    }

    private String buildSitrepPrompt(BriefingPersona persona, String data) {
        return String.format(
            "You are the %s. %s\nProvide a high-level SITREP of the global conflict landscape. " +
            "Compare momentum across theaters. 3-5 paragraphs. Markdown.\n\nDATA:\n%s\n\nGLOBAL SITREP:",
            persona.name(), persona.instruction(), data
        );
    }

    private boolean isTheaterCategory(BriefingCategory c) {
        return c == BriefingCategory.THEATER_UKRAINE || c == BriefingCategory.THEATER_MIDDLE_EAST || c == BriefingCategory.GLOBAL_SITREP;
    }

    private boolean isValidSituationalContent(String text, String url) {
        if (text == null || text.length() < 500) return false;
        String lower = text.toLowerCase();
        return !lower.contains("before you continue") && !lower.contains("accept all cookies") && !url.contains("/about");
    }
}
