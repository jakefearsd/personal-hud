import ReactMarkdown from 'react-markdown';
import type { DailyBriefing, BriefingCategory } from './types';

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
    <div className="briefing-view">
      <div className="view-header">
        <h2>{type === 'theater' ? 'Theater Intelligence' : 'Strategic Daily Briefing'}</h2>
        {onTrigger && (
          <button className="trigger-btn" onClick={onTrigger} disabled={loading}>
            {loading ? 'Generating...' : 'Refresh Briefing'}
          </button>
        )}
      </div>

      {filteredBriefings.length === 0 && !loading && (
        <div className="empty-state">
          <p>No {type === 'theater' ? 'theater reports' : 'briefings'} generated for today yet.</p>
          {onTrigger && <button onClick={onTrigger}>Generate Now</button>}
        </div>
      )}

      <div className="briefing-grid">
        {filteredBriefings.map((b) => (
          <div key={b.id} className="briefing-card">
            <div className="briefing-card-header">
              <div className="header-titles">
                <h3>{categoryTitles[b.category] || b.category}</h3>
                <span className="timestamp">{new Date(b.generatedAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}</span>
              </div>
              {b.modelName && (
                <span className="model-tag" title="Intelligence Source">
                  {b.modelName}
                </span>
              )}
            </div>
            <div className="markdown-body">
              {b.htmlContent ? (
                <div dangerouslySetInnerHTML={{ __html: b.htmlContent }} />
              ) : (
                <ReactMarkdown>{b.markdownContent}</ReactMarkdown>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
