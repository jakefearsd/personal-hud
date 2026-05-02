package com.hud.briefing;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_briefings")
public class DailyBriefing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate briefingDate;

    @Enumerated(EnumType.STRING)
    private BriefingCategory category;

    @Column(columnDefinition = "TEXT")
    private String markdownContent;

    public DailyBriefing() {}

    public DailyBriefing(LocalDate briefingDate, BriefingCategory category, String markdownContent) {
        this.briefingDate = briefingDate;
        this.category = category;
        this.markdownContent = markdownContent;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getBriefingDate() { return briefingDate; }
    public void setBriefingDate(LocalDate briefingDate) { this.briefingDate = briefingDate; }
    public BriefingCategory getCategory() { return category; }
    public void setCategory(BriefingCategory category) { this.category = category; }
    public String getMarkdownContent() { return markdownContent; }
    public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }
}
