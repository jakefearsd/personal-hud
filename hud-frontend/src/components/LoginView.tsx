import { useState } from 'react';
import { LogIn, ShieldAlert } from 'lucide-react';
import { apiFetch } from '../api';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";

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

    apiFetch('/api/auth/login', {
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
    <Dialog open={true} onOpenChange={(open) => !open && onCancel()}>
      <DialogContent className="sm:max-w-[400px] border-border/50 bg-card/95 backdrop-blur-xl shadow-2xl">
        <DialogHeader className="space-y-3">
          <div className="mx-auto bg-accent/10 p-3 rounded-full w-fit">
            <ShieldAlert className="h-6 w-6 text-accent" />
          </div>
          <DialogTitle className="text-center text-xl font-bold tracking-tight">Administrative Access</DialogTitle>
          <DialogDescription className="text-center text-muted-foreground">
            Enter your credentials to access neural configurations.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleLogin} className="space-y-6 pt-4">
          {error && (
            <div className="bg-destructive/10 border border-destructive/20 text-destructive text-xs p-3 rounded-md font-medium text-center animate-in fade-in zoom-in-95">
              {error}
            </div>
          )}
          
          <div className="space-y-2">
            <Label htmlFor="login-username">Username</Label>
            <Input 
              id="login-username"
              autoFocus
              value={username} 
              onChange={e => setUsername(e.target.value)} 
              placeholder="Admin username"
              className="bg-background/50"
            />
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="login-password">Password</Label>
            <Input 
              id="login-password"
              type="password"
              value={password} 
              onChange={e => setPassword(e.target.value)} 
              placeholder="••••••••"
              className="bg-background/50"
            />
          </div>
          
          <Button type="submit" className="w-full gap-2 font-bold h-11" disabled={loading}>
            {loading ? 'Authenticating...' : <><LogIn className="h-4 w-4"/> Login</>}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
};
