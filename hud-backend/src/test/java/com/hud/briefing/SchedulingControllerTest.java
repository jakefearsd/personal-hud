package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulingControllerTest {

    @Mock
    private BriefingScheduleRepository repository;
    @Mock
    private DynamicSchedulerService schedulerService;

    private SchedulingController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SchedulingController(repository, schedulerService);
    }

    @Test
    void testGetAllSchedules() {
        when(repository.findAll()).thenReturn(List.of(new BriefingSchedule(BriefingCategory.WORLD_NEWS, "0 0 6 * * *", true)));
        List<BriefingSchedule> schedules = controller.getAllSchedules();
        assertEquals(1, schedules.size());
        verify(repository).findAll();
    }

    @Test
    void testUpdateSchedule() {
        Long id = 1L;
        BriefingSchedule existing = new BriefingSchedule(BriefingCategory.WORLD_NEWS, "0 0 6 * * *", true);
        BriefingSchedule updated = new BriefingSchedule(BriefingCategory.WORLD_NEWS, "0 0 7 * * *", false);
        
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BriefingSchedule result = controller.updateSchedule(id, updated);

        assertEquals("0 0 7 * * *", result.getCronExpression());
        assertFalse(result.isActive());
        verify(schedulerService).refreshAllSchedules();
    }

    @Test
    void testSeedSchedules() {
        when(repository.count()).thenReturn(0L);
        controller.seedSchedules();
        verify(repository, atLeastOnce()).save(any());
        verify(schedulerService).refreshAllSchedules();
    }

    @Test
    void testSeedSchedulesAlreadySeeded() {
        when(repository.count()).thenReturn(10L);
        controller.seedSchedules();
        verify(repository, never()).save(any());
        verify(schedulerService, never()).refreshAllSchedules();
    }
}
