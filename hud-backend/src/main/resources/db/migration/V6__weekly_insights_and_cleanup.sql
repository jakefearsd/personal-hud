DROP TABLE IF EXISTS market_events;

CREATE TABLE weekly_insights (
    id UUID PRIMARY KEY,
    narrative_text TEXT NOT NULL,
    key_considerations JSONB NOT NULL,
    analysis_start_date TIMESTAMP NOT NULL,
    analysis_end_date TIMESTAMP NOT NULL,
    generated_at TIMESTAMP NOT NULL
);
