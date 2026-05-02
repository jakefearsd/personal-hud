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
    public List<DailyBriefing> getLatestBriefings(@RequestParam(required = false) String modelName) {
        if (modelName != null && !modelName.isBlank()) {
            return repository.findByModelToday(modelName);
        }
        return repository.findLatestToday();
    }

    @PostMapping("/trigger")
    public String triggerBriefing() {
        new Thread(briefingService::generateDailyBriefing).start();
        return "Briefing generation triggered in background.";
    }
}
