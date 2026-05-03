package com.hud.analytics;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.nio.file.Paths;

@Disabled("Requires a stable running containerized application")
class UIInspectionTest {
    @Test
    void takeScreenshotOfFailure() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            
            System.out.println("Navigating to dashboard to inspect 'black screen'...");
            page.navigate("http://localhost:8889", new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            
            // Wait a few seconds for potential flashing to settle or reveal itself
            page.waitForTimeout(5000);
            
            System.out.println("Capturing screenshot to target/debug_ui.png");
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("target/debug_ui.png")));
            
            System.out.println("Console Logs captured during session:");
            page.onConsoleMessage(msg -> System.out.println("CONSOLE: [" + msg.type() + "] " + msg.text()));
            
            browser.close();
        }
    }
}
