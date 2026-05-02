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

function App() {
  const [activeTab, setActiveTab] = useState<'live' | 'briefing'>('briefing')
  const [articles, setArticles] = useState<NewsArticle[]>([])
  const [briefings, setBriefings] = useState<DailyBriefing[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (activeTab === 'live') {
      fetchLiveNews()
    } else {
      fetchLatestBriefings()
    }
  }, [activeTab])

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
        <h1>HUD (Heads-Up Display)</h1>
        <nav className="tabs">
          <button 
            className={activeTab === 'briefing' ? 'active' : ''} 
            onClick={() => setActiveTab('briefing')}
          >
            Strategic Briefing
          </button>
          <button 
            className={activeTab === 'live' ? 'active' : ''} 
            onClick={() => setActiveTab('live')}
          >
            Live Feed
          </button>
        </nav>
      </header>

      <main className="app-content">
        {error && <div className="error-banner">Error: {error}</div>}
        
        {loading && <div className="loader">Processing analytics...</div>}

        {activeTab === 'briefing' ? (
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
      </main>
    </div>
  )
}

export default App
