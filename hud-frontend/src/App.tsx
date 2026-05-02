import { useState, useEffect } from 'react'
import { BriefingView } from './components/BriefingView'
import { ObservabilityView } from './components/ObservabilityView'
import { ConfigView } from './components/ConfigView'
import type { DailyBriefing, LlmConfig } from './components/types'
import './App.css'

interface NewsArticle {
  title: string;
  url: string;
}

type MainTab = 'news' | 'theaters' | 'investments' | 'config' | 'observability';
type NewsTab = 'briefing' | 'live';

function App() {
  const [activeMainTab, setActiveMainTab] = useState<MainTab>('theaters')
  const [activeNewsTab, setActiveNewsTab] = useState<NewsTab>('briefing')
  const [articles, setArticles] = useState<NewsArticle[]>([])
  const [briefings, setBriefings] = useState<DailyBriefing[]>([])
  const [configs, setConfigs] = useState<LlmConfig[]>([])
  const [selectedModel, setSelectedModel] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchConfigs()
  }, [])

  useEffect(() => {
    if (activeMainTab === 'news' || activeMainTab === 'theaters') {
      fetchLatestBriefings(selectedModel)
      if (activeMainTab === 'news' && activeNewsTab === 'live') {
        fetchLiveNews()
      }
    }
  }, [activeMainTab, activeNewsTab, selectedModel])

  const fetchConfigs = () => {
    fetch('/api/config/brains')
      .then(res => res.json())
      .then(data => {
        setConfigs(data)
        const active = data.find((c: any) => c.active)
        if (active) setSelectedModel(active.name)
      })
  }

  const fetchLiveNews = () => {
    setLoading(true)
    fetch('/api/news')
      .then(res => res.json())
      .then(data => {
        setArticles(data)
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }

  const fetchLatestBriefings = (model?: string) => {
    setLoading(true)
    const url = model ? `/api/briefings/latest?modelName=${encodeURIComponent(model)}` : '/api/briefings/latest'
    fetch(url)
      .then(res => res.json())
      .then(data => {
        setBriefings(data)
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }

  const triggerBriefing = () => {
    setLoading(true)
    fetch('/api/briefings/trigger', { method: 'POST' })
      .then(() => {
        alert('Briefing generation started for all active models.')
        setLoading(false)
      })
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-left">
          <h1>HUD</h1>
          <nav className="main-tabs">
            <button className={activeMainTab === 'news' ? 'active' : ''} onClick={() => setActiveMainTab('news')}>News</button>
            <button className={activeMainTab === 'theaters' ? 'active' : ''} onClick={() => setActiveMainTab('theaters')}>Theaters</button>
            <button className={activeMainTab === 'investments' ? 'active' : ''} onClick={() => setActiveMainTab('investments')}>Investments</button>
            <button className={activeMainTab === 'config' ? 'active' : ''} onClick={() => setActiveMainTab('config')}>Config</button>
            <button className={activeMainTab === 'observability' ? 'active' : ''} onClick={() => setActiveMainTab('observability')}>Observability</button>
          </nav>
        </div>
        
        <div className="header-right">
          {(activeMainTab === 'news' || activeMainTab === 'theaters') && (
            <div className="model-switcher">
              <label>Brain:</label>
              <select value={selectedModel} onChange={e => setSelectedModel(e.target.value)}>
                {configs.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
              </select>
            </div>
          )}
          {activeMainTab === 'news' && (
            <nav className="sub-tabs">
              <button className={activeNewsTab === 'briefing' ? 'active' : ''} onClick={() => setActiveNewsTab('briefing')}>Briefing</button>
              <button className={activeNewsTab === 'live' ? 'active' : ''} onClick={() => setActiveNewsTab('live')}>Live</button>
            </nav>
          )}
        </div>
      </header>

      <main className="app-content">
        {error && <div className="error-banner">Error: {error}</div>}
        
        {activeMainTab === 'news' && (
          <>
            {loading && <div className="loader">Fusing intelligence...</div>}
            {activeNewsTab === 'briefing' ? (
              <BriefingView 
                briefings={briefings.filter(b => !['THEATER_UKRAINE', 'THEATER_MIDDLE_EAST', 'GLOBAL_SITREP'].includes(b.category))} 
                loading={loading} 
                onTrigger={triggerBriefing} 
              />
            ) : (
              <div className="live-feed">
                <h2>Latest Headlines</h2>
                <ul className="news-list">
                  {articles.map((article, index) => (
                    <li key={index} className="news-item">
                      <a href={article.url} target="_blank" rel="noopener noreferrer">{article.title}</a>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </>
        )}

        {activeMainTab === 'theaters' && (
           <BriefingView 
             briefings={briefings.filter(b => ['THEATER_UKRAINE', 'THEATER_MIDDLE_EAST', 'GLOBAL_SITREP'].includes(b.category))} 
             loading={loading} 
             onTrigger={triggerBriefing} 
           />
        )}

        {activeMainTab === 'investments' && (
          <div className="placeholder-view">
            <h2>Investment Portfolio</h2>
            <p>Module coming soon.</p>
          </div>
        )}

        {activeMainTab === 'config' && <ConfigView />}

        {activeMainTab === 'observability' && <ObservabilityView />}
      </main>
    </div>
  )
}

export default App
