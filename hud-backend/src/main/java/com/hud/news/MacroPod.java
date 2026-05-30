package com.hud.news;

import java.util.List;

public class MacroPod {
    private String id;
    private String title;
    private String sentimentNarrative;
    private List<MacroPodMetric> metrics;

    public MacroPod() {}

    public MacroPod(String id, String title, String sentimentNarrative, List<MacroPodMetric> metrics) {
        this.id = id;
        this.title = title;
        this.sentimentNarrative = sentimentNarrative;
        this.metrics = metrics;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSentimentNarrative() { return sentimentNarrative; }
    public void setSentimentNarrative(String sentimentNarrative) { this.sentimentNarrative = sentimentNarrative; }
    public List<MacroPodMetric> getMetrics() { return metrics; }
    public void setMetrics(List<MacroPodMetric> metrics) { this.metrics = metrics; }
}
