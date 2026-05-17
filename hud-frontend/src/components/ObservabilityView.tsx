import { useState, useEffect } from 'react';
import type { PipelineRun } from './types';
import { Activity, CheckCircle, AlertCircle, Clock, RefreshCcw, Trash2 } from 'lucide-react';
import { apiFetch } from '../api';

export const ObservabilityView = () => {
  const [runs, setRuns] = useState<PipelineRun[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchRuns = () => {
    setLoading(true);
    fetch('/api/pipelines')
      .then(res => res.json())
      .then(data => {
        setRuns(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch(() => {
        setRuns([]);
        setLoading(false);
      });
  };

  const flushRuns = () => {
    if (!window.confirm('Are you sure you want to flush all observability logs?')) return;
    
    setLoading(true);
    apiFetch('/api/pipelines', { method: 'DELETE' })
      .then(() => {
        setRuns([]);
        setLoading(false);
      })
      .catch(err => {
        console.error('Failed to flush logs:', err);
        setLoading(false);
      });
  };

  const formatTimestamp = (isoString: string | null | undefined) => {
    if (!isoString) return 'N/A';
    try {
      const d = new Date(isoString);
      if (isNaN(d.getTime())) return 'N/A';
      return d.toLocaleString([], { 
        month: 'short', 
        day: '2-digit', 
        hour: '2-digit', 
        minute: '2-digit', 
        second: '2-digit',
        hour12: false 
      });
    } catch (e) {
      return 'N/A';
    }
  };

  const calculateDuration = (start: string | null | undefined, end: string | null | undefined) => {
    if (!start) return 'N/A';
    if (!end) return 'Ongoing...';
    try {
      const s = new Date(start).getTime();
      const e = new Date(end).getTime();
      if (isNaN(s) || isNaN(e)) return 'N/A';
      const duration = (e - s) / 1000;
      return duration >= 0 ? `${duration.toFixed(1)}s` : 'N/A';
    } catch (err) {
      return 'N/A';
    }
  };

  useEffect(() => {
    fetchRuns();
    const interval = setInterval(fetchRuns, 10000); // Auto refresh every 10s
    return () => clearInterval(interval);
  }, []);

  if (!Array.isArray(runs) && !loading) {
    return (
      <div className="observability-view error-state">
        <div className="empty-state">
          <AlertCircle size={48} />
          <p>Failed to load pipeline data. Please refresh.</p>
          <button className="trigger-btn" onClick={fetchRuns}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="observability-view">
      <div className="view-header">
        <h2>Pipeline Observability</h2>
        <div className="header-actions">
          <button className="trigger-btn secondary" onClick={flushRuns} disabled={loading || !runs || runs.length === 0}>
            <Trash2 size={16} />
            Flush Logs
          </button>
          <button className="trigger-btn" onClick={fetchRuns} disabled={loading}>
            <RefreshCcw size={16} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>
      </div>

      <div className="pipeline-table-container">
        <table className="pipeline-table">
          <thead>
            <tr>
              <th>Category</th>
              <th>Status</th>
              <th>Started</th>
              <th>Duration</th>
              <th>Details / Errors</th>
            </tr>
          </thead>
          <tbody>
            {runs.map(run => (
              <tr key={run.id} className={`status-${run.status?.toLowerCase() || 'unknown'}`}>
                <td className="col-category">{run.category || 'N/A'}</td>
                <td className="col-status">
                  <div className="status-badge">
                    {run.status === 'PENDING' && <Activity size={14} className="animate-pulse" />}
                    {run.status === 'SUCCESS' && <CheckCircle size={14} />}
                    {run.status === 'FAILED' && <AlertCircle size={14} />}
                    {run.status || 'UNKNOWN'}
                  </div>
                </td>
                <td className="col-time">
                  <Clock size={12} /> {formatTimestamp(run.startTime)}
                </td>
                <td className="col-duration">
                   {calculateDuration(run.startTime, run.endTime)}
                </td>
                <td className="col-error">
                  {run.errorMessage && (
                    <div className="error-container">
                      <span className="error-text">{run.errorMessage}</span>
                      {run.errorDetail && (
                        <pre className="error-detail">
                          {run.errorDetail}
                        </pre>
                      )}
                    </div>
                  )}
                  {!run.errorMessage && run.status === 'SUCCESS' && (
                    <span className="success-text">
                      {run.inputTokens != null && run.outputTokens != null ? (
                        <span className="token-metrics">
                          In: {run.inputTokens.toLocaleString()} | Out: {run.outputTokens.toLocaleString()} tokens
                        </span>
                      ) : (
                        "Nominal completion"
                      )}
                    </span>
                  )}
                </td>
              </tr>
            ))}
            {runs.length === 0 && !loading && (
               <tr>
                 <td colSpan={5} className="empty-row">No pipeline runs found.</td>
               </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
