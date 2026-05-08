package com.hud.news;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_predictions")
public class MarketPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;
    private LocalDate generationDate;
    private LocalDate targetDate;
    private Double predictedPrice;
    private Double actualPrice;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(columnDefinition = "TEXT")
    private String synthesis;

    private String modelName;
    private Long briefingId;

    public MarketPrediction() {}

    public MarketPrediction(String ticker, LocalDate generationDate, LocalDate targetDate, 
                            Double predictedPrice, String rationale, String synthesis, 
                            String modelName, Long briefingId) {
        this.ticker = ticker;
        this.generationDate = generationDate;
        this.targetDate = targetDate;
        this.predictedPrice = predictedPrice;
        this.rationale = rationale;
        this.synthesis = synthesis;
        this.modelName = modelName;
        this.briefingId = briefingId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public LocalDate getGenerationDate() { return generationDate; }
    public void setGenerationDate(LocalDate generationDate) { this.generationDate = generationDate; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public Double getPredictedPrice() { return predictedPrice; }
    public void setPredictedPrice(Double predictedPrice) { this.predictedPrice = predictedPrice; }
    public Double getActualPrice() { return actualPrice; }
    public void setActualPrice(Double actualPrice) { this.actualPrice = actualPrice; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getSynthesis() { return synthesis; }
    public void setSynthesis(String synthesis) { this.synthesis = synthesis; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Long getBriefingId() { return briefingId; }
    public void setBriefingId(Long briefingId) { this.briefingId = briefingId; }
}
