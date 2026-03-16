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

export interface SpeciesPhoto {
  id: number
  imageUrl: string
  sortOrder: number
}

export interface SpeciesTag {
  id: number
  name: string
}

export interface SpeciesProvider {
  id: number
  providerId: number
  providerName: string
  providerIdentifier: string
  imageFrontUrl: string | null
  imageBackUrl: string | null
  productUrl: string | null
}

export interface Species {
  id: number
  commonName: string
  commonNameSv: string | null
  variantName: string | null
  variantNameSv: string | null
  scientificName: string | null
  imageFrontUrl: string | null
  imageBackUrl: string | null
  photos: SpeciesPhoto[]
  daysToSprout: number | null
  daysToHarvest: number | null
  germinationTimeDays: number | null
  sowingDepthMm: number | null
  growingPositions: string[]
  soils: string[]
  heightCm: number | null
  bloomTime: string | null
  germinationRate: number | null
  groupId: number | null
  groupName: string | null
  tags: SpeciesTag[]
  providers: SpeciesProvider[]
  isSystem: boolean
  createdAt: string
}

export interface SpeciesGroup {
  id: number
  name: string
}

export interface CreateSpeciesRequest {
  commonName: string
  commonNameSv?: string | null
  variantName?: string | null
  variantNameSv?: string | null
  scientificName?: string | null
  imageFrontBase64?: string | null
  imageBackBase64?: string | null
  daysToSprout?: number | null
  daysToHarvest?: number | null
  germinationTimeDays?: number | null
  sowingDepthMm?: number | null
  growingPositions?: string[]
  soils?: string[]
  heightCm?: number | null
  bloomTime?: string | null
  germinationRate?: number | null
  groupId?: number | null
  tagIds?: number[]
}

export interface UpdateSpeciesRequest {
  commonName?: string | null
  commonNameSv?: string | null
  variantName?: string | null
  variantNameSv?: string | null
  scientificName?: string | null
  imageFrontBase64?: string | null
  imageBackBase64?: string | null
  daysToSprout?: number | null
  daysToHarvest?: number | null
  germinationTimeDays?: number | null
  sowingDepthMm?: number | null
  growingPositions?: string[] | null
  soils?: string[] | null
  heightCm?: number | null
  bloomTime?: string | null
  germinationRate?: number | null
  groupId?: number | null
  tagIds?: number[] | null
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

    getSpecies: () => apiRequest<Species[]>('/api/admin/species'),
    getSpeciesById: (id: number) => apiRequest<Species>(`/api/admin/species/${id}`),
    createSpecies: (req: CreateSpeciesRequest) =>
      apiRequest<Species>('/api/admin/species', { method: 'POST', body: JSON.stringify(req) }),
    updateSpecies: (id: number, req: UpdateSpeciesRequest) =>
      apiRequest<Species>(`/api/admin/species/${id}`, { method: 'PUT', body: JSON.stringify(req) }),
    deleteSpecies: (id: number) =>
      apiRequest<void>(`/api/admin/species/${id}`, { method: 'DELETE' }),

    uploadSpeciesPhoto: (speciesId: number, imageBase64: string) =>
      apiRequest<SpeciesPhoto>(`/api/admin/species/${speciesId}/photos`, {
        method: 'POST',
        body: JSON.stringify({ imageBase64 })
      }),
    deleteSpeciesPhoto: (speciesId: number, photoId: number) =>
      apiRequest<void>(`/api/admin/species/${speciesId}/photos/${photoId}`, { method: 'DELETE' }),

    getSpeciesGroups: () => apiRequest<SpeciesGroup[]>('/api/species/groups'),
    getSpeciesTags: () => apiRequest<SpeciesTag[]>('/api/species/tags'),
  }
}
