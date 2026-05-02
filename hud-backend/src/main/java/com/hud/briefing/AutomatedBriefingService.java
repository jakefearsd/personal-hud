package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AutomatedBriefingService {

    private final PlaywrightScraperService scraperService;
    private final ChatLanguageModel chatModel;
    private final DailyBriefingRepository repository;
    private final BriefingSourceFactory sourceFactory;

    public AutomatedBriefingService(PlaywrightScraperService scraperService, 
                                    ChatLanguageModel chatModel, 
                                    DailyBriefingRepository repository,
                                    BriefingSourceFactory sourceFactory) {
        this.scraperService = scraperService;
        this.chatModel = chatModel;
        this.repository = repository;
        this.sourceFactory = sourceFactory;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void generateDailyBriefing() {
        LocalDate today = LocalDate.now();
        
        // Diversified Sources
        try { generateForCategory(today, BriefingCategory.WORLD_NEWS, "https://feeds.bbci.co.uk/news/world/rss.xml"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.US_NEWS, "https://feeds.bbci.co.uk/news/world/us_and_canada/rss.xml"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.FINANCE, "https://feeds.bbci.co.uk/news/business/rss.xml"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.TECHNOLOGY, "hn"); } catch (Exception e) { e.printStackTrace(); }
        try { generateForCategory(today, BriefingCategory.WORLD_CONFLICTS, "isw"); } catch (Exception e) { e.printStackTrace(); }
    }

    @Transactional
    public void generateForCategory(LocalDate date, BriefingCategory category, String query) {
        BriefingPersona persona = BriefingPersona.of(category);
        System.out.println("Generating [" + persona.name() + "] briefing for: " + category);
        
        BriefingSourceStrategy strategy = sourceFactory.getStrategy(category);
        List<String> links = strategy.getLinks(query, 3);
        
        System.out.println("Found " + links.size() + " links for " + category);

        StringBuilder combinedText = new StringBuilder();
        for (String url : links) {
            String text = scraperService.extractFullText(url);
            if (isValidSituationalContent(text, url)) {
                combinedText.append("\n--- START SOURCE ---\n").append(text).append("\n--- END SOURCE ---\n");
            }
        }

        if (combinedText.length() < 800) {
            System.err.println("Insufficient signal for " + category);
            return;
        }

        String prompt = String.format(
            "You are the %s (%s). %s\n" +
            "STRICT RULES:\n" +
            "1. NO META-COMMENTARY about the reports, sources, or yourself. Jump straight into the report.\n" +
            "2. FOCUS on %s.\n" +
            "3. Use Markdown. Headers for themes. BOLD for locations/metrics.\n" +
            "4. Provide 2-5 dense paragraphs of ground-truth analysis.\n\n" +
            "INTELLIGENCE DATA:\n%s\n\n" +
            "TACTICAL BRIEFING (Markdown):",
            persona.name(), persona.role(), persona.instruction(),
            persona.focus(),
            combinedText
        );

        System.out.println("Synthesis starting for " + category + " using " + persona.name() + " persona...");
        String markdown = chatModel.generate(prompt);
        
        repository.deleteByBriefingDateAndCategory(date, category);
        DailyBriefing briefing = new DailyBriefing(date, category, markdown);
        repository.save(briefing);
    }

    private boolean isValidSituationalContent(String text, String url) {
        if (text == null || text.length() < 500) return false;
        String lower = text.toLowerCase();
        return !lower.contains("before you continue") && !lower.contains("accept all cookies") && !url.contains("/about");
    }
}
