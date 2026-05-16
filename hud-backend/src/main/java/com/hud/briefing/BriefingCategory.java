package com.hud.briefing;

/**
 * Briefing categories. Source feeds are no longer carried here — they live in
 * the news_sources table (see NewsSource / DatabaseSourceStrategy).
 */
public enum BriefingCategory {
    WORLD_NEWS(BriefingPersona.WORLD_NEWS),
    US_NEWS(BriefingPersona.US_NEWS),
    FINANCE(BriefingPersona.FINANCE),
    TECHNOLOGY(BriefingPersona.TECHNOLOGY),
    GLOBAL_SITREP(BriefingPersona.GLOBAL_SITREP),
    THEATER_UKRAINE(BriefingPersona.THEATER_UKRAINE),
    THEATER_MIDDLE_EAST(BriefingPersona.THEATER_MIDDLE_EAST);

    private final BriefingPersona persona;

    BriefingCategory(BriefingPersona persona) {
        this.persona = persona;
    }

    public BriefingPersona getPersona() {
        return persona;
    }
}
