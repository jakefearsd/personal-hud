import { useState, useEffect } from 'react';
import { Calendar, Clock, Power, Save } from 'lucide-react';
import { apiFetch } from '../api';

export interface BriefingSchedule {
  id: number;
  category: string;
  cronExpression: string;
  active: boolean;
  lastRunAt: string | null;
}

export const SchedulingConfig = () => {
  const [schedules, setSchedules] = useState<BriefingSchedule[]>([]);

  const fetchSchedules = () => {
    fetch('/api/config/schedules')
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) setSchedules(data);
      })
      .catch(() => {});
  };

  useEffect(() => {
    fetchSchedules();
  }, []);

  const handleToggle = (schedule: BriefingSchedule) => {
    const updated = { ...schedule, active: !schedule.active };
    updateSchedule(updated);
  };

  const handleCronChange = (schedule: BriefingSchedule, expr: string) => {
    const updated = { ...schedule, cronExpression: expr };
    // We don't auto-save cron to avoid partial edits triggering refreshes
    setSchedules(prev => prev.map(s => s.id === schedule.id ? updated : s));
  };

  const updateSchedule = (schedule: BriefingSchedule) => {
    apiFetch(`/api/config/schedules/${schedule.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(schedule)
    })
    .then(() => fetchSchedules());
  };

  const handleInit = () => {
    apiFetch('/api/config/schedules/init', { method: 'POST' })
      .then(() => fetchSchedules());
  };

  const formatLastRun = (dateStr: string | null) => {
    if (!dateStr) return 'Never';
    return new Date(dateStr).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
  };

  return (
    <div className="scheduling-config">
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem'}}>
        <h3>Pipeline Scheduling</h3>
        {schedules.length === 0 && (
          <button className="suggestion-btn" onClick={handleInit}>
            Initialize Default Schedules
          </button>
        )}
      </div>

      <div className="schedule-list">
        {schedules.map(s => (
          <div key={s.id} className={`schedule-card ${s.active ? 'active' : 'inactive'}`}>
            <div className="schedule-info">
              <div className="cat-icon">
                 <Calendar size={20} color={s.active ? '#58a6ff' : '#8b949e'} />
              </div>
              <div className="cat-details">
                <div className="cat-name">{s.category.replace('_', ' ')}</div>
                <div className="last-run">Last Run: {formatLastRun(s.lastRunAt)}</div>
              </div>
            </div>

            <div className="schedule-controls">
              <div className="cron-input-group">
                <Clock size={14} />
                <input 
                  value={s.cronExpression} 
                  onChange={e => handleCronChange(s, e.target.value)}
                  placeholder="0 0 6 * * *"
                  className="cron-input"
                />
                <button className="icon-btn small" onClick={() => updateSchedule(s)} title="Save Schedule">
                  <Save size={14} />
                </button>
              </div>

              <button 
                className={`toggle-btn ${s.active ? 'on' : 'off'}`} 
                onClick={() => handleToggle(s)}
                title={s.active ? 'Deactivate' : 'Activate'}
              >
                <Power size={16} />
              </button>
            </div>
          </div>
        ))}
      </div>
      
      <div className="cron-help">
        <p>Cron Format: <code>second minute hour day month weekday</code></p>
        <p>Example: <code>0 0 6 * * *</code> (Every day at 06:00:00)</p>
      </div>
    </div>
  );
};
