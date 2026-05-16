package com.hud.briefing;

import java.util.List;

/**
 * Compact, transient map-stage output for one scraped document. Small enough
 * that all digests for a category run fit into a single reduce-stage prompt —
 * this is what lifts the briefing breadth past the context-window limit.
 *
 * corroborationCount / contributingSources are set to 1 / [sourceName] by the
 * digester and updated by {@link DigestDeduplicator} when digests are merged.
 */
public record ArticleDigest(
        String sourceUrl,
        String sourceName,
        SourceTier tier,
        String headline,
        String summary,
        List<String> keyFacts,
        List<String> entities,
        List<String> datedEvents,
        double relevanceScore,
        int corroborationCount,
        List<String> contributingSources) {
}
