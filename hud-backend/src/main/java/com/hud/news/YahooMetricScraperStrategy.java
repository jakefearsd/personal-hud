package com.hud.news;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

public class YahooMetricScraperStrategy implements ScraperStrategy<MacroMetric> {
    private final String ticker;
    private final String label;

    public YahooMetricScraperStrategy(String ticker, String label) {
        this.ticker = ticker;
        this.label = label;
    }

    @Override
    public String getUrl() {
        return "https://finance.yahoo.com/quote/" + ticker;
    }

    @Override
    public MacroMetric scrape(Page page) {
        try {
            page.navigate(getUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(30000));
            
            // High-resolution stabilization wait
            page.waitForTimeout(8000);
            
            // Strictly symbol-locked selectors to prevent sidebar leakage
            Locator priceLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketPrice']").first();
            Locator changeLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketChange']").first();
            Locator pctLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketChangePercent']").first();

            if (priceLocator.count() == 0) {
                throw new RuntimeException("Ticker " + ticker + " not found on page.");
            }

            String priceStr = priceLocator.innerText().trim();
            String changeStr = changeLocator.innerText().trim();
            String pctStr = pctLocator.innerText().trim();
            
            double price = Double.parseDouble(priceStr.replace(",", ""));
            double change = Double.parseDouble(changeStr.replace(",", ""));
            double pct = Double.parseDouble(pctStr.replace("(", "").replace(")", "").replace("%", ""));
            
            return new MacroMetric(ticker, label, price, change, pct);
        } catch (Exception e) {
            System.err.println("[MARKET] Failed to scrape " + ticker + ": " + e.getMessage());
            return null;
        }
    }
}
