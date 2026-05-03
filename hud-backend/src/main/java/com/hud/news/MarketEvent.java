package com.hud.news;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_events")
public class MarketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    private LocalDateTime timestamp;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String rationale;

    public MarketEvent() {}

    public MarketEvent(String ticker, LocalDateTime timestamp, String title, String rationale) {
        this.ticker = ticker;
        this.timestamp = timestamp;
        this.title = title;
        this.rationale = rationale;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
