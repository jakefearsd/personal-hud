package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IswSourceStrategy implements BriefingSourceStrategy {

    private final PlaywrightScraperService scraperService;

    public IswSourceStrategy(PlaywrightScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public List<String> getLinks(String query, int limit) {
        System.out.println("IswSourceStrategy: Searching for '" + query + "' links...");
        String iswSelector = "a[href*='offensive-campaign-assessment'], " +
                             "a[href*='conflict-update'], " +
                             "a[href*='ukraine-conflict-updates'], " +
                             "a[href*='iran-update'], " +
                             "a[href*='israel-hamas-war-update']";
        
        List<String> links = scraperService.getIswLinks(15);
        System.out.println("IswSourceStrategy: Total raw links found: " + links.size());
        
        List<String> filtered;
        if ("ukraine".equalsIgnoreCase(query)) {
            filtered = links.stream()
                    .filter(l -> l.contains("offensive-campaign-assessment") || l.contains("ukraine"))
                    .limit(limit)
                    .collect(Collectors.toList());
        } else if ("mideast".equalsIgnoreCase(query)) {
            filtered = links.stream()
                    .filter(l -> l.contains("iran-update") || l.contains("israel-hamas-war"))
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            filtered = links.stream().limit(limit).collect(Collectors.toList());
        }
        
        System.out.println("IswSourceStrategy: Returning " + filtered.size() + " filtered links.");
        return filtered;
    }

    @Override
    public boolean supports(BriefingCategory category) {
        return category == BriefingCategory.THEATER_UKRAINE || 
               category == BriefingCategory.THEATER_MIDDLE_EAST ||
               category == BriefingCategory.GLOBAL_SITREP;
    }
}
