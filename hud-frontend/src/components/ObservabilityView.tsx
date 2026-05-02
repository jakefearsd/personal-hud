import { useState, useEffect } from 'react';
import type { PipelineRun } from './types';
import { Activity, CheckCircle, AlertCircle, Clock, RefreshCcw } from 'lucide-react';

export const ObservabilityView = () => {
  const [runs, setRuns] = useState<PipelineRun[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchRuns = () => {
    setLoading(true);
    fetch('/api/pipelines')
      .then(res => res.json())
      .then(data => {
        setRuns(data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchRuns();
    const interval = setInterval(fetchRuns, 10000); // Auto refresh every 10s
    return () => clearInterval(interval);
  }, []);

  const formatDuration = (start: string, end: string | null) => {
    if (!end) return 'Ongoing...';
    const s = new Date(start).getTime();
    const e = new Date(end).getTime();
    const diff = Math.round((e - s) / 1000);
    return `${diff}s`;
  };

  return (
    <div className="observability-view">
      <div className="view-header">
        <h2>Pipeline Observability</h2>
        <button className="trigger-btn" onClick={fetchRuns} disabled={loading}>
          <RefreshCcw size={16} className={loading ? 'animate-spin' : ''} />
          Refresh
        </button>
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
              <tr key={run.id} className={`status-${run.status.toLowerCase()}`}>
                <td className="col-category">{run.category}</td>
                <td className="col-status">
                  <div className="status-badge">
                    {run.status === 'PENDING' && <Activity size={14} className="animate-pulse" />}
                    {run.status === 'SUCCESS' && <CheckCircle size={14} />}
                    {run.status === 'FAILED' && <AlertCircle size={14} />}
                    {run.status}
                  </div>
                </td>
                <td className="col-time">
                  <Clock size={12} /> {new Date(run.startTime).toLocaleTimeString()}
                </td>
                <td className="col-duration">{formatDuration(run.startTime, run.endTime)}</td>
                <td className="col-error">
                  {run.errorMessage && <span className="error-text">{run.errorMessage}</span>}
                  {!run.errorMessage && run.status === 'SUCCESS' && <span className="success-text">Nominal completion</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
