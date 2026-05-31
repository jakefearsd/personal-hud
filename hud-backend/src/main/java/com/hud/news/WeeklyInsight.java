package com.hud.news;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "weekly_insights")
public class WeeklyInsight {
    @Id
    private UUID id = UUID.randomUUID();
    private String narrativeText;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> keyConsiderations;
    private LocalDateTime analysisStartDate;
    private LocalDateTime analysisEndDate;
    private LocalDateTime generatedAt = LocalDateTime.now();

    public WeeklyInsight() {}
    public WeeklyInsight(String narrativeText, List<String> keyConsiderations, LocalDateTime start, LocalDateTime end) {
        this.narrativeText = narrativeText;
        this.keyConsiderations = keyConsiderations;
        this.analysisStartDate = start;
        this.analysisEndDate = end;
    }
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNarrativeText() { return narrativeText; }
    public void setNarrativeText(String narrativeText) { this.narrativeText = narrativeText; }
    public List<String> getKeyConsiderations() { return keyConsiderations; }
    public void setKeyConsiderations(List<String> keyConsiderations) { this.keyConsiderations = keyConsiderations; }
    public LocalDateTime getAnalysisStartDate() { return analysisStartDate; }
    public void setAnalysisStartDate(LocalDateTime analysisStartDate) { this.analysisStartDate = analysisStartDate; }
    public LocalDateTime getAnalysisEndDate() { return analysisEndDate; }
    public void setAnalysisEndDate(LocalDateTime analysisEndDate) { this.analysisEndDate = analysisEndDate; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
