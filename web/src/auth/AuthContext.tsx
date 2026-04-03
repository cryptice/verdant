import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, setOnUnauthorized, type UserResponse } from '../api/client'

interface AuthState {
  user: UserResponse | null
  token: string | null
  loading: boolean
  login: (token: string, user: UserResponse) => void
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('verdant_token'))
  const [user, setUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(!!token)
  const navigate = useNavigate()

  const logout = useCallback(() => {
    localStorage.removeItem('verdant_token')
    setToken(null)
    setUser(null)
    navigate('/login')
  }, [navigate])

  useEffect(() => { setOnUnauthorized(logout) }, [logout])

  useEffect(() => {
    if (!token) { setLoading(false); return }
    api.user.me().then(setUser).catch(() => logout()).finally(() => setLoading(false))
  }, [token, logout])

  const login = useCallback((t: string, u: UserResponse) => {
    localStorage.removeItem('admin_token')
    localStorage.setItem('verdant_token', t)
    setToken(t)
    setUser(u)
  }, [])

  const refreshUser = useCallback(async () => {
    const u = await api.user.me()
    setUser(u)
  }, [])

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
