package com.hud.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MarkerRemovalCleanerTest {

    private MarkerRemovalCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new MarkerRemovalCleaner();
    }

    @Test
    void cleansIsWMetaData() {
        String raw = "Vital intelligence analysis. Click here to see ISW support options.";
        // Note: Marker must be after 1000 chars in current implementation to be safe from false positives
        StringBuilder sb = new StringBuilder("Valid content ".repeat(100));
        sb.append("Endnotes: Source 1");
        
        String cleaned = cleaner.clean(sb.toString());
        assertFalse(cleaned.contains("Endnotes"));
        assertTrue(cleaned.contains("Valid content"));
    }

    @Test
    void handleNullOrEmpty() {
        assertEquals("", cleaner.clean(null));
        assertEquals("", cleaner.clean("   "));
    }
}
