import { useState, useEffect, useMemo } from 'react';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import type { MacroMetric, MetricHistory } from './types';
import { RefreshCcw, BarChart2 } from 'lucide-react';

interface Props {
  metrics: MacroMetric[];
}

export const ComparisonDashboard = ({ metrics }: Props) => {
  const [leftTicker, setLeftTicker] = useState<string>('^GSPC');
  const [rightTicker, setRightTicker] = useState<string>('^VIX');
  const [duration, setDuration] = useState<number>(30); // years
  const [leftData, setLeftData] = useState<MetricHistory[]>([]);
  const [rightData, setRightData] = useState<MetricHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [leftRes, rightRes] = await Promise.all([
        fetch(`/api/investments/history/${encodeURIComponent(leftTicker)}`).then(res => res.json()),
        fetch(`/api/investments/history/${encodeURIComponent(rightTicker)}`).then(res => res.json())
      ]);
      setLeftData(Array.isArray(leftRes) ? leftRes : []);
      setRightData(Array.isArray(rightRes) ? rightRes : []);
    } catch (e) {
      console.error("Failed to fetch historical data", e);
    } finally {
      setLoading(false);
    }
  };

  const triggerSync = async () => {
    setSyncing(true);
    try {
      await fetch('/api/investments/sync', { method: 'POST' });
      await fetchData();
      alert("Historical data synchronization complete.");
    } catch (e) {
      alert("Sync failed.");
    } finally {
      setSyncing(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [leftTicker, rightTicker]);

  const mergedData = useMemo(() => {
    const cutoff = new Date();
    cutoff.setFullYear(cutoff.getFullYear() - duration);

    const dataMap: Record<string, any> = {};

    const processSeries = (data: MetricHistory[], key: 'left' | 'right') => {
      data.forEach(h => {
        const dt = new Date(h.timestamp);
        if (dt < cutoff) return;

        // Align to Monday of that week to ensure series overlap
        const day = dt.getDay();
        const diff = dt.getDate() - day + (day === 0 ? -6 : 1);
        const monday = new Date(dt);
        monday.setDate(diff);
        monday.setHours(0, 0, 0, 0);
        
        const timestamp = monday.getTime();
        const dateStr = monday.toISOString().split('T')[0];

        if (!dataMap[timestamp]) {
          dataMap[timestamp] = { 
            timestamp, 
            dateStr,
            left: null,
            right: null 
          };
        }
        dataMap[timestamp][key] = h.price;
      });
    };

    processSeries(leftData, 'left');
    processSeries(rightData, 'right');

    return Object.values(dataMap).sort((a: any, b: any) => a.timestamp - b.timestamp);
  }, [leftData, rightData, duration]);

  const leftLabel = metrics.find(m => m.ticker === leftTicker)?.label || leftTicker;
  const rightLabel = metrics.find(m => m.ticker === rightTicker)?.label || rightTicker;

  return (
    <div className="comparison-dashboard card" style={{ marginTop: '2rem', padding: '1.5rem' }}>
      <div className="dashboard-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div className="title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <BarChart2 size={20} color="var(--accent-color)" />
          <h2 style={{ margin: 0 }}>Historical Correlation Engine</h2>
        </div>
        <div className="controls" style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          <div className="selectors" style={{ display: 'flex', gap: '0.5rem' }}>
            <select value={leftTicker} onChange={e => setLeftTicker(e.target.value)} className="chart-select">
              {metrics.map(m => <option key={m.ticker} value={m.ticker}>{m.label}</option>)}
            </select>
            <span style={{ alignSelf: 'center', color: 'var(--text-secondary)' }}>vs</span>
            <select value={rightTicker} onChange={e => setRightTicker(e.target.value)} className="chart-select">
              {metrics.map(m => <option key={m.ticker} value={m.ticker}>{m.label}</option>)}
            </select>
          </div>
          
          <div className="duration-toggle">
            {[1, 5, 10, 30].map(yr => (
              <button 
                key={yr} 
                className={`toggle-btn ${duration === yr ? 'active' : ''}`}
                onClick={() => setDuration(yr)}
              >
                {yr}Y
              </button>
            ))}
          </div>

          <button className="trigger-btn" onClick={triggerSync} disabled={syncing}>
            <RefreshCcw size={14} className={syncing ? 'animate-spin' : ''} />
            Sync History
          </button>
        </div>
      </div>

      <div className="chart-container" style={{ width: '100%', height: 500 }}>
        {loading ? (
          <div className="loader">Correlating multi-decade datasets...</div>
        ) : (
          <ResponsiveContainer>
            <LineChart data={mergedData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#30363d" vertical={false} />
              <XAxis 
                dataKey="dateStr" 
                stroke="#8b949e" 
                fontSize={10} 
                tickLine={false}
                minTickGap={30}
              />
              <YAxis 
                yAxisId="left"
                stroke="#58a6ff" 
                fontSize={10} 
                tickLine={false} 
                axisLine={false} 
                domain={['auto', 'auto']}
                tickFormatter={(val) => val?.toLocaleString() || ''}
              />
              <YAxis 
                yAxisId="right"
                orientation="right"
                stroke="#d29922" 
                fontSize={10} 
                tickLine={false} 
                axisLine={false} 
                domain={['auto', 'auto']}
                tickFormatter={(val) => val?.toLocaleString() || ''}
              />
              <Tooltip 
                contentStyle={{ background: '#0d1117', border: '1px solid #30363d' }}
                itemStyle={{ fontSize: '0.8rem' }}
                labelStyle={{ color: '#8b949e', marginBottom: '0.5rem' }}
                formatter={(val: number) => val?.toLocaleString()}
              />
              <Legend />
              <Line 
                yAxisId="left"
                type="monotone" 
                dataKey="left" 
                name={leftLabel}
                stroke="#58a6ff" 
                strokeWidth={2} 
                connectNulls
                dot={false} 
                activeDot={{ r: 4 }} 
              />
              <Line 
                yAxisId="right"
                type="monotone" 
                dataKey="right" 
                name={rightLabel}
                stroke="#d29922" 
                strokeWidth={2} 
                connectNulls
                dot={false} 
                activeDot={{ r: 4 }} 
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
};
