import { useState, useEffect } from 'react';
import type { MacroPod } from './types';
import { RefreshCcw, Info } from 'lucide-react';
import { apiFetch } from '../api';
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


  useEffect(() => {
    fetchPods();
  }, []);

  return (
    <div className="flex flex-col gap-8">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-semibold tracking-tight">Macro Intelligence Center</h2>
        <div className="flex gap-3">
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

      <MarketPredictionDashboard />
    </div>
  );
};
