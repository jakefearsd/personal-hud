package com.hud.news;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroPodMetric that = (MacroPodMetric) o;
        return Double.compare(that.currentValue, currentValue) == 0 &&
               Double.compare(that.historicalPercentile, historicalPercentile) == 0 &&
               Double.compare(that.changePercent, changePercent) == 0 &&
               Objects.equals(ticker, that.ticker) &&
               Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, label, currentValue, historicalPercentile, changePercent);
    }

    @Override
    public String toString() {
        return "MacroPodMetric{" +
                "ticker='" + ticker + '\'' +
                ", label='" + label + '\'' +
                ", currentValue=" + currentValue +
                ", historicalPercentile=" + historicalPercentile +
                ", changePercent=" + changePercent +
                '}';
    }
}
