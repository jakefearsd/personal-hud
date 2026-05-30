# UI Tune-up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modernize the frontend UI to a dense, dashboard-focused layout with a minimal and refined aesthetic.

**Architecture:** We will modify `index.css` for global color/typography, restructure `App.tsx` into an App Shell with a sidebar, and refactor dashboard views like `BriefingView` to use CSS Grid.

**Tech Stack:** React, Tailwind CSS, shadcn/ui

---

### Task 1: Global Aesthetic & Theming Updates

**Files:**
- Modify: `hud-frontend/src/index.css`
- Modify: `hud-frontend/index.html`

- [ ] **Step 1: Add Inter font to index.html**

```html
<!-- Add inside <head> -->
<link rel="preconnect" href="https://rsms.me/">
<link rel="stylesheet" href="https://rsms.me/inter/inter.css">
```

- [ ] **Step 2: Update index.css variables for Slate/Zinc high contrast theme**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --card: 0 0% 100%;
    --card-foreground: 222.2 84% 4.9%;
    --popover: 0 0% 100%;
    --popover-foreground: 222.2 84% 4.9%;
    --primary: 221.2 83.2% 53.3%;
    --primary-foreground: 210 40% 98%;
    --secondary: 210 40% 96.1%;
    --secondary-foreground: 222.2 47.4% 11.2%;
    --muted: 210 40% 96.1%;
    --muted-foreground: 215.4 16.3% 46.9%;
    --accent: 210 40% 96.1%;
    --accent-foreground: 222.2 47.4% 11.2%;
    --destructive: 0 84.2% 60.2%;
    --destructive-foreground: 210 40% 98%;
    --border: 214.3 31.8% 91.4%;
    --input: 214.3 31.8% 91.4%;
    --ring: 221.2 83.2% 53.3%;
    --radius: 0.3rem;
  }

  .dark {
    --background: 222.2 84% 4.9%;
    --foreground: 210 40% 98%;
    --card: 222.2 84% 4.9%;
    --card-foreground: 210 40% 98%;
    --popover: 222.2 84% 4.9%;
    --popover-foreground: 210 40% 98%;
    --primary: 217.2 91.2% 59.8%;
    --primary-foreground: 222.2 47.4% 11.2%;
    --secondary: 217.2 32.6% 17.5%;
    --secondary-foreground: 210 40% 98%;
    --muted: 217.2 32.6% 17.5%;
    --muted-foreground: 215 20.2% 65.1%;
    --accent: 217.2 32.6% 17.5%;
    --accent-foreground: 210 40% 98%;
    --destructive: 0 62.8% 30.6%;
    --destructive-foreground: 210 40% 98%;
    --border: 217.2 32.6% 17.5%;
    --input: 217.2 32.6% 17.5%;
    --ring: 212.7 26.8% 83.9%;
  }
}

@layer base {
  * {
    @apply border-border;
  }
  body {
    @apply bg-background text-foreground;
    font-family: 'Inter', sans-serif;
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add hud-frontend/src/index.css hud-frontend/index.html
git commit -m "style: update global aesthetic to high-contrast monochrome"
```

### Task 2: App Shell & Sidebar Layout

**Files:**
- Modify: `hud-frontend/src/App.tsx`
- Modify: `hud-frontend/src/App.css`

- [ ] **Step 1: Refactor App.tsx layout**

Update `App.tsx` to use a left sidebar structure instead of a top header.
Replace the `<header className="app-header">` and `<main className="app-content">` structure with a Flex container:

```tsx
// Inside App.tsx return:
return (
  <div className="flex h-screen bg-background text-foreground overflow-hidden">
    {/* Sidebar */}
    <aside className="w-64 border-r bg-card flex flex-col justify-between">
      <div className="p-4">
        <h1 className="text-xl font-bold mb-6 text-primary tracking-tight">HUD</h1>
        <nav className="space-y-1 flex flex-col">
          <NavLink to="/news" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>News</NavLink>
          <NavLink to="/theaters" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>Theaters</NavLink>
          <NavLink to="/investments" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>Investments</NavLink>
          {isAdmin && (
            <>
              <NavLink to="/config" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>Config</NavLink>
              <NavLink to="/observability" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>Observability</NavLink>
            </>
          )}
        </nav>
      </div>
      <div className="p-4 border-t space-y-4">
        {showModelSwitcher && (
          <div className="flex flex-col space-y-1.5">
            <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Brain</label>
            <select className="text-sm bg-secondary border-none rounded p-1" value={selectedModel} onChange={e => setSelectedModel(e.target.value)}>
              <option value="global">Global Context</option>
              {configs.map(c => {
                 const providerPrefix = (c.provider === 'GEMINI' || c.provider === 'DEEPSEEK') ? 'Cloud' : 'Local';
                 const fullName = `${providerPrefix}: ${c.name} [${c.modelName}]`;
                 return <option key={c.id} value={fullName}>{fullName}</option>;
              })}
            </select>
          </div>
        )}
        <div className="flex items-center justify-between">
          {isAuthenticated ? (
            <div className="flex items-center space-x-2 text-sm text-muted-foreground">
              <User size={14} className="text-primary" />
              <span>{username}</span>
              <button onClick={handleLogout} title="Logout" className="hover:text-foreground"><LogOut size={14}/></button>
            </div>
          ) : (
            <button className="text-sm text-muted-foreground hover:text-foreground" onClick={() => setShowLogin(true)}>Admin Login</button>
          )}
          <ModeToggle />
        </div>
      </div>
    </aside>

    {/* Main Content */}
    <main className="flex-1 flex flex-col overflow-hidden bg-background">
      {/* Sub-tabs Top Bar if needed */}
      {showSubTabs && (
        <header className="h-14 border-b bg-card/50 backdrop-blur flex items-center px-6 gap-4">
          <NavLink to="/news/briefing" className={({isActive}) => `text-sm font-medium ${isActive ? 'text-primary border-b-2 border-primary h-full flex items-center' : 'text-muted-foreground hover:text-foreground'}`}>Briefing</NavLink>
          <NavLink to="/news/live" className={({isActive}) => `text-sm font-medium ${isActive ? 'text-primary border-b-2 border-primary h-full flex items-center' : 'text-muted-foreground hover:text-foreground'}`}>Live</NavLink>
        </header>
      )}
      
      <div className="flex-1 overflow-auto p-6 relative">
        {error && <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-md mb-4">Error: {error}</div>}
        <Routes>
          {/* ... existing routes ... */}
        </Routes>
      </div>
    </main>
    {/* Loaders/Modals ... */}
  </div>
)
```

- [ ] **Step 2: Clean up App.css**
Remove all the custom layout CSS from `App.css` since we're using Tailwind now. Just leave it mostly empty or remove unused classes.

- [ ] **Step 3: Commit**

```bash
git add hud-frontend/src/App.tsx hud-frontend/src/App.css
git commit -m "feat(ui): implement App Shell layout with sidebar"
```

### Task 3: Dashboard Grid Refactor

**Files:**
- Modify: `hud-frontend/src/components/BriefingView.tsx`

- [ ] **Step 1: Use CSS Grid in BriefingView**
Update `BriefingView` to use a multi-column CSS grid.

```tsx
// Inside BriefingView.tsx, replace the list/div wrapping the briefings with a grid:
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  {briefings.map((briefing, index) => (
    <Card key={index} className="flex flex-col h-full hover:shadow-md transition-shadow duration-200">
      <CardHeader className="p-4 pb-2">
        <div className="flex justify-between items-start mb-2">
          <Badge variant="outline" className="text-xs text-muted-foreground bg-secondary/50 font-mono">
             {briefing.category || briefing.theater}
          </Badge>
          <span className="text-xs text-muted-foreground">{briefing.timestamp}</span>
        </div>
        <CardTitle className="text-lg leading-tight">{briefing.topic}</CardTitle>
      </CardHeader>
      <CardContent className="p-4 pt-0 flex-1">
        <p className="text-sm text-muted-foreground leading-relaxed">
          {briefing.summary}
        </p>
      </CardContent>
    </Card>
  ))}
</div>
```

- [ ] **Step 2: Commit**

```bash
git add hud-frontend/src/components/BriefingView.tsx
git commit -m "feat(ui): refactor BriefingView to use CSS grid"
```

### Task 4: Metric Components implementation

**Files:**
- Create: `hud-frontend/src/components/MetricCard.tsx`
- Modify: `hud-frontend/src/components/InvestmentsView.tsx`

- [ ] **Step 1: Create MetricCard.tsx**
```tsx
import React from 'react';
import { Card, CardContent } from './ui/card';
import { ArrowUpRight, ArrowDownRight } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string;
  change?: string;
  isPositive?: boolean;
}

export function MetricCard({ title, value, change, isPositive }: MetricCardProps) {
  return (
    <Card className="hover:border-primary/50 transition-colors">
      <CardContent className="p-4 flex flex-col justify-between">
        <span className="text-sm font-medium text-muted-foreground">{title}</span>
        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-2xl font-bold tracking-tight font-mono">{value}</span>
          {change && (
            <span className={`flex items-center text-xs font-medium font-mono ${isPositive ? 'text-emerald-500' : 'text-red-500'}`}>
              {isPositive ? <ArrowUpRight size={14} className="mr-0.5" /> : <ArrowDownRight size={14} className="mr-0.5" />}
              {change}
            </span>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 2: Use MetricCard in InvestmentsView**
Refactor the `InvestmentsView.tsx` to use the `MetricCard` inside a `grid-cols-4` layout.

```tsx
// Inside InvestmentsView, use the new MetricCard for stats:
import { MetricCard } from './MetricCard';

// Inside render:
<div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
  <MetricCard title="SPY" value="$524.12" change="+1.2%" isPositive={true} />
  <MetricCard title="QQQ" value="$445.88" change="-0.4%" isPositive={false} />
  {/* Add other metrics as needed based on the data */}
</div>
```

- [ ] **Step 3: Commit**

```bash
git add hud-frontend/src/components/MetricCard.tsx hud-frontend/src/components/InvestmentsView.tsx
git commit -m "feat(ui): add MetricCard and apply to InvestmentsView"
```
