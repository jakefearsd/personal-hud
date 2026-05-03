package com.hud.briefing;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class EntityCoverageTest {

    @Test
    void testAppUser() {
        AppUser user = new AppUser("u", "p", "r");
        user.setId(1L);
        assertEquals(1L, user.getId());
        assertEquals("u", user.getUsername());
        assertEquals("p", user.getPassword());
        assertEquals("r", user.getRole());
        
        user.setUsername("u2");
        user.setPassword("p2");
        user.setRole("r2");
        assertEquals("u2", user.getUsername());
    }

    @Test
    void testDailyBriefing() {
        LocalDateTime now = LocalDateTime.now();
        DailyBriefing b = new DailyBriefing(now, BriefingCategory.FINANCE, "m", "content");
        b.setId(1L);
        assertEquals(1L, b.getId());
        assertEquals(now, b.getGeneratedAt());
        assertEquals(BriefingCategory.FINANCE, b.getCategory());
        assertEquals("m", b.getModelName());
        assertEquals("content", b.getMarkdownContent());
        
        b.setMarkdownContent("new");
        assertEquals("new", b.getMarkdownContent());
    }

    @Test
    void testLlmConfig() {
        LlmConfig c = new LlmConfig("n", LlmProvider.GEMINI, "m", true);
        c.setId(1L);
        c.setBaseUrl("url");
        c.setApiKey("key");
        c.setNumCtx(100);
        LocalDateTime now = LocalDateTime.now();
        c.setUpdatedAt(now);

        assertEquals(1L, c.getId());
        assertEquals("n", c.getName());
        assertEquals(LlmProvider.GEMINI, c.getProvider());
        assertEquals("m", c.getModelName());
        assertTrue(c.isActive());
        assertEquals("url", c.getBaseUrl());
        assertEquals("key", c.getApiKey());
        assertEquals(100, c.getNumCtx());
        assertEquals(now, c.getUpdatedAt());
        
        c.setName("n2");
        c.setProvider(LlmProvider.OLLAMA);
        c.setActive(false);
        assertFalse(c.isActive());
        
        c.onUpdate();
        assertNotNull(c.getUpdatedAt());
    }

    @Test
    void testPipelineRun() {
        LocalDateTime start = LocalDateTime.now();
        PipelineRun r = new PipelineRun(BriefingCategory.WORLD_NEWS, "m", PipelineStatus.PENDING, start);
        r.setId(1L);
        r.setEndTime(start.plusSeconds(10));
        r.setErrorMessage("err");

        assertEquals(1L, r.getId());
        assertEquals(BriefingCategory.WORLD_NEWS, r.getCategory());
        assertEquals("m", r.getModelName());
        assertEquals(PipelineStatus.PENDING, r.getStatus());
        assertEquals(start, r.getStartTime());
        assertEquals(start.plusSeconds(10), r.getEndTime());
        assertEquals("err", r.getErrorMessage());
        
        r.setStatus(PipelineStatus.SUCCESS);
        assertEquals(PipelineStatus.SUCCESS, r.getStatus());
    }
}
