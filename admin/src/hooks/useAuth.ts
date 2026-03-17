import { useState, useCallback } from 'react'

interface AuthState {
  token: string | null
  isAuthenticated: boolean
}

export function useAuth() {
  const [auth, setAuth] = useState<AuthState>(() => {
    const token = localStorage.getItem('admin_token')
    return { token, isAuthenticated: !!token }
  })

  const login = useCallback((token: string) => {
    localStorage.removeItem('verdant_token')
    localStorage.setItem('admin_token', token)
    setAuth({ token, isAuthenticated: true })
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('admin_token')
    setAuth({ token: null, isAuthenticated: false })
  }, [])

  return { ...auth, login, logout }
}
