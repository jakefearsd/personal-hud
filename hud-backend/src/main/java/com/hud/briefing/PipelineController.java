package com.hud.briefing;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    private final PipelineRunRepository repository;

    public PipelineController(PipelineRunRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PipelineRun> getRecentRuns() {
        return repository.findTop50ByOrderByStartTimeDesc();
    }

    @DeleteMapping
    public void flushAllRuns() {
        repository.deleteAll();
    }
}
