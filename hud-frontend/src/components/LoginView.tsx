import { useState } from 'react';
import { LogIn, X } from 'lucide-react';

interface Props {
  onLoginSuccess: () => void;
  onCancel: () => void;
}

export const LoginView = ({ onLoginSuccess, onCancel }: Props) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const params = new URLSearchParams();
    params.append('username', username);
    params.append('password', password);

    fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params
    })
    .then(async res => {
      if (res.ok) {
        onLoginSuccess();
      } else {
        const data = await res.json();
        throw new Error(data.message || 'Invalid credentials');
      }
    })
    .catch(err => {
      setError(err.message);
      setLoading(false);
    });
  };

  return (
    <div className="login-overlay">
      <div className="login-modal card">
        <div className="modal-header">
          <h3>Administrative Access</h3>
          <button className="close-btn" onClick={onCancel}><X size={20}/></button>
        </div>
        
        <form onSubmit={handleLogin} className="login-form">
          {error && <div className="error-text small">{error}</div>}
          
          <label>Username
            <input 
              autoFocus
              value={username} 
              onChange={e => setUsername(e.target.value)} 
              placeholder="Admin username"
            />
          </label>
          
          <label>Password
            <input 
              type="password"
              value={password} 
              onChange={e => setPassword(e.target.value)} 
              placeholder="••••••••"
            />
          </label>
          
          <button className="save-btn" disabled={loading}>
            {loading ? 'Authenticating...' : <><LogIn size={18}/> Login</>}
          </button>
        </form>
      </div>
    </div>
  );
};
