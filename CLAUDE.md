# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

HUD (Heads-Up Display) is an intelligence aggregator: it scrapes news/market sources, synthesizes daily briefings through configurable LLMs, and surfaces them in a React SPA. It is a multi-module Maven project where the frontend is built and embedded inside the backend JAR.

## Commands

Wrapper scripts in `bin/` are location-independent and the preferred entry points; raw `mvn` is fine too.

```bash
./bin/build.sh --clean        # Full clean build with tests + static analysis
./bin/build.sh --fast         # Build, skipping tests/PMD/SpotBugs/JaCoCo
./bin/test.sh                 # All tests + JaCoCo report
./bin/test.sh --unit          # Only tests tagged @Tag("unit")
./bin/test.sh --int           # Only @Tag("integration") tests (needs Docker/local models)
./bin/deploy.sh --build       # Rebuild artifacts, then docker compose up
./bin/db-backup.sh / db-restore.sh   # pg_dump / restore against the hud-db container
```

Raw equivalents and finer-grained commands:

```bash
mvn clean install -DskipTests                       # Build the JAR (skip tests)
mvn test -pl hud-backend -Dtest=PredictionServiceTest          # Single backend test class
mvn test -pl hud-backend -Dtest=PredictionServiceTest#methodName   # Single test method
cd hud-frontend && npm run dev                      # Vite dev server (frontend only)
cd hud-frontend && npx vitest run src/App.test.tsx  # Single frontend test file
cd hud-frontend && npm run lint                     # ESLint
```

Build/run notes:
- `mvn install` builds `hud-frontend` first: `frontend-maven-plugin` installs Node, runs `npm install`, `npm test` (test phase), `npm run build` (compile phase). The `dist/` output is copied into the backend JAR at `META-INF/resources` so Spring Boot serves the SPA.
- The JAR is built **on the host** and run inside the `mcr.microsoft.com/playwright/java` Docker image, which carries the browser system dependencies Playwright needs. Don't expect Playwright scraping to work in a bare local run without those deps.
- App listens on `8888` in-container, exposed as **8889** on the host. PostgreSQL is exposed on host port **6432**.
- Static analysis (PMD `pmd-ruleset.xml`, SpotBugs `spotbugs-exclude.xml`, JaCoCo) runs during the build but is non-failing (`failOnViolation`/`failOnError` = false).

## Database

- Production/dev: PostgreSQL via Docker (`huduser`/`hudpass`, db `hud`, named volume `hud_db_data`). `docker compose down -v` wipes all state.
- Tests: H2 in-memory (`application-test.yml`); some integration tests use Testcontainers (`postgresql`).
- Schema is managed by Hibernate `ddl-auto: update` — there are no migration files.
- `bootstrap_global.sql` / `bootstrap_history.sql` are seed datasets; `harvest_global.py` / `harvest_history.py` regenerate them by pulling Yahoo Finance history.

## Architecture

The backend (`com.hud`) has two domains:
- `com.hud.briefing` — the LLM briefing pipeline, auth, model config, scheduling.
- `com.hud.news` — scrapers, market data, macro metrics, predictive analytics.

### Multi-Brain LLM engine

Models are **database-backed**, not hardcoded. `LlmConfig` rows describe a "brain" (provider + connection params); `DynamicLlmService` builds a runtime `ChatLanguageModel` from a config via the provider abstraction (`LlmProvider` → `OllamaModelProvider`, `GeminiModelProvider`). `DatabaseSeeder` seeds a default "Local Gemma" brain from `application.yml` on first startup. Multiple brains can be active simultaneously; the pipeline runs every active brain.

### Briefing pipeline

`AutomatedBriefingService` orchestrates runs — cron `0 0 6 * * *` (daily) plus on-demand triggers, all `@Async`. For each active model × each `BriefingCategory`:
1. `BriefingProcessorFactory` builds a `BriefingProcessor` — `StandardBriefingProcessor` for general news, `DeepDiveBriefingProcessor` for theater/SITREP categories.
2. The processor scrapes sources (`BriefingSourceStrategy` selects URLs; `PlaywrightScraperService` fetches), then calls `IntelligenceSynthesizer` to produce a `SynthesisResult`.
3. Output is persisted as a `DailyBriefing` (markdown + rendered HTML via `MarkdownService`). **`modelName` is part of the briefing's identity** — each brain produces its own briefing per date/category, enabling side-by-side comparison.
4. Every category run is tracked by a `PipelineRun` (status, timing, token counts, error cause-chain) — this feeds the Observability tab.

After daily briefings, `PredictionService` generates market predictions.

### Strategy/Factory pattern

Both scraping and briefing source selection are pluggable: `ScraperStrategy<T>` (e.g. `YahooFinanceScraperStrategy`, `IswScraperStrategy`, `FredYieldScraperStrategy`) and `BriefingSourceStrategy` (`GeneralRssSourceStrategy`, `TheaterSourceStrategy`) are dispatched by factories. Add a new source by adding a strategy, not by editing the processor.

### Frontend

React 19 + Vite + TypeScript SPA (`hud-frontend/src`). `App.tsx` holds routing and shared state (auth, configs, briefing cache keyed by selected model). Routes: `/news`, `/theaters`, `/investments`, plus admin-only `/config` and `/observability`. The "Brain" switcher in the header re-fetches briefings for the chosen `modelName`.

### Security

`SecurityConfig` — form login at `/api/auth/login`, BCrypt passwords, CSRF disabled. Public: SPA routes, `GET` news/briefings/investments endpoints. `ROLE_ADMIN` required for trigger, config, and pipeline endpoints. The frontend gates admin UI on `/api/auth/status`.

## Conventions

- TDD: write tests before implementing features.
- Use Playwright (Java) for dynamic-content scraping; jsoup/readability4j for static parsing.
- Tag tests `@Tag("unit")` or `@Tag("integration")` so `./bin/test.sh --unit/--int` can filter them.
