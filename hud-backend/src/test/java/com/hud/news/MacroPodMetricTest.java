package com.hud.news;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MacroPodMetricTest {
    @Test
    void testGettersAndSettersAndEquals() {
        MacroPodMetric metric1 = new MacroPodMetric("ticker", "label", 1.0, 50.0, 0.5);
        MacroPodMetric metric2 = new MacroPodMetric("ticker", "label", 1.0, 50.0, 0.5);
        
        assertEquals(metric1, metric2);
        assertEquals(metric1.hashCode(), metric2.hashCode());
        assertNotNull(metric1.toString());
        
        metric1.setTicker("ticker2");
        assertNotEquals(metric1, metric2);
        assertEquals("ticker2", metric1.getTicker());
        
        metric1.setLabel("label2");
        assertEquals("label2", metric1.getLabel());
        
        metric1.setCurrentValue(2.0);
        assertEquals(2.0, metric1.getCurrentValue());
        
        metric1.setHistoricalPercentile(60.0);
        assertEquals(60.0, metric1.getHistoricalPercentile());
        
        metric1.setChangePercent(1.0);
        assertEquals(1.0, metric1.getChangePercent());
        
        MacroPodMetric empty = new MacroPodMetric();
        assertNull(empty.getTicker());
    }
}
