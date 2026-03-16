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
    `block px-4 py-2 rounded-lg transition-colors ${
      isActive
        ? 'bg-green-100 text-green-800 font-medium'
        : 'text-gray-600 hover:bg-gray-100'
    }`

  return (
    <div className="flex h-screen bg-[#F5F0E1]">
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-6">
          <h1 className="text-2xl font-bold text-green-600">Verdant</h1>
          <p className="text-sm text-gray-500 mt-1">Admin Panel</p>
        </div>
        <nav className="flex-1 px-4 space-y-1">
          <NavLink to="/species" className={linkClass}>Species</NavLink>
          <NavLink to="/users" className={linkClass}>Users</NavLink>
          <NavLink to="/gardens" className={linkClass}>Gardens</NavLink>
          <NavLink to="/dev" className={linkClass}>Dev Tools</NavLink>
        </nav>
        <div className="p-4 border-t">
          <button
            onClick={handleLogout}
            className="w-full px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            Sign Out
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto p-8">
        <Outlet />
      </main>
    </div>
  )
}
