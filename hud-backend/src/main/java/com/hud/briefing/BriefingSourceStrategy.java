package com.hud.briefing;

import java.util.List;

/**
 * Strategy interface for briefing data sources (GoF Strategy Pattern).
 */
public interface BriefingSourceStrategy {
    
    /**
     * Discover article links for a given category/query.
     */
    List<String> getLinks(String query, int limit);

    /**
     * Determine if this strategy handles the given category.
     */
    boolean supports(BriefingCategory category);
}
