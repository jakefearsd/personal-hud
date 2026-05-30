# Macro Intelligence Center: Insights Engine Design

## 1. Overview
Replace the existing Historical Correlation Engine with a new "Strategic Financial Considerations" insights section. This section will gather world events over a 6-12 week window using a hybrid API/scraping approach, summarize them using an LLM, and provide strategic insights for investors on a weekly cadence. Additionally, educational descriptions will be added to the four Macro Pods.

## 2. Frontend Architecture
- **Macro Pod Updates**:
  - Extend the `MacroPod` TypeScript interface and backend DTO to include `educationalDescription` (2-3 sentences) and `learnMoreLink`.
  - Update `MacroPodCard.tsx` to render this educational context below the metric values.
- **Removing Old Components**:
  - Delete `ComparisonDashboard.tsx` and any associated frontend logic for the Historical Correlation Engine.
- **New Insights Component**:
  - Create `StrategicInsightsView.tsx` to render below the Macro Pods.
  - Fetch data from `GET /api/investments/insights/latest`.
  - Display the "12-Week Macro Narrative" and a bulleted list of "Key Considerations for Investors."

## 3. Backend Architecture & Data Flow
- **Code Cleanup**:
  - Delete `EventCorrelationService.java`, `MarketEventRepository.java`, and the `MarketEvent` entity.
  - Remove the Flyway migrations/tables associated with `market_events` if possible, or drop the table in a new migration.
  - Remove the `/api/investments/correlate` endpoint.
- **Database Schema**:
  - Create a new Flyway migration for a `weekly_insights` table:
    - `id` (UUID, primary key)
    - `narrative_text` (TEXT)
    - `key_considerations` (JSONB - array of string bullets)
    - `analysis_start_date` (TIMESTAMP)
    - `analysis_end_date` (TIMESTAMP)
    - `generated_at` (TIMESTAMP)
  - Create the corresponding JPA Entity `WeeklyInsight`.
- **Hybrid Data Collection Pipeline (`WeeklyInsightsPipeline`)**:
  - Runs via Spring Boot `@Scheduled` annotation on a weekly basis (e.g., Saturday at midnight).
  - **Step A (Broad Discovery)**: `NewsDiscoveryService` hits a news API (e.g., NewsAPI or AlphaVantage) to fetch a broad list of macroeconomic events from the past 6-12 weeks.
  - **Step B (Deep Dive)**: The pipeline filters for the highest-impact URLs and passes them to the existing `PlaywrightScraperService` to extract the full text.
  - **Step C (LLM Synthesis)**: `InsightsGenerationService` combines the scraped full-text articles with the historical macro metrics and prompts the LLM to generate the final structured insight (narrative + considerations).
  - The resulting object is saved to the `weekly_insights` table.
- **API Endpoints**:
  - `GET /api/investments/insights/latest`: Returns the most recent `WeeklyInsight` row.
  - `POST /api/investments/insights/trigger`: Admin-only endpoint to force a manual generation run.

## 4. Error Handling & Testing
- The frontend will display a graceful fallback if the insights fail to load.
- If Playwright scraping fails for a specific article, the pipeline will log a warning and continue with the remaining articles.
- The pipeline will be fully unit tested, mocking out the external API and LLM responses.
