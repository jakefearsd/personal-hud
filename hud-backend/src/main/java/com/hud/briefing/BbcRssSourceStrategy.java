package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class BbcRssSourceStrategy implements BriefingSourceStrategy {

    private final PlaywrightScraperService scraperService;

    public BbcRssSourceStrategy(PlaywrightScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public List<String> getLinks(String query, int limit) {
        return scraperService.getLinksFromRss(query, limit);
    }

    @Override
    public boolean supports(BriefingCategory category) {
        // Fallback for general categories
        return category == BriefingCategory.WORLD_NEWS || 
               category == BriefingCategory.US_NEWS || 
               category == BriefingCategory.FINANCE;
    }
}
