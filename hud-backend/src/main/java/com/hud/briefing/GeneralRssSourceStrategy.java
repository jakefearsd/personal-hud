package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeneralRssSourceStrategy implements BriefingSourceStrategy {

    private final PlaywrightScraperService scraperService;

    public GeneralRssSourceStrategy(PlaywrightScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public List<String> getLinks(String query, int limit) {
        List<String> aggregatedLinks = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return aggregatedLinks;
        }

        String[] feeds = query.split(",");
        // Divide the limit across feeds to ensure diverse sourcing
        int limitPerFeed = Math.max(1, limit / feeds.length);

        for (String feed : feeds) {
            String trimmedFeed = feed.trim();
            if (!trimmedFeed.isEmpty()) {
                System.out.println("GeneralRssSourceStrategy: Scraping RSS: " + trimmedFeed);
                aggregatedLinks.addAll(scraperService.getLinksFromRss(trimmedFeed, limitPerFeed));
            }
        }

        // Return up to 'limit' total links
        return aggregatedLinks.size() > limit ? aggregatedLinks.subList(0, limit) : aggregatedLinks;
    }

    @Override
    public boolean supports(BriefingCategory category) {
        return category == BriefingCategory.WORLD_NEWS || 
               category == BriefingCategory.US_NEWS || 
               category == BriefingCategory.FINANCE ||
               category == BriefingCategory.TECHNOLOGY;
    }
}
