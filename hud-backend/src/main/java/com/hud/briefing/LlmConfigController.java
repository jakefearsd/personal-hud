package com.hud.briefing;

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
                String key = c.getApiKey();
                if (key.length() > 8) {
                    c.setApiKey(key.substring(0, 4) + "..." + key.substring(key.length() - 4));
                } else {
                    c.setApiKey("********");
                }
            }
        });
        return configs;
    }

    @PostMapping
    public LlmConfig saveConfig(@RequestBody LlmConfig config) {
        // If updating existing and key is masked, preserve the old key
        if (config.getId() != null) {
            LlmConfig existing = repository.findById(config.getId()).orElseThrow();
            if (config.getApiKey() != null && config.getApiKey().contains("...")) {
                config.setApiKey(existing.getApiKey());
            }
        }
        return repository.save(config);
    }

    @DeleteMapping("/{id}")
    public void deleteConfig(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PostMapping("/{id}/toggle")
    public LlmConfig toggleActive(@PathVariable Long id) {
        LlmConfig config = repository.findById(id).orElseThrow();
        config.setActive(!config.isActive());
        return repository.save(config);
    }

    @PostMapping("/{id}/run")
    public String runModel(@PathVariable Long id) {
        briefingService.generateForModel(id);
        return "Model specific briefing run triggered.";
    }
}
