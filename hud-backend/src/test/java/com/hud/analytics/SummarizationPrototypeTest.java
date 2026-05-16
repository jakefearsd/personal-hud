package com.hud.analytics;

import com.hud.briefing.DynamicLlmService;
import com.hud.news.NewsArticle;
import com.hud.news.PlaywrightScraperService;
import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.microsoft.playwright.*;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class SummarizationPrototypeTest {

    @Autowired
    private DynamicLlmService dynamicLlmService;

    @Autowired
    private PlaywrightScraperService scraperService;

    @Test
    void prototypeLargeScaleSummarization() throws Exception {
        System.out.println("=== STARTING SUMMARIZATION PROTOTYPE ===");
        
        var models = dynamicLlmService.getActiveModels();
        if (models.isEmpty()) return;
        var gemmaModel = models.get(0).model();
        
        // 1. Get articles from the scraper
        List<NewsArticle> articles = scraperService.scrapeYahooFinance();
        if (articles.isEmpty()) {
            System.out.println("No articles found to summarize.");
            return;
        }

        // Try to find a long article
        NewsArticle target = articles.get(0);
        
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            
            System.out.println("Navigating to: " + target.url());
            page.navigate(target.url(), new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));
            String html = page.content();
            
            Readability4J readability = new Readability4J(target.url(), html);
            Article parsedArticle = readability.parse();
            String cleanText = parsedArticle.getTextContent();
            
            if (cleanText == null || cleanText.trim().isEmpty()) {
                System.out.println("Readability4J failed to extract content.");
                browser.close();
                return;
            }
            
            System.out.println("Clean text length: " + cleanText.length() + " characters.");
            
            // 2. Perform Summarization
            String prompt = String.format(
                "You are an expert financial analyst. Provide a structured executive summary of the following text.\n" +
                "Requirements:\n" +
                "1. A concise overview (1-2 sentences).\n" +
                "2. 3-5 bullet points for Key Takeaways.\n" +
                "3. Overall sentiment (Positive/Negative/Neutral).\n\n" +
                "Text:\n%s\n\nEXECUTIVE SUMMARY:",
                cleanText
            );
            
            System.out.println("Sending summarization request to Gemma4 (this may take a moment)...");
            long startTime = System.currentTimeMillis();
            String summary = gemmaModel.generate(prompt);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("\n--- GENERATED SUMMARY (" + (duration/1000.0) + "s) ---");
            System.out.println(summary);
            System.out.println("------------------------------------------");
            
            browser.close();
        }
        
        System.out.println("=== PROTOTYPE COMPLETE ===");
    }
}
