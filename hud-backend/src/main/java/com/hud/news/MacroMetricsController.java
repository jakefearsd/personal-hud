package com.hud.news;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/investments")
public class MacroMetricsController {

    private final MacroMetricsService service;
    private final MarketEventRepository eventRepository;
    private final EventCorrelationService correlationService;
    private final PredictionService predictionService;
    private final MacroSentimentService sentimentService;

    public MacroMetricsController(MacroMetricsService service, 
                                  MarketEventRepository eventRepository,
                                  EventCorrelationService correlationService,
                                  PredictionService predictionService,
                                  MacroSentimentService sentimentService) {
        this.service = service;
        this.eventRepository = eventRepository;
        this.correlationService = correlationService;
        this.predictionService = predictionService;
        this.sentimentService = sentimentService;
    }

    @GetMapping("/vitals")
    public List<MacroMetric> getVitals() {
        return service.getLatestMetrics();
    }

    @GetMapping("/history/{ticker}")
    public List<MetricHistory> getHistory(@PathVariable String ticker) {
        return service.getHistory(ticker);
    }

    @GetMapping("/events/{ticker}")
    public List<MarketEvent> getEvents(@PathVariable String ticker) {
        return eventRepository.findByTickerOrderByTimestampDesc(ticker);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/trigger")
    public String triggerUpdate() {
        service.updateMacroMetrics();
        return "Macro update triggered.";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/correlate")
    public String triggerCorrelation() {
        correlationService.correlateEvents();
        return "Market correlation analysis triggered.";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync")
    public String triggerSync() {
        service.syncHistoricalGaps();
        return "Historical data sync triggered.";
    }

    @GetMapping("/predictions/latest")
    public List<MarketPrediction> getLatestPredictions() {
        return predictionService.getLatestPredictions();
    }

    @GetMapping("/predictions/history/{ticker}")
    public List<MarketPrediction> getPredictionHistory(@PathVariable String ticker) {
        return predictionService.getHistory(ticker);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/predictions/trigger")
    public String triggerPredictions() {
        predictionService.generateDailyPredictions();
        return "Market predictions triggered.";
    }

    @GetMapping("/macro-pods")
    public List<MacroPod> getMacroPods() {
        // Pod 1: Economic Health
        MacroPod economicPod = new MacroPod(
                "economic_health",
                "Economic Health",
                sentimentService.generatePodSentiment("Treasury Yield Curves, Core Inflation, and broad market volatility (VIX)"),
                List.of(
                        new MacroPodMetric("10Y2Y", "Yield Curve (10y-2y)", -0.45, 12.5, 5.2),
                        new MacroPodMetric("CPI", "Core Inflation", 3.2, 85.0, -1.5),
                        new MacroPodMetric("^VIX", "Volatility Index", 14.5, 30.0, 2.1)
                )
        );

        // Pod 2: Liquidity & Credit
        MacroPod liquidityPod = new MacroPod(
                "liquidity_credit",
                "Liquidity & Credit",
                sentimentService.generatePodSentiment("M2 Money Supply, Central Bank Balance Sheets, and High-Yield Corporate Credit Spreads"),
                List.of(
                        new MacroPodMetric("M2", "M2 Money Supply", 20800.5, 95.0, 0.5),
                        new MacroPodMetric("HYSpread", "High-Yield Spread", 3.8, 40.0, -2.3)
                )
        );

        // Pod 3: Global Flows
        MacroPod flowsPod = new MacroPod(
                "global_flows",
                "Global Flows",
                sentimentService.generatePodSentiment("US Dollar Index (DXY), Global Commodities (Oil/Gold), and Emerging Markets"),
                List.of(
                        new MacroPodMetric("DX-Y.NYB", "US Dollar Index", 104.2, 75.0, 0.8),
                        new MacroPodMetric("GC=F", "Gold", 2350.4, 98.0, 1.2),
                        new MacroPodMetric("CL=F", "Crude Oil", 82.5, 60.0, -0.5)
                )
        );

        // Pod 4: Valuations
        MacroPod valuationPod = new MacroPod(
                "valuations",
                "Historical Valuations",
                sentimentService.generatePodSentiment("P/E ratios for major US and European indices mapped against historical percentiles"),
                List.of(
                        new MacroPodMetric("SPX_PE", "S&P 500 P/E", 24.5, 92.0, 1.5),
                        new MacroPodMetric("STOXX_PE", "Euro Stoxx 50 P/E", 14.2, 55.0, 0.3)
                )
        );

        return List.of(economicPod, liquidityPod, flowsPod, valuationPod);
    }
}
