package com.hud.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class MacroMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MacroMetricsService.class);
    private static final int RECENT_GAP_THRESHOLD_DAYS = 7;
    private static final long SECONDS_IN_DAY = 86400;

    private final PlaywrightScraperService scraperService;
    private final MacroMetricRepository repository;
    private final MetricHistoryRepository historyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public MacroMetricsService(PlaywrightScraperService scraperService, 
                               MacroMetricRepository repository,
                               MetricHistoryRepository historyRepository) {
        this.scraperService = scraperService;
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    }

    // Constructor for testing
    public MacroMetricsService(PlaywrightScraperService scraperService, 
                               MacroMetricRepository repository,
                               MetricHistoryRepository historyRepository,
                               HttpClient httpClient) {
        this.scraperService = scraperService;
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.httpClient = httpClient;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void updateMacroMetrics() {
        logger.info("Updating Macro Vitals Dashboard...");

        Map<String, String> tickers = Map.ofEntries(
            Map.entry("^GSPC", "S&P 500"),
            Map.entry("^DJI", "Dow 30"),
            Map.entry("^VIX", "VIX (Volatility)"),
            Map.entry("^GDAXI", "DAX (Germany)"),
            Map.entry("^FTSE", "FTSE 100 (UK)"),
            Map.entry("^STOXX50E", "Euro Stoxx 50"),
            Map.entry("^N225", "Nikkei 225 (Japan)"),
            Map.entry("^HSI", "Hang Seng (HK)"),
            Map.entry("000001.SS", "Shanghai Comp (China)"),
            Map.entry("AGG", "US Aggregate Bond (AGG)"),
            Map.entry("BNDX", "Intl Aggregate Bond (BNDX)"),
            Map.entry("VXUS", "Vanguard Total Intl Stock (VXUS)"),
            Map.entry("EFV", "iShares MSCI EAFE Value (EFV)"),
            Map.entry("^TNX", "US 10Y Treasury Yield"),
            Map.entry("BZ=F", "Brent Crude Oil"),
            Map.entry("CL=F", "WTI Crude Oil"),
            Map.entry("GC=F", "Gold"),
            Map.entry("BTC-USD", "Bitcoin"),
            Map.entry("ETH-USD", "Ethereum")
        );

        for (Map.Entry<String, String> entry : tickers.entrySet()) {
            try {
                MacroMetric metric = scraperService.scrapeYahooMetric(entry.getKey(), entry.getValue());
                if (metric != null) {
                    // Update existing or save new
                    MacroMetric existing = repository.findById(entry.getKey())
                            .orElse(new MacroMetric(entry.getKey(), entry.getValue(), 0.0, 0.0, 0.0));
                    
                    existing.setPrice(metric.getPrice());
                    existing.setChange(metric.getChange());
                    existing.setChangePercent(metric.getChangePercent());
                    existing.setUpdatedAt(LocalDateTime.now());
                    repository.save(existing);

                    historyRepository.save(new MetricHistory(metric.getTicker(), metric.getPrice(), metric.getChangePercent()));
                }
            } catch (Exception e) {
                logger.error("Failed to update macro metric for {}: {}", entry.getKey(), e.getMessage(), e);
            }
        }

        // Special handling for Yield Spread
        try {
            Double spread = scraperService.scrapeFredYieldSpread();
            if (spread != null) {
                MacroMetric metric = new MacroMetric("T10Y2Y", "10Y-2Y Yield Spread", spread, 0.0, 0.0);
                repository.save(metric);
                historyRepository.save(new MetricHistory("T10Y2Y", spread, 0.0));
            }
        } catch (Exception e) {
            logger.error("Failed to update FRED yield spread: {}", e.getMessage(), e);
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void syncHistoricalGaps() {
        List<String> symbols = List.of(
            "^GSPC", "^DJI", "^VIX", "^GDAXI", "^FTSE", "^STOXX50E", 
            "^N225", "^HSI", "000001.SS", "AGG", "BNDX", "VXUS", "EFV", "^TNX", 
            "BZ=F", "CL=F", "GC=F", "BTC-USD", "ETH-USD"
        );
        long now = Instant.now().getEpochSecond();
        
        for (String symbol : symbols) {
            try {
                LocalDateTime lastPoint = historyRepository.findTopByTickerOrderByTimestampDesc(symbol)
                        .map(MetricHistory::getTimestamp)
                        .orElse(LocalDateTime.now().minusYears(30));
                
                long start = lastPoint.atZone(ZoneId.systemDefault()).toEpochSecond() + SECONDS_IN_DAY; // Start day after
                if (start >= now - SECONDS_IN_DAY * RECENT_GAP_THRESHOLD_DAYS) continue; // Less than a week of gap

                logger.info("Syncing historical gaps for {} from {} to now", symbol, lastPoint);
                fetchAndStoreYahooHistory(symbol, start, now);
                
            } catch (Exception e) {
                logger.error("Failed to sync historical gaps for {}: {}", symbol, e.getMessage(), e);
            }
        }
    }

    private void fetchAndStoreYahooHistory(String symbol, long start, long end) throws Exception {
        String encodedSymbol = java.net.URLEncoder.encode(symbol, java.nio.charset.StandardCharsets.UTF_8);
        String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1wk", encodedSymbol, start, end);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode resultNode = root.path("chart").path("result").get(0);
        
        if (resultNode.isMissingNode() || resultNode.isNull()) return;

        JsonNode timestamps = resultNode.path("timestamp");
        JsonNode indicatorNode = resultNode.path("indicators").path("quote").get(0);
        if (indicatorNode.isMissingNode()) return;
        
        JsonNode closes = indicatorNode.path("close");

        if (timestamps.isMissingNode() || closes.isMissingNode()) return;

        for (int i = 0; i < timestamps.size(); i++) {
            if (i >= closes.size()) break;
            long ts = timestamps.get(i).asLong();
            JsonNode closeNode = closes.get(i);
            if (closeNode.isNull()) continue;
            double close = closeNode.asDouble();
            if (close == 0) continue;

            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault());
            try {
                historyRepository.save(new MetricHistory(symbol, close, 0.0, dt));
            } catch (Exception e) {
                // Ignore duplicates due to unique constraint
                logger.trace("Skipping duplicate history point for {}: {}", symbol, dt);
            }
        }
    }

    public List<MacroMetric> getLatestMetrics() {
        return repository.findAllByOrderByLabelAsc();
    }

    public List<MetricHistory> getHistory(String ticker) {
        return historyRepository.findByTickerOrderByTimestampAsc(ticker);
    }
}
