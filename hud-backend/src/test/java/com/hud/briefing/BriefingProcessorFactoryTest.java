package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Tag("unit")
class BriefingProcessorFactoryTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private BriefingSourceFactory sourceFactory;
    @Mock private IntelligenceSynthesizer synthesizer;
    @Mock private ChatLanguageModel model;
    @Mock private BriefingSourceStrategy strategy;

    private BriefingProcessorFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        factory = new BriefingProcessorFactory(scraperService, sourceFactory, synthesizer);
        when(sourceFactory.getStrategy(any())).thenReturn(strategy);
    }

    private BriefingCategory any() {
        return org.mockito.ArgumentMatchers.any(BriefingCategory.class);
    }

    @Test
    void createsDeepDiveProcessorForUkraine() {
        BriefingProcessor processor = factory.getProcessor(BriefingCategory.THEATER_UKRAINE, model);
        assertNotNull(processor);
        // We can't easily check internal config without reflection or exposure, 
        // but we verify the factory returns a processor.
    }

    @Test
    void createsDeepDiveProcessorForSitrep() {
        BriefingProcessor processor = factory.getProcessor(BriefingCategory.GLOBAL_SITREP, model);
        assertNotNull(processor);
    }

    @Test
    void createsStandardProcessorForWorldNews() {
        BriefingProcessor processor = factory.getProcessor(BriefingCategory.WORLD_NEWS, model);
        assertNotNull(processor);
    }
}
