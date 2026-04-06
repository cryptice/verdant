import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useTranslation } from 'react-i18next'

export default function Layout() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const { t, i18n } = useTranslation()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-2 px-3 py-1 rounded-md text-sm transition-colors ${
      isActive
        ? 'bg-[#F0F0EE] text-[#37352F] font-medium'
        : 'text-[#787774] hover:bg-[#F0F0EE]'
    }`

  return (
    <div className="flex h-screen bg-white">
      <aside className="w-60 bg-[#FBFBFA] border-r border-[#E9E9E7] flex flex-col">
        <div className="px-4 py-5">
          <h1 className="text-lg font-semibold text-[#37352F] tracking-tight">Verdant</h1>
          <p className="text-xs text-[#A5A29C] mt-0.5">{t('nav.admin')}</p>
        </div>
        <nav className="flex-1 px-3 space-y-0.5">
          <NavLink to="/species" className={linkClass}>{t('nav.species')}</NavLink>
          <NavLink to="/users" className={linkClass}>{t('nav.users')}</NavLink>
          <NavLink to="/gardens" className={linkClass}>{t('nav.gardens')}</NavLink>
          <NavLink to="/providers" className={linkClass}>{t('nav.providers')}</NavLink>
          <NavLink to="/reset" className={linkClass}>{t('nav.resetData')}</NavLink>
          {window.location.hostname === 'localhost' && (
            <NavLink to="/dev" className={linkClass}>{t('nav.devTools')}</NavLink>
          )}
        </nav>
        <div className="px-3 py-3 border-t border-[#E9E9E7] space-y-1">
          <div className="flex gap-1 px-3 py-1">
            <button
              onClick={() => i18n.changeLanguage('sv')}
              className={`text-xs px-2 py-0.5 rounded transition-colors ${i18n.language === 'sv' ? 'bg-[#E9E9E7] text-[#37352F] font-medium' : 'text-[#A5A29C] hover:text-[#787774]'}`}
            >
              SV
            </button>
            <button
              onClick={() => i18n.changeLanguage('en')}
              className={`text-xs px-2 py-0.5 rounded transition-colors ${i18n.language === 'en' ? 'bg-[#E9E9E7] text-[#37352F] font-medium' : 'text-[#A5A29C] hover:text-[#787774]'}`}
            >
              EN
            </button>
          </div>
          <button
            onClick={handleLogout}
            className="w-full px-3 py-1 text-sm text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-left"
          >
            {t('nav.signOut')}
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto px-10 pt-6 pb-10">
        <div className="max-w-5xl">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
