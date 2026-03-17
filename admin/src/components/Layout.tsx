import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function Layout() {
  const { logout } = useAuth()
  const navigate = useNavigate()

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
          <p className="text-xs text-[#A5A29C] mt-0.5">Admin</p>
        </div>
        <nav className="flex-1 px-3 space-y-0.5">
          <NavLink to="/species" className={linkClass}>Species</NavLink>
          <NavLink to="/users" className={linkClass}>Users</NavLink>
          <NavLink to="/gardens" className={linkClass}>Gardens</NavLink>
          <NavLink to="/providers" className={linkClass}>Providers</NavLink>
          {window.location.hostname === 'localhost' && (
            <NavLink to="/dev" className={linkClass}>Dev Tools</NavLink>
          )}
        </nav>
        <div className="px-3 py-3 border-t border-[#E9E9E7]">
          <button
            onClick={handleLogout}
            className="w-full px-3 py-1 text-sm text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-left"
          >
            Sign Out
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto px-10 pt-6 pb-10">
        <div className="max-w-5xl mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
