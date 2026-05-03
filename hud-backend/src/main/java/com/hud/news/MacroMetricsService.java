package com.hud.news;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class MacroMetricsService {

    private final PlaywrightScraperService scraperService;
    private final MacroMetricRepository repository;
    private final MetricHistoryRepository historyRepository;

    public MacroMetricsService(PlaywrightScraperService scraperService, 
                               MacroMetricRepository repository,
                               MetricHistoryRepository historyRepository) {
        this.scraperService = scraperService;
        this.repository = repository;
        this.historyRepository = historyRepository;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void updateMacroMetrics() {
        System.out.println("Updating Macro Vitals Dashboard...");
        
        Map<String, String> tickers = Map.of(
            "CL=F", "WTI Crude Oil",
            "BZ=F", "Brent Crude Oil",
            "DX-Y.NYB", "US Dollar Index",
            "GC=F", "Gold",
            "^VIX", "VIX (Volatility)",
            "BTC-USD", "Bitcoin",
            "ETH-USD", "Ethereum"
        );

        for (Map.Entry<String, String> entry : tickers.entrySet()) {
            MacroMetric metric = scraperService.scrapeYahooMetric(entry.getKey(), entry.getValue());
            if (metric != null) {
                repository.save(metric);
                historyRepository.save(new MetricHistory(metric.getTicker(), metric.getPrice(), metric.getChangePercent()));
            }
        }

        // Special handling for Yield Spread
        Double spread = scraperService.scrapeFredYieldSpread();
        if (spread != null) {
            MacroMetric metric = new MacroMetric("T10Y2Y", "10Y-2Y Yield Spread", spread, 0.0, 0.0);
            repository.save(metric);
            historyRepository.save(new MetricHistory("T10Y2Y", spread, 0.0));
        }
    }

    public List<MacroMetric> getLatestMetrics() {
        return repository.findAllByOrderByLabelAsc();
    }

    public List<MetricHistory> getHistory(String ticker) {
        return historyRepository.findByTickerOrderByTimestampAsc(ticker);
    }
}
