import { useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { OnboardingRoot } from '../onboarding/OnboardingRoot'

function SidebarContent({ onClose }: { onClose?: () => void }) {
  const { user } = useAuth()
  const { t, i18n } = useTranslation()

  const allNavItems = [
    { to: '/', label: t('nav.myWorld'), icon: '🌍' },
    { to: '/seasons', label: t('nav.seasons'), icon: '📅' },
    { to: '/species', label: t('nav.species'), icon: '🌿' },
    { to: '/plants', label: t('nav.plants'), icon: '🌱' },
    { to: '/tasks', label: t('nav.tasks'), icon: '📋' },
    { to: '/seeds', label: t('nav.seeds'), icon: '🫘' },
    { to: '/successions', label: t('nav.successions'), icon: '🔄', advanced: true },
    { to: '/targets', label: t('nav.targets'), icon: '🎯', advanced: true },
    { to: '/calendar', label: t('nav.calendar'), icon: '📊', advanced: true },
    { to: '/customers', label: t('nav.customers'), icon: '👥' },
    { to: '/bouquets', label: t('nav.bouquets'), icon: '💐', advanced: true },
    { to: '/trials', label: t('nav.trials'), icon: '🔬', advanced: true },
    { to: '/pest-disease', label: t('nav.pestDisease'), icon: '🐛', advanced: true },
    { to: '/analytics', label: t('nav.analytics'), icon: '📈', advanced: true },
    { to: '/market/listings', label: t('nav.myListings'), icon: '📦', advanced: true },
    { to: '/market/incoming', label: t('nav.incomingOrders'), icon: '📥', advanced: true },
    { to: '/guide', label: t('nav.guide'), icon: '📖' },
    { to: '/account', label: t('nav.account'), icon: '👤' },
  ]

  const navItems = allNavItems.filter(item => !item.advanced || user?.advancedMode)

  const changeLang = (lang: string) => {
    localStorage.setItem('verdant-lang', lang)
    i18n.changeLanguage(lang)
  }

  return (
    <div className="flex flex-col h-full bg-sidebar border-r border-divider">
      <div className="px-4 py-5">
        <span className="font-semibold text-accent text-base tracking-tight">🌿 Verdant</span>
      </div>
      <nav className="flex-1 py-1 px-2 overflow-y-auto">
        {navItems.map(({ to, label, icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            onClick={onClose}
            className={({ isActive }) =>
              `flex items-center gap-2.5 px-3 py-2 my-0.5 rounded-xl text-sm transition-all duration-150 ${
                isActive
                  ? 'bg-accent-light text-accent font-medium shadow-sm'
                  : 'text-text-secondary hover:bg-white/60 hover:text-text-primary'
              }`
            }
          >
            <span className="text-base w-5 text-center leading-none">{icon}</span>
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="px-4 py-3 border-t border-divider/50 space-y-2">
        <p className="text-xs text-text-muted truncate">{user?.displayName}</p>
        <div className="flex gap-1">
          {(['sv', 'en'] as const).map(lang => (
            <button
              key={lang}
              onClick={() => changeLang(lang)}
              className={`text-xs px-2.5 py-1 rounded-lg transition-all duration-150 ${
                i18n.language === lang
                  ? 'bg-accent-light text-accent font-medium'
                  : 'text-text-muted hover:text-text-secondary hover:bg-white/60'
              }`}
            >
              {lang.toUpperCase()}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

export function Layout() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const [drawerOpen, setDrawerOpen] = useState(false)

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
      <div className="min-h-screen flex bg-surface">
        {/* Desktop sidebar */}
        <div className="hidden md:flex md:flex-col md:w-56 md:fixed md:inset-y-0 md:left-0 z-20">
          <SidebarContent />
        </div>

        {/* Mobile drawer overlay */}
        {drawerOpen && (
          <div className="fixed inset-0 z-30 md:hidden">
            <div className="absolute inset-0 bg-black/20" onClick={() => setDrawerOpen(false)} />
            <div className="absolute left-0 top-0 bottom-0 w-56">
              <SidebarContent onClose={() => setDrawerOpen(false)} />
            </div>
          </div>
        )}

        {/* Main content */}
        <div className="flex-1 md:ml-56 flex flex-col min-h-screen">
          {/* Mobile top bar */}
          <header className="md:hidden flex items-center gap-3 px-4 py-3 bg-sidebar border-b border-divider sticky top-0 z-10">
            <button
              onClick={() => setDrawerOpen(true)}
              className="text-text-secondary hover:text-text-primary transition-colors p-0.5"
              aria-label="Open menu"
            >
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
                <line x1="2" y1="4.5" x2="16" y2="4.5" />
                <line x1="2" y1="9" x2="16" y2="9" />
                <line x1="2" y1="13.5" x2="16" y2="13.5" />
              </svg>
            </button>
            <span className="font-semibold text-text-primary text-sm">🌿 Verdant</span>
          </header>

          <main className="flex-1 w-full px-4 md:px-8 py-6 flex flex-col">
            <Outlet />
          </main>
        </div>
      </div>
      <OnboardingRoot />
    </>
  )
}
