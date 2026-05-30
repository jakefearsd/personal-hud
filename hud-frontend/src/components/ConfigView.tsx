import { useState, useEffect } from 'react';
import type { LlmConfig, LlmProvider } from './types';
import { Save, Trash2, Power, BrainCircuit, Key, Play, Pencil, PlusCircle, ShieldCheck } from 'lucide-react';
import { SchedulingConfig } from './SchedulingConfig';
import { apiFetch } from '../api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

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
    <div className="flex flex-col gap-12 w-full">
      <div className="config-layout">
        <section className="flex flex-col gap-8">
          <div>
            <h3 className="text-lg font-semibold tracking-tight mb-4">Neural Parameters</h3>
            <LlmConfigForm 
              editingConfig={editingConfig} 
              setEditingConfig={setEditingConfig} 
              onSave={handleSave} 
              onReset={handleReset}
              loading={loading}
            />
          </div>
          <SchedulingConfig />
          <SecuritySettings />
        </section>

        <section>
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
    { label: 'Gemini 3.5 Flash', value: 'gemini-3.5-flash' },
    { label: 'Gemini 3.1 Pro', value: 'gemini-3.1-pro' },
    { label: 'Gemini 3.1 Flash-Lite', value: 'gemini-3.1-flash-lite' },
  ];

  const deepSeekModels = [
    { label: 'DeepSeek Chat (V3)', value: 'deepseek-chat' },
    { label: 'DeepSeek Reasoner (R1)', value: 'deepseek-reasoner' },
  ];

  const ollamaSuggestions = [
    { label: 'Gemma 4 (8B)', value: 'gemma4:e4b' },
    { label: 'Llama 3.1 (8B)', value: 'llama3.1:8b' },
    { label: 'Mistral (7B)', value: 'mistral' },
  ];

  return (
    <Card className="border-border/60 bg-card/60 shadow-lg">
      <CardHeader className="flex flex-row items-center justify-between border-b border-border/40 mb-4 pb-4">
        <div>
          <CardTitle className="text-xl font-bold text-white">{editingConfig.id ? 'Edit Brain' : 'Integrate New Brain'}</CardTitle>
          <CardDescription className="text-foreground/90 font-medium">Configure neural parameters and inference endpoints.</CardDescription>
        </div>
        {editingConfig.id && (
          <Button variant="outline" size="sm" onClick={onReset} className="text-xs h-8 border-accent text-accent font-bold hover:bg-accent/10">
            <PlusCircle className="mr-2 h-3.5 w-3.5" /> Create New
          </Button>
        )}
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid gap-2">
          <Label htmlFor="brain-display-label" className="text-white font-bold text-[11px] uppercase tracking-wider">Brain Display Label</Label>
          <Input 
            id="brain-display-label"
            value={editingConfig.name || ''} 
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingConfig({...editingConfig, name: e.target.value})} 
            placeholder="e.g., Tactical Analysis (Fast)" 
            className="bg-background/80 border-border/80 text-white font-medium"
          />
        </div>

        <div className="grid gap-2">
          <Label htmlFor="brain-provider-type" className="text-white font-bold text-[11px] uppercase tracking-wider">Intelligence Provider</Label>
          <Select 
            value={editingConfig.provider} 
            onValueChange={(v: string) => setEditingConfig({...editingConfig, provider: v as LlmProvider, modelName: ''})}
          >
            <SelectTrigger id="brain-provider-type" className="bg-background/80 border-border/80 text-white">
              <SelectValue placeholder="Select Provider" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="OLLAMA">Ollama (Local)</SelectItem>
              <SelectItem value="GEMINI">Google Gemini (Remote)</SelectItem>
              <SelectItem value="DEEPSEEK">DeepSeek (Remote)</SelectItem>
            </SelectContent>
          </Select>
        </div>
        
        <div className="grid gap-2">
          <Label htmlFor="brain-model-identifier" className="text-white font-bold text-[11px] uppercase tracking-wider">Model Identifier</Label>
          {editingConfig.provider === 'GEMINI' || editingConfig.provider === 'DEEPSEEK' ? (
            <Select 
              value={editingConfig.modelName || ''} 
              onValueChange={(v: string) => setEditingConfig({...editingConfig, modelName: v})}
            >
              <SelectTrigger id="brain-model-identifier" className="bg-background/80 border-border/80 text-white">
                <SelectValue placeholder="Select a model..." />
              </SelectTrigger>
              <SelectContent>
                {(editingConfig.provider === 'GEMINI' ? geminiModels : deepSeekModels).map(m => (
                  <SelectItem key={m.value} value={m.value}>{m.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          ) : (
            <div className="space-y-3">
              <Input 
                id="brain-model-identifier"
                value={editingConfig.modelName || ''} 
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingConfig({...editingConfig, modelName: e.target.value})} 
                placeholder="e.g., gemma4:e4b" 
                className="bg-background/80 border-border/80 text-white"
              />
              <div className="flex flex-wrap gap-2">
                {ollamaSuggestions.map(s => (
                  <Button 
                    key={s.value} 
                    variant="outline" 
                    size="sm" 
                    className="text-[10px] h-6 px-2 text-white bg-accent/20 border-accent/40 font-bold hover:bg-accent/30"
                    onClick={() => setEditingConfig({...editingConfig, modelName: s.value})}
                  >
                    {s.label}
                  </Button>
                ))}
              </div>
            </div>
          )}
        </div>

        {editingConfig.provider === 'OLLAMA' && (
          <div className="grid gap-2 animate-in fade-in slide-in-from-top-1">
            <Label htmlFor="brain-base-url">Inference Base URL</Label>
            <Input 
              id="brain-base-url"
              value={editingConfig.baseUrl || ''} 
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingConfig({...editingConfig, baseUrl: e.target.value})} 
              placeholder="http://host:11434" 
            />
          </div>
        )}

        {(editingConfig.provider === 'GEMINI' || editingConfig.provider === 'DEEPSEEK') && (
          <div className="grid gap-2 animate-in fade-in slide-in-from-top-1">
            <Label htmlFor="brain-api-secret">Provider API Secret</Label>
            <Input 
              id="brain-api-secret"
              type="password" 
              value={editingConfig.apiKey || ''} 
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingConfig({...editingConfig, apiKey: e.target.value})} 
              placeholder="Enter API Key" 
            />
          </div>
        )}

        <div className="grid gap-2">
          <Label htmlFor="brain-context-size">Token Context Limit</Label>
          <Input 
            id="brain-context-size"
            type="number" 
            value={editingConfig.numCtx || 0} 
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditingConfig({...editingConfig, numCtx: parseInt(e.target.value)})} 
          />
        </div>

        <Button className="w-full gap-2" onClick={onSave} disabled={loading}>
          <Save className="h-4 w-4" /> {editingConfig.id ? 'Update Neural Configuration' : 'Establish Primary Brain'}
        </Button>
      </CardContent>
    </Card>
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
      setPwdStatus('Identity credentials updated.');
      setNewPassword('');
    })
    .catch(err => setPwdStatus('Credential update failed: ' + err.message));
  };

  return (
    <Card className="border-border/50 bg-card/50">
      <CardHeader>
        <CardTitle className="text-xl flex items-center gap-2">
          <ShieldCheck className="h-5 w-5 text-accent" /> Security Protocol
        </CardTitle>
        <CardDescription>Rotate administrative access credentials.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-2">
          <Label htmlFor="admin-new-password">New Master Password</Label>
          <Input 
            id="admin-new-password"
            type="password" 
            value={newPassword} 
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setNewPassword(e.target.value)} 
            placeholder="Min 12 characters" 
          />
        </div>
        {pwdStatus && <p className="text-xs text-success font-medium italic">{pwdStatus}</p>}
        <Button variant="secondary" className="w-full gap-2" onClick={handleChangePassword}>
            <Key className="h-4 w-4" /> Rotate Access Key
        </Button>
      </CardContent>
    </Card>
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
    <div className="space-y-6">
      <h3 className="text-lg font-bold tracking-tight text-white uppercase tracking-wider">Active Neural Cluster</h3>
      <div className="flex flex-col gap-4">
        {configs.map(config => (
          <Card key={config.id} className={`transition-all border-border/60 ${config.active ? 'bg-success/10 border-success/30 shadow-lg shadow-success/5' : 'opacity-70 bg-card/40 grayscale-[0.3]'} ${editingConfigId === config.id ? 'ring-2 ring-accent border-accent' : ''}`}>
            <CardContent className="p-4 flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className={`p-2 rounded-lg ${config.active ? 'bg-accent/20 text-[#79c0ff]' : 'bg-muted text-foreground'}`}>
                  <BrainCircuit className="h-6 w-6" />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-white tracking-tight">{config.name}</h4>
                  <code className="text-[10px] text-[#79c0ff] bg-black/40 px-1.5 py-0.5 rounded uppercase font-bold tracking-widest border border-accent/20">{config.modelName}</code>
                </div>
              </div>
              <div className="flex items-center gap-1">
                <Button variant="ghost" size="icon" className="h-8 w-8 text-success" onClick={() => onRun(config.id!)} title="Execute Run" disabled={!config.active}>
                  <Play className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" className="h-8 w-8 text-accent" onClick={() => onEdit(config)} title="Modify Config">
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => onToggle(config.id!)} title={config.active ? 'Disable' : 'Enable'}>
                  <Power className={`h-4 w-4 ${config.active ? 'text-success' : 'text-destructive'}`} />
                </Button>
                <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground" onClick={() => onDelete(config.id!)} title="Purge">
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
        {configs.length === 0 && (
          <div className="text-center py-12 border-2 border-dashed border-border/50 rounded-xl">
            <p className="text-muted-foreground text-sm">No neural nodes detected.</p>
          </div>
        )}
      </div>
    </div>
  );
};
