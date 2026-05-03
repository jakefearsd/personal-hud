package com.hud.news;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MarketEventRepository extends JpaRepository<MarketEvent, Long> {
    List<MarketEvent> findByTickerOrderByTimestampDesc(String ticker);
}
