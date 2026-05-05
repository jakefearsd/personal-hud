package com.hud.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BriefingScheduleRepository extends JpaRepository<BriefingSchedule, Long> {
    Optional<BriefingSchedule> findByCategory(BriefingCategory category);
}
