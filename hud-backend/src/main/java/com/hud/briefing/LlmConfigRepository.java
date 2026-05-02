package com.hud.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LlmConfigRepository extends JpaRepository<LlmConfig, Long> {
    List<LlmConfig> findByActiveTrue();
    java.util.Optional<LlmConfig> findByName(String name);
}
