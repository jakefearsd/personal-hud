import { http, HttpResponse, delay } from 'msw'

let authenticated = true;

export const resetAuth = () => { authenticated = true; };

export const handlers = [
  http.get('/api/auth/status', () => {
    if (!authenticated) return HttpResponse.json({ authenticated: false });
    return HttpResponse.json({
      authenticated: true,
      isAdmin: true,
      username: 'admin',
      passwordChangeRequired: false
    })
  }),

  http.post('/api/auth/logout', () => {
    authenticated = false;
    return HttpResponse.json({ success: true });
  }),

  http.post('/api/briefings/trigger', async () => {
    await delay(100);
    return HttpResponse.json({ status: 'triggered' });
  }),

  http.get('/api/news/live', () => {
    return HttpResponse.json([
      { title: 'Live News 1', url: 'https://news.com/1' }
    ])
  }),

  http.get('/api/config/brains', () => {
    return HttpResponse.json([
      { id: 1, name: 'Gemini Flash', provider: 'GEMINI', modelName: 'gemini-1.5-flash', active: true, numCtx: 8000 }
    ])
  }),

  http.get('/api/briefings/latest', () => {
    return HttpResponse.json([
      { id: 1, generatedAt: new Date().toISOString(), category: 'THEATER_UKRAINE', markdownContent: 'Ukraine status update.' },
      { id: 2, generatedAt: new Date().toISOString(), category: 'WORLD_NEWS', markdownContent: 'World news update.' }
    ])
  }),

  http.get('/api/investments/macro-pods', () => {
    return HttpResponse.json([
      {
        id: 'economic_health',
        title: 'Economic Health',
        sentimentNarrative: 'The current market narrative is dominated by a productivity-debt paradox.',
        metrics: [
          { ticker: 'CPI', label: 'Core Inflation', currentValue: 3.2, historicalPercentile: 85.0, changePercent: -1.5 }
        ]
      }
    ])
  }),

  http.get('/api/investments/history/:ticker', ({ params }) => {
    const { ticker } = params
    return HttpResponse.json([
      { ticker, price: 5150, timestamp: new Date(Date.now() - 86400000).toISOString() },
      { ticker, price: 5200.5, timestamp: new Date().toISOString() }
    ])
  }),

  http.get('/api/investments/predictions/latest', () => {
    return HttpResponse.json([
      { 
        id: 1, 
        ticker: '^GSPC', 
        predictedPrice: 5300.5, 
        confidenceScore: 80, 
        targetDate: new Date(Date.now() + 604800000).toISOString().split('T')[0],
        generationDate: new Date().toISOString(), 
        rationale: 'Bullish momentum.',
        synthesis: 'Market sentiment is positive.',
        modelName: 'Gemini 3.5 Flash'
      }
    ])
  }),

  http.get('/api/config/schedules', () => {
    return HttpResponse.json([
      { id: 1, category: 'WORLD_NEWS', cronExpression: '0 0 6 * * *', active: true, lastRunAt: null }
    ])
  })
]
