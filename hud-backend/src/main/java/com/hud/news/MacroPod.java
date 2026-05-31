package com.hud.news;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MacroPod {
    private String id;
    private String title;
    private String sentimentNarrative;
    private String educationalDescription;
    private String learnMoreLink;
    private List<MacroPodMetric> metrics = new ArrayList<>();

    public MacroPod() {}

    public MacroPod(String id, String title, String sentimentNarrative, String educationalDescription, String learnMoreLink, List<MacroPodMetric> metrics) {
        this.id = id;
        this.title = title;
        this.sentimentNarrative = sentimentNarrative;
        this.educationalDescription = educationalDescription;
        this.learnMoreLink = learnMoreLink;
        this.metrics = metrics != null ? new ArrayList<>(metrics) : new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSentimentNarrative() { return sentimentNarrative; }
    public void setSentimentNarrative(String sentimentNarrative) { this.sentimentNarrative = sentimentNarrative; }
    
    public String getEducationalDescription() { return educationalDescription; }
    public void setEducationalDescription(String educationalDescription) { this.educationalDescription = educationalDescription; }
    
    public String getLearnMoreLink() { return learnMoreLink; }
    public void setLearnMoreLink(String learnMoreLink) { this.learnMoreLink = learnMoreLink; }
    
    public List<MacroPodMetric> getMetrics() { 
        return metrics != null ? Collections.unmodifiableList(metrics) : Collections.emptyList(); 
    }
    
    public void setMetrics(List<MacroPodMetric> metrics) { 
        this.metrics = metrics != null ? new ArrayList<>(metrics) : new ArrayList<>(); 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroPod macroPod = (MacroPod) o;
        return Objects.equals(id, macroPod.id) &&
               Objects.equals(title, macroPod.title) &&
               Objects.equals(sentimentNarrative, macroPod.sentimentNarrative) &&
               Objects.equals(educationalDescription, macroPod.educationalDescription) &&
               Objects.equals(learnMoreLink, macroPod.learnMoreLink) &&
               Objects.equals(metrics, macroPod.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, sentimentNarrative, educationalDescription, learnMoreLink, metrics);
    }

    @Override
    public String toString() {
        return "MacroPod{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", sentimentNarrative='" + sentimentNarrative + '\'' +
                ", educationalDescription='" + educationalDescription + '\'' +
                ", learnMoreLink='" + learnMoreLink + '\'' +
                ", metrics=" + metrics +
                '}';
    }
}
