package com.hud.news;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/investments")
public class MacroMetricsController {

    private final MacroMetricsService service;
    private final MarketEventRepository eventRepository;
    private final EventCorrelationService correlationService;

    public MacroMetricsController(MacroMetricsService service, 
                                  MarketEventRepository eventRepository,
                                  EventCorrelationService correlationService) {
        this.service = service;
        this.eventRepository = eventRepository;
        this.correlationService = correlationService;
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

    @PostMapping("/trigger")
    public String triggerUpdate() {
        service.updateMacroMetrics();
        return "Macro update triggered.";
    }

    @PostMapping("/correlate")
    public String triggerCorrelation() {
        correlationService.correlateEvents();
        return "Market correlation analysis triggered.";
    }
}
