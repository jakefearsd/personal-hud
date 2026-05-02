package com.hud.analytics;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

class IswStructuralAnalysisTest {
    @Test
    void analyzeIswStructure() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Browser.NewContextOptions options = new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            Page page = browser.newContext(options).newPage();
            
            System.out.println("--- ANALYZING ISW TAXONOMY ---");
            page.navigate("https://www.understandingwar.org/publications", new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            
            // Extract categories from the sidebar
            var categories = page.locator(".block-menu a, .views-exposed-form select option").all();
            System.out.println("Metadata elements found: " + categories.size());
            for (var c : categories) {
                try { System.out.println("Meta: " + c.innerText()); } catch (Exception e) {}
            }

            // Analyze the most recent 10 articles to see types
            System.out.println("\n--- RECENT PUBLICATIONS ANALYSIS ---");
            page.waitForSelector(".views-row", new Page.WaitForSelectorOptions().setTimeout(10000));
            var articles = page.locator(".views-row").all();
            for (int i = 0; i < Math.min(10, articles.size()); i++) {
                try {
                    String title = articles.get(i).locator("h2, .views-field-title").innerText();
                    String link = articles.get(i).locator("a").first().getAttribute("href");
                    System.out.println("Article " + i + ": " + title.trim() + " (" + link + ")");
                } catch (Exception e) {}
            }
            
            browser.close();
        }
    }
}
