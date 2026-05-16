package com.hud;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void briefingExecutorIsBounded() {
        ThreadPoolTaskExecutor executor = config.briefingExecutor();
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(4, executor.getMaxPoolSize());
        assertTrue(executor.getThreadNamePrefix().startsWith("hud-briefing-"));
    }

    @Test
    void scrapeExecutorIsBounded() {
        ThreadPoolTaskExecutor executor = config.scrapeExecutor();
        assertEquals(4, executor.getCorePoolSize());
        assertEquals(8, executor.getMaxPoolSize());
        assertTrue(executor.getThreadNamePrefix().startsWith("hud-scrape-"));
    }
}
