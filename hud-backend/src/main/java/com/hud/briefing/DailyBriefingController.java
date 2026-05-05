package com.hud.briefing;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/briefings")
public class DailyBriefingController {

    private final DailyBriefingRepository repository;
    private final AutomatedBriefingService briefingService;

    public DailyBriefingController(DailyBriefingRepository repository, AutomatedBriefingService briefingService) {
        this.repository = repository;
        this.briefingService = briefingService;
    }

    @GetMapping("/latest")
    public List<DailyBriefing> getLatestBriefings(@RequestParam(name = "modelName", required = false) String modelName) {
        if (modelName != null && !modelName.isBlank()) {
            List<DailyBriefing> briefings = repository.findByModelToday(modelName);

            // Fallback 1: Match without "Cloud: " or "Local: " prefix for today
            if (briefings.isEmpty() && modelName.contains(": ")) {
                String fallbackName = modelName.split(": ")[1];
                briefings = repository.findByModelToday(fallbackName);
            }

            // Fallback 2: If still empty (e.g., early morning before run), get most recent historical for this model
            if (briefings.isEmpty()) {
                briefings = repository.findByModelLatest(modelName);
            }

            return briefings;
        }

        // Default path: Check today's global briefings
        List<DailyBriefing> briefings = repository.findLatestToday();

        // Fallback: If no briefings generated today yet, show the most recent available set
        if (briefings.isEmpty()) {
            briefings = repository.findLatestGlobal();
        }

        return briefings;
    }

    @PostMapping("/trigger")
    public String triggerBriefing() {
        briefingService.generateDailyBriefing();
        return "Briefing generation triggered in background.";
    }
}
