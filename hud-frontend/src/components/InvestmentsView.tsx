import { useState, useEffect } from 'react';
import type { MacroMetric } from './types';
import { TrendingUp, TrendingDown, RefreshCcw, Landmark, Fuel, Shield, AlertTriangle, BarChart2 } from 'lucide-react';
import { MetricChart } from './MetricChart';

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
    fetch('/api/investments/trigger', { method: 'POST' })
      .then(() => fetchVitals());
  };

  const triggerCorrelation = () => {
    setLoading(true);
    fetch('/api/investments/correlate', { method: 'POST' })
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
    if (ticker.includes('CL=F') || ticker.includes('BZ=F')) return <Fuel size={20} color="#3fb950" />;
    if (ticker.includes('DX-Y') || ticker.includes('GC=F')) return <Landmark size={20} color="#d29922" />;
    if (ticker.includes('VIX')) return <AlertTriangle size={20} color="#f85149" />;
    return <Shield size={20} color="#58a6ff" />;
  };

  return (
    <div className="investments-view">
      <div className="view-header">
        <h2>Macro Vitals Dashboard</h2>
        <div className="actions" style={{ display: 'flex', gap: '1rem' }}>
            <button className="trigger-btn" onClick={triggerCorrelation} disabled={loading}>
              <BarChart2 size={16} />
              Correlate Events
            </button>
            <button className="trigger-btn" onClick={triggerRefresh} disabled={loading}>
              <RefreshCcw size={16} className={loading ? 'animate-spin' : ''} />
              Refresh Markets
            </button>
        </div>
      </div>

      <div className="metrics-grid">
        {metrics.map(m => (
          <div key={m.ticker} className="metric-card card clickable" onClick={() => setSelectedMetric(m)}>
            <div className="metric-header">
              <div className="metric-label">
                {getIcon(m.ticker)}
                <div>
                  <h3>{m.label}</h3>
                  <code>{m.ticker}</code>
                </div>
              </div>
              <div className={`metric-trend ${m.change >= 0 ? 'up' : 'down'}`}>
                {m.change >= 0 ? <TrendingUp size={16}/> : <TrendingDown size={16}/>}
                <span>{Math.abs(m.changePercent).toFixed(2)}%</span>
              </div>
            </div>
            
            <div className="metric-body">
              <div className="metric-price">
                <span className="currency">$</span>
                <span className="value">{m.price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
              </div>
              <div className="metric-updated">
                Updated: {new Date(m.updatedAt).toLocaleTimeString()}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="disclaimer">
        Market data is delayed by 15 minutes. High-resolution intelligence provided by HUD Analytical Engine.
      </div>

      {selectedMetric && (
        <MetricChart 
            metric={selectedMetric} 
            onClose={() => setSelectedMetric(null)} 
        />
      )}
    </div>
  );
};
