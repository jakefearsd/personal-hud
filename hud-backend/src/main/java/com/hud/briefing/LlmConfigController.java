package com.hud.briefing;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/config/brains")
public class LlmConfigController {

    private final LlmConfigRepository repository;

    public LlmConfigController(LlmConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<LlmConfig> getAllConfigs() {
        return repository.findAll();
    }

    @PostMapping
    public LlmConfig saveConfig(@RequestBody LlmConfig config) {
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
}
