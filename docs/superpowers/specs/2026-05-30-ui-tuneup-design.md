# UI Tune-up Design Specification

## 1. Goal
Modernize and tune up the `hud-frontend` application UI to meet high-end SaaS expectations (like Vercel or Linear). The redesign emphasizes a compact, data-dense dashboard layout with a minimal and refined aesthetic. It involves both global CSS styling updates and structural React component refactoring.

## 2. Global Aesthetic & Theming
- **Color Palette:** Override default `shadcn/ui` variables in `index.css` to use a high-contrast monochrome base (slate/zinc) with a sharp, singular accent color (e.g., sharp blue/indigo). Light mode will be crisp white, dark mode will be deep slate.
- **Typography:** 
  - Standard body/heading font: Clean, geometric sans-serif (e.g., `Inter` or standard system sans-serif).
  - Data/Metrics font: Monospaced or tabular numbers for all numerical metrics (tickers, scores, percentages) to ensure alignment in grids.
- **Density Compression:** 
  - Reduce standard padding across all `shadcn/ui` components (e.g., from `p-6` to `p-4` or `p-3`).
  - Tighten border radii for a sharper, more professional look.
  - Decrease line heights in dense data tables and lists.

## 3. Component Restructuring & App Layout
- **App Shell Navigation:** Convert the main entry layout to an "App Shell" with a compact, sticky sidebar or top-nav. This replaces the standard stacked page to maximize available width and height for data visualization.
- **CSS Grid Adoption:** Refactor dashboard views (`MarketPredictionDashboard`, `BriefingView`, `ComparisonDashboard`) to use strict CSS Grid systems (e.g., `grid-cols-12` or dynamic auto-fit grids). This allows tight packing of cards and responsive wrapping.
- **Micro-interactions:** Add subtle entry animations (e.g., staggered fade-ins) for dashboard cards and interactive hover states (using CSS transitions) that reveal secondary actions/data without cluttering the baseline view.

## 4. Data Visualization & Hierarchy
- **Metric Cards:** Replace generic text fields with dedicated `MetricCard` components. These will feature trend indicators (red/green deltas, arrows) and prioritize the numerical data with muted, smaller labels.
- **Typographical Hierarchy:** In text-heavy components (like `BriefingView`), heavily utilize `text-muted-foreground` for metadata (dates, authors) and distinct `shadcn/ui` badges for tags. This draws the eye directly to the primary content.

## 5. Scope & Constraints
- Focus only on `hud-frontend/src`.
- Do not rewrite the backend APIs.
- Retain existing `shadcn/ui` components where possible, applying custom utility classes (Tailwind) to achieve the new aesthetic.
- The tune-up should apply to both light and dark modes via the existing `theme-provider`.
