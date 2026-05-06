package com.hud.news;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "metric_history")
public class MetricHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    private Double price;
    private Double changePercent;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;

    public MetricHistory() {}

    public MetricHistory(String ticker, Double price, Double changePercent) {
        this(ticker, price, changePercent, LocalDateTime.now());
    }

    public MetricHistory(String ticker, Double price, Double changePercent, LocalDateTime timestamp) {
        this.ticker = ticker;
        this.price = price;
        this.changePercent = changePercent;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getChangePercent() { return changePercent; }
    public void setChangePercent(Double changePercent) { this.changePercent = changePercent; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
