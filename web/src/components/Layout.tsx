import { Outlet, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { OnboardingRoot } from '../onboarding/OnboardingRoot'
import { Sidebar } from './faltet/Sidebar'

export function Layout() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const { t } = useTranslation()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface">
        <div className="animate-spin h-7 w-7 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }

  if (!user) {
    navigate('/login', { replace: true })
    return null
  }

  return (
    <>
      <a href="#main-content" className="skip-link">{t('sidebar.skipToContent')}</a>
      <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--color-cream)' }}>
        <Sidebar />
        <main id="main-content" tabIndex={-1} style={{ flex: 1, minWidth: 0 }}>
          <Outlet />
        </main>
      </div>
      <OnboardingRoot />
    </>
  )
}
