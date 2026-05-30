import { useState, useEffect } from 'react';
import type { MacroMetric } from './types';
import { RefreshCcw, Landmark, Fuel, Shield, AlertTriangle, BarChart2, Info } from 'lucide-react';
import { apiFetch } from '../api';
import { MetricChart } from './MetricChart';
import { ComparisonDashboard } from './ComparisonDashboard';
import { MarketPredictionDashboard } from './MarketPredictionDashboard';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

export const InvestmentsView = () => {
  const [metrics, setMetrics] = useState<MacroMetric[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedMetric, setSelectedMetric] = useState<MacroMetric | null>(null);

  const fetchVitals = () => {
    setLoading(true);
    fetch('/api/investments/vitals')
      .then(res => res.json())
      .then(data => {
        setMetrics(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  const triggerRefresh = () => {
    setLoading(true);
    apiFetch('/api/investments/trigger', { method: 'POST' })
      .then(() => fetchVitals());
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
    fetchVitals();
    const interval = setInterval(fetchVitals, 300000); // Auto refresh every 5m
    return () => clearInterval(interval);
  }, []);

  const getIcon = (ticker: string) => {
    if (ticker.includes('CL=F') || ticker.includes('BZ=F')) return <Fuel className="h-4 w-4 text-success" />;
    if (ticker.includes('DX-Y') || ticker.includes('GC=F')) return <Landmark className="h-4 w-4 text-yellow-500" />;
    if (ticker.includes('VIX')) return <AlertTriangle className="h-4 w-4 text-destructive" />;
    return <Shield className="h-4 w-4 text-accent" />;
  };

  return (
    <div className="flex flex-col gap-8">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-semibold tracking-tight">Macro Vitals Dashboard</h2>
        <div className="flex gap-3">
            <Button variant="outline" size="sm" className="gap-2" onClick={triggerCorrelation} disabled={loading}>
              <BarChart2 className="h-4 w-4" />
              Correlate Events
            </Button>
            <Button variant="secondary" size="sm" className="gap-2" onClick={triggerRefresh} disabled={loading}>
              <RefreshCcw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh Markets
            </Button>
        </div>
      </div>

      <div className="metrics-grid" data-testid="vitals-grid">
        {metrics.map(m => (
          <Card key={m.ticker} className="cursor-pointer transition-all hover:ring-1 hover:ring-accent group border-border/60 bg-card/60" onClick={() => setSelectedMetric(m)}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <div className="flex items-center gap-2">
                {getIcon(m.ticker)}
                <CardTitle className="text-[11px] font-mono font-bold uppercase tracking-widest text-foreground group-hover:text-accent transition-colors">
                  {m.label}
                </CardTitle>
              </div>
              <Badge variant={m.change >= 0 ? "secondary" : "destructive"} className={`text-[10px] font-mono font-bold px-1.5 py-0 ${m.change >= 0 ? 'bg-success/20 text-success border-success/40' : 'bg-destructive/20 text-destructive border-destructive/40'}`}>
                {m.change >= 0 ? '+' : ''}{m.changePercent.toFixed(2)}%
              </Badge>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-mono font-bold tracking-tighter text-white">
                <span className="text-sm text-accent mr-1">$</span>
                {m.price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </div>
              <p className="text-[10px] text-foreground font-mono font-semibold mt-1 uppercase opacity-80">
                TS: {new Date(m.updatedAt).toLocaleTimeString()}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="flex items-center gap-2 text-xs text-white bg-accent/10 p-3 rounded-lg border border-accent/20 italic font-medium shadow-sm">
        <Info className="h-4 w-4 text-accent" />
        Market data is delayed by 15 minutes. High-resolution intelligence provided by HUD Analytical Engine.
      </div>

      <ComparisonDashboard metrics={metrics} />

      <MarketPredictionDashboard />

      {selectedMetric && (
        <MetricChart 
            metric={selectedMetric} 
            onClose={() => setSelectedMetric(null)} 
        />
      )}
    </div>
  );
};
