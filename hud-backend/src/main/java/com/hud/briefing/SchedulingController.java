package com.hud.briefing;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/config/schedules")
public class SchedulingController {

    private final BriefingScheduleRepository repository;
    private final DynamicSchedulerService schedulerService;

    public SchedulingController(BriefingScheduleRepository repository, DynamicSchedulerService schedulerService) {
        this.repository = repository;
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public List<BriefingSchedule> getAllSchedules() {
        return repository.findAll();
    }

    @PutMapping("/{id}")
    public BriefingSchedule updateSchedule(@PathVariable Long id, @RequestBody BriefingSchedule updated) {
        BriefingSchedule existing = repository.findById(id).orElseThrow();
        existing.setCronExpression(updated.getCronExpression());
        existing.setActive(updated.isActive());
        BriefingSchedule saved = repository.save(existing);
        
        // Refresh the runtime scheduler
        schedulerService.refreshAllSchedules();
        
        return saved;
    }

    @PostMapping("/init")
    public void seedSchedules() {
        if (repository.count() == 0) {
            for (BriefingCategory cat : BriefingCategory.values()) {
                // Default: 06:00 AM every day
                repository.save(new BriefingSchedule(cat, "0 0 6 * * *", true));
            }
            schedulerService.refreshAllSchedules();
        }
    }
}
