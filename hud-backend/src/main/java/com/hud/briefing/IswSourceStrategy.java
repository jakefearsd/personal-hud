package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class IswSourceStrategy implements BriefingSourceStrategy {

    private final PlaywrightScraperService scraperService;

    public IswSourceStrategy(PlaywrightScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public List<String> getLinks(String query, int limit) {
        return scraperService.getIswLinks(limit);
    }

    @Override
    public boolean supports(BriefingCategory category) {
        return category == BriefingCategory.WORLD_CONFLICTS;
    }
}
