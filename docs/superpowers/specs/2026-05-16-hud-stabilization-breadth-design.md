# HUD Stabilization + Breadth Upgrade — Design

**Date:** 2026-05-16
**Status:** Approved (design); pending implementation planning
**Branch:** `feat/stabilization-breadth-upgrade`

## Summary

Two independent efforts delivered in three phases:

- **Part A — Stabilization & Security (Phase 1):** close release-blocking security and
  reliability gaps. Independent of Part B; ships first.
- **Part B — Crawl Breadth & Depth (Phases 2–3):** replace the "concatenate raw text
  into one prompt" briefing pipeline with a map-reduce architecture over DB-backed,
  tiered sources, with deeper crawling under an enforced budget.

This document is the single design of record. Each phase will get its own
implementation plan (via the writing-plans skill) because the combined scope is too
large for one plan.

## Goals

- Make a fresh HUD deployment safe by default (no shipped credentials, CSRF on,
  schema under migration control).
- Make `mvn test` green without external services.
- Increase the breadth of intelligence that reaches the final briefing — more sources,
  deeper crawl — without being limited by the LLM context window.
- Keep crawl cost bounded and observable.

## Non-Goals

- Switching authentication to stateless tokens/JWT.
- Multi-tenancy or per-user data isolation.
- Replacing Playwright or the LLM provider abstraction.
- Embedding-based relevance ranking (considered, deferred — heuristic dedup first).

---

## Part A — Stabilization & Security (Phase 1)

### A1. Bootstrap credentials

**Problem:** `DatabaseSeeder.java:55` seeds an `ROLE_ADMIN` user with password `admin`
on every fresh database. `AuthController.java:52` permits 4-character passwords.

**Design:**
- `DatabaseSeeder` reads `HUD_ADMIN_PASSWORD` from the environment. If unset, it
  generates a random password and logs it **once** at WARN level on first seed only.
- Add `passwordChangeRequired` (boolean) to the `AppUser` entity, defaulting `true`
  for the seeded admin.
- `AuthController` rejects all requests other than `auth/status`, `auth/password`,
  and `auth/logout` while the current user has `passwordChangeRequired = true`,
  returning a distinguishable status so the SPA can react.
- The SPA routes a `passwordChangeRequired` user to a forced change-password view.
- Raise the `AuthController` password minimum from 4 to 12 characters and require at
  least one letter and one digit.

### A2. Schema migrations

**Problem:** `application.yml:10` uses Hibernate `ddl-auto: update` with no migration
tooling, no baseline, no rollback path.

**Design:**
- Add Flyway (Spring Boot starter integration).
- Generate `V1__baseline.sql` from the current Hibernate-inferred schema; verify it
  matches a clean `update` run before committing.
- Production profile: `ddl-auto: validate`, Flyway enabled.
- Test profile: keep H2 with `ddl-auto: create-drop` and `spring.flyway.enabled=false`
  (Flyway SQL is Postgres-dialect). Add one Testcontainers-Postgres test that runs the
  full migration set to catch drift between entities and migrations.
- `bootstrap_global.sql` / `bootstrap_history.sql` are **environment data, not
  schema** — they leave version control (see A5) and are regenerated/loaded via the
  existing `harvest_global.py` / `harvest_history.py` scripts, documented in the README.

### A3. CSRF

**Problem:** `SecurityConfig.java:25` disables CSRF on a cookie-session app; all admin
mutations are forgeable.

**Design:**
- Re-enable CSRF using `CookieCsrfTokenRepository.withHttpOnlyFalse()` so the SPA can
  read the `XSRF-TOKEN` cookie.
- Add an `apiFetch()` wrapper in the frontend that reads `XSRF-TOKEN` and sets the
  `X-XSRF-TOKEN` header on all mutating (`POST`/`PUT`/`DELETE`) requests. Migrate
  existing `fetch` mutation calls to it.
- Login/logout flows remain unchanged; the token is issued on session creation.

### A4. CI-safe tests

**Problem:** Smoke/integration/E2E tests need live Ollama, a Gemini key, and minutes
of wall-clock, with no gating; `mvn test` fails without that environment.

**Design:**
- Audit every test under `hud-backend/src/test`. Tag any test that needs an external
  service or is long-running (`OllamaConnectionSmokeTest`, `GeminiIntegrationTest`,
  `FullIntelligenceLifecycleE2E`, `ModelComparisonTest`, `*DebugTest`, UI inspection
  tests) with `@Tag("integration")`. Confirm fast, self-contained tests are
  `@Tag("unit")`.
- Configure Surefire with a default `<excludedGroups>integration</excludedGroups>`.
- Add a Maven `integration` profile (and keep `./bin/test.sh --int`) that clears the
  exclusion to run the full suite.
- Result: `mvn test` is green by default with no external dependencies.

### A5. Repository hygiene

**Problem:** Root-level CSVs, `cookies.txt` (a localhost `JSESSIONID`), and multi-MB
`bootstrap_*.sql` files are tracked.

**Design:**
- `git rm --cached` the root CSV files, `cookies.txt`, and `bootstrap_*.sql`.
- Add patterns to `.gitignore`: `*.csv` at root, `cookies.txt`, `bootstrap_*.sql`.
- `hud-backend/.key` is already correctly ignored — no change.
- Document in the README how to regenerate/load bootstrap data.

### A6. Playwright resource leak

**Problem:** `PlaywrightScraperService.executeInBrowser()` wraps `Playwright`,
`Browser`, and `BrowserContext` in try-with-resources but not the `Page`.

**Design:** Wrap `Page` from `context.newPage()` in try-with-resources so it is closed
on every path including exceptions.

### A7. Async executor configuration

**Problem:** `@EnableAsync` relies on the default Spring Boot pool; long-running LLM
tasks (10-minute timeout) and the model×category fan-out create an unbounded backlog.

**Design:**
- Define two explicit named `ThreadPoolTaskExecutor` beans:
  - `briefingExecutor` — orchestration of category/model runs.
  - `scrapeExecutor` — concurrent document scraping for the map step (B2/B4).
- Each has a bounded core/max pool and bounded queue with `CallerRunsPolicy`, so
  overload applies back-pressure rather than silently queueing without limit.
- Annotate `@Async` methods with the explicit executor name.

### A8. SQL logging

**Problem:** `show-sql: true` in the shared `application.yml`.

**Design:** Set `show-sql: false` by default; SQL logging moves behind a `dev` Spring
profile for local debugging.

### A9. Method-level authorization

**Problem:** Admin endpoints are protected only by path matching in `SecurityConfig`.

**Design:** Enable `@EnableMethodSecurity` and add `@PreAuthorize("hasRole('ADMIN')")`
to admin controller methods (`LlmConfigController`, `SchedulingController`,
`PipelineController` mutations, `MacroMetricsController` triggers, briefing triggers)
as defense-in-depth alongside the existing path rules.

### A10. API key masking

**Problem:** `LlmConfigController` reveals the first and last 4 characters of stored
API keys on read.

**Design:** Mask keys fully (`********`) on read. The existing "preserve old key when
an update arrives with a masked value" behavior is retained.

---

## Part B — Crawl Breadth & Depth (Phases 2–3)

### B1. DB-backed, tiered sources (Phase 2)

**Problem:** Source feeds are hardcoded in `GeneralRssSourceStrategy` and
`TheaterSourceStrategy`. Adding a source requires a code change.

**Design:**
- New JPA entity `NewsSource`: `id`, `category` (`BriefingCategory`), `name`, `url`,
  `type` (`SourceType` enum: `RSS`, `HTML_INDEX`, `ISW`, `CSIS`), `tier`
  (`SourceTier` enum: `TIER_1`, `TIER_2`, `TIER_3`), `weight` (int), `active`.
- New `NewsSourceRepository`.
- A Flyway migration seeds the currently hardcoded feeds as `NewsSource` rows with
  assigned tiers (primary wires/agencies = Tier 1, analysis sites = Tier 2,
  supplementary = Tier 3).
- `BriefingSourceStrategy` implementations are refactored to read from
  `NewsSourceRepository` (filtered by category and `active`) instead of hardcoded
  lists. The strategy/factory structure is preserved; only the source of the list
  changes. `SourceType` determines which scraping path is used.
- Tier and weight feed selection and ranking in the reduce step (B2).

**UI:** `NewsSource` gets full CRUD in the admin **Config** tab.
- New REST controller `NewsSourceController` at `/api/config/sources`: `GET` list,
  `POST` create/update, `DELETE /{id}`, `POST /{id}/toggle`. Admin-only
  (`@PreAuthorize`), CSRF-protected.
- New frontend component `SourcesConfig.tsx`, mounted within `ConfigView.tsx`
  alongside the existing brain and schedule configuration. Supports add/edit/delete,
  active toggle, and tier/weight editing per source.

### B2. Map-reduce summarization (Phase 2 — core change)

**Problem:** `BriefingProcessor.acquireSignal()` concatenates all scraped text and
truncates at 2 MB before a single synthesis prompt; content beyond the limit is
silently lost, and breadth is capped by the context window.

**Design:**
- **Map stage** — new component `DocumentDigester`. For each scraped document it makes
  one bounded LLM call producing an `ArticleDigest` record:
  `sourceUrl`, `sourceName`, `tier`, `keyFacts` (list), `entities` (list),
  `datedEvents` (list), `relevanceScore` (0–1 vs. the category query),
  `summary` (≈300–500 tokens). Output length is bounded by prompt instruction.
- **Dedup stage** — near-duplicate digests covering the same event are merged using a
  cheap heuristic (normalized title + entity-set overlap above a threshold). The
  surviving digest carries a `corroborationCount` and the list of contributing
  sources. (Embedding-based dedup is a deferred non-goal.)
- **Reduce stage** — `IntelligenceSynthesizer` reduce methods are changed to accept
  `List<ArticleDigest>` instead of a raw concatenated string. Digests are small enough
  that all of them fit in one synthesis prompt regardless of how many documents were
  crawled — this is the breadth gain. Existing persona prompts and the 3-stage theater
  fusion become the reduce stage; the fusion operates over digests. Higher-tier and
  higher-corroboration digests are ordered/weighted first in the prompt.
- `BriefingProcessor` template method becomes: get links → scrape documents →
  **map (digest)** → **dedup** → **reduce (synthesize)**. `StandardBriefingProcessor`
  and `DeepDiveBriefingProcessor` keep their roles; only the synthesis input changes.

### B3. Deeper crawl (Phase 3)

**Problem:** Crawl depth is fixed (`getScrapeDepth()` returns 0 or 1) and deep crawl
follows a hardcoded 3 same-host links.

**Design:**
- Crawl depth and per-level link fan-out become per-category configuration (persisted
  alongside `NewsSource` configuration or as category settings; final location decided
  in the Phase 3 plan).
- `PlaywrightScraperService` gains a deduplicated crawl frontier: a canonicalized
  visited-URL set prevents re-scraping within a run.
- SSRF safety: same-host restriction in `isValidForDeepCrawl()` is retained, extended
  by an explicit allowlist of known-good external domains for cross-domain links.

### B4. Bounded budget (Phase 3)

**Problem:** Deeper/wider crawling multiplies scrape time and LLM token cost with no
ceiling.

**Design:**
- New value object `CrawlBudget` per category run: `maxDocuments`, `maxDepth`,
  `maxWallClock` (duration), `maxLlmTokens` (input + output). Budgets are configurable
  per category.
- The pipeline tracks consumption across the map stage and stops gracefully when a
  limit is reached, producing a **partial briefing with an explicit note** rather than
  failing the run.
- `PipelineRun` is extended with `documentsScraped`, `documentsDigested`,
  `tokensConsumed`, `durationMs`, and a `budgetCapped` boolean.
- The map stage runs with bounded parallelism on `scrapeExecutor` (A7) so wall-clock
  stays within budget.

### Observability

`ObservabilityView` is extended to show, per `PipelineRun`: documents scraped vs.
digested, dedup reduction, budget consumption, and the `budgetCapped` flag. This makes
the breadth/cost trade-off visible to operators.

---

## Data Model Changes

| Entity | Change |
|--------|--------|
| `AppUser` | + `passwordChangeRequired` boolean |
| `PipelineRun` | + `documentsScraped`, `documentsDigested`, `tokensConsumed`, `durationMs`, `budgetCapped` |
| `NewsSource` | **new** — `category`, `name`, `url`, `type`, `tier`, `weight`, `active` |
| `ArticleDigest` | **new** — transient/record DTO carrying map-stage output (not necessarily persisted; persistence decided in the Phase 2 plan) |

All schema changes ship as Flyway migrations once A2 is in place.

## API Changes

- `NewsSourceController` at `/api/config/sources` — admin CRUD + toggle (Phase 2).
- All mutating endpoints become CSRF-protected (A3) and carry explicit
  `@PreAuthorize` (A9).

## Error Handling

- Scrape failures are distinguished from empty results: a failed document is recorded
  on the `PipelineRun` and excluded from digests, rather than silently contributing an
  empty string. A run with some failed documents still succeeds if it meets the
  minimum-content threshold.
- Budget exhaustion is an expected, non-error outcome: the run completes with
  `budgetCapped = true` and a note in the briefing.
- LLM/digest failures for a single document are logged with stack traces and skipped;
  the run continues.

## Testing Strategy

- **Unit** (`@Tag("unit")`, runs in `mvn test`): credential seeding logic, password
  validation, CSRF wrapper, `NewsSource` strategy selection, `DocumentDigester`
  prompt construction (mocked model), dedup heuristic, `CrawlBudget` enforcement,
  budget accounting on `PipelineRun`.
- **Integration** (`@Tag("integration")`, `-Pintegration`): Flyway migration run
  against Testcontainers Postgres; end-to-end map-reduce with a live/local model;
  existing Ollama/Gemini smoke tests.
- The migration-vs-entities drift test (A2) runs in the integration profile.

## Sequencing

| Phase | Scope | Depends on |
|-------|-------|-----------|
| 1 | A1–A10 stabilization | — |
| 2 | B1 DB-backed tiered sources (+ UI), B2 map-reduce | A2 (migrations), A7 (executors) |
| 3 | B3 deeper crawl, B4 bounded budget, observability | Phase 2 |

Each phase gets its own implementation plan. Phase 1 is shippable on its own.

## Open Questions / Decisions Deferred to Plans

- Whether `ArticleDigest` is persisted or kept transient (Phase 2 plan).
- Exact storage location of per-category crawl/budget settings — extend `NewsSource`,
  reuse `BriefingSchedule`, or a new settings entity (Phase 3 plan).
- Concrete budget default values per category (Phase 3 plan).
