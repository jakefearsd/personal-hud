import { useState, useEffect } from 'react'
import { BriefingView } from './components/BriefingView'
import './App.css'

interface NewsArticle {
  title: string;
  url: string;
}

interface DailyBriefing {
  id: number;
  briefingDate: string;
  category: any;
  markdownContent: string;
}

type MainTab = 'news' | 'investments' | 'config';
type NewsTab = 'briefing' | 'live';

function App() {
  const [activeMainTab, setActiveMainTab] = useState<MainTab>('news')
  const [activeNewsTab, setActiveNewsTab] = useState<NewsTab>('briefing')
  const [articles, setArticles] = useState<NewsArticle[]>([])
  const [briefings, setBriefings] = useState<DailyBriefing[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (activeMainTab === 'news') {
      if (activeNewsTab === 'live') {
        fetchLiveNews()
      } else {
        fetchLatestBriefings()
      }
    }
  }, [activeMainTab, activeNewsTab])

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

  const fetchLatestBriefings = () => {
    setLoading(true)
    fetch('/api/briefings/latest')
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
        alert('Briefing generation started. This will take a few minutes. Check back soon.')
        setLoading(false)
      })
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-left">
          <h1>HUD</h1>
          <nav className="main-tabs">
            <button 
              className={activeMainTab === 'news' ? 'active' : ''} 
              onClick={() => setActiveMainTab('news')}
            >
              News
            </button>
            <button 
              className={activeMainTab === 'investments' ? 'active' : ''} 
              onClick={() => setActiveMainTab('investments')}
            >
              Investments
            </button>
            <button 
              className={activeMainTab === 'config' ? 'active' : ''} 
              onClick={() => setActiveMainTab('config')}
            >
              Config
            </button>
          </nav>
        </div>
        
        {activeMainTab === 'news' && (
          <nav className="sub-tabs">
            <button 
              className={activeNewsTab === 'briefing' ? 'active' : ''} 
              onClick={() => setActiveNewsTab('briefing')}
            >
              Strategic Briefing
            </button>
            <button 
              className={activeNewsTab === 'live' ? 'active' : ''} 
              onClick={() => setActiveNewsTab('live')}
            >
              Live Feed
            </button>
          </nav>
        )}
      </header>

      <main className="app-content">
        {error && <div className="error-banner">Error: {error}</div>}
        
        {activeMainTab === 'news' && (
          <>
            {loading && <div className="loader">Processing analytics...</div>}
            {activeNewsTab === 'briefing' ? (
              <BriefingView briefings={briefings} loading={loading} onTrigger={triggerBriefing} />
            ) : (
              <div className="live-feed">
                <h2>Latest Financial Headlines</h2>
                <ul className="news-list">
                  {articles.map((article, index) => (
                    <li key={index} className="news-item">
                      <a href={article.url} target="_blank" rel="noopener noreferrer">
                        {article.title}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </>
        )}

        {activeMainTab === 'investments' && (
          <div className="placeholder-view">
            <h2>Investment Portfolio</h2>
            <p>Investment data and analysis module coming soon.</p>
          </div>
        )}

        {activeMainTab === 'config' && (
          <div className="placeholder-view">
            <h2>System Configuration</h2>
            <p>Administrative and scraping configuration module coming soon.</p>
          </div>
        )}
      </main>
    </div>
  )
}

export default App
