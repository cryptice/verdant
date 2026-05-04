import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useTranslation } from 'react-i18next'
import { useEffect, useState } from 'react'

export default function Layout() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const { t, i18n } = useTranslation()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const closeDrawer = () => setDrawerOpen(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  // Lock body scroll while drawer is open
  useEffect(() => {
    if (!drawerOpen) return
    const original = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = original }
  }, [drawerOpen])

  // Esc closes drawer
  useEffect(() => {
    if (!drawerOpen) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setDrawerOpen(false) }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [drawerOpen])

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-2 px-3 py-2 md:py-1 rounded-md text-sm transition-colors ${
      isActive
        ? 'bg-[#F0F0EE] text-[#37352F] font-medium'
        : 'text-[#787774] hover:bg-[#F0F0EE]'
    }`

  const sidebar = (
    <>
      <div className="px-4 py-5 hidden md:block">
        <h1 className="text-lg font-semibold text-[#37352F] tracking-tight">Verdant</h1>
        <p className="text-xs text-[#A5A29C] mt-0.5">{t('nav.admin')}</p>
      </div>
      <nav className="flex-1 px-3 pt-2 md:pt-0 space-y-0.5 overflow-y-auto">
        <NavLink to="/species" className={linkClass} onClick={closeDrawer}>{t('nav.species')}</NavLink>
        <NavLink to="/users" className={linkClass} onClick={closeDrawer}>{t('nav.users')}</NavLink>
        <NavLink to="/gardens" className={linkClass} onClick={closeDrawer}>{t('nav.gardens')}</NavLink>
        <NavLink to="/providers" className={linkClass} onClick={closeDrawer}>{t('nav.providers')}</NavLink>
        <NavLink to="/outlets" className={linkClass} onClick={closeDrawer}>{t('nav.outlets')}</NavLink>
        <NavLink to="/reset" className={linkClass} onClick={closeDrawer}>{t('nav.resetData')}</NavLink>
        {window.location.hostname === 'localhost' && (
          <NavLink to="/dev" className={linkClass} onClick={closeDrawer}>{t('nav.devTools')}</NavLink>
        )}
      </nav>
      <div className="px-3 py-3 border-t border-[#E9E9E7] space-y-1 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
        <div className="flex gap-1 px-3 py-1">
          <button
            onClick={() => i18n.changeLanguage('sv')}
            className={`text-xs px-2 py-1 rounded transition-colors ${i18n.language === 'sv' ? 'bg-[#E9E9E7] text-[#37352F] font-medium' : 'text-[#A5A29C] hover:text-[#787774]'}`}
          >
            SV
          </button>
          <button
            onClick={() => i18n.changeLanguage('en')}
            className={`text-xs px-2 py-1 rounded transition-colors ${i18n.language === 'en' ? 'bg-[#E9E9E7] text-[#37352F] font-medium' : 'text-[#A5A29C] hover:text-[#787774]'}`}
          >
            EN
          </button>
        </div>
        <button
          onClick={handleLogout}
          className="w-full px-3 py-2 md:py-1 text-sm text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-left"
        >
          {t('nav.signOut')}
        </button>
      </div>
    </>
  )

  return (
    <div className="md:flex md:h-screen bg-white min-h-screen">
      {/* Mobile top app bar */}
      <header className="md:hidden sticky top-0 z-30 flex items-center justify-between px-4 min-h-14 bg-white/85 backdrop-blur border-b border-[#E9E9E7] pt-[env(safe-area-inset-top)]">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setDrawerOpen(true)}
            aria-label={t('nav.openMenu')}
            className="-ml-2 inline-flex items-center justify-center w-10 h-10 rounded-md text-[#37352F] hover:bg-[#F0F0EE] active:bg-[#E9E9E7]"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
          <h1 className="text-base font-semibold text-[#37352F] tracking-tight">Verdant</h1>
        </div>
      </header>

      {/* Mobile drawer */}
      <div
        className={`md:hidden fixed inset-0 z-40 transition-opacity duration-200 ${drawerOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'}`}
        aria-hidden={!drawerOpen}
      >
        <button
          type="button"
          aria-label={t('nav.closeMenu')}
          tabIndex={drawerOpen ? 0 : -1}
          onClick={() => setDrawerOpen(false)}
          className="absolute inset-0 bg-black/30"
        />
        <aside
          className={`absolute left-0 top-0 bottom-0 w-72 max-w-[85%] bg-[#FBFBFA] border-r border-[#E9E9E7] flex flex-col shadow-xl transform transition-transform duration-200 ease-out ${drawerOpen ? 'translate-x-0' : '-translate-x-full'}`}
          role="dialog"
          aria-modal="true"
        >
          <div className="flex items-center justify-between px-4 min-h-14 pt-[env(safe-area-inset-top)]">
            <div>
              <h1 className="text-lg font-semibold text-[#37352F] tracking-tight">Verdant</h1>
              <p className="text-xs text-[#A5A29C] mt-0.5">{t('nav.admin')}</p>
            </div>
            <button
              type="button"
              onClick={() => setDrawerOpen(false)}
              aria-label={t('nav.closeMenu')}
              className="-mr-2 inline-flex items-center justify-center w-10 h-10 rounded-md text-[#787774] hover:bg-[#F0F0EE]"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          {sidebar}
        </aside>
      </div>

      {/* Desktop sidebar */}
      <aside className="hidden md:flex w-60 bg-[#FBFBFA] border-r border-[#E9E9E7] flex-col">
        {sidebar}
      </aside>

      <main className="flex-1 md:overflow-auto px-4 sm:px-6 md:px-10 pt-4 md:pt-6 pb-10 md:pb-10">
        <div className="max-w-5xl mx-auto md:mx-0">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
