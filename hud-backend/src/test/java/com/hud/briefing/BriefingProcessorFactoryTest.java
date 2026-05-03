package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class BriefingProcessorFactoryTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private BriefingSourceFactory sourceFactory;
    @Mock private IntelligenceSynthesizer synthesizer;
    @Mock private ChatLanguageModel model;

    private BriefingProcessorFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        factory = new BriefingProcessorFactory(scraperService, sourceFactory, synthesizer);
    }

    @Test
    void shouldReturnDeepDiveProcessorForUkraine() {
        BriefingProcessor p = factory.getProcessor(BriefingCategory.THEATER_UKRAINE, model);
        assertTrue(p instanceof DeepDiveBriefingProcessor);
    }

    @Test
    void shouldReturnDeepDiveProcessorForGlobalSitrep() {
        BriefingProcessor p = factory.getProcessor(BriefingCategory.GLOBAL_SITREP, model);
        assertTrue(p instanceof DeepDiveBriefingProcessor);
    }

    @Test
    void shouldReturnStandardProcessorForWorldNews() {
        BriefingProcessor p = factory.getProcessor(BriefingCategory.WORLD_NEWS, model);
        assertTrue(p instanceof StandardBriefingProcessor);
    }
}
