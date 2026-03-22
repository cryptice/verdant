import { useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'

export function Layout() {
  const { user, loading, logout } = useAuth()
  const navigate = useNavigate()
  const { t, i18n } = useTranslation()
  const [menuOpen, setMenuOpen] = useState(false)

  const changeLang = (lang: string) => {
    localStorage.setItem('verdant-lang', lang)
    i18n.changeLanguage(lang)
  }

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

  const navLinks = [
    { to: '/', label: t('nav.browse'), end: true },
    { to: '/orders', label: t('nav.myOrders'), end: false },
  ]

  return (
    <div className="min-h-screen flex flex-col bg-surface">
      {/* Navbar */}
      <header className="sticky top-0 z-20 bg-bg border-b border-divider">
        <div className="max-w-5xl mx-auto flex items-center justify-between px-4 h-14">
          {/* Logo */}
          <NavLink to="/" className="flex items-center gap-2 font-semibold text-accent text-base tracking-tight">
            <span className="text-lg">🌸</span>
            Verdant Market
          </NavLink>

          {/* Desktop nav */}
          <nav className="hidden sm:flex items-center gap-1">
            {navLinks.map(({ to, label, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                className={({ isActive }) =>
                  `px-3 py-1.5 rounded-lg text-sm transition-all duration-150 ${
                    isActive
                      ? 'bg-accent-light text-accent font-medium'
                      : 'text-text-secondary hover:text-text-primary hover:bg-warm'
                  }`
                }
              >
                {label}
              </NavLink>
            ))}
          </nav>

          {/* Right side: lang + user */}
          <div className="hidden sm:flex items-center gap-3">
            <div className="flex gap-0.5">
              {(['sv', 'en'] as const).map(lang => (
                <button
                  key={lang}
                  onClick={() => changeLang(lang)}
                  className={`text-xs px-2 py-1 rounded-lg transition-all duration-150 ${
                    i18n.language === lang
                      ? 'bg-accent-light text-accent font-medium'
                      : 'text-text-muted hover:text-text-secondary'
                  }`}
                >
                  {lang.toUpperCase()}
                </button>
              ))}
            </div>
            <span className="text-xs text-text-secondary truncate max-w-[120px]">{user.displayName}</span>
            <button
              onClick={logout}
              className="text-xs text-text-muted hover:text-text-secondary transition-colors"
            >
              {t('account.signOut')}
            </button>
          </div>

          {/* Mobile hamburger */}
          <button
            className="sm:hidden text-text-secondary hover:text-text-primary p-1"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Toggle menu"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
              {menuOpen ? (
                <>
                  <line x1="4" y1="4" x2="16" y2="16" />
                  <line x1="16" y1="4" x2="4" y2="16" />
                </>
              ) : (
                <>
                  <line x1="3" y1="5" x2="17" y2="5" />
                  <line x1="3" y1="10" x2="17" y2="10" />
                  <line x1="3" y1="15" x2="17" y2="15" />
                </>
              )}
            </svg>
          </button>
        </div>

        {/* Mobile menu */}
        {menuOpen && (
          <div className="sm:hidden border-t border-divider bg-bg px-4 py-3 space-y-1">
            {navLinks.map(({ to, label, end }) => (
              <NavLink
                key={to}
                to={to}
                end={end}
                onClick={() => setMenuOpen(false)}
                className={({ isActive }) =>
                  `block px-3 py-2 rounded-lg text-sm ${
                    isActive
                      ? 'bg-accent-light text-accent font-medium'
                      : 'text-text-secondary'
                  }`
                }
              >
                {label}
              </NavLink>
            ))}
            <NavLink
              to="/account"
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) =>
                `block px-3 py-2 rounded-lg text-sm ${
                  isActive
                    ? 'bg-accent-light text-accent font-medium'
                    : 'text-text-secondary'
                }`
              }
            >
              {t('nav.account')}
            </NavLink>
            <div className="flex gap-1 px-3 pt-2">
              {(['sv', 'en'] as const).map(lang => (
                <button
                  key={lang}
                  onClick={() => changeLang(lang)}
                  className={`text-xs px-2 py-1 rounded-lg ${
                    i18n.language === lang
                      ? 'bg-accent-light text-accent font-medium'
                      : 'text-text-muted'
                  }`}
                >
                  {lang.toUpperCase()}
                </button>
              ))}
            </div>
          </div>
        )}
      </header>

      {/* Main content */}
      <main className="flex-1 w-full max-w-5xl mx-auto px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
