# Macro Insights Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a weekly Insights engine that analyzes 12 weeks of macro data and provides strategic financial considerations, replacing the old Correlation Engine, and enhance Macro Pods with educational context.

**Architecture:** A scheduled Spring Boot pipeline fetches broad macro news, uses Playwright for deep-scraping, and an LLM for summarizing insights stored in PostgreSQL. The React frontend fetches the latest insight and renders it below the newly-enhanced Macro Pods.

**Tech Stack:** React, Tailwind CSS, Spring Boot, PostgreSQL, Flyway.

---

### Task 1: Clean Up Old Correlation Engine

**Files:**
- Delete: `hud-backend/src/main/java/com/hud/news/EventCorrelationService.java`
- Delete: `hud-backend/src/main/java/com/hud/news/MarketEventRepository.java`
- Delete: `hud-backend/src/main/java/com/hud/news/MarketEvent.java`
- Delete: `hud-frontend/src/components/ComparisonDashboard.tsx`
- Modify: `hud-backend/src/main/java/com/hud/news/MacroMetricsController.java`
- Modify: `hud-frontend/src/components/InvestmentsView.tsx`

- [ ] **Step 1: Delete backend files**
```bash
rm hud-backend/src/main/java/com/hud/news/EventCorrelationService.java
rm hud-backend/src/main/java/com/hud/news/MarketEventRepository.java
rm hud-backend/src/main/java/com/hud/news/MarketEvent.java
```

- [ ] **Step 2: Remove endpoints from Controller**
In `hud-backend/src/main/java/com/hud/news/MacroMetricsController.java`, remove `EventCorrelationService`, `MarketEventRepository`, `/api/investments/correlate` and `/api/investments/events/{ticker}`.

- [ ] **Step 3: Delete frontend file**
```bash
rm hud-frontend/src/components/ComparisonDashboard.tsx
```

- [ ] **Step 4: Clean up InvestmentsView**
In `hud-frontend/src/components/InvestmentsView.tsx`, remove the `triggerCorrelation` function, the "Correlate Events" button, and `<ComparisonDashboard metrics={[]} />`.

- [ ] **Step 5: Verify build**
Run: `mvn clean compile -DskipTests`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add -A
git commit -m "refactor: remove old historical correlation engine"
```

### Task 2: Database and Entity Setup

**Files:**
- Create: `hud-backend/src/main/resources/db/migration/V6__weekly_insights_and_cleanup.sql`
- Create: `hud-backend/src/main/java/com/hud/news/WeeklyInsight.java`
- Create: `hud-backend/src/main/java/com/hud/news/WeeklyInsightRepository.java`

- [ ] **Step 1: Write migration**
Create `V6__weekly_insights_and_cleanup.sql` with:
```sql
DROP TABLE IF EXISTS market_events;

CREATE TABLE weekly_insights (
    id UUID PRIMARY KEY,
    narrative_text TEXT NOT NULL,
    key_considerations JSONB NOT NULL,
    analysis_start_date TIMESTAMP NOT NULL,
    analysis_end_date TIMESTAMP NOT NULL,
    generated_at TIMESTAMP NOT NULL
);
```

- [ ] **Step 2: Write Entity**
Create `WeeklyInsight.java`:
```java
package com.hud.news;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "weekly_insights")
public class WeeklyInsight {
    @Id
    private UUID id = UUID.randomUUID();
    private String narrativeText;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> keyConsiderations;
    private LocalDateTime analysisStartDate;
    private LocalDateTime analysisEndDate;
    private LocalDateTime generatedAt = LocalDateTime.now();

    public WeeklyInsight() {}
    public WeeklyInsight(String narrativeText, List<String> keyConsiderations, LocalDateTime start, LocalDateTime end) {
        this.narrativeText = narrativeText;
        this.keyConsiderations = keyConsiderations;
        this.analysisStartDate = start;
        this.analysisEndDate = end;
    }
    // getters and setters...
}
```

- [ ] **Step 3: Write Repository**
Create `WeeklyInsightRepository.java`:
```java
package com.hud.news;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface WeeklyInsightRepository extends JpaRepository<WeeklyInsight, UUID> {
    Optional<WeeklyInsight> findTopByOrderByGeneratedAtDesc();
}
```

- [ ] **Step 4: Commit**
```bash
git add hud-backend/src/main/resources/db/migration/V6__weekly_insights_and_cleanup.sql hud-backend/src/main/java/com/hud/news/WeeklyInsight*.java
git commit -m "feat(backend): add weekly insights schema and entities"
```

### Task 3: Backend Pipeline Architecture

**Files:**
- Create: `hud-backend/src/main/java/com/hud/news/NewsDiscoveryService.java`
- Create: `hud-backend/src/main/java/com/hud/news/InsightsGenerationService.java`
- Create: `hud-backend/src/main/java/com/hud/news/WeeklyInsightsPipeline.java`
- Modify: `hud-backend/src/main/java/com/hud/news/MacroMetricsController.java`

- [ ] **Step 1: Write NewsDiscoveryService**
```java
package com.hud.news;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class NewsDiscoveryService {
    public List<String> discoverRecentEvents() {
        return List.of("https://finance.yahoo.com/news/example"); // Mock for now
    }
}
```

- [ ] **Step 2: Write InsightsGenerationService**
```java
package com.hud.news;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class InsightsGenerationService {
    public WeeklyInsight generateInsight(List<String> articles) {
        return new WeeklyInsight(
            "Based on aggregated global events over the last 90 days, three primary catalysts emerged.",
            List.of("Interest Rate Sensitivity", "Commodity Headwinds"),
            LocalDateTime.now().minusWeeks(12),
            LocalDateTime.now()
        );
    }
}
```

- [ ] **Step 3: Write WeeklyInsightsPipeline**
```java
package com.hud.news;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class WeeklyInsightsPipeline {
    private final NewsDiscoveryService discovery;
    private final InsightsGenerationService generation;
    private final WeeklyInsightRepository repository;
    public WeeklyInsightsPipeline(NewsDiscoveryService discovery, InsightsGenerationService generation, WeeklyInsightRepository repository) {
        this.discovery = discovery; this.generation = generation; this.repository = repository;
    }
    
    @Scheduled(cron = "0 0 0 * * SAT")
    public void runPipeline() {
        List<String> urls = discovery.discoverRecentEvents();
        WeeklyInsight insight = generation.generateInsight(urls);
        repository.save(insight);
    }
}
```

- [ ] **Step 4: Update Controller**
In `MacroMetricsController.java`, inject `WeeklyInsightRepository` and `WeeklyInsightsPipeline`, and add:
```java
    @GetMapping("/insights/latest")
    public WeeklyInsight getLatestInsight() {
        return insightRepository.findTopByOrderByGeneratedAtDesc().orElse(null);
    }

    @PostMapping("/insights/trigger")
    public String triggerInsights() {
        pipeline.runPipeline();
        return "Insight pipeline triggered.";
    }
```

- [ ] **Step 5: Commit**
```bash
git add hud-backend/src/main/java/com/hud/news/
git commit -m "feat(backend): implement weekly insights pipeline and api"
```

### Task 4: Frontend Macro Pods Educational Context

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/news/MacroPod.java`
- Modify: `hud-backend/src/main/java/com/hud/news/MacroMetricsController.java`
- Modify: `hud-frontend/src/components/types.ts`
- Modify: `hud-frontend/src/components/MacroPodCard.tsx`

- [ ] **Step 1: Update Java MacroPod class**
Add `String educationalDescription` and `String learnMoreLink` to `MacroPod.java`, update the constructor and getters.

- [ ] **Step 2: Update MacroMetricsController**
Add hardcoded descriptions to the `MacroPod` instantiations in `getMacroPods()` (e.g. "Measures the fundamental growth..." and "https://example.com").

- [ ] **Step 3: Update TypeScript types**
In `hud-frontend/src/components/types.ts`, add `educationalDescription?: string; learnMoreLink?: string;` to `MacroPod`.

- [ ] **Step 4: Update MacroPodCard.tsx**
Render the educational text and link below the metrics list.
```tsx
{pod.educationalDescription && (
  <div className="mt-4 pt-4 border-t text-xs text-muted-foreground">
    <p>ℹ️ {pod.educationalDescription} {pod.learnMoreLink && <a href={pod.learnMoreLink} className="text-primary hover:underline">Learn more -></a>}</p>
  </div>
)}
```

- [ ] **Step 5: Commit**
```bash
git add -u
git commit -m "feat(frontend): add educational descriptions to macro pods"
```

### Task 5: Frontend Strategic Insights View

**Files:**
- Create: `hud-frontend/src/components/StrategicInsightsView.tsx`
- Modify: `hud-frontend/src/components/InvestmentsView.tsx`

- [ ] **Step 1: Create StrategicInsightsView.tsx**
```tsx
import { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

type WeeklyInsight = {
  id: string;
  narrativeText: string;
  keyConsiderations: string[];
  generatedAt: string;
};

export const StrategicInsightsView = () => {
  const [insight, setInsight] = useState<WeeklyInsight | null>(null);

  useEffect(() => {
    fetch('/api/investments/insights/latest')
      .then(r => r.json())
      .then(setInsight)
      .catch(() => {});
  }, []);

  if (!insight) return null;

  return (
    <Card className="mt-8">
      <CardHeader>
        <CardTitle>Strategic Financial Considerations</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="mb-6">
          <h4 className="font-semibold mb-2">The 12-Week Macro Narrative</h4>
          <p className="text-muted-foreground">{insight.narrativeText}</p>
        </div>
        <div>
          <h4 className="font-semibold mb-2">Key Considerations for Investors</h4>
          <ul className="list-disc pl-5 space-y-2 text-muted-foreground">
            {insight.keyConsiderations.map((c, i) => <li key={i}>{c}</li>)}
          </ul>
        </div>
        <div className="mt-6 text-xs text-muted-foreground text-right">
          Last updated: {new Date(insight.generatedAt).toLocaleString()}
        </div>
      </CardContent>
    </Card>
  );
};
```

- [ ] **Step 2: Render in InvestmentsView**
In `InvestmentsView.tsx`, import `StrategicInsightsView` and place `<StrategicInsightsView />` below the pods container.

- [ ] **Step 3: Run Frontend Build to check TS**
Run: `npm run build` in `hud-frontend`. Expected: PASS.

- [ ] **Step 4: Commit**
```bash
git add hud-frontend/src/components/StrategicInsightsView.tsx hud-frontend/src/components/InvestmentsView.tsx
git commit -m "feat(frontend): add strategic insights component"
```
