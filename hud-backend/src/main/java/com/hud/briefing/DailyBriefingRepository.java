package com.hud.briefing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface DailyBriefingRepository extends JpaRepository<DailyBriefing, Long> {
    
    @Query("SELECT b FROM DailyBriefing b WHERE CAST(b.generatedAt AS date) = CURRENT_DATE")
    List<DailyBriefing> findLatestToday();

    @Query("SELECT b FROM DailyBriefing b WHERE CAST(b.generatedAt AS date) = CURRENT_DATE AND b.modelName = :modelName")
    List<DailyBriefing> findByModelToday(String modelName);

    void deleteByCategoryAndModelNameAndGeneratedAtAfter(BriefingCategory category, String modelName, java.time.LocalDateTime after);
}
