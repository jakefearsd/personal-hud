import { useState, useEffect, useCallback } from 'react'
import { Routes, Route, Navigate, NavLink, useLocation } from 'react-router-dom'
import { BriefingView } from './components/BriefingView'
import { ObservabilityView } from './components/ObservabilityView'
import { ConfigView } from './components/ConfigView'
import { InvestmentsView } from './components/InvestmentsView'
import { LoginView } from './components/LoginView'
import { ChangePasswordView } from './components/ChangePasswordView'
import type { DailyBriefing, LlmConfig } from './components/types'
import { User, LogOut } from 'lucide-react'
import './App.css'

interface NewsArticle {
  title: string;
  url: string;
}

function App() {
  const location = useLocation()
  const [articles, setArticles] = useState<NewsArticle[]>([])
  const [briefingCache, setBriefingCache] = useState<Record<string, DailyBriefing[]>>({})
  const [configs, setConfigs] = useState<LlmConfig[]>([])
  const [selectedModel, setSelectedModel] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  // Auth State
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)
  const [username, setUsername] = useState('')
  const [showLogin, setShowLogin] = useState(false)
  const [passwordChangeRequired, setPasswordChangeRequired] = useState(false)

  const fetchAuthStatus = useCallback(() => {
    fetch('/api/auth/status')
      .then(res => res.json())
      .then(data => {
        setIsAuthenticated(data.authenticated)
        setIsAdmin(data.isAdmin)
        setUsername(data.username || '')
        setPasswordChangeRequired(data.passwordChangeRequired || false)
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
          if (active && !selectedModel) {
            const providerPrefix = active.provider === 'GEMINI' ? 'Cloud' : 'Local';
            setSelectedModel(`${providerPrefix}: ${active.name} [${active.modelName}]`)
          }
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
    const currentModel = model || 'global';
    const url = model ? `/api/briefings/latest?modelName=${encodeURIComponent(model)}` : '/api/briefings/latest'
    
    fetch(url)
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
           setBriefingCache(prev => ({
             ...prev,
             [currentModel]: data
           }));
        }
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
    const path = location.pathname
    if (path.startsWith('/news') || path === '/theaters') {
      fetchLatestBriefings(selectedModel)
      if (path === '/news/live') {
        fetchLiveNews()
      }
    }
  }, [location.pathname, selectedModel, fetchLatestBriefings, fetchLiveNews])

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
      })
  }

  const modelBriefings = briefingCache[selectedModel];
  const globalBriefings = briefingCache['global'] || [];
  const briefings = (modelBriefings && modelBriefings.length > 0) ? modelBriefings : globalBriefings;

  const showSubTabs = location.pathname.startsWith('/news');
  const showModelSwitcher = (location.pathname.startsWith('/news') || location.pathname === '/theaters') && configs.length > 0;

  if (isAuthenticated && passwordChangeRequired) {
    return (
      <div className="app-container">
        <ChangePasswordView onChanged={() => { setPasswordChangeRequired(false); fetchAuthStatus() }} />
      </div>
    )
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-left">
          <h1>HUD</h1>
          <nav className="main-tabs">
            <NavLink to="/news">News</NavLink>
            <NavLink to="/theaters">Theaters</NavLink>
            <NavLink to="/investments">Investments</NavLink>
            {isAdmin && (
              <>
                <NavLink to="/config">Config</NavLink>
                <NavLink to="/observability">Observability</NavLink>
              </>
            )}
          </nav>
        </div>
        
        <div className="header-right">
          {showModelSwitcher && (
            <div className="model-switcher">
              <label>Brain:</label>
              <select value={selectedModel} onChange={e => setSelectedModel(e.target.value)}>
                {configs.map(c => {
                   const providerPrefix = c.provider === 'GEMINI' ? 'Cloud' : 'Local';
                   const fullName = `${providerPrefix}: ${c.name} [${c.modelName}]`;
                   return <option key={c.id} value={fullName}>{fullName}</option>;
                })}
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

          {showSubTabs && (
            <nav className="sub-tabs">
              <NavLink to="/news/briefing">Briefing</NavLink>
              <NavLink to="/news/live">Live</NavLink>
            </nav>
          )}
        </div>
      </header>

      <main className="app-content">
        {error && <div className="error-banner">Error: {error}</div>}
        
        <div className={`content-wrapper ${loading ? 'is-loading' : ''}`}>
          <Routes>
            <Route path="/" element={<Navigate to="/theaters" replace />} />
            
            <Route path="/news" element={<Navigate to="/news/briefing" replace />} />
            <Route path="/news/briefing" element={
              <BriefingView 
                briefings={briefings} 
                type="general"
                loading={loading} 
                onTrigger={isAdmin ? triggerBriefing : undefined} 
              />
            } />
            <Route path="/news/live" element={
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
            } />

            <Route path="/theaters" element={
              <BriefingView 
                briefings={briefings} 
                type="theater"
                loading={loading} 
                onTrigger={isAdmin ? triggerBriefing : undefined} 
              />
            } />

            <Route path="/investments" element={<InvestmentsView />} />

            <Route path="/config" element={isAdmin ? <ConfigView /> : <Navigate to="/theaters" replace />} />
            
            <Route path="/observability" element={isAdmin ? <ObservabilityView /> : <Navigate to="/theaters" replace />} />

            <Route path="*" element={<Navigate to="/theaters" replace />} />
          </Routes>
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
