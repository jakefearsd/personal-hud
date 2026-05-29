import { useState, useEffect } from 'react';
import type { LlmConfig, LlmProvider } from './types';
import { Save, Trash2, Power, BrainCircuit, Key, Play, Pencil, PlusCircle } from 'lucide-react';
import { SchedulingConfig } from './SchedulingConfig';
import { apiFetch } from '../api';

export const ConfigView = () => {
  const [configs, setConfigs] = useState<LlmConfig[]>([]);
  const [loading, setLoading] = useState(true);

  const initialFormState: Partial<LlmConfig> = {
    name: '',
    provider: 'OLLAMA',
    baseUrl: '',
    modelName: '',
    apiKey: '',
    numCtx: 131072,
    active: true
  };

  const [editingConfig, setEditingConfig] = useState<Partial<LlmConfig>>(initialFormState);

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
    apiFetch('/api/config/brains', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editingConfig)
    })
    .then(res => res.json())
    .then(() => {
      fetchConfigs();
      setEditingConfig(initialFormState);
      alert('Brain configuration saved successfully.');
    });
  };

  const handleEdit = (config: LlmConfig) => {
    setEditingConfig({ ...config });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleReset = () => {
    setEditingConfig(initialFormState);
  };

  const handleToggle = (id: number) => {
    apiFetch(`/api/config/brains/${id}/toggle`, { method: 'POST' })
      .then(() => fetchConfigs());
  };

  const handleRunModel = (id: number) => {
    apiFetch(`/api/config/brains/${id}/run`, { method: 'POST' })
      .then(() => alert('Model-specific run triggered. Check Observability tab for progress.'));
  };

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this configuration?')) {
      apiFetch(`/api/config/brains/${id}`, { method: 'DELETE' })
        .then(() => fetchConfigs());
    }
  };

  return (
    <div className="config-view">
      <div className="config-layout">
        <section className="config-form-section">
          <LlmConfigForm 
            editingConfig={editingConfig} 
            setEditingConfig={setEditingConfig} 
            onSave={handleSave} 
            onReset={handleReset}
            loading={loading}
          />
          <SchedulingConfig />
          <SecuritySettings />
        </section>

        <section className="config-list-section">
          <ActiveBrainsList 
            configs={configs} 
            editingConfigId={editingConfig.id} 
            onEdit={handleEdit} 
            onToggle={handleToggle} 
            onDelete={handleDelete} 
            onRun={handleRunModel} 
          />
        </section>
      </div>
    </div>
  );
};

/* --- Sub-Components --- */

interface LlmConfigFormProps {
  editingConfig: Partial<LlmConfig>;
  setEditingConfig: (config: Partial<LlmConfig>) => void;
  onSave: () => void;
  onReset: () => void;
  loading: boolean;
}

const LlmConfigForm = ({ editingConfig, setEditingConfig, onSave, onReset, loading }: LlmConfigFormProps) => {
const geminiModels = [
  { label: 'Gemini 3.5 Flash (New / Fastest)', value: 'gemini-3.5-flash' },
  { label: 'Gemini 3.1 Pro (Stable / Frontier)', value: 'gemini-3.1-pro' },
  { label: 'Gemini 3.1 Flash-Lite (Stable / Efficiency)', value: 'gemini-3.1-flash-lite' },
];
  const ollamaSuggestions = [
    { label: 'Gemma 4 (8B)', value: 'gemma4:e4b' },
    { label: 'Llama 3.1 (8B)', value: 'llama3.1:8b' },
    { label: 'Mistral (7B)', value: 'mistral' },
  ];

  return (
    <>
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
        <h3>{editingConfig.id ? 'Edit Brain' : 'Add New Brain'}</h3>
        {editingConfig.id && (
          <button 
            onClick={onReset} 
            style={{fontSize: '0.75rem', background: 'transparent', border: '1px solid var(--border-color)', padding: '0.25rem 0.5rem', borderRadius: '4px', cursor: 'pointer'}}
          >
            <PlusCircle size={12} style={{marginRight: '4px'}}/> Create New Instead
          </button>
        )}
      </div>
      <div className="config-form card">
        <label htmlFor="brain-display-label">Brain Display Label
          <input 
            id="brain-display-label"
            name="brain-display-label"
            value={editingConfig.name} 
            onChange={e => setEditingConfig({...editingConfig, name: e.target.value})} 
            placeholder="e.g., Local Gemma High-Ctx" 
            autoComplete="off"
          />
        </label>
        <label htmlFor="brain-provider-type">Intelligence Provider
          <select 
            id="brain-provider-type"
            name="brain-provider-type"
            value={editingConfig.provider} 
            onChange={e => setEditingConfig({...editingConfig, provider: e.target.value as LlmProvider, modelName: ''})}
          >
            <option value="OLLAMA">Ollama (Local)</option>
            <option value="GEMINI">Google Gemini (Remote)</option>
          </select>
        </label>
        
        <label htmlFor="brain-model-identifier">Model Identifier
          {editingConfig.provider === 'GEMINI' ? (
            <select 
              id="brain-model-identifier"
              name="brain-model-identifier"
              value={editingConfig.modelName} 
              onChange={e => setEditingConfig({...editingConfig, modelName: e.target.value})}
            >
              <option value="">Select a model...</option>
              {geminiModels.map(m => <option key={m.value} value={m.value}>{m.label}</option>)}
            </select>
          ) : (
            <div style={{display: 'flex', flexDirection: 'column', gap: '0.5rem'}}>
              <input 
                id="brain-model-identifier"
                name="brain-model-identifier"
                value={editingConfig.modelName} 
                onChange={e => setEditingConfig({...editingConfig, modelName: e.target.value})} 
                placeholder="e.g., gemma4:e4b" 
                autoComplete="off"
              />
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
          <label htmlFor="brain-base-url">Inference Base URL
            <input 
              id="brain-base-url"
              name="brain-base-url"
              value={editingConfig.baseUrl} 
              onChange={e => setEditingConfig({...editingConfig, baseUrl: e.target.value})} 
              placeholder="http://host:11434" 
              autoComplete="off"
            />
          </label>
        )}
        {editingConfig.provider === 'GEMINI' && (
          <label htmlFor="brain-api-secret">Provider API Secret
            <input 
              id="brain-api-secret"
              name="brain-api-secret"
              type="password" 
              value={editingConfig.apiKey} 
              onChange={e => setEditingConfig({...editingConfig, apiKey: e.target.value})} 
              placeholder="AIza..." 
              autoComplete="new-password"
            />
          </label>
        )}
        <label htmlFor="brain-context-size">Token Context Limit
          <input 
            id="brain-context-size"
            name="brain-context-size"
            type="number" 
            value={editingConfig.numCtx} 
            onChange={e => setEditingConfig({...editingConfig, numCtx: parseInt(e.target.value)})} 
          />
        </label>
        <button className="save-btn" onClick={onSave} disabled={loading}>
          <Save size={16}/> {editingConfig.id ? 'Update Brain' : 'Save New Brain'}
        </button>
      </div>
    </>
  );
};

const SecuritySettings = () => {
  const [newPassword, setNewPassword] = useState('');
  const [pwdStatus, setPwdStatus] = useState<string | null>(null);

  const handleChangePassword = () => {
    if (!newPassword || newPassword.length < 12) {
      alert('Password must be at least 12 characters.');
      return;
    }
    apiFetch('/api/auth/password', {
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

  return (
    <>
      <h3 style={{marginTop: '3rem'}}>Account Security</h3>
      <div className="config-form card">
        <label htmlFor="admin-new-password">Change Administrator Password
          <input 
            id="admin-new-password"
            name="admin-new-password"
            type="password" 
            value={newPassword} 
            onChange={e => setNewPassword(e.target.value)} 
            placeholder="New password" 
            autoComplete="new-password"
          />
        </label>
        {pwdStatus && <div className="status-text small" style={{color: '#3fb950', marginBottom: '1rem'}}>{pwdStatus}</div>}
        <button className="save-btn" style={{backgroundColor: '#21262d', border: '1px solid var(--border-color)'}} onClick={handleChangePassword}>
            <Key size={16}/> Update Password
        </button>
      </div>
    </>
  );
};

interface ActiveBrainsListProps {
  configs: LlmConfig[];
  editingConfigId?: number;
  onEdit: (config: LlmConfig) => void;
  onToggle: (id: number) => void;
  onDelete: (id: number) => void;
  onRun: (id: number) => void;
}

const ActiveBrainsList = ({ configs, editingConfigId, onEdit, onToggle, onDelete, onRun }: ActiveBrainsListProps) => {
  return (
    <>
      <h3>Active Brains</h3>
      <div className="brain-list">
        {configs.map(config => (
          <div key={config.id} className={`brain-card ${config.active ? 'active' : ''} ${editingConfigId === config.id ? 'editing' : ''}`}>
            <div className="brain-info">
              <BrainCircuit size={24} color={config.provider === 'GEMINI' ? '#58a6ff' : '#8b949e'} />
              <div>
                <h4>{config.name}</h4>
                <code>{config.modelName}</code>
              </div>
            </div>
            <div className="brain-actions">
              <button onClick={() => onRun(config.id!)} title="Run This Model Now" disabled={!config.active}>
                <Play size={18} color={config.active ? '#3fb950' : '#8b949e'} />
              </button>
              <button onClick={() => onEdit(config)} title="Edit Configuration">
                <Pencil size={18} color={editingConfigId === config.id ? '#58a6ff' : '#8b949e'} />
              </button>
              <button onClick={() => onToggle(config.id!)} title={config.active ? 'Disable' : 'Enable'}>
                <Power size={18} color={config.active ? '#3fb950' : '#f85149'} />
              </button>
              <button onClick={() => onDelete(config.id!)} title="Delete">
                <Trash2 size={18} color="#8b949e" />
              </button>
            </div>
          </div>
        ))}
      </div>
    </>
  );
};
