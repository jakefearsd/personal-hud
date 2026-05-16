import { useState } from 'react'

interface Props {
  onChanged: () => void
}

export function ChangePasswordView({ onChanged }: Props) {
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    if (password !== confirm) {
      setError('Passwords do not match')
      return
    }
    fetch('/api/auth/password', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newPassword: password }),
    })
      .then(res => {
        if (res.ok) {
          onChanged()
        } else {
          res.json().then(d => setError(d.message || 'Password rejected'))
        }
      })
      .catch(err => setError(err.message))
  }

  return (
    <div className="change-password-view">
      <h2>Set a New Password</h2>
      <p>Your account requires a password change before you can continue.</p>
      <form onSubmit={submit}>
        <input
          type="password"
          placeholder="New password (min 12 chars, letter + digit)"
          value={password}
          onChange={e => setPassword(e.target.value)}
        />
        <input
          type="password"
          placeholder="Confirm password"
          value={confirm}
          onChange={e => setConfirm(e.target.value)}
        />
        {error && <div className="error-banner">{error}</div>}
        <button type="submit">Update Password</button>
      </form>
    </div>
  )
}
