package com.hud.briefing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline_runs")
public class PipelineRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private BriefingCategory category;

    private String modelName;

    @Enumerated(EnumType.STRING)
    private PipelineStatus status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String errorDetail;

    private Integer inputTokens;
    private Integer outputTokens;

    public PipelineRun() {}

    public PipelineRun(BriefingCategory category, String modelName, PipelineStatus status, LocalDateTime startTime) {
        this.category = category;
        this.modelName = modelName;
        this.status = status;
        this.startTime = startTime;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BriefingCategory getCategory() { return category; }
    public void setCategory(BriefingCategory category) { this.category = category; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public PipelineStatus getStatus() { return status; }
    public void setStatus(PipelineStatus status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
}
