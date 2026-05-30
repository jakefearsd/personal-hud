import ReactMarkdown from 'react-markdown';
import type { DailyBriefing, BriefingCategory } from './types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Clock, RefreshCcw } from 'lucide-react';

interface Props {
  briefings: DailyBriefing[];
  loading: boolean;
  type: 'general' | 'theater';
  onTrigger?: () => void;
}

const categoryTitles: Record<BriefingCategory, string> = {
  'WORLD_NEWS': 'World News',
  'US_NEWS': 'US News',
  'FINANCE': 'Financial Briefing',
  'TECHNOLOGY': 'Technology & AI',
  'GLOBAL_SITREP': 'Global SITREP (Multi-Theater)',
  'THEATER_UKRAINE': 'European Theater: Ukraine',
  'THEATER_MIDDLE_EAST': 'Middle East Theater'
};

const THEATER_CATEGORIES: BriefingCategory[] = ['THEATER_UKRAINE', 'THEATER_MIDDLE_EAST', 'GLOBAL_SITREP'];

export const BriefingView = ({ briefings, loading, type, onTrigger }: Props) => {
  const filteredBriefings = briefings.filter(b => 
    type === 'theater' 
      ? THEATER_CATEGORIES.includes(b.category)
      : !THEATER_CATEGORIES.includes(b.category)
  );

  return (
    <div className="flex flex-col gap-8">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-semibold tracking-tight">
          {type === 'theater' ? 'Theater Intelligence' : 'Strategic Daily Briefing'}
        </h2>
        {onTrigger && (
          <Button 
            variant="secondary" 
            onClick={onTrigger} 
            disabled={loading}
            className="gap-2"
          >
            {loading ? <RefreshCcw className="h-4 w-4 animate-spin" /> : <RefreshCcw className="h-4 w-4" />}
            {loading ? 'Generating...' : 'Refresh Briefing'}
          </Button>
        )}
      </div>

      {filteredBriefings.length === 0 && !loading && (
        <Card className="flex flex-col items-center justify-center p-12 text-center bg-muted/30 border-dashed">
          <p className="text-white font-medium mb-4">No {type === 'theater' ? 'theater reports' : 'briefings'} generated for today yet.</p>
          {onTrigger && <Button onClick={onTrigger} variant="outline" className="border-accent text-accent hover:bg-accent/10">Generate Now</Button>}
        </Card>
      )}

      <div className="briefing-grid">
        {filteredBriefings.map((b) => (
          <Card key={b.id} className="overflow-hidden border-border/60 bg-card/60 backdrop-blur-md transition-all hover:border-accent/80 group">
            <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-4 border-b border-border/50 bg-muted/20">
              <div className="space-y-1">
                <CardTitle className="text-[11px] font-mono font-bold uppercase tracking-widest text-accent group-hover:text-[#79c0ff] transition-colors">
                  {categoryTitles[b.category] || b.category}
                </CardTitle>
                <div className="flex items-center text-[10px] text-foreground font-mono font-semibold gap-1 opacity-90">
                  <Clock className="h-3 w-3 text-accent" />
                  {new Date(b.generatedAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                </div>
              </div>
              {b.modelName && (
                <Badge variant="outline" className="text-[10px] uppercase font-mono py-0 tracking-tighter border-accent/40 text-[#79c0ff] bg-accent/10 font-bold">
                  {b.modelName}
                </Badge>
              )}
            </CardHeader>
            <CardContent className="pt-6">
              <div className="markdown-body prose dark:prose-invert max-w-none prose-sm leading-relaxed">
                {b.htmlContent ? (
                  <div dangerouslySetInnerHTML={{ __html: b.htmlContent }} />
                ) : (
                  <ReactMarkdown>{b.markdownContent}</ReactMarkdown>
                )}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};
