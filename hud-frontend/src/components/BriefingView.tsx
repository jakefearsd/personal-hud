import ReactMarkdown from 'react-markdown';
import type { DailyBriefing, BriefingCategory } from './types';

interface Props {
  briefings: DailyBriefing[];
  loading: boolean;
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

export const BriefingView = ({ briefings, loading, onTrigger }: Props) => {
  return (
    <div className="briefing-view">
      <div className="view-header">
        <h2>Strategic Daily Briefing</h2>
        {onTrigger && (
          <button className="trigger-btn" onClick={onTrigger} disabled={loading}>
            {loading ? 'Generating...' : 'Refresh Briefing'}
          </button>
        )}
      </div>

      {briefings.length === 0 && !loading && (
        <div className="empty-state">
          <p>No briefings generated for today yet.</p>
          {onTrigger && <button onClick={onTrigger}>Generate Now</button>}
        </div>
      )}

      <div className="briefing-grid">
        {briefings.map((b) => (
          <div key={b.id} className="briefing-card">
            <h3>{categoryTitles[b.category]}</h3>
            <div className="markdown-body">
              <ReactMarkdown>{b.markdownContent}</ReactMarkdown>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
