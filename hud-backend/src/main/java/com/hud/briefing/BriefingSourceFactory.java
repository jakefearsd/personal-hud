package com.hud.briefing;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Factory for BriefingSourceStrategy (GoF Factory/Strategy Orchestrator).
 */
@Component
public class BriefingSourceFactory {

    private final List<BriefingSourceStrategy> strategies;

    public BriefingSourceFactory(List<BriefingSourceStrategy> strategies) {
        this.strategies = strategies;
    }

    public BriefingSourceStrategy getStrategy(BriefingCategory category) {
        return strategies.stream()
                .filter(s -> s.supports(category))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No strategy found for category: " + category));
    }
}
