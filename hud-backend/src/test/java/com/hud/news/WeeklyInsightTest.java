package com.hud.news;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WeeklyInsightTest {
    @Test
    void testGettersAndSetters() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        WeeklyInsight insight = new WeeklyInsight("Narrative", List.of("Consideration"), start, end);
        
        UUID id = UUID.randomUUID();
        insight.setId(id);
        assertEquals(id, insight.getId());
        
        insight.setNarrativeText("New Narrative");
        assertEquals("New Narrative", insight.getNarrativeText());
        
        insight.setKeyConsiderations(List.of("New Consideration"));
        assertEquals(1, insight.getKeyConsiderations().size());
        
        LocalDateTime newStart = LocalDateTime.now();
        insight.setAnalysisStartDate(newStart);
        assertEquals(newStart, insight.getAnalysisStartDate());
        
        insight.setAnalysisEndDate(end);
        assertEquals(end, insight.getAnalysisEndDate());
        
        LocalDateTime gen = LocalDateTime.now();
        insight.setGeneratedAt(gen);
        assertEquals(gen, insight.getGeneratedAt());
        
        WeeklyInsight empty = new WeeklyInsight();
        assertNotNull(empty.getId());
    }
}
