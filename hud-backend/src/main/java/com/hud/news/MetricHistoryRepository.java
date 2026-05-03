package com.hud.news;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetricHistoryRepository extends JpaRepository<MetricHistory, Long> {
    List<MetricHistory> findByTickerOrderByTimestampAsc(String ticker);
}
