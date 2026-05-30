import { useState, useEffect, useCallback } from 'react'
import { Routes, Route, Navigate, NavLink, useLocation } from 'react-router-dom'
import { BriefingView } from './components/BriefingView'
import { ObservabilityView } from './components/ObservabilityView'
import { ConfigView } from './components/ConfigView'
import { InvestmentsView } from './components/InvestmentsView'
import { LoginView } from './components/LoginView'
import { ChangePasswordView } from './components/ChangePasswordView'
import type { DailyBriefing, LlmConfig } from './components/types'
import { User, LogOut, Activity } from 'lucide-react'
import { apiFetch } from './api'
import { ModeToggle } from './components/mode-toggle'
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
            const providerPrefix = (active.provider === 'GEMINI' || active.provider === 'DEEPSEEK') ? 'Cloud' : 'Local';
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
    apiFetch('/api/briefings/trigger', { method: 'POST' })
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
    apiFetch('/api/auth/logout', { method: 'POST' })
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
      <div className="flex h-screen items-center justify-center bg-background text-foreground">
        <ChangePasswordView onChanged={() => { setPasswordChangeRequired(false); fetchAuthStatus() }} />
      </div>
    )
  }

  return (
    <div className="flex h-screen bg-background text-foreground overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 border-r bg-card flex flex-col justify-between">
        <div className="p-4">
          <h1 className="text-xl font-bold mb-6 text-primary tracking-tight">HUD</h1>
          <nav className="space-y-1 flex flex-col">
            <NavLink to="/news" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>News</NavLink>
            <NavLink to="/theaters" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>Theaters</NavLink>
            <NavLink to="/investments" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>Investments</NavLink>
            {isAdmin && (
              <>
                <NavLink to="/config" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>Config</NavLink>
                <NavLink to="/observability" className={({isActive}) => `px-3 py-2 rounded-md text-sm font-medium ${isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-secondary'}`}>Observability</NavLink>
              </>
            )}
          </nav>
        </div>
        <div className="p-4 border-t space-y-4">
          {showModelSwitcher && (
            <div className="flex flex-col space-y-1.5">
              <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Brain</label>
              <select className="text-sm bg-secondary border-none rounded p-1" value={selectedModel} onChange={e => setSelectedModel(e.target.value)}>
                <option value="global">Global Context</option>
                {configs.map(c => {
                   const providerPrefix = (c.provider === 'GEMINI' || c.provider === 'DEEPSEEK') ? 'Cloud' : 'Local';
                   const fullName = `${providerPrefix}: ${c.name} [${c.modelName}]`;
                   return <option key={c.id} value={fullName}>{fullName}</option>;
                })}
              </select>
            </div>
          )}
          <div className="flex items-center justify-between">
            {isAuthenticated ? (
              <div className="flex items-center space-x-2 text-sm text-muted-foreground">
                <User size={14} className="text-primary" />
                <span>{username}</span>
                <button onClick={handleLogout} title="Logout" className="hover:text-foreground"><LogOut size={14}/></button>
              </div>
            ) : (
              <button className="text-sm text-muted-foreground hover:text-foreground" onClick={() => setShowLogin(true)}>Admin Login</button>
            )}
            <ModeToggle />
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden bg-background">
        {/* Sub-tabs Top Bar if needed */}
        {showSubTabs && (
          <header className="h-14 border-b bg-card/50 backdrop-blur flex items-center px-6 gap-4">
            <NavLink to="/news/briefing" className={({isActive}) => `text-sm font-medium ${isActive ? 'text-primary border-b-2 border-primary h-full flex items-center' : 'text-muted-foreground hover:text-foreground'}`}>Briefing</NavLink>
            <NavLink to="/news/live" className={({isActive}) => `text-sm font-medium ${isActive ? 'text-primary border-b-2 border-primary h-full flex items-center' : 'text-muted-foreground hover:text-foreground'}`}>Live</NavLink>
          </header>
        )}
        
        <div className="flex-1 overflow-auto p-6 relative">
          {error && <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-md mb-4">Error: {error}</div>}
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
      </main>
      
      {loading && (
        <div className="fixed bottom-8 right-8 bg-card/95 backdrop-blur text-primary px-7 py-4 rounded-full font-bold shadow-2xl z-50 flex items-center gap-3 border border-primary/40 font-mono text-sm tracking-wide uppercase">
          <Activity className="h-5 w-5 animate-spin" />
          Fusing intelligence...
        </div>
      )}
      
      {showLogin && (
        <LoginView 
          onLoginSuccess={() => { fetchAuthStatus(); setShowLogin(false); }} 
          onCancel={() => setShowLogin(false)} 
        />
      )}
    </div>
  );
}

export default App
