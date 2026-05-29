package com.hud.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("unit")
class PlaywrightScraperServiceTest {

    @Mock private PlaywrightBrowserManager browserManager;
    @Mock private ContentCleaner contentCleaner;
    @Mock private RssClient rssClient;

    private PlaywrightScraperService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PlaywrightScraperService(browserManager, contentCleaner, rssClient);
    }

    @Test
    void serviceStartsSuccessfully() {
        assertNotNull(service);
    }
}
