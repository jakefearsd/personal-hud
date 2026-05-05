package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.Mockito.*;

class DynamicSchedulerServiceTest {

    @Mock
    private BriefingScheduleRepository repository;
    @Mock
    private AutomatedBriefingService briefingService;
    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private DynamicSchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        schedulerService = new DynamicSchedulerService(repository, briefingService, taskScheduler);
    }

    @Test
    void testRefreshAllSchedules() {
        BriefingSchedule schedule1 = new BriefingSchedule(BriefingCategory.WORLD_NEWS, "0 0 6 * * *", true);
        BriefingSchedule schedule2 = new BriefingSchedule(BriefingCategory.US_NEWS, "0 0 7 * * *", false);
        
        when(repository.findAll()).thenReturn(List.of(schedule1, schedule2));
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(Trigger.class));

        schedulerService.refreshAllSchedules();

        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void testScheduleTaskExecution() {
        BriefingSchedule schedule = new BriefingSchedule(BriefingCategory.WORLD_NEWS, "0 0 6 * * *", true);
        when(repository.findAll()).thenReturn(List.of(schedule));
        when(repository.findByCategory(BriefingCategory.WORLD_NEWS)).thenReturn(Optional.of(schedule));
        
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(scheduledFuture).when(taskScheduler).schedule(runnableCaptor.capture(), any(Trigger.class));

        schedulerService.refreshAllSchedules();

        // Simulate task execution
        Runnable capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.run();

        verify(briefingService).generateForCategory(BriefingCategory.WORLD_NEWS);
        verify(repository).save(schedule);
    }
}
