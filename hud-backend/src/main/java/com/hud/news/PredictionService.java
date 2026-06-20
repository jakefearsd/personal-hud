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
    private final MacroMetricRepository macroMetricRepository;
    private final DynamicLlmService llmService;

    private static final List<String> INPUT_SYMBOLS = List.of(
        "^GSPC", "^DJI", "^GDAXI", "^N225", "BNDX", "^FTSE", "GC=F", "^VIX", "AGG", "CL=F", "VXUS", "EFV"
    );

    private static final List<String> TARGET_SYMBOLS = List.of(
        "^GSPC", "^DJI", "AGG", "BNDX", "VXUS", "EFV"
    );

    public PredictionService(DailyBriefingRepository briefingRepository,
                             MetricHistoryRepository historyRepository,
                             MarketPredictionRepository predictionRepository,
                             MacroMetricRepository macroMetricRepository,
                             DynamicLlmService llmService) {
        this.briefingRepository = briefingRepository;
        this.historyRepository = historyRepository;
        this.predictionRepository = predictionRepository;
        this.macroMetricRepository = macroMetricRepository;
        this.llmService = llmService;
    }

    @org.springframework.scheduling.annotation.Async
    @Scheduled(cron = "0 45 6 * * *") // Runs 45 mins after daily briefing
    public void generateDailyPredictions() {
        logger.info("Initiating Pro Rolling 7-Day Market Projections...");
        
        List<DailyBriefing> briefings = briefingRepository.findLatestGlobal();
        String financeBriefing = briefings.stream()
                .filter(b -> b.getCategory() == BriefingCategory.FINANCE)
                .map(DailyBriefing::getMarkdownContent)
                .findFirst()
                .orElse("No recent financial context available.");

        // Macro Grounding
        List<MacroMetric> vitals = macroMetricRepository.findAll();
        StringBuilder macroData = new StringBuilder("CORE MACRO VITALS:\n");
        macroData.append("Indicator | Value | Change\n");
        for (MacroMetric m : vitals) {
            macroData.append(String.format("%s | %.2f | %.2f%%\n", m.getLabel(), m.getPrice(), m.getChangePercent()));
        }

        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        StringBuilder historyData = new StringBuilder("WEEKLY HISTORICAL TRENDS (6 MONTHS):\n");
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
            "COMMAND DIRECTIVE: You are a Lead Quantitative Macro Strategist.\n" +
            "CONTEXT: Current financial intelligence, Core Macro Vitals (VIX, Yield Spreads, Inflation), and 6 months of historical trends.\n" +
            "TASK:\n" +
            "1. Identify the current MARKET REGIME (e.g., Reflation, Stagflation, or Goldilocks).\n" +
            "2. Write a 1-paragraph synthesis that reconciles Technicians (momentum) and Macro Analysts (fundamentals).\n" +
            "3. Predict the closing price for S&P 500 (^GSPC), Dow 30 (^DJI), BNDX, AGG, VXUS (Intl Equity), and EFV (Intl Value) for exactly 7 days from today (%s).\n" +
            "4. For each asset, provide a 1-sentence rationale and a Confidence Score (0-100).\n" +
            "5. ADVERSARIAL REASONING: Identify one specific 'Tail Risk' for the next 7 days that would invalidate your baseline view.\n\n" +
            "FORMAT RULES:\n" +
            "Line 1: REGIME | [Name]\n" +
            "Paragraph: [The Synthesis]\n" +
            "Line: TAIL RISK | [Specific Risk Description]\n" +
            "Then provide predictions in this exact pipe-delimited format:\n" +
            "PREDICTION | [Ticker] | [PredictedPrice] | [ConfidenceScore] | [Rationale]\n\n" +
            "FINANCIAL BRIEFING:\n%s\n\n" +
            "%s\n\n" +
            "%s\n\n" +
            "PRO MARKET PROJECTION:",
            targetDate, financeBriefing, macroData.toString(), historyData.toString()
        );

        try {
            String response = model.generate(prompt);
            parseAndSavePredictions(response, namedModel.name(), targetDate);
        } catch (Exception e) {
            logger.error("Failed to generate pro market predictions: {}", e.getMessage(), e);
        }
    }

    private void parseAndSavePredictions(String response, String modelName, LocalDate targetDate) {
        String[] lines = response.split("\n");
        String synthesis = "No synthesis available.";
        String tailRisk = "None identified.";
        String regime = "Unknown";

        // Find the synthesis paragraph (first multi-sentence line or line after REGIME)
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("REGIME |")) {
                regime = line.substring(8).trim();
            } else if (line.startsWith("TAIL RISK |")) {
                tailRisk = line.substring(11).trim();
            } else if (line.length() > 50 && !line.contains("|")) {
                synthesis = line;
            }
        }
        
        final String finalSynthesis = String.format("[%s Regime] %s", regime, synthesis);
        final String finalTailRisk = tailRisk;

        for (String line : lines) {
            if (line.startsWith("PREDICTION |")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    try {
                        String ticker = parts[1].trim();
                        Double price = Double.parseDouble(parts[2].trim().replace(",", ""));
                        Integer confidence = Integer.parseInt(parts[3].trim());
                        String rationale = parts[4].trim();
                        
                        MarketPrediction prediction = new MarketPrediction(
                            ticker, LocalDate.now(), targetDate, price, rationale, 
                            finalSynthesis, modelName, null, confidence, finalTailRisk
                        );
                        predictionRepository.save(prediction);
                        logger.info("Saved pro prediction for {}: {} (Conf: {}%)", ticker, price, confidence);
                    } catch (Exception e) {
                        logger.warn("Failed to parse prediction line: {}. Error: {}", line, e.getMessage());
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0 0 21 * * *") // Runs nightly to settle past predictions
    public void settlePendingPredictions() {
        logger.info("Settling pending market predictions...");
        List<MarketPrediction> pending = predictionRepository.findByTargetDateLessThanEqualAndActualPriceIsNull(LocalDate.now());
        
        for (MarketPrediction p : pending) {
            historyRepository.findTopByTickerAndTimestampLessThanEqualOrderByTimestampDesc(
                    p.getTicker(), p.getTargetDate().atTime(23, 59, 59))
                .ifPresent(h -> {
                    p.setActualPrice(h.getPrice());
                    predictionRepository.save(p);
                    logger.info("Settled prediction for {}: Actual {} (Target was: {})", 
                        p.getTicker(), h.getPrice(), p.getTargetDate());
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
