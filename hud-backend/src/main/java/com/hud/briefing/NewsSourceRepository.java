package com.hud.briefing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {

    /** Active sources for one category, used by DatabaseSourceStrategy. */
    List<NewsSource> findByCategoryAndActiveTrue(BriefingCategory category);
}
