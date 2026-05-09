package com.hud.news;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class MetricHistoryTest {

    @Test
    void shouldConstructAndGetSet() {
        MetricHistory h = new MetricHistory();
        LocalDateTime now = LocalDateTime.now();
        h.setTicker("AAPL");
        h.setPrice(150.0);
        h.setChangePercent(1.5);
        h.setTimestamp(now);

        assertEquals("AAPL", h.getTicker());
        assertEquals(150.0, h.getPrice());
        assertEquals(1.5, h.getChangePercent());
        assertEquals(now, h.getTimestamp());
    }

    @Test
    void shouldConstructWithArgs() {
        MetricHistory h = new MetricHistory("AAPL", 150.0, 1.5);
        assertEquals("AAPL", h.getTicker());
        assertEquals(150.0, h.getPrice());
        assertNotNull(h.getTimestamp());
    }
}
