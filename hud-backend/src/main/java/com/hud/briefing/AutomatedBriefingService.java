package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutomatedBriefingService {

    private final PlaywrightScraperService scraperService;
    private final ChatLanguageModel chatModel;
    private final DailyBriefingRepository repository;
    private final BriefingSourceFactory sourceFactory;
    private final TransactionTemplate transactionTemplate;
    private final PipelineRunRepository pipelineRunRepository;

    public AutomatedBriefingService(PlaywrightScraperService scraperService, 
                                    ChatLanguageModel chatModel, 
                                    DailyBriefingRepository repository,
                                    BriefingSourceFactory sourceFactory,
                                    TransactionTemplate transactionTemplate,
                                    PipelineRunRepository pipelineRunRepository) {
        this.scraperService = scraperService;
        this.chatModel = chatModel;
        this.repository = repository;
        this.sourceFactory = sourceFactory;
        this.transactionTemplate = transactionTemplate;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void generateDailyBriefing() {
        LocalDate today = LocalDate.now();
        
        // News Domain
        try { generateForCategory(today, BriefingCategory.WORLD_NEWS, "https://feeds.bbci.co.uk/news/world/rss.xml"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.US_NEWS, "https://feeds.bbci.co.uk/news/world/us_and_canada/rss.xml"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.FINANCE, "https://feeds.bbci.co.uk/news/business/rss.xml"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.TECHNOLOGY, "hn"); } catch (Exception e) { e.printStackTrace(); }
        
        // Intelligence Domain (Theaters)
        try { generateForCategory(today, BriefingCategory.THEATER_UKRAINE, "ukraine"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.THEATER_MIDDLE_EAST, "mideast"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.GLOBAL_SITREP, "all"); } catch (Exception e) { e.printStackTrace(); }
    }

    public void generateForCategory(LocalDate date, BriefingCategory category, String query) {
        PipelineRun run = new PipelineRun(category, PipelineStatus.PENDING, LocalDateTime.now());
        final PipelineRun savedRun = transactionTemplate.execute(status -> pipelineRunRepository.save(run));

        try {
            if (isTheaterCategory(category)) {
                generateDeepDiveIntelligence(date, category, query);
            } else {
                generateStandardBriefing(date, category, query);
            }
            
            transactionTemplate.execute(status -> {
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

    private void generateStandardBriefing(LocalDate date, BriefingCategory category, String query) {
        BriefingPersona persona = BriefingPersona.of(category);
        System.out.println("Generating [" + persona.name() + "] briefing for: " + category);
        
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

        String prompt = buildStandardPrompt(persona, category, combinedText.toString());
        String markdown = chatModel.generate(prompt);
        
        saveBriefing(date, category, markdown);
    }

    private void generateDeepDiveIntelligence(LocalDate date, BriefingCategory category, String query) {
        BriefingPersona persona = BriefingPersona.of(category);
        System.out.println("Executing Intelligence Fusion for: " + category + " [" + persona.name() + "]");
        
        BriefingSourceStrategy strategy = sourceFactory.getStrategy(category);
        List<String> links = strategy.getLinks(query, (category == BriefingCategory.GLOBAL_SITREP) ? 3 : 1);
        
        if (links.isEmpty()) {
            throw new RuntimeException("No deep-dive sources found for " + category);
        }

        StringBuilder combinedRaw = new StringBuilder();
        for (String url : links) {
            String text = scraperService.extractFullText(url);
            if (text != null && text.length() > 2000) {
                combinedRaw.append(text).append("\n\n");
            }
        }

        if (combinedRaw.length() < 2000) {
            throw new RuntimeException("Insufficient raw intelligence for deep-dive: " + category);
        }

        String intelligenceText = combinedRaw.length() > 40000 ? combinedRaw.substring(0, 40000) : combinedRaw.toString();

        String finalMarkdown;
        if (category == BriefingCategory.GLOBAL_SITREP) {
            finalMarkdown = chatModel.generate(buildSitrepPrompt(persona, intelligenceText));
        } else {
            // 3-Stage Fusion for active kinetic theaters
            String tempo = chatModel.generate("Summarize tactical momentum and significant territorial changes in 2 dense paragraphs. Focus on the 'Ground Truth'. Format with header '## Tactical Momentum'.\n\nDATA:\n" + intelligenceText);
            String strikes = chatModel.generate("Extract all impactful strikes. Include Target, Location, and Border Distance (if applicable). Format with header '## Kinetic Impact'.\n\nDATA:\n" + intelligenceText);
            String innovation = chatModel.generate("Identify battlefield innovation (new tactics, drone tech, EW). Format with header '## Innovation & Adaptation'.\n\nDATA:\n" + intelligenceText);
            
            finalMarkdown = String.format("# %s THEATER REPORT\n\n%s\n\n%s\n\n%s", 
                category.name().replace("THEATER_", ""), tempo, strikes, innovation);
        }

        saveBriefing(date, category, finalMarkdown);
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

    @Transactional
    public void saveBriefing(LocalDate date, BriefingCategory category, String content) {
        repository.deleteByBriefingDateAndCategory(date, category);
        DailyBriefing briefing = new DailyBriefing(date, category, content);
        repository.save(briefing);
    }

    private boolean isValidSituationalContent(String text, String url) {
        if (text == null || text.length() < 500) return false;
        String lower = text.toLowerCase();
        return !lower.contains("before you continue") && !lower.contains("accept all cookies") && !url.contains("/about");
    }
}
