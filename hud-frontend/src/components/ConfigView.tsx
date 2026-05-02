import { useState, useEffect } from 'react';
import type { LlmConfig, LlmProvider } from './types';
import { Save, Trash2, Power, BrainCircuit } from 'lucide-react';

export const ConfigView = () => {
  const [configs, setConfigs] = useState<LlmConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingConfig, setEditingConfig] = useState<Partial<LlmConfig>>({
    name: '',
    provider: 'OLLAMA',
    baseUrl: '',
    modelName: '',
    apiKey: '',
    numCtx: 32768,
    active: true
  });

  const fetchConfigs = () => {
    setLoading(true);
    fetch('/api/config/brains')
      .then(res => res.json())
      .then(data => {
        setConfigs(data);
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
              <select value={editingConfig.provider} onChange={e => setEditingConfig({...editingConfig, provider: e.target.value as LlmProvider})}>
                <option value="OLLAMA">Ollama (Local)</option>
                <option value="GEMINI">Google Gemini (Remote)</option>
              </select>
            </label>
            <label>Model Name
              <input value={editingConfig.modelName} onChange={e => setEditingConfig({...editingConfig, modelName: e.target.value})} placeholder="e.g., gemma4:e4b" />
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
