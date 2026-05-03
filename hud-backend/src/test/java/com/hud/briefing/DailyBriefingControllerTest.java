package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Tag("unit")
class DailyBriefingControllerTest {

    @Mock private DailyBriefingRepository repository;
    @Mock private AutomatedBriefingService briefingService;

    private DailyBriefingController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new DailyBriefingController(repository, briefingService);
    }

    @Test
    void shouldReturnLatestBriefings() {
        when(repository.findLatestToday()).thenReturn(List.of(new DailyBriefing()));
        List<DailyBriefing> result = controller.getLatestBriefings(null);
        assertEquals(1, result.size());
        verify(repository).findLatestToday();
    }

    @Test
    void shouldReturnBriefingsByModel() {
        when(repository.findByModelToday("Gemma")).thenReturn(List.of(new DailyBriefing()));
        List<DailyBriefing> result = controller.getLatestBriefings("Gemma");
        assertEquals(1, result.size());
        verify(repository).findByModelToday("Gemma");
    }

    @Test
    void shouldTriggerBriefing() {
        String result = controller.triggerBriefing();
        assertEquals("Briefing generation triggered in background.", result);
        verify(briefingService).generateDailyBriefing();
    }
}
