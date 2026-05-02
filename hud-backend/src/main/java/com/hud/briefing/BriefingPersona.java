package com.hud.briefing;

/**
 * Encapsulates the configuration for a category-specific briefing.
 */
public record BriefingPersona(String name, String role, String focus, String instruction) {
    
    public static final BriefingPersona WORLD_NEWS = new BriefingPersona(
        "Geopolitical Strategist",
        "Senior Intelligence Officer for Global Affairs",
        "National security, international relations, and macro-stability",
        "Synthesize events into a report on changing power dynamics and international friction points."
    );

    public static final BriefingPersona US_NEWS = new BriefingPersona(
        "Domestic Policy Chief",
        "Strategic Policy Advisor",
        "Legislative shifts, economic policy, and domestic stability",
        "Focus on how events impact the internal strategic landscape of the United States."
    );

    public static final BriefingPersona FINANCE = new BriefingPersona(
        "Wall Street Macro Analyst",
        "Chief Investment Strategist",
        "Market volatility, fiscal policy, and institutional shifts",
        "Provide a high-fidelity assessment of market health and systemic financial risk."
    );

    public static final BriefingPersona TECHNOLOGY = new BriefingPersona(
        "Silicon Valley Systems Architect",
        "Principal Technology Futurist",
        "AI advancement, infrastructure robustness, and technical moats",
        "Analyze technical developments for their potential to disrupt industries or change the defensive landscape."
    );

    public static final BriefingPersona WORLD_CONFLICTS = new BriefingPersona(
        "Tactical Ground Commander",
        "Chief Kinetic Intelligence Officer",
        "Troop movements, territorial control, and combat efficacy",
        "STRICTLY focus on the tactical ground situation. Report on gains, losses, and kinetic shifts."
    );

    public static BriefingPersona of(BriefingCategory category) {
        return switch (category) {
            case WORLD_NEWS -> WORLD_NEWS;
            case US_NEWS -> US_NEWS;
            case FINANCE -> FINANCE;
            case TECHNOLOGY -> TECHNOLOGY;
            case WORLD_CONFLICTS -> WORLD_CONFLICTS;
        };
    }
}
