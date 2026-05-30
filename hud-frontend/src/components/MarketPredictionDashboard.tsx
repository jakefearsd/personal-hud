import { useState, useEffect } from 'react';
import {
  LineChart, Line, ResponsiveContainer, ReferenceDot
} from 'recharts';
import type { MarketPrediction, MetricHistory } from './types';
import { BrainCircuit, Target, Activity, RefreshCcw } from 'lucide-react';
import { apiFetch } from '../api';

export const MarketPredictionDashboard = () => {
  const [predictions, setPredictions] = useState<MarketPrediction[]>([]);
  const [loading, setLoading] = useState(true);
  const [histories, setHistories] = useState<Record<string, MetricHistory[]>>({});

  const fetchPredictions = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/investments/predictions/latest');
      const data = await res.json();
      setPredictions(Array.isArray(data) ? data : []);
      
      // Fetch history for the 4 target assets
      const tickers = ['^GSPC', '^DJI', 'AGG', 'BNDX'];
      const historyData: Record<string, MetricHistory[]> = {};
      
      await Promise.all(tickers.map(async (t) => {
        const hRes = await fetch(`/api/investments/history/${encodeURIComponent(t)}`);
        const hData = await hRes.json();
        historyData[t] = Array.isArray(hData) ? hData : [];
      }));
      
      setHistories(historyData);
    } catch (e) {
      console.error("Failed to fetch market predictions", e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPredictions();
  }, []);

  const synthesis = predictions.length > 0 ? predictions[0].synthesis : "No rolling 7-day projections available yet. Click 'Run Projections' to start the analysis.";
  const tailRisk = predictions.length > 0 ? predictions[0].tailRisk : null;

  const renderSparkline = (ticker: string, label: string) => {
    const history = histories[ticker] || [];
    const preds = predictions.filter(p => p.ticker === ticker);
    
    // Merge history for the chart (last 30 points)
    const data = history.slice(-30).map(h => ({
      date: h.timestamp.split('T')[0],
      price: h.price
    }));

    return (
      <div className="prediction-sparkline card" key={ticker}>
        <div className="sparkline-header">
          <div className="label">
            <h4>{label}</h4>
            <code>{ticker}</code>
          </div>
          {preds.length > 0 && (
             <div className="current-pred-group">
                <div className="current-pred">
                   <Target size={14} color="var(--accent-color)" />
                   <span>{preds[0].predictedPrice.toLocaleString()}</span>
                </div>
                <div className="confidence-tag" title="Confidence Score">
                   {preds[0].confidenceScore}%
                </div>
             </div>
          )}
        </div>
        <div className="sparkline-body" style={{ height: 120 }}>
          <ResponsiveContainer>
            <LineChart data={data}>
              <Line 
                type="monotone" 
                dataKey="price" 
                stroke="#8b949e" 
                strokeWidth={1.5} 
                dot={false} 
              />
              {preds.map(p => (
                   <ReferenceDot 
                     key={p.id}
                     x={p.targetDate}
                     y={p.predictedPrice} 
                     r={3} 
                     fill="var(--accent-color)" 
                     stroke="none"
                   />
              ))}
            </LineChart>
          </ResponsiveContainer>
        </div>
        {preds.length > 0 && (
          <div className="rationale" title={preds[0].rationale}>
            {preds[0].rationale}
          </div>
        )}
      </div>
    );
  };

  const triggerPredictions = async () => {
    setLoading(true);
    try {
      await apiFetch('/api/investments/predictions/trigger', { method: 'POST' });
      alert("Predictive analytics triggered. Projections will be available shortly.");
      await fetchPredictions();
    } catch (e) {
      alert("Trigger failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="market-prediction-dashboard" data-testid="prediction-dashboard">
      <div className="section-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <BrainCircuit size={20} color="var(--accent-color)" />
          <h3>Predictive Analytics & Weekly Projections</h3>
        </div>
        <button className="trigger-btn" onClick={triggerPredictions} disabled={loading}>
          <RefreshCcw size={14} className={loading ? 'animate-spin' : ''} />
          Run Projections
        </button>
      </div>

      <div className="pulse-container">
        <div className="pulse-synthesis card">
          <div className="card-header">
            <Activity size={16} color="#58a6ff" />
            <h4>Market Pulse (Rolling 7-Day Context)</h4>
          </div>
          <p>{synthesis}</p>
          
          {tailRisk && (
            <div className="tail-risk-box">
              <strong>Tail Risk Identified:</strong>
              <p>{tailRisk}</p>
            </div>
          )}

          <div className="model-tag">Generated by {predictions[0]?.modelName || 'Neural Engine'}</div>
        </div>
        
        <div className="prediction-grid">
          {renderSparkline('^GSPC', 'S&P 500')}
          {renderSparkline('^DJI', 'Dow 30')}
          {renderSparkline('VXUS', 'Intl Equity (VXUS)')}
          {renderSparkline('EFV', 'Intl Value (EFV)')}
          {renderSparkline('AGG', 'US Bond (AGG)')}
          {renderSparkline('BNDX', 'Intl Bond (BNDX)')}
        </div>
      </div>

      <style>{`
        .market-prediction-dashboard {
          margin-top: 2rem;
          display: flex;
          flex-direction: column;
          gap: 1rem;
        }
        .pulse-container {
          display: grid;
          grid-template-columns: 1fr 2fr;
          gap: 1rem;
        }
        .pulse-synthesis {
          padding: 1.5rem;
          font-size: 0.95rem;
          line-height: 1.6;
          color: var(--text-secondary);
          display: flex;
          flex-direction: column;
          justify-content: space-between;
        }
        .pulse-synthesis h4 {
          margin: 0;
          color: var(--text-main);
        }
        .model-tag {
          font-size: 0.7rem;
          color: var(--text-muted);
          margin-top: 1rem;
          text-transform: uppercase;
          letter-spacing: 0.05rem;
        }
        .prediction-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 1rem;
        }
        .prediction-sparkline {
          padding: 1rem;
          display: flex;
          flex-direction: column;
          gap: 0.5rem;
        }
        .sparkline-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
        }
        .sparkline-header h4 { margin: 0; font-size: 0.9rem; }
        .sparkline-header code { font-size: 0.7rem; color: var(--text-muted); }
        .current-pred {
          display: flex;
          align-items: center;
          gap: 0.3rem;
          font-size: 0.85rem;
          font-weight: 600;
          color: var(--accent-color);
        }
        .current-pred-group {
          display: flex;
          flex-direction: column;
          align-items: flex-end;
          gap: 0.2rem;
        }
        .confidence-tag {
          font-size: 0.65rem;
          background: rgba(88, 166, 255, 0.1);
          color: #58a6ff;
          padding: 1px 4px;
          border-radius: 3px;
          border: 1px solid rgba(88, 166, 255, 0.2);
        }
        .tail-risk-box {
          margin-top: 1rem;
          padding: 0.8rem;
          background: rgba(248, 81, 73, 0.05);
          border-left: 3px solid #f85149;
          font-size: 0.85rem;
        }
        .tail-risk-box strong {
          display: block;
          color: #f85149;
          font-size: 0.75rem;
          text-transform: uppercase;
          margin-bottom: 0.3rem;
        }
        .tail-risk-box p {
          margin: 0;
          color: var(--text-secondary);
        }
        .rationale {
          font-size: 0.75rem;
          color: var(--text-muted);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          border-top: 1px solid var(--border-color);
          padding-top: 0.5rem;
        }
        .card-header {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            margin-bottom: 1rem;
        }
        .section-header {
            margin-bottom: 1rem;
        }
      `}</style>
    </div>
  );
};
