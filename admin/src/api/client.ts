const API_BASE = ''

function getToken(): string | null {
  return localStorage.getItem('admin_token')
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...((options.headers as Record<string, string>) || {})
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })

  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem('admin_token')
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Request failed' }))
    throw new Error(error.message || `HTTP ${response.status}`)
  }

  if (response.status === 204) return undefined as T
  return response.json()
}

export interface AuthResponse {
  token: string
  user: User
}

export interface User {
  id: number
  email: string
  displayName: string
  avatarUrl: string | null
  role: 'USER' | 'ADMIN'
  createdAt: string
}

export interface Garden {
  id: number
  name: string
  description: string | null
  emoji: string | null
  createdAt: string
  updatedAt: string
}

export const api = {
  auth: {
    login: (email: string, password: string) =>
      apiRequest<AuthResponse>('/api/auth/admin', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      })
  },
  admin: {
    getUsers: () => apiRequest<User[]>('/api/admin/users'),
    deleteUser: (id: number) => apiRequest<void>(`/api/admin/users/${id}`, { method: 'DELETE' }),
    getGardens: () => apiRequest<Garden[]>('/api/admin/gardens'),
  }
}
