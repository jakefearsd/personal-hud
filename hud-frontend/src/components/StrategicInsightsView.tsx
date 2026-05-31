import { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

type WeeklyInsight = {
  id: string;
  narrativeText: string;
  keyConsiderations: string[];
  generatedAt: string;
};

export const StrategicInsightsView = () => {
  const [insight, setInsight] = useState<WeeklyInsight | null>(null);

  useEffect(() => {
    fetch('/api/investments/insights/latest')
      .then(r => r.json())
      .then(data => {
         if (data && data.id) setInsight(data);
      })
      .catch(() => {});
  }, []);

  if (!insight) return null;

  return (
    <Card className="mt-8">
      <CardHeader>
        <CardTitle>Strategic Financial Considerations</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="mb-6">
          <h4 className="font-semibold mb-2">The 12-Week Macro Narrative</h4>
          <p className="text-muted-foreground">{insight.narrativeText}</p>
        </div>
        <div>
          <h4 className="font-semibold mb-2">Key Considerations for Investors</h4>
          <ul className="list-disc pl-5 space-y-2 text-muted-foreground">
            {insight.keyConsiderations.map((c, i) => <li key={i}>{c}</li>)}
          </ul>
        </div>
        <div className="mt-6 text-xs text-muted-foreground text-right">
          Last updated: {new Date(insight.generatedAt).toLocaleString()}
        </div>
      </CardContent>
    </Card>
  );
};
