package com.hud.briefing;

/**
 * Configuration for a BriefingProcessor.
 */
public record BriefingProcessorConfiguration(
    int linkLimit,
    int minRequiredChars,
    int scrapeDepth
) {
    public static final BriefingProcessorConfiguration STANDARD = 
        new BriefingProcessorConfiguration(15, 500, 0);
    
    public static final BriefingProcessorConfiguration DEEP_DIVE = 
        new BriefingProcessorConfiguration(15, 2500, 1);
    
    public static final BriefingProcessorConfiguration GLOBAL_SITREP = 
        new BriefingProcessorConfiguration(25, 2500, 1);
}
