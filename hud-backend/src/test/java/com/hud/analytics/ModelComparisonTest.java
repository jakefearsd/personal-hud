package com.hud.analytics;

import ai.djl.ModelException;
import ai.djl.huggingface.translator.ZeroShotClassificationTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.nlp.translator.ZeroShotClassificationInput;
import ai.djl.modality.nlp.translator.ZeroShotClassificationOutput;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.hud.news.NewsArticle;
import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.microsoft.playwright.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class ModelComparisonTest {

    @Autowired
    private ChatLanguageModel gemmaModel;

    @Autowired
    private PlaywrightScraperService scraperService;

    @Test
    void compareGemmaAndDjlForAdDetection() throws Exception {
        System.out.println("=== STARTING MODEL COMPARISON ===");
        
        // 1. Get articles
        List<NewsArticle> articles = scraperService.scrapeYahooFinance();
        if (articles.isEmpty()) {
            System.out.println("No articles found to compare.");
            return;
        }

        // Limit to 3 for speed
        List<NewsArticle> subset = articles.stream().limit(3).toList();
        
        // 2. Setup DJL Zero-Shot Model
        Criteria<ZeroShotClassificationInput, ZeroShotClassificationOutput> criteria = Criteria.builder()
                .setTypes(ZeroShotClassificationInput.class, ZeroShotClassificationOutput.class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/facebook/bart-large-mnli")
                .optEngine("PyTorch")
                .optTranslatorFactory(new ZeroShotClassificationTranslatorFactory())
                .build();

        try (ZooModel<ZeroShotClassificationInput, ZeroShotClassificationOutput> djlModel = criteria.loadModel();
             Predictor<ZeroShotClassificationInput, ZeroShotClassificationOutput> djlPredictor = djlModel.newPredictor();
             Playwright playwright = Playwright.create()) {
            
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            for (NewsArticle art : subset) {
                System.out.println("\n--- ANALYZING ARTICLE: " + art.title() + " ---");
                System.out.println("URL: " + art.url());

                // Fetch full content
                try {
                    page.navigate(art.url(), new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));
                    String html = page.content();
                    
                    // Extract core content with Readability4J
                    Readability4J readability = new Readability4J(art.url(), html);
                    Article parsedArticle = readability.parse();
                    String cleanText = parsedArticle.getTextContent();
                    
                    if (cleanText == null || cleanText.trim().isEmpty()) {
                        System.out.println("Readability4J failed to extract content.");
                        continue;
                    }
                    
                    System.out.println("Extracted text length: " + cleanText.length());

                    // A. GEMMA CLASSIFICATION
                    String gemmaPrompt = String.format(
                        "Classify the following text into exactly one of these categories: [NEWS, ADVERTISEMENT]. " +
                        "An ADVERTISEMENT is a promotional piece, a 'sponsored' article, or a piece designed primarily to sell a specific financial product rather than report news. " +
                        "Text: %s\n\nCATEGORY:", 
                        cleanText.length() > 2000 ? cleanText.substring(0, 2000) : cleanText
                    );
                    String gemmaResult = gemmaModel.generate(gemmaPrompt).trim();
                    System.out.println("Gemma4: " + gemmaResult);

                    // B. DJL ZERO-SHOT CLASSIFICATION
                    List<String> labels = Arrays.asList("genuine financial news", "advertising promotional material");
                    // BERT models have a small context window (512 tokens), so we truncate heavily for DJL
                    String truncatedText = cleanText.length() > 1000 ? cleanText.substring(0, 1000) : cleanText;
                    ZeroShotClassificationInput input = new ZeroShotClassificationInput(truncatedText, labels.toArray(new String[0]));
                    ZeroShotClassificationOutput djlResult = djlPredictor.predict(input);
                    
                    // Find best result
                    double maxProb = -1;
                    String bestLabel = "unknown";
                    
                    // Using scores and labels from output
                    String[] resultLabels = djlResult.getLabels();
                    double[] resultScores = djlResult.getScores();
                    
                    for (int i = 0; i < resultLabels.length; i++) {
                        if (resultScores[i] > maxProb) {
                            maxProb = resultScores[i];
                            bestLabel = resultLabels[i];
                        }
                    }
                    System.out.println("DJL (BART): " + bestLabel + " (" + maxProb + ")");

                } catch (Exception e) {
                    System.err.println("Error processing " + art.url() + ": " + e.getMessage());
                }
            }
            browser.close();
        }
        
        System.out.println("\n=== COMPARISON COMPLETE ===");
    }
}
