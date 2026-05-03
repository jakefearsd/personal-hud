import { useState, useEffect, useCallback } from 'react'
import { BriefingView } from './components/BriefingView'
import { ObservabilityView } from './components/ObservabilityView'
import { ConfigView } from './components/ConfigView'
import { InvestmentsView } from './components/InvestmentsView'
import { LoginView } from './components/LoginView'
import type { DailyBriefing, LlmConfig } from './components/types'
import { User, LogOut } from 'lucide-react'
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
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  // Auth State
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)
  const [username, setUsername] = useState('')
  const [showLogin, setShowLogin] = useState(false)

  const fetchAuthStatus = useCallback(() => {
    fetch('/api/auth/status')
      .then(res => res.json())
      .then(data => {
        setIsAuthenticated(data.authenticated)
        setIsAdmin(data.isAdmin)
        setUsername(data.username || '')
      })
      .catch(err => console.error("Auth check failed", err))
  }, [])

  const fetchConfigs = useCallback(() => {
    fetch('/api/config/brains')
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          setConfigs(data)
          const active = data.find((c: any) => c.active)
          if (active && !selectedModel) setSelectedModel(active.name)
        }
      })
      .catch(err => console.error("Failed to fetch configs", err))
  }, [selectedModel])

  const fetchLiveNews = useCallback(() => {
    setLoading(true)
    fetch('/api/news')
      .then(res => res.json())
      .then(data => {
        setArticles(Array.isArray(data) ? data : [])
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }, [])

  const fetchLatestBriefings = useCallback((model?: string) => {
    setLoading(true)
    const url = model ? `/api/briefings/latest?modelName=${encodeURIComponent(model)}` : '/api/briefings/latest'
    fetch(url)
      .then(res => res.json())
      .then(data => {
        setBriefings(Array.isArray(data) ? data : [])
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }, [])

  useEffect(() => {
    fetchAuthStatus()
    fetchConfigs()
  }, [fetchAuthStatus, fetchConfigs])

  useEffect(() => {
    if (activeMainTab === 'news' || activeMainTab === 'theaters') {
      fetchLatestBriefings(selectedModel)
      if (activeMainTab === 'news' && activeNewsTab === 'live') {
        fetchLiveNews()
      }
    }
  }, [activeMainTab, activeNewsTab, selectedModel, fetchLatestBriefings, fetchLiveNews])

  const triggerBriefing = () => {
    if (!isAdmin) return;
    setLoading(true)
    fetch('/api/briefings/trigger', { method: 'POST' })
      .then(res => {
        if (res.ok) {
           alert('Briefing generation started for all active models.')
        } else {
           alert('Failed to trigger briefing. Administrative access required.')
        }
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }

  const handleLogout = () => {
    fetch('/api/auth/logout', { method: 'POST' })
      .then(() => {
        setIsAuthenticated(false)
        setIsAdmin(false)
        setUsername('')
        if (activeMainTab === 'config' || activeMainTab === 'observability') {
          setActiveMainTab('theaters')
        }
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
            {isAdmin && (
              <>
                <button className={activeMainTab === 'config' ? 'active' : ''} onClick={() => setActiveMainTab('config')}>Config</button>
                <button className={activeMainTab === 'observability' ? 'active' : ''} onClick={() => setActiveMainTab('observability')}>Observability</button>
              </>
            )}
          </nav>
        </div>
        
        <div className="header-right">
          {(activeMainTab === 'news' || activeMainTab === 'theaters') && configs.length > 0 && (
            <div className="model-switcher">
              <label>Brain:</label>
              <select value={selectedModel} onChange={e => setSelectedModel(e.target.value)}>
                {configs.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
              </select>
            </div>
          )}
          
          <div className="auth-controls">
            {isAuthenticated ? (
              <div className="user-info">
                <User size={16} color="#58a6ff" />
                <span className="username">{username}</span>
                <button className="icon-btn" onClick={handleLogout} title="Logout"><LogOut size={16}/></button>
              </div>
            ) : (
              <button className="login-trigger-btn" onClick={() => setShowLogin(true)}>Admin Login</button>
            )}
          </div>

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
        
        <div className={`content-wrapper ${loading ? 'is-loading' : ''}`}>
          {activeMainTab === 'news' && (
            <>
              {activeNewsTab === 'briefing' ? (
                <BriefingView 
                  briefings={briefings.filter(b => !['THEATER_UKRAINE', 'THEATER_MIDDLE_EAST', 'GLOBAL_SITREP'].includes(b.category))} 
                  loading={loading} 
                  onTrigger={isAdmin ? triggerBriefing : undefined} 
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
               onTrigger={isAdmin ? triggerBriefing : undefined} 
             />
          )}

          {activeMainTab === 'investments' && <InvestmentsView />}

          {activeMainTab === 'config' && isAdmin && <ConfigView />}

          {activeMainTab === 'observability' && isAdmin && <ObservabilityView />}
        </div>

        {loading && <div className="global-loader-overlay">Fusing intelligence...</div>}
        
        {showLogin && (
          <LoginView 
            onLoginSuccess={() => { fetchAuthStatus(); setShowLogin(false); }} 
            onCancel={() => setShowLogin(false)} 
          />
        )}
      </main>
    </div>
  )
}

export default App
