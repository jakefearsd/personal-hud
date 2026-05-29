package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The single briefing source strategy. Reads active {@link NewsSource} rows for
 * a category from the database and discovers article links by dispatching on
 * {@link SourceType}. Replaces the previously hardcoded GeneralRss/Theater strategies.
 */
@Component
public class DatabaseSourceStrategy implements BriefingSourceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSourceStrategy.class);

    private final NewsSourceRepository sourceRepository;
    private final PlaywrightScraperService scraperService;

    public DatabaseSourceStrategy(NewsSourceRepository sourceRepository,
                                  PlaywrightScraperService scraperService) {
        this.sourceRepository = sourceRepository;
        this.scraperService = scraperService;
    }

    @Override
    public List<SourceLink> getLinks(BriefingCategory category, int limit) {
        List<NewsSource> sources = new ArrayList<>(
                sourceRepository.findByCategoryAndActiveTrue(category));
        if (sources.isEmpty()) {
            return List.of();
        }
        // Higher-weight sources first, so a truncating limit keeps the best ones.
        sources.sort(Comparator.comparingInt(NewsSource::getWeight).reversed());
        int limitPerSource = Math.max(1, limit / sources.size());

        List<SourceLink> aggregated = new ArrayList<>();
        for (NewsSource source : sources) {
            for (String url : resolveUrls(source, limitPerSource, category)) {
                aggregated.add(new SourceLink(url, source.getName(), source.getTier()));
            }
        }
        return aggregated.size() > limit
                ? new ArrayList<>(aggregated.subList(0, limit))
                : aggregated;
    }

    /**
     * Returns true for every category: this is the sole BriefingSourceStrategy.
     * The supports() contract is retained so BriefingSourceFactory's selection
     * still works and additional strategies can be introduced later.
     */
    @Override
    public boolean supports(BriefingCategory category) {
        return true;
    }

    private List<String> resolveUrls(NewsSource source, int limit, BriefingCategory category) {
        switch (source.getType()) {
            case RSS:
                logger.info("DatabaseSourceStrategy: RSS {}", source.getName());
                return scraperService.getLinksFromRss(source.getUrl(), limit);
            case CSIS:
                logger.info("DatabaseSourceStrategy: CSIS {}", source.getName());
                return scraperService.getCsisLinks(limit);
            case ISW:
                logger.info("DatabaseSourceStrategy: ISW {} for {}", source.getName(), category);
                return scraperService.getIswLinks(limit, category);
            default:
                logger.warn("Unsupported source type {} for {}", source.getType(), source.getName());
                return List.of();
        }
    }
}
