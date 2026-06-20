package com.hud.news;

import com.hud.briefing.BriefingCategory;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ScraperStrategyEfficacyTest {

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void iswUkraineScraperEfficacy() throws IOException {
        String html = Files.readString(Paths.get("src/test/resources/html/isw_library.html"));
        page.setContent(html);

        // We use a subclass to avoid the hardcoded navigate call for testing
        IswScraperStrategy strategy = new IswScraperStrategy(10, BriefingCategory.THEATER_UKRAINE) {
            @Override
            protected void navigate(Page page) {
                // Skip navigation and just run the extraction logic
            }
        };

        List<String> links = strategy.scrape(page);

        assertEquals(2, links.size());
        assertTrue(links.contains("https://understandingwar.org/research/russia-ukraine-offensive-1"));
        assertTrue(links.contains("https://understandingwar.org/research/ukraine-update-special"));
        assertFalse(links.contains("https://understandingwar.org/research/iran-update-1"));
    }

    @Test
    void iswMiddleEastScraperEfficacy() throws IOException {
        String html = Files.readString(Paths.get("src/test/resources/html/isw_library.html"));
        page.setContent(html);

        IswScraperStrategy strategy = new IswScraperStrategy(10, BriefingCategory.THEATER_MIDDLE_EAST) {
            @Override
            protected void navigate(Page page) {
                // Skip navigation
            }
        };

        List<String> links = strategy.scrape(page);

        assertEquals(1, links.size());
        assertTrue(links.contains("https://understandingwar.org/research/iran-update-1"));
    }
}
