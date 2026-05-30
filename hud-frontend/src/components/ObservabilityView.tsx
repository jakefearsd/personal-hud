import { useState, useEffect } from 'react';
import type { PipelineRun } from './types';
import { Activity, CheckCircle, AlertCircle, RefreshCcw, Trash2, Cpu } from 'lucide-react';
import { apiFetch } from '../api';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

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

  return (
    <div className="flex flex-col gap-8 w-full">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-semibold tracking-tight">Pipeline Observability</h2>
          <p className="text-sm text-muted-foreground">Real-time status of intelligence acquisition and synthesis cycles.</p>
        </div>
        <div className="flex gap-3">
          <Button variant="outline" size="sm" className="gap-2 text-destructive border-destructive/20 hover:bg-destructive/10 hover:text-destructive" onClick={flushRuns} disabled={loading || !runs || runs.length === 0}>
            <Trash2 className="h-4 w-4" />
            Flush Logs
          </Button>
          <Button variant="secondary" size="sm" className="gap-2" onClick={fetchRuns} disabled={loading}>
            <RefreshCcw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
        </div>
      </div>

      <Card className="border-border/50 bg-card/50 overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="bg-muted/30">
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Category</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Status</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Neural Node</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Started</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Duration</TableHead>
              <TableHead className="font-mono text-[10px] uppercase tracking-wider">Telemetry / Payload</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {runs.map(run => (
              <TableRow key={run.id} className="hover:bg-muted/20 transition-colors border-border/50">
                <TableCell className="py-4">
                  <span className="font-mono text-sm font-bold text-white uppercase tracking-wider">
                    {run.category?.replace('_', ' ') || 'N/A'}
                  </span>
                </TableCell>
                <TableCell>
                  <div className="flex items-center gap-2 font-mono text-[10px] font-bold">
                    {run.status === 'PENDING' && <Activity className="h-3.5 w-3.5 animate-pulse text-yellow-400" />}
                    {run.status === 'SUCCESS' && <CheckCircle className="h-3.5 w-3.5 text-success" />}
                    {run.status === 'FAILED' && <AlertCircle className="h-3.5 w-3.5 text-destructive" />}
                    <span className={
                      run.status === 'SUCCESS' ? 'text-success' : 
                      run.status === 'FAILED' ? 'text-destructive' : 
                      'text-yellow-400'
                    }>
                      {run.status || 'UNKNOWN'}
                    </span>
                  </div>
                </TableCell>
                <TableCell className="text-[11px] font-mono text-foreground whitespace-nowrap">
                   <div className="flex items-center gap-1.5">
                     <Cpu className="h-3.5 w-3.5 text-accent" />
                     {run.modelName || 'Neural Engine'}
                   </div>
                </TableCell>
                <TableCell className="text-[11px] font-mono text-foreground whitespace-nowrap">
                   {formatTimestamp(run.startTime)}
                </TableCell>
                <TableCell className="text-[11px] font-mono text-foreground font-semibold">
                   {calculateDuration(run.startTime, run.endTime)}
                </TableCell>
                <TableCell>
                  {run.errorMessage && (
                    <div className="space-y-2">
                      <div className="text-[11px] text-destructive font-bold flex items-center gap-1">
                        <AlertCircle className="h-3.5 w-3.5" />
                        {run.errorMessage}
                      </div>
                      {run.errorDetail && (
                        <div className="max-h-32 overflow-y-auto bg-black/60 p-3 rounded border border-destructive/30 shadow-inner">
                          <pre className="text-[11px] leading-relaxed font-mono text-white/90 break-all whitespace-pre-wrap">
                            {run.errorDetail}
                          </pre>
                        </div>
                      )}
                    </div>
                  )}
                  {!run.errorMessage && run.status === 'SUCCESS' && (
                    <div className="flex items-center gap-3 text-[11px] font-mono text-foreground">
                      {run.inputTokens != null && run.outputTokens != null ? (
                        <>
                          <Badge variant="outline" className="border-border bg-muted/40 text-white font-bold">IN: {run.inputTokens.toLocaleString()}</Badge>
                          <Badge variant="outline" className="border-border bg-muted/40 text-white font-bold">OUT: {run.outputTokens.toLocaleString()}</Badge>
                        </>
                      ) : (
                        <span className="italic text-muted-foreground font-medium">Nominal completion</span>
                      )}
                    </div>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {runs.length === 0 && !loading && (
               <TableRow>
                 <TableCell colSpan={6} className="h-32 text-center text-muted-foreground">
                    No pipeline cycles detected in current horizon.
                 </TableCell>
               </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>
    </div>
  );
};

