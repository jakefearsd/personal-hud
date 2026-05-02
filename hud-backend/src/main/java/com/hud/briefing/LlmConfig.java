package com.hud.briefing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_configs")
public class LlmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmProvider provider;

    private String baseUrl;
    private String modelName;
    private String apiKey;
    
    @Column(name = "num_ctx")
    private Integer numCtx;
    
    private boolean active;
    
    private LocalDateTime updatedAt;

    public LlmConfig() {}

    public LlmConfig(String name, LlmProvider provider, String modelName, boolean active) {
        this.name = name;
        this.provider = provider;
        this.modelName = modelName;
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LlmProvider getProvider() { return provider; }
    public void setProvider(LlmProvider provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Integer getNumCtx() { return numCtx; }
    public void setNumCtx(Integer numCtx) { this.numCtx = numCtx; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
