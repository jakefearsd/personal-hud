package com.hud.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InsightsGenerationServiceTest {
    @Test
    void testGenerateInsight() {
        InsightsGenerationService service = new InsightsGenerationService();
        WeeklyInsight insight = service.generateInsight(List.of("url1", "url2"));
        assertNotNull(insight);
        assertNotNull(insight.getNarrativeText());
        assertFalse(insight.getKeyConsiderations().isEmpty());
    }
}
