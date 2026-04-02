import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
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

const features = [
  { icon: '🌱', key: 'gardens' },
  { icon: '🌿', key: 'plants' },
  { icon: '🫘', key: 'seeds' },
  { icon: '📋', key: 'tasks' },
] as const

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

  return (
    <div className="min-h-screen bg-surface overflow-hidden">
      {/* Decorative background elements */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-32 -right-32 w-96 h-96 rounded-full opacity-[0.04]"
          style={{ background: 'radial-gradient(circle, var(--color-accent) 0%, transparent 70%)' }} />
        <div className="absolute top-1/3 -left-48 w-[500px] h-[500px] rounded-full opacity-[0.03]"
          style={{ background: 'radial-gradient(circle, var(--color-accent) 0%, transparent 70%)' }} />
        <div className="absolute -bottom-24 right-1/4 w-80 h-80 rounded-full opacity-[0.04]"
          style={{ background: 'radial-gradient(circle, var(--color-accent) 0%, transparent 70%)' }} />
      </div>

      {/* Nav */}
      <nav className="relative z-10 flex items-center justify-between px-6 md:px-12 py-5 max-w-6xl mx-auto">
        <div className="flex items-center gap-2.5">
          <span className="text-2xl">🌿</span>
          <span className="text-lg font-semibold text-text-primary tracking-tight">Verdant</span>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => { const newLang = i18n.language === 'sv' ? 'en' : 'sv'; i18n.changeLanguage(newLang); localStorage.setItem('verdant-lang', newLang) }}
            className="text-xs font-medium text-text-secondary hover:text-text-primary transition-colors px-2 py-1 rounded-md hover:bg-bg cursor-pointer"
          >
            {i18n.language === 'sv' ? 'EN' : 'SV'}
          </button>
          <a href="#sign-in" className="text-sm font-medium text-accent hover:text-accent-hover transition-colors">
            {t('landing.signIn')}
          </a>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative z-10 max-w-6xl mx-auto px-6 md:px-12 pt-16 md:pt-28 pb-20 md:pb-32">
        <div className="max-w-2xl">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-accent-light text-accent text-xs font-medium mb-8"
            style={{ animation: 'fadeUp 0.6s ease-out both' }}>
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-accent" />
            {t('landing.badge')}
          </div>
          <h1 className="text-4xl md:text-[3.25rem] md:leading-[1.15] font-bold text-text-primary tracking-tight mb-5"
            style={{ animation: 'fadeUp 0.6s ease-out 0.1s both' }}>
            {t('landing.heroTitle')}
          </h1>
          <p className="text-lg md:text-xl text-text-secondary leading-relaxed max-w-lg mb-10"
            style={{ animation: 'fadeUp 0.6s ease-out 0.2s both' }}>
            {t('landing.heroSubtitle')}
          </p>
          <div style={{ animation: 'fadeUp 0.6s ease-out 0.3s both' }}>
            <div id="google-signin-btn" className="mb-3" />
            {error && <p className="text-error text-sm">{error}</p>}
          </div>
        </div>

        {/* Decorative botanical illustration (CSS art) */}
        <div className="hidden lg:block absolute right-12 top-20 w-72 h-80 opacity-[0.08]"
          style={{ animation: 'fadeUp 0.8s ease-out 0.4s both' }}>
          <svg viewBox="0 0 200 280" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-full h-full">
            <path d="M100 280V140" stroke="var(--color-accent)" strokeWidth="2" />
            <path d="M100 140C100 140 60 120 40 80C20 40 50 10 100 40" stroke="var(--color-accent)" strokeWidth="1.5" fill="var(--color-accent)" fillOpacity="0.15" />
            <path d="M100 140C100 140 140 110 150 60C160 10 130 -10 100 30" stroke="var(--color-accent)" strokeWidth="1.5" fill="var(--color-accent)" fillOpacity="0.1" />
            <path d="M100 200C100 200 70 180 55 150C40 120 60 100 100 120" stroke="var(--color-accent)" strokeWidth="1.5" fill="var(--color-accent)" fillOpacity="0.12" />
            <path d="M100 200C100 200 135 185 145 155C155 125 135 110 100 125" stroke="var(--color-accent)" strokeWidth="1.5" fill="var(--color-accent)" fillOpacity="0.08" />
            <circle cx="100" cy="25" r="6" fill="var(--color-accent)" fillOpacity="0.2" />
            <circle cx="45" cy="70" r="4" fill="var(--color-accent)" fillOpacity="0.15" />
            <circle cx="150" cy="50" r="5" fill="var(--color-accent)" fillOpacity="0.15" />
          </svg>
        </div>
      </section>

      {/* Features */}
      <section className="relative z-10 max-w-6xl mx-auto px-6 md:px-12 pb-20 md:pb-32">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-5">
          {features.map((f, i) => (
            <div
              key={f.key}
              className="group bg-bg border border-divider rounded-xl p-6 hover:border-accent/30 hover:shadow-sm transition-all duration-300"
              style={{ animation: `fadeUp 0.5s ease-out ${0.4 + i * 0.08}s both` }}
            >
              <div className="text-3xl mb-4 group-hover:scale-110 transition-transform duration-300 inline-block">
                {f.icon}
              </div>
              <h3 className="font-semibold text-text-primary mb-1.5">
                {t(`landing.feature.${f.key}.title`)}
              </h3>
              <p className="text-sm text-text-secondary leading-relaxed">
                {t(`landing.feature.${f.key}.description`)}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section className="relative z-10 max-w-6xl mx-auto px-6 md:px-12 pb-20 md:pb-32">
        <div className="bg-bg border border-divider rounded-2xl p-8 md:p-12">
          <h2 className="text-2xl font-bold text-text-primary tracking-tight mb-8">
            {t('landing.howItWorks')}
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 md:gap-12">
            {[1, 2, 3].map(step => (
              <div key={step} className="flex gap-4">
                <div className="shrink-0 w-8 h-8 rounded-full bg-accent-light text-accent text-sm font-bold flex items-center justify-center">
                  {step}
                </div>
                <div>
                  <h3 className="font-semibold text-text-primary mb-1">
                    {t(`landing.step${step}.title`)}
                  </h3>
                  <p className="text-sm text-text-secondary leading-relaxed">
                    {t(`landing.step${step}.description`)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section id="sign-in" className="relative z-10 max-w-6xl mx-auto px-6 md:px-12 pb-24 md:pb-36">
        <div className="text-center max-w-md mx-auto">
          <span className="text-4xl block mb-4">🌻</span>
          <h2 className="text-2xl font-bold text-text-primary tracking-tight mb-3">
            {t('landing.ctaTitle')}
          </h2>
          <p className="text-text-secondary mb-8">
            {t('landing.ctaSubtitle')}
          </p>
          <div className="inline-block">
            <div id="google-signin-btn-bottom" className="mb-3" />
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="relative z-10 border-t border-divider">
        <div className="max-w-6xl mx-auto px-6 md:px-12 py-6 flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm text-text-secondary">
            <span>🌿</span>
            <span>Verdant</span>
          </div>
          <p className="text-xs text-text-muted">{t('landing.footer')}</p>
        </div>
      </footer>

      <style>{`
        @keyframes fadeUp {
          from { opacity: 0; transform: translateY(16px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  )
}
