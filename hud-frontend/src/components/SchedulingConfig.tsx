import { useState, useEffect } from 'react';
import { Calendar, Clock, Power, Save, RefreshCw } from 'lucide-react';
import { apiFetch } from '../api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';

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
    <Card className="border-border/50 bg-card/50">
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle className="text-xl flex items-center gap-2">
            <Clock className="h-5 w-5 text-accent" /> Pipeline Scheduling
          </CardTitle>
          <CardDescription>Configure automated situational awareness cycles.</CardDescription>
        </div>
        {schedules.length === 0 && (
          <Button variant="outline" size="sm" onClick={handleInit} className="gap-2">
            <RefreshCw className="h-4 w-4" /> Initialize
          </Button>
        )}
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-col gap-3">
          {schedules.map(s => (
            <div key={s.id} className={`flex items-center justify-between p-3 rounded-lg border transition-all ${s.active ? 'border-border/80 bg-muted/40' : 'border-dashed opacity-70 bg-card/20'}`}>
              <div className="flex items-center gap-4">
                <div className={`p-2 rounded-md ${s.active ? 'bg-accent/20 text-[#79c0ff]' : 'bg-muted text-foreground'}`}>
                  <Calendar className="h-4 w-4" />
                </div>
                <div>
                  <div className="text-[11px] font-bold uppercase tracking-wider text-white">{s.category.replace('_', ' ')}</div>
                  <div className="text-[10px] text-foreground font-mono font-bold opacity-90">LR: {formatLastRun(s.lastRunAt)}</div>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2 bg-background/80 border border-border/80 rounded-md px-2 py-1">
                  <Input 
                    value={s.cronExpression} 
                    onChange={e => handleCronChange(s, e.target.value)}
                    className="h-6 w-56 border-none bg-transparent font-mono text-[10px] p-0 focus-visible:ring-0 text-white font-bold"
                  />
                  <Button variant="ghost" size="icon" className="h-6 w-6 text-[#79c0ff] hover:text-white" onClick={() => updateSchedule(s)}>
                    <Save className="h-3.5 w-3.5" />
                  </Button>
                </div>

                <Button 
                  variant={s.active ? "secondary" : "outline"} 
                  size="icon" 
                  className={`h-8 w-8 rounded-full border-border/60 ${s.active ? 'text-success hover:text-white bg-success/20' : 'text-white/60'}`}
                  onClick={() => handleToggle(s)}
                >
                  <Power className="h-4 w-4" />
                </Button>
              </div>
            </div>
          ))}
        </div>
        
        <div className="text-[10px] text-white bg-accent/20 p-2.5 rounded border border-accent/40 font-mono font-bold shadow-sm">
          FORMAT: sec min hour day month weekday | EX: <code className="text-[#79c0ff]">0 0 6 * * *</code> (06:00 Daily)
        </div>
      </CardContent>
    </Card>
  );
};
