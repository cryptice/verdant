import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { api } from '../api/client'

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: Record<string, unknown>) => void
          renderButton: (el: HTMLElement, config: Record<string, unknown>) => void
        }
      }
    }
  }
}

export function LoginPage() {
  const { login, token } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (token) navigate('/', { replace: true })
  }, [token, navigate])

  const handleCredentialResponse = useCallback(async (response: { credential: string }) => {
    setError(null)
    try {
      const res = await api.auth.google(response.credential)
      login(res.token, res.user)
      navigate('/')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Sign-in failed')
    }
  }, [login, navigate])

  useEffect(() => {
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.onload = () => {
      window.google?.accounts.id.initialize({
        client_id: (import.meta as { env?: Record<string, string> }).env?.['VITE_GOOGLE_CLIENT_ID'] ?? '',
        callback: handleCredentialResponse,
      })
      const el = document.getElementById('google-signin-btn')
      if (el) {
        window.google?.accounts.id.renderButton(el, {
          theme: 'outline',
          size: 'large',
          width: 300,
          text: 'signin_with',
        })
      }
    }
    document.head.appendChild(script)
    return () => { document.head.removeChild(script) }
  }, [handleCredentialResponse])

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-cream px-4">
      <div className="card max-w-sm w-full flex flex-col items-center gap-6 p-8">
        <h1 className="text-3xl font-bold text-green-dark">Verdant</h1>
        <p className="text-text-secondary text-center">Sign in to manage your garden</p>
        <div id="google-signin-btn" />
        {error && <p className="text-error text-sm">{error}</p>}
      </div>
    </div>
  )
}
