package com.hud.briefing;

/**
 * Editorial quality tier of a source. TIER_1 (ordinal 0) is the highest;
 * ordinal order is used to rank and order digests in the reduce stage.
 */
public enum SourceTier {
    TIER_1,
    TIER_2,
    TIER_3
}
