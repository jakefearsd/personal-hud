package com.hud.news;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "macro_metrics")
public class MacroMetric {

    @Id
    private String ticker; // e.g., CL=F, ^VIX

    private String label;  // e.g., WTI Crude, Volatility Index
    private Double price;
    private Double change;
    private Double changePercent;
    private LocalDateTime updatedAt;

    public MacroMetric() {}

    public MacroMetric(String ticker, String label, Double price, Double change, Double changePercent) {
        this.ticker = ticker;
        this.label = label;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getChange() { return change; }
    public void setChange(Double change) { this.change = change; }
    public Double getChangePercent() { return changePercent; }
    public void setChangePercent(Double changePercent) { this.changePercent = changePercent; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
