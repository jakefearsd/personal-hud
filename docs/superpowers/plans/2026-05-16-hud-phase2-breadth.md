# HUD Phase 2 — DB-Backed Tiered Sources & Map-Reduce Briefings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace hardcoded news feeds with database-backed, tiered sources (with full admin UI), and replace the "concatenate raw text into one prompt" briefing pipeline with a map-reduce architecture so briefing breadth is no longer capped by the LLM context window.

**Architecture:** Sources move from the `BriefingCategory` enum into a `NewsSource` JPA entity seeded by a Flyway migration; a single `DatabaseSourceStrategy` reads them and dispatches scraping by `SourceType`. The briefing pipeline gains a **map** stage (`DocumentDigester` turns each scraped document into a compact `ArticleDigest` via one bounded LLM call), a **dedup** stage (`DigestDeduplicator` merges near-duplicate digests by entity overlap), and a **reduce** stage (`IntelligenceSynthesizer` synthesizes over the digest list instead of raw text). Existing strategy/factory structure and persona prompts are preserved.

**Tech Stack:** Spring Boot 3.2.5, Java 21, Maven, PostgreSQL, Flyway, LangChain4j, JUnit 5, Mockito, Testcontainers, React 19 / Vite / TypeScript, Vitest, lucide-react.

**Source spec:** `docs/superpowers/specs/2026-05-16-hud-stabilization-breadth-design.md` (Part B, items B1 and B2).

---

## Deviations from the design spec (read first)

Two deliberate, scoped decisions differ from the approved design. They are called out here so the reviewer is not surprised:

1. **One `DatabaseSourceStrategy` instead of refactoring both `GeneralRssSourceStrategy` and `TheaterSourceStrategy`.** Once both strategies read from `NewsSource` rows and dispatch by `SourceType`, they become identical except for which categories they claim. A single strategy that `supports()` all categories removes that duplication while preserving the strategy/factory pattern (`BriefingSourceFactory` still selects by `supports()`; more strategies can be added later). The two old strategy classes and their tests are deleted.
2. **`SourceType` enum is `RSS`, `ISW`, `CSIS` — `HTML_INDEX` is omitted.** No current or seeded source needs it, and there is no generic HTML-index scraper in `PlaywrightScraperService`. Adding `HTML_INDEX` to the UI without a working scraper would be a dead option. It can be added in a later phase when a source requires it, together with the scraper.

`ArticleDigest` is implemented as a **transient Java record** (the open question in the spec). It is produced by the map stage and consumed by the reduce stage within a single run; it is never persisted. `PipelineRun` is **not** changed in this phase — the `documentsScraped`/`tokensConsumed`/`budgetCapped` columns belong to Phase 3 (B4).

---

## File Structure

| File | Responsibility | Tasks |
|------|----------------|-------|
| `hud-backend/.../briefing/SourceType.java` | Enum: `RSS`, `ISW`, `CSIS` | 1 |
| `hud-backend/.../briefing/SourceTier.java` | Enum: `TIER_1`, `TIER_2`, `TIER_3` | 1 |
| `hud-backend/.../briefing/NewsSource.java` | JPA entity for a configurable source | 1 |
| `hud-backend/.../briefing/NewsSourceRepository.java` | Repository for `NewsSource` | 1 |
| `hud-backend/src/main/resources/db/migration/V3__create_news_sources.sql` | Table + seed | 2 |
| `hud-backend/.../briefing/SourceLink.java` | Record: a discovered link + its source name/tier | 3 |
| `hud-backend/.../briefing/DatabaseSourceStrategy.java` | DB-backed source strategy | 3 |
| `hud-backend/.../briefing/BriefingSourceStrategy.java` | Interface — signature change | 3 |
| `hud-backend/.../briefing/GeneralRssSourceStrategy.java` | **Deleted** | 3 |
| `hud-backend/.../briefing/TheaterSourceStrategy.java` | **Deleted** | 3 |
| `hud-backend/.../briefing/BriefingCategory.java` | Remove `defaultQuery` | 3 |
| `hud-backend/.../briefing/BriefingProcessor.java` | Template method — link type, then map-reduce | 3, 6 |
| `hud-backend/.../briefing/StandardBriefingProcessor.java` | Standard processor | 3, 6 |
| `hud-backend/.../briefing/DeepDiveBriefingProcessor.java` | Deep-dive processor | 3, 6 |
| `hud-backend/.../briefing/BriefingProcessorFactory.java` | Builds processors | 3, 6 |
| `hud-backend/.../briefing/AutomatedBriefingService.java` | Drop `query` arg, call `process()` | 3 |
| `hud-backend/.../briefing/ArticleDigest.java` | Record: map-stage output | 4 |
| `hud-backend/.../briefing/DocumentDigester.java` | Map stage: document → `ArticleDigest` | 4 |
| `hud-backend/.../briefing/DigestDeduplicator.java` | Dedup stage: merge near-duplicates | 5 |
| `hud-backend/.../briefing/IntelligenceSynthesizer.java` | Reduce stage: synthesize over digests | 6 |
| `hud-backend/.../briefing/NewsSourceController.java` | Admin CRUD REST API | 7 |
| `hud-frontend/src/components/types.ts` | `NewsSource` / `SourceType` / `SourceTier` types | 8 |
| `hud-frontend/src/components/SourcesConfig.tsx` | Admin UI for sources | 8 |
| `hud-frontend/src/components/SourcesConfig.test.tsx` | Component test | 8 |
| `hud-frontend/src/components/ConfigView.tsx` | Mount `SourcesConfig` | 8 |
| Backend test files | Updated for new signatures | 3, 4, 5, 6, 7 |

Task order is dependency-driven: data model → migration → DB-backed source layer (build green) → digester → deduplicator → map-reduce wiring (build green) → REST API → frontend.

---

## Task 1: Source data model — enums, entity, repository

**Files:**
- Create: `hud-backend/src/main/java/com/hud/briefing/SourceType.java`
- Create: `hud-backend/src/main/java/com/hud/briefing/SourceTier.java`
- Create: `hud-backend/src/main/java/com/hud/briefing/NewsSource.java`
- Create: `hud-backend/src/main/java/com/hud/briefing/NewsSourceRepository.java`

- [ ] **Step 1: Create the `SourceType` enum**

Create `hud-backend/src/main/java/com/hud/briefing/SourceType.java`:
```java
package com.hud.briefing;

/**
 * How a {@link NewsSource}'s links are discovered.
 * RSS  — the url is an RSS/Atom feed parsed for article links.
 * ISW  — Institute for the Study of War; the url holds a theater keyword
 *        ("ukraine", "mideast", "global") used to filter the publications index.
 * CSIS — Center for Strategic and International Studies analysis index.
 */
public enum SourceType {
    RSS,
    ISW,
    CSIS
}
```

- [ ] **Step 2: Create the `SourceTier` enum**

Create `hud-backend/src/main/java/com/hud/briefing/SourceTier.java`:
```java
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
```

- [ ] **Step 3: Create the `NewsSource` entity**

Create `hud-backend/src/main/java/com/hud/briefing/NewsSource.java`:
```java
package com.hud.briefing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A configurable intelligence source. Replaces the feed lists that were
 * previously hardcoded in the BriefingCategory enum and the scraper strategies.
 */
@Entity
@Table(name = "news_sources")
public class NewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BriefingCategory category;

    @Column(nullable = false)
    private String name;

    /** RSS feed URL, or — for ISW — a theater keyword (ukraine/mideast/global). */
    @Column(nullable = false, length = 1024)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceTier tier;

    /** Selection priority; higher-weight sources are kept first when a limit truncates. */
    @Column(nullable = false)
    private int weight;

    @Column(nullable = false)
    private boolean active;

    public NewsSource() {}

    public NewsSource(BriefingCategory category, String name, String url,
                      SourceType type, SourceTier tier, int weight, boolean active) {
        this.category = category;
        this.name = name;
        this.url = url;
        this.type = type;
        this.tier = tier;
        this.weight = weight;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BriefingCategory getCategory() { return category; }
    public void setCategory(BriefingCategory category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public SourceType getType() { return type; }
    public void setType(SourceType type) { this.type = type; }

    public SourceTier getTier() { return tier; }
    public void setTier(SourceTier tier) { this.tier = tier; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

- [ ] **Step 4: Create the repository**

Create `hud-backend/src/main/java/com/hud/briefing/NewsSourceRepository.java`:
```java
package com.hud.briefing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {

    /** Active sources for one category, used by DatabaseSourceStrategy. */
    List<NewsSource> findByCategoryAndActiveTrue(BriefingCategory category);
}
```

- [ ] **Step 5: Compile to confirm the entity is valid**

Run: `mvn -q -pl hud-backend compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/SourceType.java \
  hud-backend/src/main/java/com/hud/briefing/SourceTier.java \
  hud-backend/src/main/java/com/hud/briefing/NewsSource.java \
  hud-backend/src/main/java/com/hud/briefing/NewsSourceRepository.java
git commit -m "feat: add NewsSource entity for DB-backed intelligence sources"
```

---

## Task 2: V3 migration — `news_sources` table and seed

**Files:**
- Create: `hud-backend/src/main/resources/db/migration/V3__create_news_sources.sql`

- [ ] **Step 1: Create the migration**

Create `hud-backend/src/main/resources/db/migration/V3__create_news_sources.sql`. The column types and `check` constraint style match `V1__baseline.sql`. The seed rows reproduce the feeds previously hardcoded in the `BriefingCategory` enum, with assigned tiers (major wires/agencies = `TIER_1`, analysis sites = `TIER_2`, supplementary = `TIER_3`):
```sql
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
```

- [ ] **Step 2: Run the migration drift test**

Run: `mvn test -pl hud-backend -Pintegration -Dtest=MigrationIntegrationTest`
Expected: PASS — Flyway applies V1 + V2 + V3 against Testcontainers Postgres, then `ddl-auto=validate` confirms the `NewsSource` entity matches the migrated `news_sources` table. (Requires Docker.) If it fails with a Hibernate `SchemaManagementException`, a column name/type in the entity and the migration disagree — reconcile them.

- [ ] **Step 3: Commit**

```bash
git add hud-backend/src/main/resources/db/migration/V3__create_news_sources.sql
git commit -m "feat: add news_sources migration with seeded tiered feeds"
```

---

## Task 3: DB-backed source layer (replaces hardcoded feeds)

This task is a coordinated refactor: it changes the `BriefingSourceStrategy` interface, introduces the DB-backed strategy, deletes the two hardcoded strategies, removes `BriefingCategory.defaultQuery`, and adapts the processor chain. The build is green again at the final step. The reduce stage still concatenates raw text — map-reduce comes in Task 6.

**Files:**
- Create: `hud-backend/src/main/java/com/hud/briefing/SourceLink.java`
- Create: `hud-backend/src/main/java/com/hud/briefing/DatabaseSourceStrategy.java`
- Create: `hud-backend/src/test/java/com/hud/briefing/DatabaseSourceStrategyTest.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/BriefingSourceStrategy.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/BriefingCategory.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/BriefingProcessor.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/StandardBriefingProcessor.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/DeepDiveBriefingProcessor.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/BriefingProcessorFactory.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/AutomatedBriefingService.java`
- Delete: `hud-backend/src/main/java/com/hud/briefing/GeneralRssSourceStrategy.java`
- Delete: `hud-backend/src/main/java/com/hud/briefing/TheaterSourceStrategy.java`
- Delete: `hud-backend/src/test/java/com/hud/briefing/GeneralRssSourceStrategyTest.java`
- Delete: `hud-backend/src/test/java/com/hud/briefing/TheaterSourceStrategyTest.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/BriefingProcessorTest.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/DeepDiveBriefingProcessorTest.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/AutomatedBriefingServiceTest.java`

- [ ] **Step 1: Create the `SourceLink` record**

Create `hud-backend/src/main/java/com/hud/briefing/SourceLink.java`:
```java
package com.hud.briefing;

/**
 * A single discovered article URL together with the originating source's
 * display name and quality tier, so the tier can flow into the map stage.
 */
public record SourceLink(String url, String sourceName, SourceTier tier) {
}
```

- [ ] **Step 2: Change the `BriefingSourceStrategy` interface**

Replace `hud-backend/src/main/java/com/hud/briefing/BriefingSourceStrategy.java` with:
```java
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
```

- [ ] **Step 3: Write the failing `DatabaseSourceStrategy` test**

Create `hud-backend/src/test/java/com/hud/briefing/DatabaseSourceStrategyTest.java`:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class DatabaseSourceStrategyTest {

    @Mock private NewsSourceRepository sourceRepository;
    @Mock private PlaywrightScraperService scraperService;

    private DatabaseSourceStrategy strategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new DatabaseSourceStrategy(sourceRepository, scraperService);
    }

    @Test
    void supportsEveryCategory() {
        for (BriefingCategory c : BriefingCategory.values()) {
            assertTrue(strategy.supports(c));
        }
    }

    @Test
    void returnsEmptyWhenNoSourcesConfigured() {
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.WORLD_NEWS))
                .thenReturn(List.of());
        assertTrue(strategy.getLinks(BriefingCategory.WORLD_NEWS, 10).isEmpty());
    }

    @Test
    void resolvesRssSourceAndCarriesNameAndTier() {
        NewsSource bbc = new NewsSource(BriefingCategory.WORLD_NEWS, "BBC World",
                "https://bbc/rss", SourceType.RSS, SourceTier.TIER_1, 100, true);
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.WORLD_NEWS))
                .thenReturn(List.of(bbc));
        when(scraperService.getLinksFromRss(eq("https://bbc/rss"), anyInt()))
                .thenReturn(List.of("https://bbc/article-1"));

        List<SourceLink> links = strategy.getLinks(BriefingCategory.WORLD_NEWS, 10);

        assertEquals(1, links.size());
        assertEquals("https://bbc/article-1", links.get(0).url());
        assertEquals("BBC World", links.get(0).sourceName());
        assertEquals(SourceTier.TIER_1, links.get(0).tier());
    }

    @Test
    void resolvesIswSourceFilteredByUkraineTheaterKeyword() {
        NewsSource isw = new NewsSource(BriefingCategory.THEATER_UKRAINE, "ISW Ukraine",
                "ukraine", SourceType.ISW, SourceTier.TIER_1, 100, true);
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.THEATER_UKRAINE))
                .thenReturn(List.of(isw));
        when(scraperService.getIswLinks(anyInt())).thenReturn(List.of(
                "https://isw/offensive-campaign-assessment-1",
                "https://isw/iran-update-1",
                "https://isw/offensive-campaign-assessment-2"));

        List<SourceLink> links = strategy.getLinks(BriefingCategory.THEATER_UKRAINE, 10);

        assertEquals(2, links.size());
        assertTrue(links.stream().allMatch(l -> l.url().contains("offensive-campaign-assessment")));
        assertEquals("ISW Ukraine", links.get(0).sourceName());
    }

    @Test
    void resolvesCsisSource() {
        NewsSource csis = new NewsSource(BriefingCategory.GLOBAL_SITREP, "CSIS Analysis",
                "https://www.csis.org/analysis", SourceType.CSIS, SourceTier.TIER_1, 100, true);
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.GLOBAL_SITREP))
                .thenReturn(List.of(csis));
        when(scraperService.getCsisLinks(anyInt())).thenReturn(List.of("https://csis/report-1"));

        List<SourceLink> links = strategy.getLinks(BriefingCategory.GLOBAL_SITREP, 10);

        assertEquals(1, links.size());
        verify(scraperService).getCsisLinks(anyInt());
    }

    @Test
    void aggregatesMultipleSourcesAndCapsAtLimit() {
        NewsSource a = new NewsSource(BriefingCategory.FINANCE, "Feed A", "https://a/rss",
                SourceType.RSS, SourceTier.TIER_1, 100, true);
        NewsSource b = new NewsSource(BriefingCategory.FINANCE, "Feed B", "https://b/rss",
                SourceType.RSS, SourceTier.TIER_2, 80, true);
        when(sourceRepository.findByCategoryAndActiveTrue(BriefingCategory.FINANCE))
                .thenReturn(List.of(a, b));
        when(scraperService.getLinksFromRss(eq("https://a/rss"), anyInt()))
                .thenReturn(List.of("a1", "a2"));
        when(scraperService.getLinksFromRss(eq("https://b/rss"), anyInt()))
                .thenReturn(List.of("b1", "b2"));

        List<SourceLink> links = strategy.getLinks(BriefingCategory.FINANCE, 3);

        assertEquals(3, links.size());
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `mvn test -pl hud-backend -Dtest=DatabaseSourceStrategyTest -DexcludedGroups=`
Expected: FAIL — `DatabaseSourceStrategy` does not exist (compilation error).

- [ ] **Step 5: Create `DatabaseSourceStrategy`**

Create `hud-backend/src/main/java/com/hud/briefing/DatabaseSourceStrategy.java`:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The single briefing source strategy. Reads active {@link NewsSource} rows for
 * a category from the database and discovers article links by dispatching on
 * {@link SourceType}. Replaces the previously hardcoded GeneralRss/Theater strategies.
 */
@Component
public class DatabaseSourceStrategy implements BriefingSourceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSourceStrategy.class);
    private static final int ISW_FETCH_LIMIT = 15;

    private final NewsSourceRepository sourceRepository;
    private final PlaywrightScraperService scraperService;

    public DatabaseSourceStrategy(NewsSourceRepository sourceRepository,
                                  PlaywrightScraperService scraperService) {
        this.sourceRepository = sourceRepository;
        this.scraperService = scraperService;
    }

    @Override
    public List<SourceLink> getLinks(BriefingCategory category, int limit) {
        List<NewsSource> sources = new ArrayList<>(
                sourceRepository.findByCategoryAndActiveTrue(category));
        if (sources.isEmpty()) {
            return List.of();
        }
        // Higher-weight sources first, so a truncating limit keeps the best ones.
        sources.sort(Comparator.comparingInt(NewsSource::getWeight).reversed());
        int limitPerSource = Math.max(1, limit / sources.size());

        List<SourceLink> aggregated = new ArrayList<>();
        for (NewsSource source : sources) {
            for (String url : resolveUrls(source, limitPerSource)) {
                aggregated.add(new SourceLink(url, source.getName(), source.getTier()));
            }
        }
        return aggregated.size() > limit
                ? new ArrayList<>(aggregated.subList(0, limit))
                : aggregated;
    }

    @Override
    public boolean supports(BriefingCategory category) {
        return true;
    }

    private List<String> resolveUrls(NewsSource source, int limit) {
        switch (source.getType()) {
            case RSS:
                logger.info("DatabaseSourceStrategy: RSS {}", source.getName());
                return scraperService.getLinksFromRss(source.getUrl(), limit);
            case CSIS:
                logger.info("DatabaseSourceStrategy: CSIS {}", source.getName());
                return scraperService.getCsisLinks(limit);
            case ISW:
                logger.info("DatabaseSourceStrategy: ISW {} ({})", source.getName(), source.getUrl());
                return filterIswLinks(scraperService.getIswLinks(ISW_FETCH_LIMIT),
                        source.getUrl(), limit);
            default:
                logger.warn("Unsupported source type {} for {}", source.getType(), source.getName());
                return List.of();
        }
    }

    /** Filters the ISW publications index by the theater keyword stored in the source url. */
    private List<String> filterIswLinks(List<String> links, String theater, int limit) {
        String key = theater == null ? "" : theater.trim().toLowerCase(Locale.ROOT);
        if ("ukraine".equals(key)) {
            List<String> filtered = links.stream()
                    .filter(l -> l.contains("offensive-campaign-assessment"))
                    .limit(limit)
                    .collect(Collectors.toList());
            if (filtered.isEmpty()) {
                filtered = links.stream()
                        .filter(l -> l.contains("ukraine"))
                        .limit(limit)
                        .collect(Collectors.toList());
            }
            return filtered;
        }
        if ("mideast".equals(key)) {
            return links.stream()
                    .filter(l -> l.contains("iran-update")
                            || l.contains("israel-hamas-war")
                            || l.contains("middle-east"))
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        return links.stream().limit(limit).collect(Collectors.toList());
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `mvn test -pl hud-backend -Dtest=DatabaseSourceStrategyTest -DexcludedGroups=`
Expected: PASS (all 6 tests).

- [ ] **Step 7: Delete the hardcoded strategies and their tests**

Run:
```bash
cd /home/jakefear/source/hud
git rm hud-backend/src/main/java/com/hud/briefing/GeneralRssSourceStrategy.java \
  hud-backend/src/main/java/com/hud/briefing/TheaterSourceStrategy.java \
  hud-backend/src/test/java/com/hud/briefing/GeneralRssSourceStrategyTest.java \
  hud-backend/src/test/java/com/hud/briefing/TheaterSourceStrategyTest.java
```

- [ ] **Step 8: Remove `defaultQuery` from `BriefingCategory`**

Replace `hud-backend/src/main/java/com/hud/briefing/BriefingCategory.java` with:
```java
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
```

- [ ] **Step 9: Update `BriefingProcessor` for `SourceLink` and a no-arg `process()`**

Replace `hud-backend/src/main/java/com/hud/briefing/BriefingProcessor.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Template method for turning raw signal into intelligence. In Phase 2 this is
 * a transitional form: links now carry source name/tier, but the reduce stage
 * still operates on concatenated text. Task 6 replaces acquireSignal/synthesize
 * with the map-reduce pipeline.
 */
public abstract class BriefingProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(BriefingProcessor.class);

    protected final PlaywrightScraperService scraperService;
    protected final ChatLanguageModel chatModel;
    protected final BriefingSourceStrategy sourceStrategy;
    protected final IntelligenceSynthesizer synthesizer;
    protected final BriefingCategory category;

    protected BriefingProcessor(PlaywrightScraperService scraperService,
                                ChatLanguageModel chatModel,
                                BriefingSourceStrategy sourceStrategy,
                                IntelligenceSynthesizer synthesizer,
                                BriefingCategory category) {
        this.scraperService = scraperService;
        this.chatModel = chatModel;
        this.sourceStrategy = sourceStrategy;
        this.synthesizer = synthesizer;
        this.category = category;
    }

    public final SynthesisResult process() {
        List<SourceLink> links = sourceStrategy.getLinks(category, getLinkLimit());

        if (links.isEmpty()) {
            throw new IllegalStateException("No signal sources found for: " + category);
        }

        String consolidatedSignal = acquireSignal(links);

        if (consolidatedSignal.length() < getMinRequiredChars()) {
            throw new IllegalStateException("Insufficient situational signal captured.");
        }

        return synthesize(consolidatedSignal);
    }

    protected abstract int getLinkLimit();
    protected abstract int getMinRequiredChars();
    protected abstract int getScrapeDepth();
    protected abstract SynthesisResult synthesize(String rawSignal);

    protected String acquireSignal(List<SourceLink> links) {
        StringBuilder sb = new StringBuilder();
        for (SourceLink link : links) {
            String text = scraperService.extractFullText(link.url(), getScrapeDepth());
            if (isPlausibleContent(text, link.url())) {
                sb.append("\n--- START SOURCE ---\n").append(text).append("\n--- END SOURCE ---\n");
            }
        }
        return sb.toString();
    }

    protected boolean isPlausibleContent(String text, String url) {
        if (text == null || text.length() < 500) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return !lower.contains("before you continue")
                && !lower.contains("accept all cookies")
                && !url.contains("/about");
    }
}
```

- [ ] **Step 10: Update `StandardBriefingProcessor`**

Replace `hud-backend/src/main/java/com/hud/briefing/StandardBriefingProcessor.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class StandardBriefingProcessor extends BriefingProcessor {

    private final BriefingPersona persona;

    public StandardBriefingProcessor(PlaywrightScraperService scraperService,
                                     ChatLanguageModel chatModel,
                                     BriefingSourceStrategy sourceStrategy,
                                     IntelligenceSynthesizer synthesizer,
                                     BriefingCategory category) {
        super(scraperService, chatModel, sourceStrategy, synthesizer, category);
        this.persona = category.getPersona();
    }

    @Override
    protected int getLinkLimit() { return 15; }

    @Override
    protected int getMinRequiredChars() { return 1500; }

    @Override
    protected int getScrapeDepth() { return 0; }

    @Override
    protected SynthesisResult synthesize(String rawSignal) {
        return synthesizer.synthesizeStandard(chatModel, persona, rawSignal);
    }
}
```

- [ ] **Step 11: Update `DeepDiveBriefingProcessor`**

Replace `hud-backend/src/main/java/com/hud/briefing/DeepDiveBriefingProcessor.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class DeepDiveBriefingProcessor extends BriefingProcessor {

    public DeepDiveBriefingProcessor(PlaywrightScraperService scraperService,
                                     ChatLanguageModel chatModel,
                                     BriefingSourceStrategy sourceStrategy,
                                     IntelligenceSynthesizer synthesizer,
                                     BriefingCategory category) {
        super(scraperService, chatModel, sourceStrategy, synthesizer, category);
    }

    @Override
    protected int getLinkLimit() {
        return (category == BriefingCategory.GLOBAL_SITREP) ? 25 : 15;
    }

    @Override
    protected int getMinRequiredChars() { return 2500; }

    @Override
    protected int getScrapeDepth() { return 1; }

    @Override
    protected SynthesisResult synthesize(String rawSignal) {
        if (category == BriefingCategory.GLOBAL_SITREP) {
            return synthesizer.synthesizeGlobalSitrep(chatModel, rawSignal);
        }
        return synthesizer.fuseTheaterIntelligence(chatModel, category, rawSignal);
    }
}
```

- [ ] **Step 12: Update `BriefingProcessorFactory`**

In `hud-backend/src/main/java/com/hud/briefing/BriefingProcessorFactory.java`, change the `getProcessor` method body so processors receive the `category` (not `category.getPersona()`):
```java
    public BriefingProcessor getProcessor(BriefingCategory category, ChatLanguageModel model) {
        BriefingSourceStrategy strategy = sourceFactory.getStrategy(category);

        if (isTheaterCategory(category)) {
            return new DeepDiveBriefingProcessor(scraperService, model, strategy, synthesizer, category);
        } else {
            return new StandardBriefingProcessor(scraperService, model, strategy, synthesizer, category);
        }
    }
```
(The constructor and `isTheaterCategory` are unchanged.)

- [ ] **Step 13: Update `AutomatedBriefingService`**

In `hud-backend/src/main/java/com/hud/briefing/AutomatedBriefingService.java`:

In `generateForCategory(BriefingCategory category)`, remove the `query` variable and the argument:
```java
    @Async("briefingExecutor")
    public void generateForCategory(BriefingCategory category) {
        List<DynamicLlmService.NamedChatModel> activeModels = llmService.getActiveModels();
        LocalDate today = LocalDate.now();

        for (DynamicLlmService.NamedChatModel model : activeModels) {
            try {
                generateForCategory(today, category, model);
            } catch (Exception e) {
                logger.error("Async category run failed for {} [{}]: {}", category, model.name(), e.getMessage(), e);
            }
        }
    }
```

In `executeFullPipeline`, remove the `query` variable:
```java
    private void executeFullPipeline(LocalDate today, DynamicLlmService.NamedChatModel model) {
        // News Domain
        for (BriefingCategory category : BriefingCategory.values()) {
            try {
                generateForCategory(today, category, model);
            } catch (Exception e) {
                logger.error("Failed generation for {} [{}]: {}", category, model.name(), e.getMessage(), e);
            }
        }
    }
```

Change the `generateForCategory` overload signature to drop `String query` and call `process()` with no argument:
```java
    public void generateForCategory(LocalDate date, BriefingCategory category, DynamicLlmService.NamedChatModel model) {
        PipelineRun run = new PipelineRun(category, model.name(), PipelineStatus.PENDING, LocalDateTime.now());
        final PipelineRun savedRun = transactionTemplate.execute(status -> pipelineRunRepository.save(run));

        try {
            BriefingProcessor processor = processorFactory.getProcessor(category, model.model());
            SynthesisResult result = processor.process();
```
(Everything below `SynthesisResult result = processor.process();` in that method is unchanged.)

- [ ] **Step 14: Update `BriefingProcessorTest`**

Replace `hud-backend/src/test/java/com/hud/briefing/BriefingProcessorTest.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class BriefingProcessorTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private ChatLanguageModel chatModel;
    @Mock private BriefingSourceStrategy sourceStrategy;
    @Mock private IntelligenceSynthesizer synthesizer;

    private StandardBriefingProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new StandardBriefingProcessor(scraperService, chatModel, sourceStrategy,
                synthesizer, BriefingCategory.WORLD_NEWS);
    }

    private SourceLink link(String url) {
        return new SourceLink(url, "Test Feed", SourceTier.TIER_1);
    }

    @Test
    void shouldProcessSuccessfully() {
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://test.com/1")));
        String longContent = "Valid situational intelligence report that provides enough textual density to satisfy the high-resolution requirements of the analytic heads-up display system.".repeat(15);
        when(scraperService.extractFullText(eq("http://test.com/1"), anyInt())).thenReturn(longContent);
        when(synthesizer.synthesizeStandard(any(), any(), anyString()))
                .thenReturn(new SynthesisResult("Synthesized Intelligence", 100, 10));

        SynthesisResult result = processor.process();

        assertEquals("Synthesized Intelligence", result.content());
        verify(sourceStrategy).getLinks(BriefingCategory.WORLD_NEWS, 15);
        verify(scraperService).extractFullText(eq("http://test.com/1"), anyInt());
        verify(synthesizer).synthesizeStandard(eq(chatModel), any(), contains("Valid situational intelligence"));
    }

    @Test
    void shouldThrowExceptionWhenNoLinksFound() {
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt())).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> processor.process());
        assertTrue(ex.getMessage().contains("No signal sources found"));
    }

    @Test
    void shouldThrowExceptionWhenInsufficientSignal() {
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://test.com")));
        when(scraperService.extractFullText(anyString(), anyInt())).thenReturn("Too short signal");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> processor.process());
        assertTrue(ex.getMessage().contains("Insufficient situational signal"));
    }

    @Test
    void shouldFilterNonPlausibleContent() {
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://test.com/cookie"), link("http://test.com/valid")));
        when(scraperService.extractFullText(eq("http://test.com/cookie"), anyInt()))
                .thenReturn("Before you continue... Accept all cookies");
        String longContent = "A long piece of valid situational content that meets the length requirements for processing and provides the necessary analytical depth.".repeat(15);
        when(scraperService.extractFullText(eq("http://test.com/valid"), anyInt())).thenReturn(longContent);
        when(synthesizer.synthesizeStandard(any(), any(), anyString()))
                .thenReturn(new SynthesisResult("Success", 100, 10));

        processor.process();

        verify(synthesizer).synthesizeStandard(any(), any(), argThat(s -> !s.contains("Accept all cookies")));
    }
}
```

- [ ] **Step 15: Update `DeepDiveBriefingProcessorTest`**

Replace `hud-backend/src/test/java/com/hud/briefing/DeepDiveBriefingProcessorTest.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class DeepDiveBriefingProcessorTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private ChatLanguageModel chatModel;
    @Mock private BriefingSourceStrategy sourceStrategy;
    @Mock private IntelligenceSynthesizer synthesizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private SourceLink link(String url) {
        return new SourceLink(url, "Theater Feed", SourceTier.TIER_1);
    }

    @Test
    void shouldProcessUkraineTheaterWithHighSignal() {
        DeepDiveBriefingProcessor processor = new DeepDiveBriefingProcessor(scraperService, chatModel,
                sourceStrategy, synthesizer, BriefingCategory.THEATER_UKRAINE);

        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://test.com/intel")));
        when(scraperService.extractFullText(eq("http://test.com/intel"), anyInt()))
                .thenReturn("A very long piece of tactical field intelligence from the frontline that is definitely longer than 2500 characters so that the deep-dive processor doesn't complain about insufficient signal during its rigorous analytical lifecycle.".repeat(20));
        when(synthesizer.fuseTheaterIntelligence(any(), any(), anyString()))
                .thenReturn(new SynthesisResult("Fused Intel Report", 100, 10));

        SynthesisResult result = processor.process();

        assertEquals("Fused Intel Report", result.content());
        verify(sourceStrategy).getLinks(BriefingCategory.THEATER_UKRAINE, 15);
        verify(synthesizer).fuseTheaterIntelligence(eq(chatModel), eq(BriefingCategory.THEATER_UKRAINE), anyString());
    }

    @Test
    void shouldProcessGlobalSitrepWithMultiLinks() {
        DeepDiveBriefingProcessor processor = new DeepDiveBriefingProcessor(scraperService, chatModel,
                sourceStrategy, synthesizer, BriefingCategory.GLOBAL_SITREP);

        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("http://a.com"), link("http://b.com")));
        when(scraperService.extractFullText(anyString(), anyInt()))
                .thenReturn("Valid strategic content for the global situational report meeting the character limit requirements.".repeat(25));
        when(synthesizer.synthesizeGlobalSitrep(any(), anyString()))
                .thenReturn(new SynthesisResult("Global Summary", 100, 10));

        SynthesisResult result = processor.process();

        assertEquals("Global Summary", result.content());
        verify(sourceStrategy).getLinks(BriefingCategory.GLOBAL_SITREP, 25);
        verify(synthesizer).synthesizeGlobalSitrep(eq(chatModel), anyString());
    }
}
```

- [ ] **Step 16: Update `AutomatedBriefingServiceTest`**

In `hud-backend/src/test/java/com/hud/briefing/AutomatedBriefingServiceTest.java`, make these three replacements:

1. Every occurrence of `when(mockProcessor.process(anyString()))` becomes `when(mockProcessor.process())` (appears 3 times — in `shouldTriggerBriefingForAllActiveModels`, `shouldTriggerBriefingForSpecificModel`, `shouldGenerateForSpecificCategory`).
2. `when(mockProcessor.process(anyString())).thenThrow(new RuntimeException("API Error"));` becomes `when(mockProcessor.process()).thenThrow(new RuntimeException("API Error"));`.
3. In `shouldHandleProcessingFailure`, the call `service.generateForCategory(LocalDate.now(), BriefingCategory.WORLD_NEWS, "query", model);` becomes `service.generateForCategory(LocalDate.now(), BriefingCategory.WORLD_NEWS, model);`.

No imports change — `anyString` is no longer referenced but remains available via the existing `import static org.mockito.Mockito.*` wildcard, which does not warn.

- [ ] **Step 17: Run the full backend unit suite**

Run: `mvn test -pl hud-backend`
Expected: BUILD SUCCESS — all unit tests pass; `BriefingSourceFactoryTest` is unaffected (it mocks the interface). No external services are contacted.

- [ ] **Step 18: Commit**

```bash
git add -A hud-backend/src/main/java/com/hud/briefing hud-backend/src/test/java/com/hud/briefing
git commit -m "feat: replace hardcoded feeds with DB-backed DatabaseSourceStrategy"
```

---

## Task 4: Map stage — `ArticleDigest` and `DocumentDigester`

**Files:**
- Create: `hud-backend/src/main/java/com/hud/briefing/ArticleDigest.java`
- Create: `hud-backend/src/main/java/com/hud/briefing/DocumentDigester.java`
- Create: `hud-backend/src/test/java/com/hud/briefing/DocumentDigesterTest.java`

This task is purely additive — nothing calls the digester until Task 6.

- [ ] **Step 1: Create the `ArticleDigest` record**

Create `hud-backend/src/main/java/com/hud/briefing/ArticleDigest.java`:
```java
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
```

- [ ] **Step 2: Write the failing `DocumentDigester` test**

Create `hud-backend/src/test/java/com/hud/briefing/DocumentDigesterTest.java`:
```java
package com.hud.briefing;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
class DocumentDigesterTest {

    @Mock private ChatLanguageModel model;
    private DocumentDigester digester;

    private static final SourceLink SOURCE =
            new SourceLink("http://news/article-1", "BBC World", SourceTier.TIER_1);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        digester = new DocumentDigester();
    }

    @Test
    void parsesAWellFormedDigest() {
        String llm = String.join("\n",
                "HEADLINE: Major escalation reported",
                "RELEVANCE: 0.9",
                "SUMMARY: Forces advanced overnight.",
                "Heavy shelling continued at dawn.",
                "KEY FACTS:",
                "- 12 strikes recorded",
                "- supply line cut",
                "ENTITIES: Kyiv, NATO, Black Sea",
                "DATED EVENTS:",
                "- 2026-05-14: counteroffensive began");

        ArticleDigest d = DocumentDigester.parseDigest(llm, SOURCE);

        assertEquals("Major escalation reported", d.headline());
        assertEquals(0.9, d.relevanceScore(), 0.0001);
        assertEquals("Forces advanced overnight. Heavy shelling continued at dawn.", d.summary());
        assertEquals(2, d.keyFacts().size());
        assertEquals(3, d.entities().size());
        assertEquals(1, d.datedEvents().size());
        assertEquals("http://news/article-1", d.sourceUrl());
        assertEquals(SourceTier.TIER_1, d.tier());
        assertEquals(1, d.corroborationCount());
        assertEquals(java.util.List.of("BBC World"), d.contributingSources());
    }

    @Test
    void appliesSafeDefaultsForMissingOrUnparseableFields() {
        ArticleDigest d = DocumentDigester.parseDigest("totally unstructured model babble", SOURCE);

        assertEquals("BBC World", d.headline());      // falls back to source name
        assertEquals(0.5, d.relevanceScore(), 0.0001); // default relevance
        assertEquals("", d.summary());
        assertTrue(d.keyFacts().isEmpty());
        assertTrue(d.entities().isEmpty());
    }

    @Test
    void clampsRelevanceIntoUnitRange() {
        ArticleDigest high = DocumentDigester.parseDigest("RELEVANCE: 7.5", SOURCE);
        ArticleDigest low = DocumentDigester.parseDigest("RELEVANCE: -3", SOURCE);
        assertEquals(1.0, high.relevanceScore(), 0.0001);
        assertEquals(0.0, low.relevanceScore(), 0.0001);
    }

    @Test
    void digestCallsTheModelAndReportsTokenUsage() {
        Response<AiMessage> response = Response.from(
                AiMessage.from("HEADLINE: Test\nSUMMARY: A summary."),
                new TokenUsage(120, 40));
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        DocumentDigester.DigestResult result =
                digester.digest(model, "Some long document body.", SOURCE, "national security");

        assertEquals("Test", result.digest().headline());
        assertEquals(120, result.inputTokens());
        assertEquals(40, result.outputTokens());
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn test -pl hud-backend -Dtest=DocumentDigesterTest -DexcludedGroups=`
Expected: FAIL — `DocumentDigester` does not exist (compilation error).

- [ ] **Step 4: Create `DocumentDigester`**

Create `hud-backend/src/main/java/com/hud/briefing/DocumentDigester.java`:
```java
package com.hud.briefing;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Map stage of the briefing pipeline. Turns one scraped document into a compact
 * {@link ArticleDigest} via a single bounded LLM call. The digest is small, so
 * every document a run scrapes can later be fed to one reduce-stage prompt.
 */
@Component
public class DocumentDigester {

    /** Upper bound on document text sent to the model, to keep each map call bounded. */
    private static final int MAX_DOC_CHARS = 200_000;
    private static final double DEFAULT_RELEVANCE = 0.5;

    /** Map-stage result: the digest plus the token cost of producing it. */
    public record DigestResult(ArticleDigest digest, int inputTokens, int outputTokens) {}

    public DigestResult digest(ChatLanguageModel model, String documentText,
                               SourceLink source, String relevanceContext) {
        String body = documentText == null ? "" : documentText;
        if (body.length() > MAX_DOC_CHARS) {
            body = body.substring(0, MAX_DOC_CHARS);
        }
        String prompt = buildPrompt(body, relevanceContext);

        Response<AiMessage> response = model.generate(UserMessage.from(prompt));
        TokenUsage usage = response.tokenUsage();
        ArticleDigest digest = parseDigest(response.content().text(), source);

        return new DigestResult(
                digest,
                usage != null ? usage.inputTokenCount() : 0,
                usage != null ? usage.outputTokenCount() : 0);
    }

    static String buildPrompt(String documentText, String relevanceContext) {
        return "You are an intelligence analyst. Read the SOURCE DOCUMENT and extract a structured digest.\n"
                + "Relevance context: " + relevanceContext + "\n"
                + "Respond ONLY in this exact format, with no extra commentary:\n\n"
                + "HEADLINE: <one concise line naming the core event>\n"
                + "RELEVANCE: <number 0.0 to 1.0 — relevance to the context above>\n"
                + "SUMMARY: <2-4 sentences of dense factual summary>\n"
                + "KEY FACTS:\n- <fact>\n- <fact>\n"
                + "ENTITIES: <comma-separated people, organizations, places>\n"
                + "DATED EVENTS:\n- <date>: <what happened>\n\n"
                + "SOURCE DOCUMENT:\n" + documentText;
    }

    /**
     * Lenient parser for the digester's labelled-section format. Unknown lines
     * are treated as continuations of the current section; missing fields fall
     * back to safe defaults so a malformed model response never fails the run.
     */
    static ArticleDigest parseDigest(String llmText, SourceLink source) {
        String headline = "";
        double relevance = DEFAULT_RELEVANCE;
        StringBuilder summary = new StringBuilder();
        List<String> keyFacts = new ArrayList<>();
        List<String> entities = new ArrayList<>();
        List<String> datedEvents = new ArrayList<>();

        String section = "";
        if (llmText != null) {
            for (String raw : llmText.split("\\r?\\n")) {
                String line = raw.strip();
                if (line.isEmpty()) continue;
                String upper = line.toUpperCase(Locale.ROOT);

                if (upper.startsWith("HEADLINE:")) {
                    headline = valueAfterColon(line);
                    section = "HEADLINE";
                } else if (upper.startsWith("RELEVANCE:")) {
                    relevance = parseRelevance(valueAfterColon(line));
                    section = "RELEVANCE";
                } else if (upper.startsWith("SUMMARY:")) {
                    appendText(summary, valueAfterColon(line));
                    section = "SUMMARY";
                } else if (upper.startsWith("KEY FACTS:")) {
                    section = "KEY_FACTS";
                } else if (upper.startsWith("ENTITIES:")) {
                    addCsv(entities, valueAfterColon(line));
                    section = "ENTITIES";
                } else if (upper.startsWith("DATED EVENTS:")) {
                    section = "DATED_EVENTS";
                } else {
                    switch (section) {
                        case "SUMMARY"      -> appendText(summary, line);
                        case "KEY_FACTS"    -> addBullet(keyFacts, line);
                        case "DATED_EVENTS" -> addBullet(datedEvents, line);
                        case "ENTITIES"     -> addCsv(entities, line);
                        default             -> { /* ignore stray pre-amble */ }
                    }
                }
            }
        }
        if (headline.isBlank()) {
            headline = source.sourceName();
        }
        return new ArticleDigest(
                source.url(), source.sourceName(), source.tier(),
                headline, summary.toString().strip(),
                List.copyOf(keyFacts), List.copyOf(entities), List.copyOf(datedEvents),
                relevance, 1, List.of(source.sourceName()));
    }

    private static String valueAfterColon(String line) {
        int idx = line.indexOf(':');
        return idx >= 0 ? line.substring(idx + 1).strip() : "";
    }

    private static double parseRelevance(String value) {
        try {
            double parsed = Double.parseDouble(value.strip());
            return Math.max(0.0, Math.min(1.0, parsed));
        } catch (NumberFormatException e) {
            return DEFAULT_RELEVANCE;
        }
    }

    private static void appendText(StringBuilder sb, String text) {
        if (text == null || text.isBlank()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(text.strip());
    }

    private static void addBullet(List<String> target, String line) {
        String cleaned = line.replaceFirst("^[-*\\u2022]\\s*", "").strip();
        if (!cleaned.isEmpty()) target.add(cleaned);
    }

    private static void addCsv(List<String> target, String csv) {
        for (String part : csv.split(",")) {
            String t = part.strip();
            if (!t.isEmpty()) target.add(t);
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn test -pl hud-backend -Dtest=DocumentDigesterTest -DexcludedGroups=`
Expected: PASS (all 4 tests).

- [ ] **Step 6: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/ArticleDigest.java \
  hud-backend/src/main/java/com/hud/briefing/DocumentDigester.java \
  hud-backend/src/test/java/com/hud/briefing/DocumentDigesterTest.java
git commit -m "feat: add map-stage DocumentDigester producing compact ArticleDigests"
```

---

## Task 5: Dedup stage — `DigestDeduplicator`

**Files:**
- Create: `hud-backend/src/main/java/com/hud/briefing/DigestDeduplicator.java`
- Create: `hud-backend/src/test/java/com/hud/briefing/DigestDeduplicatorTest.java`

Purely additive — nothing calls it until Task 6.

- [ ] **Step 1: Write the failing test**

Create `hud-backend/src/test/java/com/hud/briefing/DigestDeduplicatorTest.java`:
```java
package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class DigestDeduplicatorTest {

    private DigestDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        deduplicator = new DigestDeduplicator();
    }

    private ArticleDigest digest(String name, SourceTier tier, String headline,
                                 List<String> entities, double relevance) {
        return new ArticleDigest("http://" + name, name, tier, headline, "summary",
                List.of("fact"), entities, List.of("2026-05-10: event"),
                relevance, 1, List.of(name));
    }

    @Test
    void keepsDistinctDigests() {
        ArticleDigest a = digest("A", SourceTier.TIER_1, "Ukraine offensive advances",
                List.of("Kyiv", "NATO"), 0.8);
        ArticleDigest b = digest("B", SourceTier.TIER_2, "Tech earnings season opens",
                List.of("Apple", "Nvidia"), 0.7);

        List<ArticleDigest> result = deduplicator.dedupe(List.of(a, b));

        assertEquals(2, result.size());
    }

    @Test
    void mergesDigestsWithIdenticalNormalizedHeadline() {
        ArticleDigest a = digest("A", SourceTier.TIER_2, "Ukraine Offensive Advances!",
                List.of("Kyiv"), 0.6);
        ArticleDigest b = digest("B", SourceTier.TIER_1, "ukraine offensive advances",
                List.of("Moscow"), 0.9);

        List<ArticleDigest> result = deduplicator.dedupe(List.of(a, b));

        assertEquals(1, result.size());
        ArticleDigest merged = result.get(0);
        assertEquals(2, merged.corroborationCount());
        assertEquals(SourceTier.TIER_1, merged.tier(), "higher tier survives");
        assertTrue(merged.contributingSources().containsAll(List.of("A", "B")));
    }

    @Test
    void mergesDigestsWithHighEntityOverlapEvenIfHeadlinesDiffer() {
        ArticleDigest a = digest("A", SourceTier.TIER_1, "Strikes reported overnight",
                List.of("Kyiv", "NATO", "Black Sea", "Drones"), 0.8);
        ArticleDigest b = digest("B", SourceTier.TIER_2, "Overnight bombardment continues",
                List.of("Kyiv", "NATO", "Black Sea", "Missiles"), 0.7);

        List<ArticleDigest> result = deduplicator.dedupe(List.of(a, b));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).corroborationCount());
    }

    @Test
    void doesNotMergeWhenEntityOverlapIsLow() {
        ArticleDigest a = digest("A", SourceTier.TIER_1, "Headline one",
                List.of("Kyiv", "NATO"), 0.8);
        ArticleDigest b = digest("B", SourceTier.TIER_2, "Headline two",
                List.of("Apple", "Nvidia"), 0.7);

        assertEquals(2, deduplicator.dedupe(List.of(a, b)).size());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -pl hud-backend -Dtest=DigestDeduplicatorTest -DexcludedGroups=`
Expected: FAIL — `DigestDeduplicator` does not exist (compilation error).

- [ ] **Step 3: Create `DigestDeduplicator`**

Create `hud-backend/src/main/java/com/hud/briefing/DigestDeduplicator.java`:
```java
package com.hud.briefing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dedup stage. Merges near-duplicate digests covering the same event using a
 * cheap heuristic: identical normalized headline, or entity-set overlap above
 * a threshold. The surviving digest keeps the better-tier source's content and
 * accumulates corroborationCount and the contributing-source list.
 */
@Component
public class DigestDeduplicator {

    private static final double ENTITY_OVERLAP_THRESHOLD = 0.5;

    public List<ArticleDigest> dedupe(List<ArticleDigest> digests) {
        List<ArticleDigest> survivors = new ArrayList<>();
        for (ArticleDigest candidate : digests) {
            int matchIndex = -1;
            for (int i = 0; i < survivors.size(); i++) {
                if (sameEvent(survivors.get(i), candidate)) {
                    matchIndex = i;
                    break;
                }
            }
            if (matchIndex >= 0) {
                survivors.set(matchIndex, merge(survivors.get(matchIndex), candidate));
            } else {
                survivors.add(candidate);
            }
        }
        return survivors;
    }

    static boolean sameEvent(ArticleDigest a, ArticleDigest b) {
        String ha = normalize(a.headline());
        String hb = normalize(b.headline());
        if (!ha.isEmpty() && ha.equals(hb)) {
            return true;
        }
        return entityJaccard(a.entities(), b.entities()) >= ENTITY_OVERLAP_THRESHOLD;
    }

    static ArticleDigest merge(ArticleDigest a, ArticleDigest b) {
        ArticleDigest base = preferred(a, b);
        ArticleDigest other = (base == a) ? b : a;

        List<String> sources = new ArrayList<>(base.contributingSources());
        for (String s : other.contributingSources()) {
            if (!sources.contains(s)) {
                sources.add(s);
            }
        }
        return new ArticleDigest(
                base.sourceUrl(), base.sourceName(), base.tier(),
                base.headline(), base.summary(),
                base.keyFacts(), base.entities(), base.datedEvents(),
                base.relevanceScore(),
                a.corroborationCount() + b.corroborationCount(),
                List.copyOf(sources));
    }

    /** Better tier wins (TIER_1 has ordinal 0); ties broken by higher relevance. */
    private static ArticleDigest preferred(ArticleDigest a, ArticleDigest b) {
        if (a.tier().ordinal() != b.tier().ordinal()) {
            return a.tier().ordinal() < b.tier().ordinal() ? a : b;
        }
        return a.relevanceScore() >= b.relevanceScore() ? a : b;
    }

    static double entityJaccard(List<String> a, List<String> b) {
        Set<String> sa = lowerSet(a);
        Set<String> sb = lowerSet(b);
        if (sa.isEmpty() || sb.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(sa);
        intersection.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> lowerSet(List<String> values) {
        Set<String> set = new HashSet<>();
        for (String v : values) {
            String t = v.strip().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        return set;
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -pl hud-backend -Dtest=DigestDeduplicatorTest -DexcludedGroups=`
Expected: PASS (all 4 tests).

- [ ] **Step 5: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/DigestDeduplicator.java \
  hud-backend/src/test/java/com/hud/briefing/DigestDeduplicatorTest.java
git commit -m "feat: add dedup stage merging near-duplicate digests by entity overlap"
```

---

## Task 6: Reduce stage — wire the map-reduce pipeline

This task changes `IntelligenceSynthesizer` to synthesize over `List<ArticleDigest>` and rewires the `BriefingProcessor` chain to run map → dedup → reduce. `IntelligenceSynthesizer` and `BriefingProcessor` are coupled (the processor calls the synthesizer), so they change together; the build is green again at the final step.

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/briefing/IntelligenceSynthesizer.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/BriefingProcessor.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/StandardBriefingProcessor.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/DeepDiveBriefingProcessor.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/BriefingProcessorFactory.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/IntelligenceSynthesizerTest.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/BriefingProcessorTest.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/DeepDiveBriefingProcessorTest.java`
- Modify: `hud-backend/src/test/java/com/hud/briefing/BriefingProcessorFactoryTest.java`

- [ ] **Step 1: Rewrite `IntelligenceSynthesizer` to consume digests**

Replace `hud-backend/src/main/java/com/hud/briefing/IntelligenceSynthesizer.java` with:
```java
package com.hud.briefing;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Reduce stage. Synthesizes a briefing from the deduplicated {@link ArticleDigest}
 * list. Digests are compact, so the whole set fits in one prompt regardless of how
 * many documents were crawled — the breadth gain. Higher-tier and more strongly
 * corroborated digests are ordered first in the prompt.
 */
@Component
public class IntelligenceSynthesizer {

    public SynthesisResult synthesizeStandard(ChatLanguageModel model, BriefingPersona persona,
                                              List<ArticleDigest> digests) {
        String data = formatDigestsForPrompt(digests);
        String prompt = String.format(
            "You are the %s. %s\nSTRICT RULES: NO META-COMMENTARY. FOCUS on %s. Use Markdown. 2-5 dense paragraphs.\n\nINTELLIGENCE DATA:\n%s\n\nTACTICAL BRIEFING:",
            persona.name(), persona.instruction(), persona.focus(), data
        );
        Response<AiMessage> response = model.generate(UserMessage.from(prompt));
        TokenUsage usage = response.tokenUsage();
        return new SynthesisResult(
            response.content().text(),
            usage != null ? usage.inputTokenCount() : 0,
            usage != null ? usage.outputTokenCount() : 0
        );
    }

    public SynthesisResult fuseTheaterIntelligence(ChatLanguageModel model, BriefingCategory category,
                                                   List<ArticleDigest> digests) {
        String data = formatDigestsForPrompt(digests);

        Response<AiMessage> tempoRes = model.generate(UserMessage.from(
            "COMMAND DIRECTIVE: You are a Tactical Ground Analyst. " +
            "TASK: Re-write the situational momentum into 2 dense narrative paragraphs. " +
            "IGNORE all citation brackets [1], [2] and links. " +
            "OUTPUT: Narrative Ground Truth only.\n\nDATA:\n" + data));

        Response<AiMessage> strikesRes = model.generate(UserMessage.from(
            "TASK: Extract kinetic strike data (Target, Location, Distance). " +
            "STRICT RULE: Output MUST be a valid GFM Markdown Table. " +
            "STRICT RULE: Every Markdown row MUST be on its own line. " +
            "STRICT RULE: Ensure the separator row (e.g., | --- | --- |) is present. " +
            "HEADER: '## Kinetic Impact'\n\nDATA:\n" + data));

        Response<AiMessage> innovationRes = model.generate(UserMessage.from(
            "TASK: Identify battlefield innovations (Tactics, EW, Drones). " +
            "OUTPUT: Clean bullet points. " +
            "HEADER: '## Innovation & Adaptation'\n\nDATA:\n" + data));

        String combinedContent = String.format("# %s THEATER REPORT\n\n## Tactical Momentum\n%s\n\n%s\n\n%s",
            category.name().replace("THEATER_", ""),
            tempoRes.content().text(),
            strikesRes.content().text(),
            innovationRes.content().text());

        int inputTokens = 0;
        int outputTokens = 0;
        for (Response<AiMessage> res : List.of(tempoRes, strikesRes, innovationRes)) {
            if (res.tokenUsage() != null) {
                inputTokens += res.tokenUsage().inputTokenCount();
                outputTokens += res.tokenUsage().outputTokenCount();
            }
        }
        return new SynthesisResult(combinedContent, inputTokens, outputTokens);
    }

    public SynthesisResult synthesizeGlobalSitrep(ChatLanguageModel model, List<ArticleDigest> digests) {
        String data = formatDigestsForPrompt(digests);
        String prompt = String.format(
            "COMMAND DIRECTIVE: You are a Global Theater Strategist. " +
            "TASK: Re-write the following intelligence digests into a cross-theater strategic summary. " +
            "RESTRICTION: DO NOT describe the text or sources. Provide a 3-5 paragraph narrative report.\n\nDATA:\n%s\n\nGLOBAL SITREP:",
            data
        );
        Response<AiMessage> response = model.generate(UserMessage.from(prompt));
        TokenUsage usage = response.tokenUsage();
        return new SynthesisResult(
            response.content().text(),
            usage != null ? usage.inputTokenCount() : 0,
            usage != null ? usage.outputTokenCount() : 0
        );
    }

    /**
     * Renders digests into a single prompt block, ordered by tier (TIER_1 first),
     * then corroboration, then relevance.
     */
    String formatDigestsForPrompt(List<ArticleDigest> digests) {
        List<ArticleDigest> ordered = new ArrayList<>(digests);
        ordered.sort(Comparator
                .comparingInt((ArticleDigest d) -> d.tier().ordinal())
                .thenComparing(Comparator.comparingInt(ArticleDigest::corroborationCount).reversed())
                .thenComparing(Comparator.comparingDouble(ArticleDigest::relevanceScore).reversed()));

        StringBuilder sb = new StringBuilder();
        sb.append("=== INTELLIGENCE DIGESTS (").append(ordered.size()).append(" sources) ===\n");
        int n = 1;
        for (ArticleDigest d : ordered) {
            sb.append("\n[#").append(n++).append("] SOURCE: ").append(d.sourceName())
              .append(" | ").append(d.tier())
              .append(" | corroboration: ").append(d.corroborationCount())
              .append(" | relevance: ")
              .append(String.format(Locale.ROOT, "%.2f", d.relevanceScore())).append('\n');
            sb.append("HEADLINE: ").append(d.headline()).append('\n');
            sb.append("SUMMARY: ").append(d.summary()).append('\n');
            if (!d.keyFacts().isEmpty()) {
                sb.append("KEY FACTS:\n");
                for (String f : d.keyFacts()) sb.append("- ").append(f).append('\n');
            }
            if (!d.entities().isEmpty()) {
                sb.append("ENTITIES: ").append(String.join(", ", d.entities())).append('\n');
            }
            if (!d.datedEvents().isEmpty()) {
                sb.append("DATED EVENTS:\n");
                for (String e : d.datedEvents()) sb.append("- ").append(e).append('\n');
            }
        }
        return sb.toString();
    }
}
```

- [ ] **Step 2: Rewrite `BriefingProcessor` as the map-reduce template**

Replace `hud-backend/src/main/java/com/hud/briefing/BriefingProcessor.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Template method for the map-reduce briefing pipeline:
 * discover links -> scrape documents -> map (digest each) -> dedup -> reduce (synthesize).
 * Digests are compact enough that breadth is no longer capped by the context window.
 */
public abstract class BriefingProcessor {

    protected static final Logger logger = LoggerFactory.getLogger(BriefingProcessor.class);

    protected final PlaywrightScraperService scraperService;
    protected final ChatLanguageModel chatModel;
    protected final BriefingSourceStrategy sourceStrategy;
    protected final IntelligenceSynthesizer synthesizer;
    protected final DocumentDigester digester;
    protected final DigestDeduplicator deduplicator;
    protected final BriefingCategory category;

    protected BriefingProcessor(PlaywrightScraperService scraperService,
                                ChatLanguageModel chatModel,
                                BriefingSourceStrategy sourceStrategy,
                                IntelligenceSynthesizer synthesizer,
                                DocumentDigester digester,
                                DigestDeduplicator deduplicator,
                                BriefingCategory category) {
        this.scraperService = scraperService;
        this.chatModel = chatModel;
        this.sourceStrategy = sourceStrategy;
        this.synthesizer = synthesizer;
        this.digester = digester;
        this.deduplicator = deduplicator;
        this.category = category;
    }

    public final SynthesisResult process() {
        List<SourceLink> links = sourceStrategy.getLinks(category, getLinkLimit());
        if (links.isEmpty()) {
            throw new IllegalStateException("No signal sources found for: " + category);
        }

        String relevanceContext = category.getPersona().focus();
        List<ArticleDigest> digests = new ArrayList<>();
        int mapInputTokens = 0;
        int mapOutputTokens = 0;

        // Map stage: one bounded LLM call per scraped document.
        for (SourceLink link : links) {
            try {
                String text = scraperService.extractFullText(link.url(), getScrapeDepth());
                if (!isPlausibleContent(text, link.url())) {
                    continue;
                }
                DocumentDigester.DigestResult dr =
                        digester.digest(chatModel, text, link, relevanceContext);
                digests.add(dr.digest());
                mapInputTokens += dr.inputTokens();
                mapOutputTokens += dr.outputTokens();
            } catch (Exception e) {
                // A single failed document must not fail the run.
                logger.warn("Digest failed for {}: {}", link.url(), e.getMessage(), e);
            }
        }

        if (digests.size() < getMinRequiredDigests()) {
            throw new IllegalStateException(
                    "Insufficient situational signal captured: only " + digests.size() + " digests.");
        }

        // Dedup stage, then reduce stage.
        List<ArticleDigest> deduped = deduplicator.dedupe(digests);
        SynthesisResult reduceResult = synthesize(deduped);

        // Fold map-stage token cost into the run total.
        return new SynthesisResult(
                reduceResult.content(),
                reduceResult.inputTokens() + mapInputTokens,
                reduceResult.outputTokens() + mapOutputTokens);
    }

    protected abstract int getLinkLimit();
    protected abstract int getMinRequiredDigests();
    protected abstract int getScrapeDepth();
    protected abstract SynthesisResult synthesize(List<ArticleDigest> digests);

    protected boolean isPlausibleContent(String text, String url) {
        if (text == null || text.length() < 500) return false;
        String lower = text.toLowerCase(Locale.ROOT);
        return !lower.contains("before you continue")
                && !lower.contains("accept all cookies")
                && !url.contains("/about");
    }
}
```

- [ ] **Step 3: Update `StandardBriefingProcessor`**

Replace `hud-backend/src/main/java/com/hud/briefing/StandardBriefingProcessor.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

public class StandardBriefingProcessor extends BriefingProcessor {

    private final BriefingPersona persona;

    public StandardBriefingProcessor(PlaywrightScraperService scraperService,
                                     ChatLanguageModel chatModel,
                                     BriefingSourceStrategy sourceStrategy,
                                     IntelligenceSynthesizer synthesizer,
                                     DocumentDigester digester,
                                     DigestDeduplicator deduplicator,
                                     BriefingCategory category) {
        super(scraperService, chatModel, sourceStrategy, synthesizer, digester, deduplicator, category);
        this.persona = category.getPersona();
    }

    @Override
    protected int getLinkLimit() { return 15; }

    @Override
    protected int getMinRequiredDigests() { return 3; }

    @Override
    protected int getScrapeDepth() { return 0; }

    @Override
    protected SynthesisResult synthesize(List<ArticleDigest> digests) {
        return synthesizer.synthesizeStandard(chatModel, persona, digests);
    }
}
```

- [ ] **Step 4: Update `DeepDiveBriefingProcessor`**

Replace `hud-backend/src/main/java/com/hud/briefing/DeepDiveBriefingProcessor.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;

public class DeepDiveBriefingProcessor extends BriefingProcessor {

    public DeepDiveBriefingProcessor(PlaywrightScraperService scraperService,
                                     ChatLanguageModel chatModel,
                                     BriefingSourceStrategy sourceStrategy,
                                     IntelligenceSynthesizer synthesizer,
                                     DocumentDigester digester,
                                     DigestDeduplicator deduplicator,
                                     BriefingCategory category) {
        super(scraperService, chatModel, sourceStrategy, synthesizer, digester, deduplicator, category);
    }

    @Override
    protected int getLinkLimit() {
        return (category == BriefingCategory.GLOBAL_SITREP) ? 25 : 15;
    }

    @Override
    protected int getMinRequiredDigests() { return 3; }

    @Override
    protected int getScrapeDepth() { return 1; }

    @Override
    protected SynthesisResult synthesize(List<ArticleDigest> digests) {
        if (category == BriefingCategory.GLOBAL_SITREP) {
            return synthesizer.synthesizeGlobalSitrep(chatModel, digests);
        }
        return synthesizer.fuseTheaterIntelligence(chatModel, category, digests);
    }
}
```

- [ ] **Step 5: Update `BriefingProcessorFactory` to inject the digester and deduplicator**

Replace `hud-backend/src/main/java/com/hud/briefing/BriefingProcessorFactory.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

@Component
public class BriefingProcessorFactory {

    private final PlaywrightScraperService scraperService;
    private final BriefingSourceFactory sourceFactory;
    private final IntelligenceSynthesizer synthesizer;
    private final DocumentDigester digester;
    private final DigestDeduplicator deduplicator;

    public BriefingProcessorFactory(PlaywrightScraperService scraperService,
                                    BriefingSourceFactory sourceFactory,
                                    IntelligenceSynthesizer synthesizer,
                                    DocumentDigester digester,
                                    DigestDeduplicator deduplicator) {
        this.scraperService = scraperService;
        this.sourceFactory = sourceFactory;
        this.synthesizer = synthesizer;
        this.digester = digester;
        this.deduplicator = deduplicator;
    }

    public BriefingProcessor getProcessor(BriefingCategory category, ChatLanguageModel model) {
        BriefingSourceStrategy strategy = sourceFactory.getStrategy(category);

        if (isTheaterCategory(category)) {
            return new DeepDiveBriefingProcessor(scraperService, model, strategy, synthesizer,
                    digester, deduplicator, category);
        } else {
            return new StandardBriefingProcessor(scraperService, model, strategy, synthesizer,
                    digester, deduplicator, category);
        }
    }

    private boolean isTheaterCategory(BriefingCategory c) {
        return c == BriefingCategory.THEATER_UKRAINE
                || c == BriefingCategory.THEATER_MIDDLE_EAST
                || c == BriefingCategory.GLOBAL_SITREP;
    }
}
```

- [ ] **Step 6: Rewrite `IntelligenceSynthesizerTest` for digest input**

Replace `hud-backend/src/test/java/com/hud/briefing/IntelligenceSynthesizerTest.java` with:
```java
package com.hud.briefing;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
class IntelligenceSynthesizerTest {

    @Mock private ChatLanguageModel model;
    private IntelligenceSynthesizer synthesizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        synthesizer = new IntelligenceSynthesizer();
    }

    private ArticleDigest digest(String name, SourceTier tier) {
        return new ArticleDigest("http://" + name, name, tier,
                "Headline for " + name, "Dense factual summary.",
                List.of("key fact one"), List.of("Entity A", "Entity B"),
                List.of("2026-05-12: an event"), 0.8, 1, List.of(name));
    }

    @Test
    void shouldSynthesizeStandardBriefing() {
        Response<AiMessage> response = Response.from(AiMessage.from("Narrative Output"), new TokenUsage(10, 5));
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        SynthesisResult result = synthesizer.synthesizeStandard(model,
                BriefingCategory.WORLD_NEWS.getPersona(), List.of(digest("BBC", SourceTier.TIER_1)));

        assertEquals("Narrative Output", result.content());
        assertEquals(10, result.inputTokens());
        assertEquals(5, result.outputTokens());
        verify(model).generate(argThat((UserMessage m) -> m.text().contains("Geopolitical Strategist")));
        verify(model).generate(argThat((UserMessage m) -> m.text().contains("Headline for BBC")));
    }

    @Test
    void shouldFuseTheaterIntelligence() {
        Response<AiMessage> tempo = Response.from(AiMessage.from("Tempo Analysis"), new TokenUsage(10, 5));
        Response<AiMessage> kinetic = Response.from(AiMessage.from("Kinetic Table"), new TokenUsage(10, 5));
        Response<AiMessage> innovations = Response.from(AiMessage.from("Innovations"), new TokenUsage(10, 5));
        when(model.generate(any(UserMessage.class))).thenReturn(tempo, kinetic, innovations);

        SynthesisResult result = synthesizer.fuseTheaterIntelligence(model,
                BriefingCategory.THEATER_UKRAINE, List.of(digest("ISW", SourceTier.TIER_1)));

        assertTrue(result.content().contains("# UKRAINE THEATER REPORT"));
        assertTrue(result.content().contains("## Tactical Momentum"));
        assertTrue(result.content().contains("Tempo Analysis"));
        assertTrue(result.content().contains("Kinetic Table"));
        assertTrue(result.content().contains("Innovations"));
        assertEquals(30, result.inputTokens());
        assertEquals(15, result.outputTokens());
        verify(model, times(3)).generate(any(UserMessage.class));
    }

    @Test
    void shouldSynthesizeGlobalSitrep() {
        Response<AiMessage> response = Response.from(AiMessage.from("Global Overview"), new TokenUsage(20, 10));
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        SynthesisResult result = synthesizer.synthesizeGlobalSitrep(model,
                List.of(digest("CSIS", SourceTier.TIER_1)));

        assertEquals("Global Overview", result.content());
        assertEquals(20, result.inputTokens());
        assertEquals(10, result.outputTokens());
        verify(model).generate(argThat((UserMessage m) -> m.text().contains("Global Theater Strategist")));
    }

    @Test
    void formatsDigestsOrderedByTierThenCorroboration() {
        ArticleDigest tier2 = digest("Verge", SourceTier.TIER_2);
        ArticleDigest tier1 = digest("BBC", SourceTier.TIER_1);

        String block = synthesizer.formatDigestsForPrompt(List.of(tier2, tier1));

        assertTrue(block.contains("INTELLIGENCE DIGESTS (2 sources)"));
        assertTrue(block.indexOf("BBC") < block.indexOf("Verge"), "TIER_1 digest must come first");
    }
}
```

- [ ] **Step 7: Rewrite `BriefingProcessorTest` for the map-reduce flow**

Replace `hud-backend/src/test/java/com/hud/briefing/BriefingProcessorTest.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class BriefingProcessorTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private ChatLanguageModel chatModel;
    @Mock private BriefingSourceStrategy sourceStrategy;
    @Mock private IntelligenceSynthesizer synthesizer;
    @Mock private DocumentDigester digester;
    @Mock private DigestDeduplicator deduplicator;

    private StandardBriefingProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new StandardBriefingProcessor(scraperService, chatModel, sourceStrategy,
                synthesizer, digester, deduplicator, BriefingCategory.WORLD_NEWS);
    }

    private SourceLink link(String url) {
        return new SourceLink(url, "Test Feed", SourceTier.TIER_1);
    }

    private ArticleDigest sampleDigest(String url) {
        return new ArticleDigest(url, "Test Feed", SourceTier.TIER_1, "Headline", "Summary",
                List.of("fact"), List.of("Entity"), List.of("2026-05-10: event"),
                0.8, 1, List.of("Test Feed"));
    }

    private String longContent() {
        return "Valid situational intelligence report that provides enough textual density to satisfy the processor.".repeat(10);
    }

    @Test
    void shouldRunMapDedupReduceSuccessfully() {
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("u1"), link("u2"), link("u3")));
        when(scraperService.extractFullText(anyString(), anyInt())).thenReturn(longContent());
        when(digester.digest(any(), anyString(), any(SourceLink.class), anyString()))
                .thenAnswer(inv -> new DocumentDigester.DigestResult(
                        sampleDigest(((SourceLink) inv.getArgument(2)).url()), 50, 5));
        when(deduplicator.dedupe(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(synthesizer.synthesizeStandard(any(), any(), anyList()))
                .thenReturn(new SynthesisResult("Synthesized Intelligence", 100, 10));

        SynthesisResult result = processor.process();

        assertEquals("Synthesized Intelligence", result.content());
        // Reduce tokens (100/10) plus map tokens (3 x 50 / 3 x 5).
        assertEquals(250, result.inputTokens());
        assertEquals(25, result.outputTokens());
        verify(sourceStrategy).getLinks(BriefingCategory.WORLD_NEWS, 15);
        verify(digester, times(3)).digest(any(), anyString(), any(SourceLink.class), anyString());
        verify(deduplicator).dedupe(anyList());
    }

    @Test
    void shouldThrowExceptionWhenNoLinksFound() {
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt())).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> processor.process());
        assertTrue(ex.getMessage().contains("No signal sources found"));
    }

    @Test
    void shouldThrowExceptionWhenTooFewDigests() {
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("u1")));
        when(scraperService.extractFullText(anyString(), anyInt())).thenReturn(longContent());
        when(digester.digest(any(), anyString(), any(SourceLink.class), anyString()))
                .thenReturn(new DocumentDigester.DigestResult(sampleDigest("u1"), 50, 5));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> processor.process());
        assertTrue(ex.getMessage().contains("Insufficient situational signal"));
    }

    @Test
    void shouldSkipImplausibleContentBeforeDigesting() {
        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("cookie"), link("u1"), link("u2"), link("u3")));
        when(scraperService.extractFullText(eq("cookie"), anyInt()))
                .thenReturn("Before you continue... Accept all cookies");
        when(scraperService.extractFullText(eq("u1"), anyInt())).thenReturn(longContent());
        when(scraperService.extractFullText(eq("u2"), anyInt())).thenReturn(longContent());
        when(scraperService.extractFullText(eq("u3"), anyInt())).thenReturn(longContent());
        when(digester.digest(any(), anyString(), any(SourceLink.class), anyString()))
                .thenAnswer(inv -> new DocumentDigester.DigestResult(
                        sampleDigest(((SourceLink) inv.getArgument(2)).url()), 50, 5));
        when(deduplicator.dedupe(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(synthesizer.synthesizeStandard(any(), any(), anyList()))
                .thenReturn(new SynthesisResult("Done", 100, 10));

        processor.process();

        // The cookie-wall page is never digested; the three valid pages are.
        verify(digester, times(3)).digest(any(), anyString(), any(SourceLink.class), anyString());
    }
}
```

- [ ] **Step 8: Rewrite `DeepDiveBriefingProcessorTest` for the map-reduce flow**

Replace `hud-backend/src/test/java/com/hud/briefing/DeepDiveBriefingProcessorTest.java` with:
```java
package com.hud.briefing;

import com.hud.news.PlaywrightScraperService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class DeepDiveBriefingProcessorTest {

    @Mock private PlaywrightScraperService scraperService;
    @Mock private ChatLanguageModel chatModel;
    @Mock private BriefingSourceStrategy sourceStrategy;
    @Mock private IntelligenceSynthesizer synthesizer;
    @Mock private DocumentDigester digester;
    @Mock private DigestDeduplicator deduplicator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private SourceLink link(String url) {
        return new SourceLink(url, "Theater Feed", SourceTier.TIER_1);
    }

    private ArticleDigest sampleDigest(String url) {
        return new ArticleDigest(url, "Theater Feed", SourceTier.TIER_1, "Headline", "Summary",
                List.of("fact"), List.of("Entity"), List.of("2026-05-10: event"),
                0.8, 1, List.of("Theater Feed"));
    }

    private String longContent() {
        return "Tactical field intelligence from the frontline meeting the processor's content threshold.".repeat(10);
    }

    private void stubMapAndDedup() {
        when(scraperService.extractFullText(anyString(), anyInt())).thenReturn(longContent());
        when(digester.digest(any(), anyString(), any(SourceLink.class), anyString()))
                .thenAnswer(inv -> new DocumentDigester.DigestResult(
                        sampleDigest(((SourceLink) inv.getArgument(2)).url()), 50, 5));
        when(deduplicator.dedupe(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldProcessUkraineTheater() {
        DeepDiveBriefingProcessor processor = new DeepDiveBriefingProcessor(scraperService, chatModel,
                sourceStrategy, synthesizer, digester, deduplicator, BriefingCategory.THEATER_UKRAINE);

        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("a"), link("b"), link("c")));
        stubMapAndDedup();
        when(synthesizer.fuseTheaterIntelligence(any(), any(), anyList()))
                .thenReturn(new SynthesisResult("Fused Intel Report", 100, 10));

        SynthesisResult result = processor.process();

        assertEquals("Fused Intel Report", result.content());
        verify(sourceStrategy).getLinks(BriefingCategory.THEATER_UKRAINE, 15);
        verify(synthesizer).fuseTheaterIntelligence(eq(chatModel), eq(BriefingCategory.THEATER_UKRAINE), anyList());
    }

    @Test
    void shouldProcessGlobalSitrep() {
        DeepDiveBriefingProcessor processor = new DeepDiveBriefingProcessor(scraperService, chatModel,
                sourceStrategy, synthesizer, digester, deduplicator, BriefingCategory.GLOBAL_SITREP);

        when(sourceStrategy.getLinks(any(BriefingCategory.class), anyInt()))
                .thenReturn(List.of(link("a"), link("b"), link("c")));
        stubMapAndDedup();
        when(synthesizer.synthesizeGlobalSitrep(any(), anyList()))
                .thenReturn(new SynthesisResult("Global Summary", 100, 10));

        SynthesisResult result = processor.process();

        assertEquals("Global Summary", result.content());
        verify(sourceStrategy).getLinks(BriefingCategory.GLOBAL_SITREP, 25);
        verify(synthesizer).synthesizeGlobalSitrep(eq(chatModel), anyList());
    }
}
```

- [ ] **Step 9: Update `BriefingProcessorFactoryTest` for the new constructor**

In `hud-backend/src/test/java/com/hud/briefing/BriefingProcessorFactoryTest.java`, add two mocks and pass them to the constructor. Add these fields next to the existing `@Mock` fields:
```java
    @Mock private DocumentDigester digester;
    @Mock private DigestDeduplicator deduplicator;
```
Change the `setUp` factory construction line to:
```java
        factory = new BriefingProcessorFactory(scraperService, sourceFactory, synthesizer, digester, deduplicator);
```
(The three test methods are unchanged.)

- [ ] **Step 10: Run the full backend unit suite**

Run: `mvn test -pl hud-backend`
Expected: BUILD SUCCESS — all unit tests pass, including the rewritten synthesizer and processor tests. No external services are contacted.

- [ ] **Step 11: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing hud-backend/src/test/java/com/hud/briefing
git commit -m "feat: wire map-reduce briefing pipeline over article digests"
```

---

## Task 7: Admin REST API for sources — `NewsSourceController`

**Files:**
- Create: `hud-backend/src/main/java/com/hud/briefing/NewsSourceController.java`
- Create: `hud-backend/src/test/java/com/hud/briefing/NewsSourceControllerTest.java`

`/api/config/**` is already restricted to `ROLE_ADMIN` by a path rule in `SecurityConfig` and CSRF is enabled globally, so no `SecurityConfig` change is needed. The `@PreAuthorize` annotations below are defense-in-depth, consistent with `LlmConfigController`.

- [ ] **Step 1: Write the failing controller test**

Create `hud-backend/src/test/java/com/hud/briefing/NewsSourceControllerTest.java`:
```java
package com.hud.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
class NewsSourceControllerTest {

    @Mock private NewsSourceRepository repository;
    private NewsSourceController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new NewsSourceController(repository);
    }

    private NewsSource source(String name) {
        return new NewsSource(BriefingCategory.WORLD_NEWS, name, "https://feed",
                SourceType.RSS, SourceTier.TIER_1, 100, true);
    }

    @Test
    void shouldReturnAllSources() {
        when(repository.findAll()).thenReturn(List.of(source("BBC")));

        List<NewsSource> result = controller.getAllSources();

        assertEquals(1, result.size());
        assertEquals("BBC", result.get(0).getName());
    }

    @Test
    void shouldSaveSource() {
        NewsSource s = source("NPR");
        when(repository.save(any())).thenReturn(s);

        NewsSource result = controller.saveSource(s);

        assertEquals("NPR", result.getName());
        verify(repository).save(s);
    }

    @Test
    void shouldDeleteSource() {
        controller.deleteSource(5L);
        verify(repository).deleteById(5L);
    }

    @Test
    void shouldToggleActiveStatus() {
        NewsSource s = source("Toggle");
        when(repository.findById(1L)).thenReturn(Optional.of(s));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NewsSource result = controller.toggleActive(1L);

        assertFalse(result.isActive());
        verify(repository).save(s);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -pl hud-backend -Dtest=NewsSourceControllerTest -DexcludedGroups=`
Expected: FAIL — `NewsSourceController` does not exist (compilation error).

- [ ] **Step 3: Create the controller**

Create `hud-backend/src/main/java/com/hud/briefing/NewsSourceController.java`:
```java
package com.hud.briefing;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin CRUD for configurable intelligence sources. Mounted under /api/config,
 * which SecurityConfig restricts to ROLE_ADMIN; @PreAuthorize is defense-in-depth.
 */
@RestController
@RequestMapping("/api/config/sources")
public class NewsSourceController {

    private final NewsSourceRepository repository;

    public NewsSourceController(NewsSourceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<NewsSource> getAllSources() {
        return repository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public NewsSource saveSource(@RequestBody NewsSource source) {
        return repository.save(source);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteSource(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/toggle")
    public NewsSource toggleActive(@PathVariable Long id) {
        NewsSource source = repository.findById(id).orElseThrow();
        source.setActive(!source.isActive());
        return repository.save(source);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -pl hud-backend -Dtest=NewsSourceControllerTest -DexcludedGroups=`
Expected: PASS (all 4 tests).

- [ ] **Step 5: Commit**

```bash
git add hud-backend/src/main/java/com/hud/briefing/NewsSourceController.java \
  hud-backend/src/test/java/com/hud/briefing/NewsSourceControllerTest.java
git commit -m "feat: add admin REST API for news source CRUD"
```

---

## Task 8: Frontend — sources admin UI

**Files:**
- Modify: `hud-frontend/src/components/types.ts`
- Create: `hud-frontend/src/components/SourcesConfig.tsx`
- Create: `hud-frontend/src/components/SourcesConfig.test.tsx`
- Modify: `hud-frontend/src/components/ConfigView.tsx`

- [ ] **Step 1: Add the `NewsSource` types**

Append to `hud-frontend/src/components/types.ts`:
```ts
export type SourceType = 'RSS' | 'ISW' | 'CSIS';
export type SourceTier = 'TIER_1' | 'TIER_2' | 'TIER_3';

export interface NewsSource {
  id?: number;
  category: BriefingCategory;
  name: string;
  url: string;
  type: SourceType;
  tier: SourceTier;
  weight: number;
  active: boolean;
}
```

- [ ] **Step 2: Write the failing component test**

Create `hud-frontend/src/components/SourcesConfig.test.tsx`:
```tsx
import { render, screen, waitFor } from '@testing-library/react';
import { expect, test, vi, beforeEach } from 'vitest';
import { SourcesConfig } from './SourcesConfig';

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=test-token';
});

test('renders configured news sources from the API', async () => {
  const mockSources = [
    {
      id: 1, name: 'BBC World', url: 'https://feeds.bbci.co.uk/news/world/rss.xml',
      category: 'WORLD_NEWS', type: 'RSS', tier: 'TIER_1', weight: 100, active: true,
    },
  ];
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(mockSources),
  }));

  render(<SourcesConfig />);

  await waitFor(() => {
    expect(screen.getByText('BBC World')).toBeInTheDocument();
    expect(screen.getByText(/WORLD NEWS · RSS · TIER 1 · w100/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `cd hud-frontend && npx vitest run src/components/SourcesConfig.test.tsx`
Expected: FAIL — `./SourcesConfig` does not exist.

- [ ] **Step 4: Create the `SourcesConfig` component**

Create `hud-frontend/src/components/SourcesConfig.tsx`:
```tsx
import { useState, useEffect } from 'react';
import type { NewsSource, SourceType, SourceTier, BriefingCategory } from './types';
import { Save, Trash2, Power, Rss, Pencil, PlusCircle } from 'lucide-react';
import { apiFetch } from '../api';

const CATEGORIES: BriefingCategory[] = [
  'WORLD_NEWS', 'US_NEWS', 'FINANCE', 'TECHNOLOGY',
  'GLOBAL_SITREP', 'THEATER_UKRAINE', 'THEATER_MIDDLE_EAST',
];
const TYPES: SourceType[] = ['RSS', 'ISW', 'CSIS'];
const TIERS: SourceTier[] = ['TIER_1', 'TIER_2', 'TIER_3'];

const emptyForm: Partial<NewsSource> = {
  name: '', url: '', category: 'WORLD_NEWS', type: 'RSS', tier: 'TIER_2', weight: 80, active: true,
};

export const SourcesConfig = () => {
  const [sources, setSources] = useState<NewsSource[]>([]);
  const [editing, setEditing] = useState<Partial<NewsSource>>(emptyForm);

  const fetchSources = () => {
    fetch('/api/config/sources')
      .then(res => res.json())
      .then(data => { if (Array.isArray(data)) setSources(data); })
      .catch(() => {});
  };

  useEffect(() => { fetchSources(); }, []);

  const handleSave = () => {
    if (!editing.name || !editing.url) {
      alert('Source name and URL are required.');
      return;
    }
    apiFetch('/api/config/sources', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editing),
    })
      .then(res => res.json())
      .then(() => { fetchSources(); setEditing(emptyForm); });
  };

  const handleEdit = (s: NewsSource) => {
    setEditing({ ...s });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleToggle = (id: number) => {
    apiFetch(`/api/config/sources/${id}/toggle`, { method: 'POST' }).then(() => fetchSources());
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Delete this source?')) {
      apiFetch(`/api/config/sources/${id}`, { method: 'DELETE' }).then(() => fetchSources());
    }
  };

  return (
    <div className="sources-config" style={{ marginTop: '3rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3>{editing.id ? 'Edit News Source' : 'Add News Source'}</h3>
        {editing.id && (
          <button className="suggestion-btn" onClick={() => setEditing(emptyForm)}>
            <PlusCircle size={12} style={{ marginRight: '4px' }} /> Create New Instead
          </button>
        )}
      </div>

      <div className="config-form card">
        <label htmlFor="source-name">Source Name
          <input id="source-name" value={editing.name}
            onChange={e => setEditing({ ...editing, name: e.target.value })}
            placeholder="e.g., BBC World" autoComplete="off" />
        </label>
        <label htmlFor="source-url">Feed URL / ISW Theater Keyword
          <input id="source-url" value={editing.url}
            onChange={e => setEditing({ ...editing, url: e.target.value })}
            placeholder="https://...  (or ukraine / mideast / global for ISW)" autoComplete="off" />
        </label>
        <label htmlFor="source-category">Category
          <select id="source-category" value={editing.category}
            onChange={e => setEditing({ ...editing, category: e.target.value as BriefingCategory })}>
            {CATEGORIES.map(c => <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>)}
          </select>
        </label>
        <label htmlFor="source-type">Source Type
          <select id="source-type" value={editing.type}
            onChange={e => setEditing({ ...editing, type: e.target.value as SourceType })}>
            {TYPES.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </label>
        <label htmlFor="source-tier">Quality Tier
          <select id="source-tier" value={editing.tier}
            onChange={e => setEditing({ ...editing, tier: e.target.value as SourceTier })}>
            {TIERS.map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
          </select>
        </label>
        <label htmlFor="source-weight">Weight (selection priority)
          <input id="source-weight" type="number" value={editing.weight}
            onChange={e => setEditing({ ...editing, weight: parseInt(e.target.value) || 0 })} />
        </label>
        <button className="save-btn" onClick={handleSave}>
          <Save size={16} /> {editing.id ? 'Update Source' : 'Save New Source'}
        </button>
      </div>

      <h3 style={{ marginTop: '2rem' }}>Configured Sources ({sources.length})</h3>
      <div className="brain-list">
        {sources.map(s => (
          <div key={s.id} className={`brain-card ${s.active ? 'active' : ''}`}>
            <div className="brain-info">
              <Rss size={20} color={s.active ? '#3fb950' : '#8b949e'} />
              <div>
                <h4>{s.name}</h4>
                <code>{s.category.replace(/_/g, ' ')} · {s.type} · {s.tier.replace('_', ' ')} · w{s.weight}</code>
              </div>
            </div>
            <div className="brain-actions">
              <button onClick={() => handleEdit(s)} title="Edit Source">
                <Pencil size={18} color="#8b949e" />
              </button>
              <button onClick={() => handleToggle(s.id!)} title={s.active ? 'Disable' : 'Enable'}>
                <Power size={18} color={s.active ? '#3fb950' : '#f85149'} />
              </button>
              <button onClick={() => handleDelete(s.id!)} title="Delete Source">
                <Trash2 size={18} color="#8b949e" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd hud-frontend && npx vitest run src/components/SourcesConfig.test.tsx`
Expected: PASS.

- [ ] **Step 6: Mount `SourcesConfig` in `ConfigView`**

In `hud-frontend/src/components/ConfigView.tsx`, add the import next to the `SchedulingConfig` import:
```tsx
import { SourcesConfig } from './SourcesConfig';
```
In the JSX, add `<SourcesConfig />` immediately after `<SchedulingConfig />` inside the `config-form-section`:
```tsx
          <SchedulingConfig />
          <SourcesConfig />
          <SecuritySettings />
```

- [ ] **Step 7: Build and test the frontend**

Run: `cd hud-frontend && npm run build && npx vitest run`
Expected: `tsc -b` and `vite build` succeed with no type errors; all Vitest tests pass.

- [ ] **Step 8: Commit**

```bash
git add hud-frontend/src/components/types.ts \
  hud-frontend/src/components/SourcesConfig.tsx \
  hud-frontend/src/components/SourcesConfig.test.tsx \
  hud-frontend/src/components/ConfigView.tsx
git commit -m "feat: add admin UI for managing tiered news sources"
```

---

## Final Verification

- [ ] **Step 1: Full default build**

Run: `mvn clean install`
Expected: BUILD SUCCESS for `hud-frontend` and `hud-backend`; backend runs unit tests only (integration tests excluded by the Phase 1 Surefire config); frontend runs Vitest including `SourcesConfig.test.tsx`.

- [ ] **Step 2: Migration + entity drift check**

Run: `mvn test -pl hud-backend -Pintegration -Dtest=MigrationIntegrationTest`
Expected: PASS — Flyway applies V1 + V2 + V3 against Testcontainers Postgres, the seed rows load, and `ddl-auto=validate` confirms the `NewsSource` entity matches the `news_sources` table. (Requires Docker.)

- [ ] **Step 3: Smoke-test the running stack**

Run `./bin/deploy.sh --build`, log in at `http://localhost:8889`, open the admin **Config** tab, and confirm the **News Sources** section lists the 25 seeded sources. Add a source, toggle one off, and edit a tier — confirm each change persists after a refresh. Trigger a briefing run from the **Config** tab and confirm in **Observability** that the run reaches `SUCCESS` with non-zero token counts (these now include the map-stage digest calls).

- [ ] **Step 4: Update the README**

In `README.md`, document that intelligence feeds are managed in the **Config → News Sources** admin UI (no longer hardcoded), that each source has a quality tier and weight, and that the briefing pipeline now runs a map-reduce flow (per-document digest → dedup → synthesis) so briefing breadth scales with the number of sources. Commit:
```bash
git add README.md
git commit -m "docs: document DB-backed sources and the map-reduce briefing pipeline"
```

---

## Notes for the Implementer

- All commands assume the repo root `/home/jakefear/source/hud` unless a `cd` is shown.
- `-DexcludedGroups=` on per-task test runs lets a single test class run despite the Phase 1 default `integration` exclusion; it is harmless for unit tests.
- The branch and PR policy follow Phase 1: commit per task, do not open a PR until the user asks. Branch first if on `master`.
- Tasks 3 and 6 are the coordinated refactors; the build is intentionally red between their internal steps and green only at each task's final test step. Do not split a commit before that point.
- `ArticleDigest` is transient by design — do not add `@Entity` or a repository for it.
- Phase 3 (deeper crawl, bounded `CrawlBudget`, `PipelineRun` budget columns, Observability extensions) is out of scope here and gets its own plan.
