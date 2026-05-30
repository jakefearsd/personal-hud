package com.hud.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {
    List<PipelineRun> findTop50ByOrderByStartTimeDesc();
    boolean existsByCategoryAndStatus(BriefingCategory category, PipelineStatus status);
    List<PipelineRun> findByStatus(PipelineStatus status);
}
