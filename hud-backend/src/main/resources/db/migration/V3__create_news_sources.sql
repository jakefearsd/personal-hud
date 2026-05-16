create table news_sources (
    id bigserial not null,
    active boolean not null,
    weight integer not null,
    name varchar(255) not null,
    url varchar(1024) not null,
    category varchar(255) not null check (category in ('WORLD_NEWS','US_NEWS','FINANCE','TECHNOLOGY','GLOBAL_SITREP','THEATER_UKRAINE','THEATER_MIDDLE_EAST')),
    source_type varchar(255) not null check (source_type in ('RSS','ISW','CSIS')),
    tier varchar(255) not null check (tier in ('TIER_1','TIER_2','TIER_3')),
    primary key (id)
);

insert into news_sources (category, name, url, source_type, tier, weight, active) values
    ('WORLD_NEWS', 'BBC World', 'https://feeds.bbci.co.uk/news/world/rss.xml', 'RSS', 'TIER_1', 100, true),
    ('WORLD_NEWS', 'Al Jazeera', 'https://www.aljazeera.com/xml/rss/all.xml', 'RSS', 'TIER_2', 80, true),
    ('WORLD_NEWS', 'NYT World', 'https://rss.nytimes.com/services/xml/rss/nyt/World.xml', 'RSS', 'TIER_1', 100, true),

    ('US_NEWS', 'BBC US & Canada', 'https://feeds.bbci.co.uk/news/world/us_and_canada/rss.xml', 'RSS', 'TIER_1', 100, true),
    ('US_NEWS', 'NYT US', 'https://rss.nytimes.com/services/xml/rss/nyt/US.xml', 'RSS', 'TIER_1', 100, true),
    ('US_NEWS', 'NPR News', 'https://feeds.npr.org/1001/rss.xml', 'RSS', 'TIER_1', 90, true),

    ('FINANCE', 'BBC Business', 'https://feeds.bbci.co.uk/news/business/rss.xml', 'RSS', 'TIER_1', 100, true),
    ('FINANCE', 'CNBC', 'https://search.cnbc.com/rs/search/combinedcms/view.xml?id=10000664', 'RSS', 'TIER_2', 80, true),
    ('FINANCE', 'WSJ US Business', 'https://feeds.a.dj.com/rss/WSJcomUSBusiness.xml', 'RSS', 'TIER_1', 100, true),
    ('FINANCE', 'Yahoo Finance', 'https://finance.yahoo.com/news/rss', 'RSS', 'TIER_3', 60, true),

    ('TECHNOLOGY', 'Hacker News', 'https://hnrss.org/best?points=100', 'RSS', 'TIER_3', 60, true),
    ('TECHNOLOGY', 'TechCrunch', 'https://techcrunch.com/feed/', 'RSS', 'TIER_2', 80, true),
    ('TECHNOLOGY', 'The Verge', 'https://www.theverge.com/rss/index.xml', 'RSS', 'TIER_2', 80, true),

    ('GLOBAL_SITREP', 'War on the Rocks', 'https://warontherocks.com/feed/', 'RSS', 'TIER_2', 80, true),
    ('GLOBAL_SITREP', 'Defense One', 'https://www.defenseone.com/rss/all/', 'RSS', 'TIER_2', 80, true),
    ('GLOBAL_SITREP', 'ISW Global', 'global', 'ISW', 'TIER_1', 100, true),
    ('GLOBAL_SITREP', 'CSIS Analysis', 'https://www.csis.org/analysis', 'CSIS', 'TIER_1', 100, true),

    ('THEATER_UKRAINE', 'War on the Rocks', 'https://warontherocks.com/feed/', 'RSS', 'TIER_2', 80, true),
    ('THEATER_UKRAINE', 'Defense One', 'https://www.defenseone.com/rss/all/', 'RSS', 'TIER_2', 80, true),
    ('THEATER_UKRAINE', 'ISW Ukraine', 'ukraine', 'ISW', 'TIER_1', 100, true),
    ('THEATER_UKRAINE', 'CSIS Analysis', 'https://www.csis.org/analysis', 'CSIS', 'TIER_1', 100, true),

    ('THEATER_MIDDLE_EAST', 'War on the Rocks', 'https://warontherocks.com/feed/', 'RSS', 'TIER_2', 80, true),
    ('THEATER_MIDDLE_EAST', 'Defense One', 'https://www.defenseone.com/rss/all/', 'RSS', 'TIER_2', 80, true),
    ('THEATER_MIDDLE_EAST', 'ISW Mideast', 'mideast', 'ISW', 'TIER_1', 100, true),
    ('THEATER_MIDDLE_EAST', 'CSIS Analysis', 'https://www.csis.org/analysis', 'CSIS', 'TIER_1', 100, true);
