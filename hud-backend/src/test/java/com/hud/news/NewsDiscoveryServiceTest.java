package com.hud.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NewsDiscoveryServiceTest {
    @Test
    void testDiscoverRecentEvents() {
        NewsDiscoveryService service = new NewsDiscoveryService();
        List<String> links = service.discoverRecentEvents();
        assertNotNull(links);
        assertFalse(links.isEmpty());
    }
}
