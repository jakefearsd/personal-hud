package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class DatabaseSourceStrategyTest {

    @Mock private NewsSourceRepository sourceRepository;
    @Mock private PlaywrightScraperService scraperService;

    private DatabaseSourceStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new DatabaseSourceStrategy(sourceRepository, scraperService);
    }

    @Test
    void supportsEveryCategory() {
        for (BriefingCategory c : BriefingCategory.values()) {
            assertTrue(strategy.supports(c));
        }
    }

    @Test
    void returnsEmptyWhenNoSourcesConfigured() {
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.WORLD_NEWS))
                .thenReturn(List.of());
        assertTrue(strategy.getLinks(BriefingCategory.WORLD_NEWS, 10).isEmpty());
    }

    @Test
    void resolvesRssSourceAndCarriesNameAndTier() {
        NewsSource bbc = new NewsSource(BriefingCategory.WORLD_NEWS, "BBC World",
                "https://bbc/rss", SourceType.RSS, SourceTier.TIER_1, 100, true);
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.WORLD_NEWS))
                .thenReturn(List.of(bbc));
        when(scraperService.getLinksFromRss(eq("https://bbc/rss"), anyInt()))
                .thenReturn(List.of("https://bbc/article-1"));

        List<SourceLink> links = strategy.getLinks(BriefingCategory.WORLD_NEWS, 10);

        assertEquals(1, links.size());
        assertEquals("https://bbc/article-1", links.get(0).url());
        assertEquals("BBC World", links.get(0).sourceName());
        assertEquals(SourceTier.TIER_1, links.get(0).tier());
    }

    @Test
    void resolvesIswSourceFilteredByUkraineTheaterKeyword() {
        NewsSource isw = new NewsSource(BriefingCategory.THEATER_UKRAINE, "ISW Ukraine",
                "ukraine", SourceType.ISW, SourceTier.TIER_1, 100, true);
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.THEATER_UKRAINE))
                .thenReturn(List.of(isw));
        when(scraperService.getIswLinks(anyInt())).thenReturn(List.of(
                "https://isw/offensive-campaign-assessment-1",
                "https://isw/iran-update-1",
                "https://isw/offensive-campaign-assessment-2"));

        List<SourceLink> links = strategy.getLinks(BriefingCategory.THEATER_UKRAINE, 10);

        assertEquals(2, links.size());
        assertTrue(links.stream().allMatch(l -> l.url().contains("offensive-campaign-assessment")));
        assertEquals("ISW Ukraine", links.get(0).sourceName());
    }

    @Test
    void resolvesCsisSource() {
        NewsSource csis = new NewsSource(BriefingCategory.GLOBAL_SITREP, "CSIS Analysis",
                "https://www.csis.org/analysis", SourceType.CSIS, SourceTier.TIER_1, 100, true);
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.GLOBAL_SITREP))
                .thenReturn(List.of(csis));
        when(scraperService.getCsisLinks(anyInt())).thenReturn(List.of("https://csis/report-1"));

        List<SourceLink> links = strategy.getLinks(BriefingCategory.GLOBAL_SITREP, 10);

        assertEquals(1, links.size());
        verify(scraperService).getCsisLinks(anyInt());
    }

    @Test
    void aggregatesMultipleSourcesAndCapsAtLimit() {
        NewsSource a = new NewsSource(BriefingCategory.FINANCE, "Feed A", "https://a/rss",
                SourceType.RSS, SourceTier.TIER_1, 100, true);
        NewsSource b = new NewsSource(BriefingCategory.FINANCE, "Feed B", "https://b/rss",
                SourceType.RSS, SourceTier.TIER_2, 80, true);
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.FINANCE))
                .thenReturn(List.of(a, b));
        when(scraperService.getLinksFromRss(eq("https://a/rss"), anyInt()))
                .thenReturn(List.of("a1", "a2"));
        when(scraperService.getLinksFromRss(eq("https://b/rss"), anyInt()))
                .thenReturn(List.of("b1", "b2"));

        List<SourceLink> links = strategy.getLinks(BriefingCategory.FINANCE, 3);

        assertEquals(3, links.size());
    }
}
