import { useState, useEffect, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { api } from '../api/client'
import { Rule } from '../components/faltet'

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

const featureKeys = ['gardens', 'plants', 'seeds', 'tasks'] as const

export function LandingPage() {
  const { login, token } = useAuth()
  const navigate = useNavigate()
  const { t, i18n } = useTranslation()
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
        client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '',
        callback: handleCredentialResponse,
      })
      for (const id of ['google-signin-btn', 'google-signin-btn-bottom']) {
        const el = document.getElementById(id)
        if (el) {
          window.google?.accounts.id.renderButton(el, {
            theme: 'outline',
            size: 'large',
            width: 300,
            text: 'signin_with',
            locale: i18n.language,
          })
        }
      }
    }
    document.head.appendChild(script)
    return () => {
      if (script.parentNode) {
        document.head.removeChild(script)
      }
    }
  }, [handleCredentialResponse, i18n.language])

  const headline = t('landing.hero.headline')
  const headlineParts = headline.split('.')

  return (
    <div style={{ minHeight: '100vh', background: 'var(--color-cream)' }}>
      {/* Top strip */}
      <div
        style={{
          display: 'flex',
          alignItems: 'baseline',
          justifyContent: 'space-between',
          padding: '22px 40px',
          borderBottom: '1px solid var(--color-ink)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
          <span
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 32,
              fontWeight: 300,
              color: 'var(--color-ink)',
            }}
          >
            Verdant<span style={{ color: 'var(--color-accent)' }}>.</span>
          </span>
          <span
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
            }}
          >
            {t('app.subtitle')}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <button
            onClick={() => {
              const newLang = i18n.language === 'sv' ? 'en' : 'sv'
              i18n.changeLanguage(newLang)
              localStorage.setItem('verdant-lang', newLang)
            }}
            style={{
              background: 'transparent',
              border: 'none',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              cursor: 'pointer',
            }}
          >
            {i18n.language === 'sv' ? 'EN' : 'SV'}
          </button>
          <div id="google-signin-btn" />
        </div>
      </div>

      {/* Hero */}
      <div style={{ maxWidth: 860, margin: '80px auto 60px', padding: '0 40px' }}>
        <h1
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: 80,
            fontWeight: 300,
            letterSpacing: -1.5,
            lineHeight: 1,
            margin: 0,
            fontVariationSettings: '"SOFT" 100, "opsz" 144',
          }}
        >
          {headlineParts[0]}
          <span style={{ color: 'var(--color-accent)' }}>.</span>
          {headlineParts.length > 1 && headlineParts[1]?.trim() && (
            <>
              <br />
              <span style={{ fontStyle: 'italic' }}>{headlineParts[1].trim()}</span>
              <span style={{ color: 'var(--color-accent)' }}>.</span>
            </>
          )}
        </h1>
        <p
          style={{
            fontFamily: 'Georgia, var(--font-display)',
            fontSize: 18,
            lineHeight: 1.6,
            color: 'var(--color-forest)',
            marginTop: 28,
          }}
        >
          {t('landing.hero.sub')}
        </p>
      </div>

      <Rule variant="ink" />

      {/* Features — 4-up grid */}
      <div style={{ maxWidth: 1100, margin: '60px auto', padding: '0 40px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 28 }}>
          {featureKeys.map((key, i) => (
            <div key={key}>
              <div
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 9,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-forest)',
                  opacity: 0.7,
                }}
              >
                {String.fromCharCode(8470)} {String(i + 1).padStart(2, '0')}
              </div>
              <div
                style={{
                  fontFamily: 'var(--font-display)',
                  fontSize: 22,
                  fontWeight: 300,
                  marginTop: 6,
                }}
              >
                {t(`landing.features.${key}.title`)}
              </div>
              <p
                style={{
                  fontFamily: 'Georgia, var(--font-display)',
                  fontSize: 15,
                  lineHeight: 1.6,
                  color: 'var(--color-forest)',
                  marginTop: 8,
                }}
              >
                {t(`landing.features.${key}.description`)}
              </p>
            </div>
          ))}
        </div>
      </div>

      {error && (
        <p
          style={{
            color: 'var(--color-accent)',
            textAlign: 'center',
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            padding: '0 40px',
          }}
        >
          {error}
        </p>
      )}

      {/* Bottom sign-in */}
      <div style={{ maxWidth: 860, margin: '60px auto', padding: '0 40px', textAlign: 'center' }}>
        <div id="google-signin-btn-bottom" style={{ display: 'inline-block' }} />
      </div>

      {/* Footer */}
      <div style={{ borderTop: '1px solid var(--color-ink)', padding: '20px 40px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 14,
            color: 'var(--color-forest)',
          }}
        >
          Verdant<span style={{ color: 'var(--color-accent)' }}>.</span>
        </span>
        <div style={{ display: 'flex', gap: 24, alignItems: 'center' }}>
          <span
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              opacity: 0.6,
            }}
          >
            {t('landing.footer')}
          </span>
          <Link
            to="/privacy"
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              opacity: 0.6,
              textDecoration: 'none',
            }}
          >
            {t('privacy.title')}
          </Link>
        </div>
      </div>
    </div>
  )
}
