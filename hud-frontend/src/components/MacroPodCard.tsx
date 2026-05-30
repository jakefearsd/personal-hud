import React from 'react';
import type { MacroPod } from './types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { BrainCircuit, Activity } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

interface Props {
  pod: MacroPod;
}

export const MacroPodCard: React.FC<Props> = ({ pod }) => {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
      <Card className="lg:col-span-2 border-border/60 bg-card/60">
        <CardHeader className="pb-3 border-b border-border/50">
          <CardTitle className="text-lg flex items-center gap-2">
            <Activity className="h-5 w-5 text-accent" />
            {pod.title} Data
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-4">
          <div className="space-y-4">
            {pod.metrics.map(metric => (
              <div key={metric.ticker} className="flex items-center justify-between p-3 rounded-md bg-secondary/20">
                <div className="flex-1">
                  <div className="text-sm font-semibold">{metric.label}</div>
                  <div className="text-xs text-muted-foreground font-mono">{metric.ticker}</div>
                </div>
                <div className="flex-1 text-center">
                  <div className="text-sm text-muted-foreground">Historical Percentile</div>
                  <div className="text-base font-bold text-primary">{metric.historicalPercentile.toFixed(1)}th</div>
                </div>
                <div className="flex-1 text-right flex flex-col items-end">
                  <div className="text-lg font-mono font-bold">{metric.currentValue.toFixed(2)}</div>
                  <Badge variant={metric.changePercent >= 0 ? "secondary" : "destructive"} className={`mt-1 text-[10px] font-mono font-bold px-1.5 py-0 ${metric.changePercent >= 0 ? 'bg-success/20 text-success border-success/40' : 'bg-destructive/20 text-destructive border-destructive/40'}`}>
                    {metric.changePercent >= 0 ? '+' : ''}{metric.changePercent.toFixed(2)}%
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
      
      <Card className="border-border/60 bg-card/60">
        <CardHeader className="pb-3 border-b border-border/50">
          <CardTitle className="text-lg flex items-center gap-2">
            <BrainCircuit className="h-5 w-5 text-emerald-500" />
            Sentiment Analysis
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-4 flex flex-col justify-between h-[calc(100%-60px)]">
          <p className="text-sm leading-relaxed text-muted-foreground italic">
            "{pod.sentimentNarrative}"
          </p>
          <div className="mt-4 text-[10px] uppercase tracking-widest text-muted-foreground opacity-70">
            Powered by HUD Analytical Engine
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
