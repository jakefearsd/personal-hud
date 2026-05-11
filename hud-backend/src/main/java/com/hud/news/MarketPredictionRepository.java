package com.hud.news;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketPredictionRepository extends JpaRepository<MarketPrediction, Long> {
    List<MarketPrediction> findByGenerationDate(LocalDate date);
    List<MarketPrediction> findByTargetDateAndActualPriceIsNull(LocalDate date);
    List<MarketPrediction> findByTargetDateLessThanEqualAndActualPriceIsNull(LocalDate date);
    List<MarketPrediction> findByTickerOrderByTargetDateAsc(String ticker);
    Optional<MarketPrediction> findTopByTickerOrderByGenerationDateDesc(String ticker);
    long countByGenerationDate(LocalDate date);
}
