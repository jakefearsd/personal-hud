package com.hud.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DailyBriefingRepository extends JpaRepository<DailyBriefing, Long> {
    List<DailyBriefing> findByBriefingDate(LocalDate date);
    void deleteByBriefingDateAndCategory(LocalDate date, BriefingCategory category);
}
