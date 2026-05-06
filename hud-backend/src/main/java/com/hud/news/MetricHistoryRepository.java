package com.hud.news;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MetricHistoryRepository extends JpaRepository<MetricHistory, Long> {
    List<MetricHistory> findByTickerOrderByTimestampAsc(String ticker);
    Optional<MetricHistory> findTopByTickerOrderByTimestampDesc(String ticker);
}
