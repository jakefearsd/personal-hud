package com.hud.news;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YahooMetricScraperStrategy implements ScraperStrategy<MacroMetric> {
    private static final Logger logger = LoggerFactory.getLogger(YahooMetricScraperStrategy.class);
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
            page.waitForTimeout(5000);
            
            // Primary strategy: Strictly symbol-locked selectors
            Locator priceLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketPrice']").first();
            Locator changeLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketChange']").first();
            Locator pctLocator = page.locator("fin-streamer[data-symbol='" + ticker + "'][data-field='regularMarketChangePercent']").first();

            // Fallback strategy: Broad selectors for major indices/commodities
            if (priceLocator.count() == 0) {
                logger.debug("[MARKET] Symbol-locked locator failed for {}, trying fallback...", ticker);
                priceLocator = page.locator("fin-streamer[data-field='regularMarketPrice']").first();
                changeLocator = page.locator("fin-streamer[data-field='regularMarketChange']").first();
                pctLocator = page.locator("fin-streamer[data-field='regularMarketChangePercent']").first();
            }

            if (priceLocator.count() == 0) {
                throw new RuntimeException("Ticker data not found on page using primary or fallback selectors.");
            }

            String priceStr = priceLocator.innerText().trim();
            String changeStr = changeLocator.innerText().trim();
            String pctStr = pctLocator.innerText().trim();
            
            double price = parseValue(priceStr);
            double change = parseValue(changeStr);
            double pct = parseValue(pctStr.replace("(", "").replace(")", "").replace("%", ""));
            
            return new MacroMetric(ticker, label, price, change, pct);
        } catch (Exception e) {
            logger.error("[MARKET] Failed to scrape {} from {}: {}", ticker, getUrl(), e.getMessage(), e);
            return null;
        }
    }

    private double parseValue(String val) {
        if (val == null || val.isBlank() || val.equals("--")) return 0.0;
        return Double.parseDouble(val.replace(",", "").replace("+", ""));
    }
}
