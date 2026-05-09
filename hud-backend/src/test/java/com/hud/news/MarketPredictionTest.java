package com.hud.news;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class MarketPredictionTest {

    @Test
    void shouldConstructAndGetSet() {
        MarketPrediction p = new MarketPrediction();
        p.setTicker("AAPL");
        p.setPredictedPrice(150.0);
        p.setActualPrice(155.0);
        p.setRationale("Good news");
        p.setSynthesis("Full report");
        p.setGenerationDate(LocalDate.now());
        p.setTargetDate(LocalDate.now().plusDays(7));
        p.setModelName("Gemma");
        p.setBriefingId(10L);

        assertEquals("AAPL", p.getTicker());
        assertEquals(150.0, p.getPredictedPrice());
        assertEquals(155.0, p.getActualPrice());
        assertEquals("Good news", p.getRationale());
        assertEquals("Full report", p.getSynthesis());
        assertNotNull(p.getGenerationDate());
        assertNotNull(p.getTargetDate());
        assertEquals("Gemma", p.getModelName());
        assertEquals(10L, p.getBriefingId());
    }

    @Test
    void shouldConstructWithFullArgs() {
        LocalDate now = LocalDate.now();
        MarketPrediction p = new MarketPrediction("AAPL", now, now.plusDays(7), 150.0, "Rationale", "Synthesis", "Gemma", 10L);
        
        assertEquals("AAPL", p.getTicker());
        assertEquals(150.0, p.getPredictedPrice());
        assertEquals("Rationale", p.getRationale());
        assertEquals("Gemma", p.getModelName());
    }
}
