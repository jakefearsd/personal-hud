package com.hud.news;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.List;
import static org.mockito.Mockito.*;

class WeeklyInsightsPipelineTest {
    @Test
    void testRunPipeline() {
        NewsDiscoveryService discovery = mock(NewsDiscoveryService.class);
        InsightsGenerationService generation = mock(InsightsGenerationService.class);
        WeeklyInsightRepository repository = mock(WeeklyInsightRepository.class);
        
        when(discovery.discoverRecentEvents()).thenReturn(List.of("url"));
        when(generation.generateInsight(anyList())).thenReturn(new WeeklyInsight());
        
        WeeklyInsightsPipeline pipeline = new WeeklyInsightsPipeline(discovery, generation, repository);
        pipeline.runPipeline();
        
        verify(repository, times(1)).save(any(WeeklyInsight.class));
    }
}
