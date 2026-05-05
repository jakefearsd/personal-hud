package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TheaterSourceStrategy implements BriefingSourceStrategy {

    private final PlaywrightScraperService scraperService;

    public TheaterSourceStrategy(PlaywrightScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public List<String> getLinks(String query, int limit) {
        List<String> aggregatedLinks = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return aggregatedLinks;
        }

        String[] sources = query.split(",");
        int limitPerSource = Math.max(1, limit / sources.length);

        for (String source : sources) {
            String trimmed = source.trim();
            if (trimmed.startsWith("http")) {
                // RSS feed (like Defense One, War on the Rocks)
                System.out.println("TheaterSourceStrategy: Scraping RSS: " + trimmed);
                aggregatedLinks.addAll(scraperService.getLinksFromRss(trimmed, limitPerSource));
            } else if (trimmed.startsWith("isw-")) {
                // ISW specific parsing
                String iswQuery = trimmed.substring(4);
                System.out.println("TheaterSourceStrategy: Fetching ISW for: " + iswQuery);
                aggregatedLinks.addAll(getIswLinks(iswQuery, limitPerSource));
            }
        }

        return aggregatedLinks.size() > limit ? aggregatedLinks.subList(0, limit) : aggregatedLinks;
    }

    private List<String> getIswLinks(String iswQuery, int limit) {
        List<String> links = scraperService.getIswLinks(15);
        List<String> filtered;
        
        if ("ukraine".equalsIgnoreCase(iswQuery)) {
            filtered = links.stream()
                    .filter(l -> l.contains("offensive-campaign-assessment"))
                    .limit(limit)
                    .collect(Collectors.toList());
            
            if (filtered.isEmpty()) {
                filtered = links.stream()
                        .filter(l -> l.contains("ukraine"))
                        .limit(limit)
                        .collect(Collectors.toList());
            }
        } else if ("mideast".equalsIgnoreCase(iswQuery)) {
            filtered = links.stream()
                    .filter(l -> l.contains("iran-update") || l.contains("israel-hamas-war") || l.contains("middle-east"))
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            filtered = links.stream().limit(limit).collect(Collectors.toList());
        }
        return filtered;
    }

    @Override
    public boolean supports(BriefingCategory category) {
        return category == BriefingCategory.THEATER_UKRAINE || 
               category == BriefingCategory.THEATER_MIDDLE_EAST ||
               category == BriefingCategory.GLOBAL_SITREP;
    }
}
