package com.hud.news;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MacroMetricRepository extends JpaRepository<MacroMetric, String> {
    List<MacroMetric> findAllByOrderByLabelAsc();
}
