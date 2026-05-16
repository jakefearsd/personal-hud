package com.hud.analytics;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("Requires a running application on http://localhost:8889")
@Tag("integration")
class UiSmokeTest {
    @Test
    void verifyDashboardBasics() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            
            System.out.println("Starting Smoke Test on http://localhost:8889...");
            // Wait for app to be ready
            page.navigate("http://localhost:8889", new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            
            String title = page.title();
            System.out.println("Page Title: " + title);
            
            System.out.println("Checking for HUD branding...");
            var header = page.getByText("HUD").first();
            try {
                header.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            } catch (Exception e) {
                System.out.println("DEBUG: Header not found. Page content: " + page.content());
                throw e;
            }
            assertTrue(header.isVisible(), "HUD branding should be visible");
            
            assertTrue(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Theaters")).isVisible(), "Theaters tab should be visible");
            assertTrue(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("News")).isVisible(), "News tab should be visible");

            // Verify default view (Theaters)
            assertTrue(page.locator("button:has-text('Theaters').active").isVisible(), "Theaters should be active by default");
            
            // Test tab switching
            page.click("button:has-text('News')");
            assertTrue(page.locator("button:has-text('Strategic Briefing')").isVisible(), "News sub-tabs should appear");
            
            System.out.println("Smoke Test: SUCCESS");
            browser.close();
        } catch (Exception e) {
            System.err.println("Smoke Test FAILED: " + e.getMessage());
            throw e;
        }
    }
}
