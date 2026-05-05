package com.hud.briefing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "briefing_schedules")
public class BriefingSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private BriefingCategory category;

    @Column(nullable = false)
    private String cronExpression; // e.g., "0 0 6 * * *"

    private boolean active;

    private LocalDateTime lastRunAt;

    public BriefingSchedule() {}

    public BriefingSchedule(BriefingCategory category, String cronExpression, boolean active) {
        this.category = category;
        this.cronExpression = cronExpression;
        this.active = active;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BriefingCategory getCategory() { return category; }
    public void setCategory(BriefingCategory category) { this.category = category; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
}
