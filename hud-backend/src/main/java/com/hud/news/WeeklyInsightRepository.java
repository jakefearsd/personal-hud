package com.hud.news;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface WeeklyInsightRepository extends JpaRepository<WeeklyInsight, UUID> {
    Optional<WeeklyInsight> findTopByOrderByGeneratedAtDesc();
}
