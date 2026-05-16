package com.hud.briefing;

/**
 * A single discovered article URL together with the originating source's
 * display name and quality tier, so the tier can flow into the map stage.
 */
public record SourceLink(String url, String sourceName, SourceTier tier) {
}
