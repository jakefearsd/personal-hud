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

    public MacroMetricsController(MacroMetricsService service, 
                                  MarketEventRepository eventRepository,
                                  EventCorrelationService correlationService,
                                  PredictionService predictionService) {
        this.service = service;
        this.eventRepository = eventRepository;
        this.correlationService = correlationService;
        this.predictionService = predictionService;
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
}
