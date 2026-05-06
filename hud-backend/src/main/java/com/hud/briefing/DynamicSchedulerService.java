package com.hud.briefing;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Service
@EnableScheduling
public class DynamicSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicSchedulerService.class);
    private final BriefingScheduleRepository repository;
    private final AutomatedBriefingService briefingService;
    private final TaskScheduler taskScheduler;

    private final Map<BriefingCategory, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    public DynamicSchedulerService(BriefingScheduleRepository repository, 
                                   AutomatedBriefingService briefingService,
                                   TaskScheduler taskScheduler) {
        this.repository = repository;
        this.briefingService = briefingService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        refreshAllSchedules();
    }

    public synchronized void refreshAllSchedules() {
        logger.info("[SCHEDULER] Refreshing all briefing schedules...");
        // Cancel all existing tasks
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();

        List<BriefingSchedule> schedules = repository.findAll();
        for (BriefingSchedule schedule : schedules) {
            if (schedule.isActive()) {
                scheduleTask(schedule);
            }
        }
    }

    private void scheduleTask(BriefingSchedule schedule) {
        logger.debug("[SCHEDULER] Scheduling task for {} with cron {}", schedule.getCategory(), schedule.getCronExpression());
        ScheduledFuture<?> future = taskScheduler.schedule(() -> {
            logger.info("[SCHEDULER] Triggering run for category: {}", schedule.getCategory());
            briefingService.generateForCategory(schedule.getCategory());

            // Update last run time (separate transaction)
            repository.findByCategory(schedule.getCategory()).ifPresent(s -> {
                s.setLastRunAt(LocalDateTime.now());
                repository.save(s);
            });

        }, new CronTrigger(schedule.getCronExpression()));

        scheduledTasks.put(schedule.getCategory(), future);
    }
}
