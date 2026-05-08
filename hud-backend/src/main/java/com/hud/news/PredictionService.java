package com.hud.news;

import com.hud.briefing.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private static final Logger logger = LoggerFactory.getLogger(PredictionService.class);
    private final DailyBriefingRepository briefingRepository;
    private final MetricHistoryRepository historyRepository;
    private final MarketPredictionRepository predictionRepository;
    private final DynamicLlmService llmService;

    private static final List<String> INPUT_SYMBOLS = List.of(
        "^GSPC", "^DJI", "^GDAXI", "^N225", "BNDX", "^FTSE", "GC=F", "^VIX", "AGG", "CL=F"
    );

    private static final List<String> TARGET_SYMBOLS = List.of(
        "^GSPC", "^DJI", "AGG", "BNDX"
    );

    public PredictionService(DailyBriefingRepository briefingRepository,
                             MetricHistoryRepository historyRepository,
                             MarketPredictionRepository predictionRepository,
                             DynamicLlmService llmService) {
        this.briefingRepository = briefingRepository;
        this.historyRepository = historyRepository;
        this.predictionRepository = predictionRepository;
        this.llmService = llmService;
    }

    @Scheduled(cron = "0 45 6 * * *") // Runs 45 mins after daily briefing
    public void generateDailyPredictions() {
        logger.info("Initiating Rolling 7-Day Market Projections...");
        
        List<DailyBriefing> briefings = briefingRepository.findLatestGlobal();
        String financeBriefing = briefings.stream()
                .filter(b -> b.getCategory() == BriefingCategory.FINANCE)
                .map(DailyBriefing::getMarkdownContent)
                .findFirst()
                .orElse("No recent financial context available.");

        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        StringBuilder historyData = new StringBuilder("WEEKLY HISTORICAL DATA (6 MONTHS):\n");
        historyData.append("Date | Symbol | Price\n");

        for (String symbol : INPUT_SYMBOLS) {
            List<MetricHistory> history = historyRepository.findByTickerOrderByTimestampAsc(symbol);
            List<MetricHistory> filtered = history.stream()
                    .filter(h -> h.getTimestamp().toLocalDate().isAfter(sixMonthsAgo))
                    .collect(Collectors.toList());
            
            for (MetricHistory h : filtered) {
                historyData.append(String.format("%s | %s | %.2f\n", 
                    h.getTimestamp().toLocalDate(), symbol, h.getPrice()));
            }
        }

        var models = llmService.getActiveModels();
        if (models.isEmpty()) return;
        
        DynamicLlmService.NamedChatModel namedModel = models.get(0);
        ChatLanguageModel model = namedModel.model();

        LocalDate targetDate = LocalDate.now().plusDays(7);
        
        String prompt = String.format(
            "COMMAND DIRECTIVE: You are a Quantitative Macro Strategist.\n" +
            "CONTEXT: Current financial intelligence and 6 months of weekly historical trends.\n" +
            "TASK:\n" +
            "1. Write a 1-paragraph (max 4 sentences) synthesis of the current global market momentum.\n" +
            "2. Predict the closing price for S&P 500 (^GSPC), Dow 30 (^DJI), BNDX, and AGG for exactly 7 days from today (%s).\n" +
            "3. Provide a 1-sentence rationale for each prediction based on the data.\n\n" +
            "FORMAT RULES:\n" +
            "Start with the synthesis paragraph.\n" +
            "Then provide predictions in this exact pipe-delimited format:\n" +
            "PREDICTION | [Ticker] | [PredictedPrice] | [Rationale]\n\n" +
            "FINANCIAL BRIEFING:\n%s\n\n" +
            "%s\n\n" +
            "MARKET PULSE AND PROJECTION:",
            targetDate, financeBriefing, historyData.toString()
        );

        try {
            String response = model.generate(prompt);
            parseAndSavePredictions(response, namedModel.name(), targetDate);
        } catch (Exception e) {
            logger.error("Failed to generate market predictions: {}", e.getMessage(), e);
        }
    }

    private void parseAndSavePredictions(String response, String modelName, LocalDate targetDate) {
        String[] lines = response.split("\n");
        String synthesis = lines[0].trim(); // First paragraph is synthesis
        
        for (String line : lines) {
            if (line.startsWith("PREDICTION |")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    String ticker = parts[1].trim();
                    Double price = Double.parseDouble(parts[2].trim().replace(",", ""));
                    String rationale = parts[3].trim();
                    
                    MarketPrediction prediction = new MarketPrediction(
                        ticker, LocalDate.now(), targetDate, price, rationale, synthesis, modelName, null
                    );
                    predictionRepository.save(prediction);
                    logger.info("Saved prediction for {}: {} (Target: {})", ticker, price, targetDate);
                }
            }
        }
    }

    @Scheduled(cron = "0 0 21 * * *") // Runs nightly to settle past predictions
    public void settlePendingPredictions() {
        logger.info("Settling pending market predictions...");
        List<MarketPrediction> pending = predictionRepository.findByTargetDateAndActualPriceIsNull(LocalDate.now());
        
        for (MarketPrediction p : pending) {
            historyRepository.findTopByTickerOrderByTimestampDesc(p.getTicker()).ifPresent(h -> {
                // If the latest point is today (or very recent), use it as settlement
                if (h.getTimestamp().toLocalDate().equals(LocalDate.now())) {
                    p.setActualPrice(h.getPrice());
                    predictionRepository.save(p);
                    logger.info("Settled prediction for {}: Actual {}", p.getTicker(), h.getPrice());
                }
            });
        }
    }

    public List<MarketPrediction> getLatestPredictions() {
        return predictionRepository.findByGenerationDate(LocalDate.now());
    }

    public List<MarketPrediction> getHistory(String ticker) {
        return predictionRepository.findByTickerOrderByTargetDateAsc(ticker);
    }
}
