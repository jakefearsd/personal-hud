import { useState, useEffect } from 'react';
import type { LlmConfig, LlmProvider } from './types';
import { Save, Trash2, Power, BrainCircuit, Key } from 'lucide-react';

export const ConfigView = () => {
  const [configs, setConfigs] = useState<LlmConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [newPassword, setNewPassword] = useState('');
  const [pwdStatus, setPwdStatus] = useState<string | null>(null);

  const [editingConfig, setEditingConfig] = useState<Partial<LlmConfig>>({
    name: '',
    provider: 'OLLAMA',
    baseUrl: '',
    modelName: '',
    apiKey: '',
    numCtx: 131072,
    active: true
  });

  const fetchConfigs = () => {
    setLoading(true);
    fetch('/api/config/brains')
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) setConfigs(data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => {
    fetchConfigs();
  }, []);

  const handleSave = () => {
    fetch('/api/config/brains', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editingConfig)
    })
    .then(res => res.json())
    .then(() => {
      fetchConfigs();
      alert('Config saved successfully.');
    });
  };

  const handleChangePassword = () => {
    if (!newPassword || newPassword.length < 4) {
      alert('Password must be at least 4 characters.');
      return;
    }
    fetch('/api/auth/password', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newPassword })
    })
    .then(res => res.json())
    .then(() => {
      setPwdStatus('Password updated successfully.');
      setNewPassword('');
    })
    .catch(err => setPwdStatus('Failed to update password: ' + err.message));
  };

  const handleToggle = (id: number) => {
    fetch(`/api/config/brains/${id}/toggle`, { method: 'POST' })
      .then(() => fetchConfigs());
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this configuration?')) {
      fetch(`/api/config/brains/${id}`, { method: 'DELETE' })
        .then(() => fetchConfigs());
    }
  };

  const geminiModels = [
    { label: 'Gemini 3.1 Pro (Ultra-class)', value: 'gemini-3.1-pro' },
    { label: 'Gemini 3.1 Pro Preview', value: 'gemini-3.1-pro-preview' },
    { label: 'Gemini 3 Flash', value: 'gemini-3-flash' },
    { label: 'Gemini 3.1 Flash-Lite', value: 'gemini-3.1-flash-lite' },
    { label: 'Gemini 3 Deep Think', value: 'gemini-3-deep-think' },
    { label: 'Gemini 1.5 Pro (Legacy)', value: 'gemini-1.5-pro' },
    { label: 'Gemini 1.5 Flash (Legacy)', value: 'gemini-1.5-flash' },
  ];

  const ollamaSuggestions = [
    { label: 'Gemma 4 (8B)', value: 'gemma4:e4b' },
    { label: 'Llama 3.1 (8B)', value: 'llama3.1:8b' },
    { label: 'Mistral (7B)', value: 'mistral' },
  ];

  return (
    <div className="config-view">
      <div className="config-layout">
        <section className="config-form-section">
          <h3>Add / Edit Brain</h3>
          <div className="config-form card">
            <label>Name (Label)
              <input value={editingConfig.name} onChange={e => setEditingConfig({...editingConfig, name: e.target.value})} placeholder="e.g., Local Gemma High-Ctx" />
            </label>
            <label>Provider
              <select value={editingConfig.provider} onChange={e => setEditingConfig({...editingConfig, provider: e.target.value as LlmProvider, modelName: ''})}>
                <option value="OLLAMA">Ollama (Local)</option>
                <option value="GEMINI">Google Gemini (Remote)</option>
              </select>
            </label>
            
            <label>Model Name
              {editingConfig.provider === 'GEMINI' ? (
                <select value={editingConfig.modelName} onChange={e => setEditingConfig({...editingConfig, modelName: e.target.value})}>
                  <option value="">Select a model...</option>
                  {geminiModels.map(m => <option key={m.value} value={m.value}>{m.label}</option>)}
                </select>
              ) : (
                <div style={{display: 'flex', flexDirection: 'column', gap: '0.5rem'}}>
                  <input value={editingConfig.modelName} onChange={e => setEditingConfig({...editingConfig, modelName: e.target.value})} placeholder="e.g., gemma4:e4b" />
                  <div className="suggestions" style={{display: 'flex', gap: '0.5rem', flexWrap: 'wrap'}}>
                    {ollamaSuggestions.map(s => (
                      <button 
                        key={s.value} 
                        className="suggestion-btn" 
                        onClick={() => setEditingConfig({...editingConfig, modelName: s.value})}
                        style={{fontSize: '0.65rem', padding: '0.2rem 0.5rem', background: '#21262d', border: '1px solid var(--border-color)', borderRadius: '4px', cursor: 'pointer', color: 'var(--text-secondary)'}}
                      >
                        {s.label}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </label>
            {editingConfig.provider === 'OLLAMA' && (
              <label>Base URL
                <input value={editingConfig.baseUrl} onChange={e => setEditingConfig({...editingConfig, baseUrl: e.target.value})} placeholder="http://host:11434" />
              </label>
            )}
            {editingConfig.provider === 'GEMINI' && (
              <label>API Key
                <input type="password" value={editingConfig.apiKey} onChange={e => setEditingConfig({...editingConfig, apiKey: e.target.value})} placeholder="AIza..." />
              </label>
            )}
            <label>Context Window
              <input type="number" value={editingConfig.numCtx} onChange={e => setEditingConfig({...editingConfig, numCtx: parseInt(e.target.value)})} />
            </label>
            <button className="save-btn" onClick={handleSave} disabled={loading}><Save size={16}/> Save Configuration</button>
          </div>

          <h3 style={{marginTop: '3rem'}}>Account Security</h3>
          <div className="config-form card">
            <label>Change Administrator Password
              <input 
                type="password" 
                value={newPassword} 
                onChange={e => setNewPassword(e.target.value)} 
                placeholder="New password" 
              />
            </label>
            {pwdStatus && <div className="status-text small" style={{color: '#3fb950', marginBottom: '1rem'}}>{pwdStatus}</div>}
            <button className="save-btn" style={{backgroundColor: '#21262d', border: '1px solid var(--border-color)'}} onClick={handleChangePassword}>
               <Key size={16}/> Update Password
            </button>
          </div>
        </section>

        <section className="config-list-section">
          <h3>Active Brains</h3>
          <div className="brain-list">
            {configs.map(config => (
              <div key={config.id} className={`brain-card ${config.active ? 'active' : ''}`}>
                <div className="brain-info">
                  <BrainCircuit size={24} color={config.provider === 'GEMINI' ? '#58a6ff' : '#8b949e'} />
                  <div>
                    <h4>{config.name}</h4>
                    <code>{config.modelName}</code>
                  </div>
                </div>
                <div className="brain-actions">
                  <button onClick={() => handleToggle(config.id!)} title={config.active ? 'Disable' : 'Enable'}>
                    <Power size={18} color={config.active ? '#3fb950' : '#f85149'} />
                  </button>
                  <button onClick={() => handleDelete(config.id!)} title="Delete">
                    <Trash2 size={18} color="#8b949e" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
};
