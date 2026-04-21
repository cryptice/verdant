import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { OnboardingRoot } from '../onboarding/OnboardingRoot'
import { Sidebar } from './faltet/Sidebar'

export function Layout() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()

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
      <div style={{ display: 'flex', minHeight: '100vh', background: 'var(--color-cream)' }}>
        <Sidebar />
        <main style={{ flex: 1, minWidth: 0 }}>
          <Outlet />
        </main>
      </div>
      <OnboardingRoot />
    </>
  )
}
