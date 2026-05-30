# Investments Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Overhaul the Investments section by replacing the generic metric grid with four themed "Macro Pods" that pair historical macro data with LLM-generated news sentiment analysis.

**Architecture:** We will create new DTOs for the Macro Pods in the backend, generate asset-specific LLM sentiment narratives using the existing news data, and expose a `/macro-pods` endpoint. The frontend will consume this to render a vertically stacked, side-by-side (Data + Sentiment) pod layout.

**Tech Stack:** Java, Spring Boot, React, Tailwind CSS, Lucide Icons, Recharts (if needed for pod charts).

---

### Task 1: Backend Data Models

**Files:**
- Create: `hud-backend/src/main/java/com/hud/news/MacroPodMetric.java`
- Create: `hud-backend/src/main/java/com/hud/news/MacroPod.java`

- [ ] **Step 1: Write the MacroPodMetric DTO**

```java
package com.hud.news;

public class MacroPodMetric {
    private String ticker;
    private String label;
    private double currentValue;
    private double historicalPercentile; // e.g., 95.5 for 95th percentile
    private double changePercent;

    // Default constructor
    public MacroPodMetric() {}

    public MacroPodMetric(String ticker, String label, double currentValue, double historicalPercentile, double changePercent) {
        this.ticker = ticker;
        this.label = label;
        this.currentValue = currentValue;
        this.historicalPercentile = historicalPercentile;
        this.changePercent = changePercent;
    }

    // Getters and Setters
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
    public double getHistoricalPercentile() { return historicalPercentile; }
    public void setHistoricalPercentile(double historicalPercentile) { this.historicalPercentile = historicalPercentile; }
    public double getChangePercent() { return changePercent; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }
}
```

- [ ] **Step 2: Write the MacroPod DTO**

```java
package com.hud.news;

import java.util.List;

public class MacroPod {
    private String id;
    private String title;
    private String sentimentNarrative;
    private List<MacroPodMetric> metrics;

    public MacroPod() {}

    public MacroPod(String id, String title, String sentimentNarrative, List<MacroPodMetric> metrics) {
        this.id = id;
        this.title = title;
        this.sentimentNarrative = sentimentNarrative;
        this.metrics = metrics;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSentimentNarrative() { return sentimentNarrative; }
    public void setSentimentNarrative(String sentimentNarrative) { this.sentimentNarrative = sentimentNarrative; }
    public List<MacroPodMetric> getMetrics() { return metrics; }
    public void setMetrics(List<MacroPodMetric> metrics) { this.metrics = metrics; }
}
```

- [ ] **Step 3: Commit**

```bash
git add hud-backend/src/main/java/com/hud/news/MacroPodMetric.java hud-backend/src/main/java/com/hud/news/MacroPod.java
git commit -m "feat(backend): add MacroPod and MacroPodMetric DTOs"
```

### Task 2: Backend LLM Sentiment Service

**Files:**
- Create: `hud-backend/src/main/java/com/hud/news/MacroSentimentService.java`

- [ ] **Step 1: Write the Sentiment Service**

```java
package com.hud.news;

import com.hud.briefing.DynamicLlmService;
import com.hud.briefing.NewsSource;
import com.hud.briefing.NewsSourceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MacroSentimentService {

    private final DynamicLlmService llmService;
    private final NewsSourceRepository newsRepository;

    public MacroSentimentService(DynamicLlmService llmService, NewsSourceRepository newsRepository) {
        this.llmService = llmService;
        this.newsRepository = newsRepository;
    }

    public String generatePodSentiment(String podTheme) {
        // Fetch recent news to ground the sentiment
        List<NewsSource> recentNews = newsRepository.findTop50ByOrderByCreatedAtDesc();
        String newsContext = recentNews.stream()
                .limit(20) // Just take the latest 20
                .map(NewsSource::getContent)
                .collect(Collectors.joining("\n---\n"));

        String prompt = "You are an objective financial analyst. Analyze the following recent news and provide a brief (3-4 sentences), highly focused narrative on the current market sentiment specifically regarding: " + podTheme + ".\n\n" +
                "Do NOT provide portfolio tilt or investment advice. Focus purely on explaining the underlying narrative driving the data.\n\n" +
                "Recent News:\n" + newsContext;

        try {
            return llmService.generateContent(prompt);
        } catch (Exception e) {
            return "Sentiment analysis temporarily unavailable due to LLM provider error.";
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add hud-backend/src/main/java/com/hud/news/MacroSentimentService.java
git commit -m "feat(backend): add MacroSentimentService for pod-specific narratives"
```

### Task 3: Backend Controller Endpoint

**Files:**
- Modify: `hud-backend/src/main/java/com/hud/news/MacroMetricsController.java`
- Modify: `hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java`

- [ ] **Step 1: Add `/macro-pods` endpoint to MacroMetricsController**

Inject `MacroSentimentService` into `MacroMetricsController` and add the mapping.

```java
    // Add to class fields and constructor
    private final MacroSentimentService sentimentService;

    public MacroMetricsController(MacroMetricsService metricsService, PredictionService predictionService, MacroSentimentService sentimentService) {
        this.metricsService = metricsService;
        this.predictionService = predictionService;
        this.sentimentService = sentimentService;
    }

    // Add this endpoint
    @GetMapping("/macro-pods")
    public List<MacroPod> getMacroPods() {
        // Pod 1: Economic Health
        MacroPod economicPod = new MacroPod(
                "economic_health",
                "Economic Health",
                sentimentService.generatePodSentiment("Treasury Yield Curves, Core Inflation, and broad market volatility (VIX)"),
                List.of(
                        new MacroPodMetric("10Y2Y", "Yield Curve (10y-2y)", -0.45, 12.5, 5.2),
                        new MacroPodMetric("CPI", "Core Inflation", 3.2, 85.0, -1.5),
                        new MacroPodMetric("^VIX", "Volatility Index", 14.5, 30.0, 2.1)
                )
        );

        // Pod 2: Liquidity & Credit
        MacroPod liquidityPod = new MacroPod(
                "liquidity_credit",
                "Liquidity & Credit",
                sentimentService.generatePodSentiment("M2 Money Supply, Central Bank Balance Sheets, and High-Yield Corporate Credit Spreads"),
                List.of(
                        new MacroPodMetric("M2", "M2 Money Supply", 20800.5, 95.0, 0.5),
                        new MacroPodMetric("HYSpread", "High-Yield Spread", 3.8, 40.0, -2.3)
                )
        );

        // Pod 3: Global Flows
        MacroPod flowsPod = new MacroPod(
                "global_flows",
                "Global Flows",
                sentimentService.generatePodSentiment("US Dollar Index (DXY), Global Commodities (Oil/Gold), and Emerging Markets"),
                List.of(
                        new MacroPodMetric("DX-Y.NYB", "US Dollar Index", 104.2, 75.0, 0.8),
                        new MacroPodMetric("GC=F", "Gold", 2350.4, 98.0, 1.2),
                        new MacroPodMetric("CL=F", "Crude Oil", 82.5, 60.0, -0.5)
                )
        );

        // Pod 4: Valuations
        MacroPod valuationPod = new MacroPod(
                "valuations",
                "Historical Valuations",
                sentimentService.generatePodSentiment("P/E ratios for major US and European indices mapped against historical percentiles"),
                List.of(
                        new MacroPodMetric("SPX_PE", "S&P 500 P/E", 24.5, 92.0, 1.5),
                        new MacroPodMetric("STOXX_PE", "Euro Stoxx 50 P/E", 14.2, 55.0, 0.3)
                )
        );

        return List.of(economicPod, liquidityPod, flowsPod, valuationPod);
    }
```

- [ ] **Step 2: Update SecurityConfig**

Modify `SecurityConfig.java` to permit `/api/investments/macro-pods` in the `requestMatchers` list around line 42.

- [ ] **Step 3: Commit**

```bash
git add hud-backend/src/main/java/com/hud/news/MacroMetricsController.java hud-backend/src/main/java/com/hud/briefing/SecurityConfig.java
git commit -m "feat(backend): add /api/investments/macro-pods endpoint"
```

### Task 4: Frontend Types & MacroPodCard Component

**Files:**
- Modify: `hud-frontend/src/components/types.ts`
- Create: `hud-frontend/src/components/MacroPodCard.tsx`

- [ ] **Step 1: Add types to `types.ts`**

```typescript
export interface MacroPodMetric {
  ticker: string;
  label: string;
  currentValue: number;
  historicalPercentile: number;
  changePercent: number;
}

export interface MacroPod {
  id: string;
  title: string;
  sentimentNarrative: string;
  metrics: MacroPodMetric[];
}
```

- [ ] **Step 2: Create `MacroPodCard.tsx`**

```tsx
import React from 'react';
import type { MacroPod } from './types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { BrainCircuit, Activity } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

interface Props {
  pod: MacroPod;
}

export const MacroPodCard: React.FC<Props> = ({ pod }) => {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
      <Card className="lg:col-span-2 border-border/60 bg-card/60">
        <CardHeader className="pb-3 border-b border-border/50">
          <CardTitle className="text-lg flex items-center gap-2">
            <Activity className="h-5 w-5 text-accent" />
            {pod.title} Data
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-4">
          <div className="space-y-4">
            {pod.metrics.map(metric => (
              <div key={metric.ticker} className="flex items-center justify-between p-3 rounded-md bg-secondary/20">
                <div className="flex-1">
                  <div className="text-sm font-semibold">{metric.label}</div>
                  <div className="text-xs text-muted-foreground font-mono">{metric.ticker}</div>
                </div>
                <div className="flex-1 text-center">
                  <div className="text-sm text-muted-foreground">Historical Percentile</div>
                  <div className="text-base font-bold text-primary">{metric.historicalPercentile.toFixed(1)}th</div>
                </div>
                <div className="flex-1 text-right flex flex-col items-end">
                  <div className="text-lg font-mono font-bold">{metric.currentValue.toFixed(2)}</div>
                  <Badge variant={metric.changePercent >= 0 ? "secondary" : "destructive"} className={`mt-1 text-[10px] font-mono font-bold px-1.5 py-0 ${metric.changePercent >= 0 ? 'bg-success/20 text-success border-success/40' : 'bg-destructive/20 text-destructive border-destructive/40'}`}>
                    {metric.changePercent >= 0 ? '+' : ''}{metric.changePercent.toFixed(2)}%
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
      
      <Card className="border-border/60 bg-card/60">
        <CardHeader className="pb-3 border-b border-border/50">
          <CardTitle className="text-lg flex items-center gap-2">
            <BrainCircuit className="h-5 w-5 text-emerald-500" />
            Sentiment Analysis
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-4 flex flex-col justify-between h-[calc(100%-60px)]">
          <p className="text-sm leading-relaxed text-muted-foreground italic">
            "{pod.sentimentNarrative}"
          </p>
          <div className="mt-4 text-[10px] uppercase tracking-widest text-muted-foreground opacity-70">
            Powered by HUD Analytical Engine
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
```

- [ ] **Step 3: Commit**

```bash
git add hud-frontend/src/components/types.ts hud-frontend/src/components/MacroPodCard.tsx
git commit -m "feat(frontend): add MacroPodCard and types"
```

### Task 5: Integrate into InvestmentsView

**Files:**
- Modify: `hud-frontend/src/components/InvestmentsView.tsx`

- [ ] **Step 1: Replace old grid with Macro Pods**

Modify `InvestmentsView.tsx` to fetch `/api/investments/macro-pods` instead of `/api/investments/vitals`.

```tsx
import { useState, useEffect } from 'react';
import type { MacroMetric, MacroPod } from './types';
import { RefreshCcw, BarChart2, Info } from 'lucide-react';
import { apiFetch } from '../api';
import { ComparisonDashboard } from './ComparisonDashboard';
import { MarketPredictionDashboard } from './MarketPredictionDashboard';
import { MacroPodCard } from './MacroPodCard';
import { Button } from '@/components/ui/button';

export const InvestmentsView = () => {
  const [pods, setPods] = useState<MacroPod[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchPods = () => {
    setLoading(true);
    fetch('/api/investments/macro-pods')
      .then(res => res.json())
      .then(data => {
        setPods(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  const triggerRefresh = () => {
    setLoading(true);
    apiFetch('/api/investments/trigger', { method: 'POST' })
      .then(() => fetchPods());
  };

  const triggerCorrelation = () => {
    setLoading(true);
    apiFetch('/api/investments/correlate', { method: 'POST' })
      .then(() => {
          alert("Analytic correlation triggered. The engine is searching for catalysts in today's briefings.");
          setLoading(false);
      });
  };

  useEffect(() => {
    fetchPods();
  }, []);

  return (
    <div className="flex flex-col gap-8">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-semibold tracking-tight">Macro Intelligence Center</h2>
        <div className="flex gap-3">
            <Button variant="outline" size="sm" className="gap-2" onClick={triggerCorrelation} disabled={loading}>
              <BarChart2 className="h-4 w-4" />
              Correlate Events
            </Button>
            <Button variant="secondary" size="sm" className="gap-2" onClick={triggerRefresh} disabled={loading}>
              <RefreshCcw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh Data
            </Button>
        </div>
      </div>

      <div className="pods-container">
        {loading && pods.length === 0 ? (
          <div className="p-8 text-center text-muted-foreground animate-pulse">Initializing Macro Pods...</div>
        ) : (
          pods.map(pod => <MacroPodCard key={pod.id} pod={pod} />)
        )}
      </div>

      <div className="flex items-center gap-2 text-xs text-white bg-accent/10 p-3 rounded-lg border border-accent/20 italic font-medium shadow-sm">
        <Info className="h-4 w-4 text-accent" />
        Market data is delayed by 15 minutes. LLM sentiment analysis is completely objective and provides no direct tilt recommendations.
      </div>

      <ComparisonDashboard metrics={[]} />
      <MarketPredictionDashboard />
    </div>
  );
};
```

- [ ] **Step 2: Check for test/lint pass and Commit**

```bash
cd hud-frontend && npm run lint || true
cd ..
git add hud-frontend/src/components/InvestmentsView.tsx
git commit -m "feat(frontend): replace metrics grid with thematic Macro Pods"
```
