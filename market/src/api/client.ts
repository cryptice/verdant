const API_BASE = ''

function getToken(): string | null {
  return localStorage.getItem('market_token')
}

export class ApiError extends Error {
  status?: number
  isNetworkError: boolean
  constructor(message: string, status?: number, isNetworkError = false) {
    super(message)
    this.status = status
    this.isNetworkError = isNetworkError
  }
}

let onUnauthorized: (() => void) | null = null
export function setOnUnauthorized(cb: () => void) {
  onUnauthorized = cb
}

async function apiRequest<T>(path: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(`${API_BASE}${path}`, { ...options, headers: { ...headers, ...options?.headers } })
  } catch {
    throw new ApiError('Network error — check your connection', undefined, true)
  }

  if (res.status === 401 || res.status === 403) {
    localStorage.removeItem('market_token')
    onUnauthorized?.()
    throw new ApiError('Unauthorized', res.status)
  }

  if (!res.ok) {
    const body = await res.text()
    let message = `Request failed (${res.status})`
    try { message = JSON.parse(body).message ?? message } catch { /* use default */ }
    throw new ApiError(message, res.status)
  }

  if (res.status === 204) return undefined as T
  return res.json()
}

// -- Types --

export interface AuthResponse { token: string; user: UserResponse }
export interface UserResponse {
  id: number; email: string; displayName: string; avatarUrl?: string
  role: string; language?: string; createdAt: string
}

export interface ListingResponse {
  id: number; sellerName: string; producerName: string
  speciesId: number; speciesName: string; speciesNameSv?: string
  title: string; description?: string
  quantityAvailable: number; pricePerStemCents: number
  availableFrom: string; availableUntil: string
  imageUrl?: string; isActive: boolean; createdAt: string
}

export interface MarketOrderResponse {
  id: number; purchaserId: number; purchaserName: string
  producerId: number; producerName: string
  status: string; deliveryDate?: string
  totalCents: number; notes?: string
  items: OrderItemResponse[]
  createdAt: string; updatedAt: string
}

export interface OrderItemResponse {
  id: number; listingId: number
  speciesId: number; speciesName: string
  quantity: number; pricePerStemCents: number
}

// -- API --

export const api = {
  auth: {
    google: (idToken: string) =>
      apiRequest<AuthResponse>('/api/auth/google', { method: 'POST', body: JSON.stringify({ idToken }) }),
  },

  user: {
    me: () => apiRequest<UserResponse>('/api/users/me'),
  },

  market: {
    listings: () => apiRequest<ListingResponse[]>('/api/market/listings'),
    placeOrder: (data: Record<string, unknown>) =>
      apiRequest<MarketOrderResponse>('/api/market/orders', { method: 'POST', body: JSON.stringify(data) }),
    myOrders: () => apiRequest<MarketOrderResponse[]>('/api/market/orders/mine'),
    getOrder: (id: number) => apiRequest<MarketOrderResponse>(`/api/market/orders/${id}`),
    updateOrderStatus: (id: number, status: string) =>
      apiRequest<MarketOrderResponse>(`/api/market/orders/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),
  },
}
