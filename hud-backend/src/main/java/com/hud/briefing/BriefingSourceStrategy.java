package com.hud.briefing;

import java.util.List;

/**
 * Strategy interface for briefing data sources (GoF Strategy Pattern).
 */
public interface BriefingSourceStrategy {

    /**
     * Discover article links (with their source name + tier) for a category.
     */
    List<SourceLink> getLinks(BriefingCategory category, int limit);

    /**
     * Determine if this strategy handles the given category.
     */
    boolean supports(BriefingCategory category);
}
