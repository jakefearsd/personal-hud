package com.hud.news;

import com.hud.briefing.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PredictionServiceTest {

    @Mock private DailyBriefingRepository briefingRepository;
    @Mock private MetricHistoryRepository historyRepository;
    @Mock private MarketPredictionRepository predictionRepository;
    @Mock private MacroMetricRepository macroMetricRepository;
    @Mock private DynamicLlmService llmService;
    @Mock private ChatLanguageModel chatModel;

    private PredictionService predictionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        predictionService = new PredictionService(briefingRepository, historyRepository, predictionRepository, macroMetricRepository, llmService);
    }

    @Test
    void shouldGenerateProPredictions() {
        DailyBriefing briefing = new DailyBriefing();
        briefing.setCategory(BriefingCategory.FINANCE);
        briefing.setMarkdownContent("Market is bullish.");
        when(briefingRepository.findLatestGlobal()).thenReturn(List.of(briefing));

        MacroMetric vix = new MacroMetric("^VIX", "Volatility", 15.0, 0.0, 0.0);
        when(macroMetricRepository.findAll()).thenReturn(List.of(vix));

        MetricHistory history = new MetricHistory("^GSPC", 4500.0, 0.5, LocalDateTime.now().minusDays(2));
        when(historyRepository.findByTickerOrderByTimestampAsc(anyString())).thenReturn(List.of(history));

        DynamicLlmService.NamedChatModel namedModel = new DynamicLlmService.NamedChatModel("Gemma", chatModel);
        when(llmService.getActiveModels()).thenReturn(List.of(namedModel));
        
        String llmResponse = "REGIME | Goldilocks\nThe macro outlook is balanced with strong technical momentum.\nTAIL RISK | Sudden oil price spike\nPREDICTION | ^GSPC | 4600.0 | 85 | Growth trend\nPREDICTION | ^DJI | 35000 | 70 | Stability";
        when(chatModel.generate(anyString())).thenReturn(llmResponse);

        predictionService.generateDailyPredictions();

        verify(predictionRepository, times(2)).save(any(MarketPrediction.class));
        verify(chatModel).generate(contains("Lead Quantitative Macro Strategist"));
        verify(chatModel).generate(contains("REGIME"));
    }

    @Test
    void shouldSettlePendingPredictions() {
        MarketPrediction p = new MarketPrediction("^GSPC", LocalDate.now().minusDays(7), LocalDate.now(), 4500.0, "Rationale", "Synth", "Model", null, 100, "None");
        when(predictionRepository.findByTargetDateLessThanEqualAndActualPriceIsNull(LocalDate.now())).thenReturn(List.of(p));

        MetricHistory latest = new MetricHistory("^GSPC", 4550.0, 0.1, LocalDateTime.now());
        when(historyRepository.findTopByTickerAndTimestampLessThanEqualOrderByTimestampDesc(eq("^GSPC"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(latest));

        predictionService.settlePendingPredictions();

        assertEquals(4550.0, p.getActualPrice());
        verify(predictionRepository).save(p);
    }

    @Test
    void shouldSettleWeekendPredictionsUsingLastAvailableData() {
        // Target date was Saturday (2 days ago), we are running settlement on Monday
        LocalDate targetDate = LocalDate.now().minusDays(2); 
        MarketPrediction p = new MarketPrediction("^GSPC", targetDate.minusDays(7), targetDate, 4500.0, "Rationale", "Synth", "Model", null, 100, "None");
        
        when(predictionRepository.findByTargetDateLessThanEqualAndActualPriceIsNull(LocalDate.now())).thenReturn(List.of(p));

        // Latest data before/on Saturday was Friday
        MetricHistory fridayClose = new MetricHistory("^GSPC", 4480.0, -0.2, targetDate.minusDays(1).atTime(16, 0));
        when(historyRepository.findTopByTickerAndTimestampLessThanEqualOrderByTimestampDesc(eq("^GSPC"), argThat(t -> t.toLocalDate().equals(targetDate))))
                .thenReturn(Optional.of(fridayClose));

        predictionService.settlePendingPredictions();

        assertEquals(4480.0, p.getActualPrice());
        verify(predictionRepository).save(p);
    }

    @Test
    void shouldReturnLatestPredictions() {
        predictionService.getLatestPredictions();
        verify(predictionRepository).findByGenerationDate(LocalDate.now());
    }

    @Test
    void shouldReturnPredictionHistory() {
        predictionService.getHistory("AAPL");
        verify(predictionRepository).findByTickerOrderByTargetDateAsc("AAPL");
    }
}
