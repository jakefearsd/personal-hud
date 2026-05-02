package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class HnSourceStrategy implements BriefingSourceStrategy {

    private final PlaywrightScraperService scraperService;

    public HnSourceStrategy(PlaywrightScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public List<String> getLinks(String query, int limit) {
        // query is ignored, use high-point stories from hnrss.org
        return scraperService.getLinksFromRss("https://hnrss.org/best?points=100", limit);
    }

    @Override
    public boolean supports(BriefingCategory category) {
        return category == BriefingCategory.TECHNOLOGY;
    }
}
