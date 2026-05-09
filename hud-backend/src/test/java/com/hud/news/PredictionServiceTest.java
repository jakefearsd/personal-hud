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
    @Mock private DynamicLlmService llmService;
    @Mock private ChatLanguageModel chatModel;

    private PredictionService predictionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        predictionService = new PredictionService(briefingRepository, historyRepository, predictionRepository, llmService);
    }

    @Test
    void shouldGeneratePredictions() {
        DailyBriefing briefing = new DailyBriefing();
        briefing.setCategory(BriefingCategory.FINANCE);
        briefing.setMarkdownContent("Market is bullish.");
        when(briefingRepository.findLatestGlobal()).thenReturn(List.of(briefing));

        MetricHistory history = new MetricHistory("^GSPC", 4500.0, 0.5);
        history.setTimestamp(LocalDateTime.now().minusDays(2));
        when(historyRepository.findByTickerOrderByTimestampAsc(anyString())).thenReturn(List.of(history));

        DynamicLlmService.NamedChatModel namedModel = new DynamicLlmService.NamedChatModel("Gemma", chatModel);
        when(llmService.getActiveModels()).thenReturn(List.of(namedModel));
        
        String llmResponse = "Market pulse is strong.\nPREDICTION | ^GSPC | 4600.0 | Growth trend\nPREDICTION | ^DJI | 35000 | Stability";
        when(chatModel.generate(anyString())).thenReturn(llmResponse);

        predictionService.generateDailyPredictions();

        verify(predictionRepository, times(2)).save(any(MarketPrediction.class));
        verify(chatModel).generate(contains("Quantitative Macro Strategist"));
    }

    @Test
    void shouldSettlePendingPredictions() {
        MarketPrediction p = new MarketPrediction("^GSPC", LocalDate.now().minusDays(7), LocalDate.now(), 4500.0, "Rationale", "Synth", "Model", null);
        when(predictionRepository.findByTargetDateAndActualPriceIsNull(LocalDate.now())).thenReturn(List.of(p));

        MetricHistory latest = new MetricHistory("^GSPC", 4550.0, 0.1);
        latest.setTimestamp(LocalDateTime.now());
        when(historyRepository.findTopByTickerOrderByTimestampDesc("^GSPC")).thenReturn(Optional.of(latest));

        predictionService.settlePendingPredictions();

        assertEquals(4550.0, p.getActualPrice());
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
