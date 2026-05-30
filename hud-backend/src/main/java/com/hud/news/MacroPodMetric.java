package com.hud.news;

public class MacroPodMetric {
    private String ticker;
    private String label;
    private double currentValue;
    private double historicalPercentile; // e.g., 95.5 for 95th percentile
    private double changePercent;

    // Default constructor
    public MacroPodMetric() {}

    public MacroPodMetric(String ticker, String label, double currentValue, double historicalPercentile, double changePercent) {
        this.ticker = ticker;
        this.label = label;
        this.currentValue = currentValue;
        this.historicalPercentile = historicalPercentile;
        this.changePercent = changePercent;
    }

    // Getters and Setters
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    public double getHistoricalPercentile() { return historicalPercentile; }
    public void setHistoricalPercentile(double historicalPercentile) { this.historicalPercentile = historicalPercentile; }
    public double getChangePercent() { return changePercent; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }
}
