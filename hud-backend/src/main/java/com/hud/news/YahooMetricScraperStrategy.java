package com.hud.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class YahooMetricScraperStrategy implements ScraperStrategy<MacroMetric> {
    private static final Logger logger = LoggerFactory.getLogger(YahooMetricScraperStrategy.class);
    private final String ticker;
    private final String label;
    private final ObjectMapper mapper = new ObjectMapper();

    public YahooMetricScraperStrategy(String ticker, String label) {
        this.ticker = ticker;
        this.label = label;
    }

    @Override
    public String getUrl() {
        String encodedTicker = URLEncoder.encode(ticker, StandardCharsets.UTF_8);
        return "https://query1.finance.yahoo.com/v8/finance/chart/" + encodedTicker + "?interval=1d&range=1d";
    }

    @Override
    public MacroMetric scrape(Page page) {
        // PREFERRED: Yahoo Chart JSON API
        try {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getUrl()))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode resultNode = root.path("chart").path("result").get(0);
                if (resultNode != null) {
                    JsonNode meta = resultNode.path("meta");
                    double price = meta.path("regularMarketPrice").asDouble();
                    double prevClose = meta.path("previousClose").asDouble();
                    
                    if (price != 0) {
                        double change = price - prevClose;
                        double pct = (prevClose != 0) ? (change / prevClose * 100) : 0.0;
                        logger.info("[MARKET] API Success for {}: {}", ticker, price);
                        return new MacroMetric(ticker, label, price, change, pct);
                    }
                }
            } else {
                logger.warn("[MARKET] API failed for {} with status {}", ticker, response.statusCode());
            }
        } catch (Exception e) {
            logger.warn("[MARKET] API exception for {}: {}", ticker, e.getMessage());
        }

        // FALLBACK: Playwright (Enhanced DOM Selectors)
        try {
            logger.info("[MARKET] Falling back to Playwright for {}...", ticker);
            page.navigate("https://finance.yahoo.com/quote/" + ticker);
            page.waitForTimeout(7000);
            
            // Priority list of selectors for the main price
            String[] selectors = {
                "[data-field='regularMarketPrice']", 
                "[data-testid='quote-price']",
                ".livePrice span",
                "fin-streamer[data-symbol='" + ticker + "']"
            };

            for (String selector : selectors) {
                var loc = page.locator(selector).first();
                if (loc.count() > 0 && !loc.innerText().isBlank()) {
                    String priceStr = loc.innerText().replaceAll("[^0-9.\\-]", "");
                    if (!priceStr.isEmpty()) {
                        double price = Double.parseDouble(priceStr);
                        logger.info("[MARKET] Playwright Success for {} using {}: {}", ticker, selector, price);
                        return new MacroMetric(ticker, label, price, 0.0, 0.0);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[MARKET] Total scraper failure for {}: {}", ticker, e.getMessage());
        }
        return null;
    }
}
