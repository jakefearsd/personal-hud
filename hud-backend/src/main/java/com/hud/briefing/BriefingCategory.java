package com.hud.briefing;

import static com.hud.briefing.BriefingPersona.*;

public enum BriefingCategory {
    WORLD_NEWS(
        "https://feeds.bbci.co.uk/news/world/rss.xml,https://www.aljazeera.com/xml/rss/all.xml,https://rss.nytimes.com/services/xml/rss/nyt/World.xml", 
        BriefingPersona.WORLD_NEWS
    ),
    US_NEWS(
        "https://feeds.bbci.co.uk/news/world/us_and_canada/rss.xml,https://rss.nytimes.com/services/xml/rss/nyt/US.xml,https://feeds.npr.org/1001/rss.xml", 
        BriefingPersona.US_NEWS
    ),
    FINANCE(
        "https://feeds.bbci.co.uk/news/business/rss.xml,https://search.cnbc.com/rs/search/combinedcms/view.xml?id=10000664,https://feeds.a.dj.com/rss/WSJcomUSBusiness.xml,https://finance.yahoo.com/news/rss", 
        BriefingPersona.FINANCE
    ),
    TECHNOLOGY(
        "https://hnrss.org/best?points=100,https://techcrunch.com/feed/,https://www.theverge.com/rss/index.xml", 
        BriefingPersona.TECHNOLOGY
    ),
    GLOBAL_SITREP(
        "https://warontherocks.com/feed/,https://www.defenseone.com/rss/all/,isw-global,csis-global", 
        BriefingPersona.GLOBAL_SITREP
    ),
    THEATER_UKRAINE(
        "https://warontherocks.com/feed/,https://www.defenseone.com/rss/all/,isw-ukraine,csis-global", 
        BriefingPersona.THEATER_UKRAINE
    ),
    THEATER_MIDDLE_EAST(
        "https://warontherocks.com/feed/,https://www.defenseone.com/rss/all/,isw-mideast,csis-global", 
        BriefingPersona.THEATER_MIDDLE_EAST
    );

    private final String defaultQuery;
    private final BriefingPersona persona;

    BriefingCategory(String defaultQuery, BriefingPersona persona) {
        this.defaultQuery = defaultQuery;
        this.persona = persona;
    }

    public String getDefaultQuery() {
        return defaultQuery;
    }

    public BriefingPersona getPersona() {
        return persona;
    }
}
