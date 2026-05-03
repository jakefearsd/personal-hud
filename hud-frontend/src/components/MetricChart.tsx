import { useState, useEffect } from 'react';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceDot 
} from 'recharts';
import type { MacroMetric, MetricHistory, MarketEvent } from './types';
import { X } from 'lucide-react';

interface Props {
  metric: MacroMetric;
  onClose: () => void;
}

export const MetricChart = ({ metric, onClose }: Props) => {
  const [history, setHistory] = useState<MetricHistory[]>([]);
  const [events, setEvents] = useState<MarketEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      fetch(`/api/investments/history/${encodeURIComponent(metric.ticker)}`).then(res => res.json()),
      fetch(`/api/investments/events/${encodeURIComponent(metric.ticker)}`).then(res => res.json())
    ]).then(([historyData, eventData]) => {
      setHistory(Array.isArray(historyData) ? historyData : []);
      setEvents(Array.isArray(eventData) ? eventData : []);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [metric.ticker]);

  const formatData = history.map(h => ({
    ...h,
    time: new Date(h.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    fullDate: new Date(h.timestamp).toLocaleString()
  }));

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      const event = events.find(e => 
        new Date(e.timestamp).getTime() > new Date(data.timestamp).getTime() - 3600000 &&
        new Date(e.timestamp).getTime() < new Date(data.timestamp).getTime() + 3600000
      );

      return (
        <div className="custom-tooltip card" style={{ padding: '1rem', border: '1px solid var(--border-color)' }}>
          <p className="label" style={{ margin: 0, fontWeight: 'bold' }}>{data.fullDate}</p>
          <p className="price" style={{ color: 'var(--accent-color)', fontSize: '1.2rem' }}>${data.price.toLocaleString()}</p>
          {event && (
            <div className="event-annotation" style={{ marginTop: '0.5rem', borderTop: '1px solid var(--border-color)', paddingTop: '0.5rem' }}>
              <span className="badge" style={{ background: 'var(--success-color)', fontSize: '0.6rem', padding: '2px 4px', borderRadius: '4px' }}>CATALYST</span>
              <p style={{ fontSize: '0.8rem', margin: '4px 0', color: '#d29922' }}>{event.title}</p>
              <p style={{ fontSize: '0.7rem', color: 'var(--text-secondary)' }}>{event.rationale}</p>
            </div>
          )}
        </div>
      );
    }
    return null;
  };

  return (
    <div className="chart-overlay">
      <div className="chart-modal card">
        <div className="modal-header">
          <div className="title">
            <h2>{metric.label} Performance</h2>
            <code>{metric.ticker}</code>
          </div>
          <button className="close-btn" onClick={onClose}><X size={24}/></button>
        </div>

        <div className="chart-container" style={{ width: '100%', height: 400 }}>
          {loading ? (
            <div className="loader">Reconstructing timeseries...</div>
          ) : (
            <ResponsiveContainer>
              <LineChart data={formatData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#30363d" vertical={false} />
                <XAxis 
                    dataKey="time" 
                    stroke="#8b949e" 
                    fontSize={10} 
                    tickLine={false} 
                    axisLine={false} 
                />
                <YAxis 
                    stroke="#8b949e" 
                    fontSize={10} 
                    tickLine={false} 
                    axisLine={false} 
                    domain={['auto', 'auto']}
                    tickFormatter={(val) => `$${val.toLocaleString()}`}
                />
                <Tooltip content={<CustomTooltip />} />
                <Line 
                    type="monotone" 
                    dataKey="price" 
                    stroke="#58a6ff" 
                    strokeWidth={2} 
                    dot={false} 
                    activeDot={{ r: 6 }} 
                />
                {events.map(ev => {
                    const match = formatData.find(d => 
                        new Date(d.timestamp).getTime() > new Date(ev.timestamp).getTime() - 1800000 &&
                        new Date(d.timestamp).getTime() < new Date(ev.timestamp).getTime() + 1800000
                    );
                    return match ? (
                        <ReferenceDot 
                            key={ev.id}
                            x={match.time} 
                            y={match.price} 
                            r={5} 
                            fill="#d29922" 
                            stroke="white" 
                        />
                    ) : null;
                })}
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  );
};
