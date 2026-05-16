package com.hud.briefing;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/config/brains")
public class LlmConfigController {

    private final LlmConfigRepository repository;
    private final AutomatedBriefingService briefingService;

    public LlmConfigController(LlmConfigRepository repository, AutomatedBriefingService briefingService) {
        this.repository = repository;
        this.briefingService = briefingService;
    }

    @GetMapping
    public List<LlmConfig> getAllConfigs() {
        List<LlmConfig> configs = repository.findAll();
        configs.forEach(c -> {
            if (c.getApiKey() != null && !c.getApiKey().isBlank()) {
                c.setApiKey("********");
            }
        });
        return configs;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public LlmConfig saveConfig(@RequestBody LlmConfig config) {
        // If updating existing and key is masked, preserve the old key
        if (config.getId() != null) {
            LlmConfig existing = repository.findById(config.getId()).orElseThrow();
            if (config.getApiKey() != null
                    && (config.getApiKey().equals("********") || config.getApiKey().isBlank())) {
                config.setApiKey(existing.getApiKey());
            }
        }
        return repository.save(config);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteConfig(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/toggle")
    public LlmConfig toggleActive(@PathVariable Long id) {
        LlmConfig config = repository.findById(id).orElseThrow();
        config.setActive(!config.isActive());
        return repository.save(config);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/run")
    public String runModel(@PathVariable Long id) {
        briefingService.generateForModel(id);
        return "Model specific briefing run triggered.";
    }
}
