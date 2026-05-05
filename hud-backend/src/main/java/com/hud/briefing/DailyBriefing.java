package com.hud.briefing;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_briefings")
public class DailyBriefing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generated_at")
    private java.time.LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    private BriefingCategory category;

    private String modelName;

    @Column(columnDefinition = "TEXT")
    private String markdownContent;

    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    public DailyBriefing() {}

    public DailyBriefing(java.time.LocalDateTime generatedAt, BriefingCategory category, String modelName, String markdownContent) {
        this.generatedAt = generatedAt;
        this.category = category;
        this.modelName = modelName;
        this.markdownContent = markdownContent;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public java.time.LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(java.time.LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public BriefingCategory getCategory() { return category; }
    public void setCategory(BriefingCategory category) { this.category = category; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getMarkdownContent() { return markdownContent; }
    public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }
    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }
}
