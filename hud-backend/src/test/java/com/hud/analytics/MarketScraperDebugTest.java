package com.hud.analytics;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.nio.file.Paths;

@Tag("integration")
class MarketScraperDebugTest {
    @Test
    void debugAllMetrics() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            
            String[] tickers = {"CL=F", "BZ=F", "GC=F", "DX-Y.NYB", "^VIX", "BTC-USD"};
            
            for (String ticker : tickers) {
                System.out.println("\n--- DEBUGGING TICKER: " + ticker + " ---");
                page.navigate("https://finance.yahoo.com/quote/" + ticker);
                
                // Handle consent
                try {
                    String[] sels = {"button:has-text('Accept all')", "button:has-text('Agree')", "button[name='agree']"};
                    for (String s : sels) {
                        var btn = page.locator(s).first();
                        if (btn.isVisible()) {
                            btn.click();
                            page.waitForLoadState();
                            break;
                        }
                    }
                } catch (Exception e) {}

                page.waitForTimeout(5000);
                
                Locator specific = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketPrice']").first();
                if (specific.isVisible()) {
                    System.out.println("SPECIFIC SELECTOR SUCCESS: " + specific.innerText());
                } else {
                    System.out.println("SPECIFIC SELECTOR FAILED for " + ticker);
                    // Check if it's there but not visible
                    System.out.println("COUNT: " + page.locator("fin-streamer[data-symbol='" + ticker + "']").count());
                }
            }
            browser.close();
        }
    }
}
