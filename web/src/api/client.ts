const API_BASE = ''

function getToken(): string | null {
  return localStorage.getItem('verdant_token')
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
    localStorage.removeItem('verdant_token')
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

// ── Types ──

export interface AuthResponse { token: string; user: UserResponse }
export interface UserResponse {
  id: number; email: string; displayName: string; avatarUrl?: string
  role: string; language?: string; createdAt: string
}

export interface DashboardResponse {
  user: UserResponse
  gardens: GardenSummary[]
  stats: DashboardStats
}
export interface GardenSummary {
  id: number; name: string; emoji?: string; bedCount: number; plantCount: number
}
export interface DashboardStats { totalGardens: number; totalBeds: number; totalPlants: number }

export interface GardenResponse {
  id: number; name: string; description?: string; emoji?: string
  latitude?: number; longitude?: number; address?: string
  createdAt: string; updatedAt: string
}
export interface BedResponse {
  id: number; name: string; description?: string; gardenId: number
  createdAt: string; updatedAt: string
}
export interface BedWithGardenResponse extends BedResponse { gardenName: string }

export interface PlantResponse {
  id: number; name: string; speciesId?: number; speciesName?: string
  plantedDate?: string; status: string; seedCount?: number; survivingCount?: number
  bedId?: number; createdAt: string; updatedAt: string
}

export interface PlantEventResponse {
  id: number; plantId: number; eventType: string; eventDate: string
  plantCount?: number; weightGrams?: number; quantity?: number
  notes?: string; imageUrl?: string; createdAt: string
}

export interface SpeciesResponse {
  id: number; commonName: string; commonNameSv?: string
  variantName?: string; variantNameSv?: string; scientificName?: string
  daysToSprout?: number; daysToHarvest?: number; sowingDepthMm?: number
  heightCm?: number; germinationRate?: number
  bloomMonths?: string; sowingMonths?: string
  photos: { id: number; imageUrl: string; sortOrder: number }[]
  tags: { id: number; name: string }[]
  custom: boolean
}

export interface SeedInventoryResponse {
  id: number; speciesId: number; speciesName: string
  quantity: number; collectionDate?: string; expirationDate?: string; createdAt: string
}

export interface ScheduledTaskResponse {
  id: number; speciesId: number; speciesName: string; activityType: string
  deadline: string; targetCount: number; remainingCount: number
  status: string; notes?: string; createdAt: string; updatedAt: string
}

export interface HarvestStatRow {
  species: string; totalWeightGrams: number; totalQuantity: number; harvestCount: number
}

export interface TraySummaryEntry { speciesName: string; status: string; count: number }

export interface SpeciesPlantSummary {
  speciesId: number; speciesName: string; scientificName?: string
  activePlantCount: number; totalPlantCount: number
}

export interface PlantLocationGroup {
  gardenName: string; bedName?: string; bedId?: number
  status: string; count: number; year: number
}

export interface FrequentCommentResponse { id: number; text: string; useCount: number }

export interface PlantGroupResponse {
  speciesId: number; speciesName: string; bedId?: number; bedName?: string
  gardenName?: string; plantedDate?: string; status: string; count: number
}

// ── API ──

export const api = {
  auth: {
    google: (idToken: string) =>
      apiRequest<AuthResponse>('/api/auth/google', { method: 'POST', body: JSON.stringify({ idToken }) }),
  },

  user: {
    me: () => apiRequest<UserResponse>('/api/users/me'),
    update: (data: { displayName?: string; language?: string }) =>
      apiRequest<UserResponse>('/api/users/me', { method: 'PUT', body: JSON.stringify(data) }),
    delete: () => apiRequest<void>('/api/users/me', { method: 'DELETE' }),
  },

  dashboard: () => apiRequest<DashboardResponse>('/api/dashboard'),

  gardens: {
    list: () => apiRequest<GardenResponse[]>('/api/gardens'),
    get: (id: number) => apiRequest<GardenResponse>(`/api/gardens/${id}`),
    create: (data: { name: string; description?: string; emoji?: string }) =>
      apiRequest<GardenResponse>('/api/gardens', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: { name: string; description?: string; emoji?: string }) =>
      apiRequest<GardenResponse>(`/api/gardens/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/gardens/${id}`, { method: 'DELETE' }),
    beds: (gardenId: number) => apiRequest<BedResponse[]>(`/api/gardens/${gardenId}/beds`),
  },

  beds: {
    list: () => apiRequest<BedWithGardenResponse[]>('/api/beds'),
    get: (id: number) => apiRequest<BedResponse>(`/api/beds/${id}`),
    create: (gardenId: number, data: { name: string; description?: string }) =>
      apiRequest<BedResponse>(`/api/gardens/${gardenId}/beds`, { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: { name: string; description?: string }) =>
      apiRequest<BedResponse>(`/api/beds/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/beds/${id}`, { method: 'DELETE' }),
    plants: (bedId: number) => apiRequest<PlantResponse[]>(`/api/beds/${bedId}/plants`),
  },

  plants: {
    list: (status?: string) =>
      apiRequest<PlantResponse[]>(`/api/plants${status ? `?status=${status}` : ''}`),
    get: (id: number) => apiRequest<PlantResponse>(`/api/plants/${id}`),
    create: (bedId: number, data: { name: string; speciesId?: number; seedCount?: number }) =>
      apiRequest<PlantResponse>(`/api/beds/${bedId}/plants`, { method: 'POST', body: JSON.stringify(data) }),
    createWithoutBed: (data: { name: string; speciesId?: number; seedCount?: number }) =>
      apiRequest<PlantResponse>('/api/plants', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: { name?: string; status?: string }) =>
      apiRequest<PlantResponse>(`/api/plants/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/plants/${id}`, { method: 'DELETE' }),
    events: (id: number) => apiRequest<PlantEventResponse[]>(`/api/plants/${id}/events`),
    addEvent: (id: number, data: {
      eventType: string; eventDate?: string; plantCount?: number
      weightGrams?: number; quantity?: number; notes?: string; imageBase64?: string
    }) => apiRequest<PlantEventResponse>(`/api/plants/${id}/events`, { method: 'POST', body: JSON.stringify(data) }),
    deleteEvent: (plantId: number, eventId: number) =>
      apiRequest<void>(`/api/plants/${plantId}/events/${eventId}`, { method: 'DELETE' }),
    batchSow: (data: {
      bedId?: number; speciesId: number; name: string; seedCount: number
      notes?: string; imageBase64?: string
    }) => apiRequest<{ plantIds: number[]; count: number }>('/api/plants/batch-sow', { method: 'POST', body: JSON.stringify(data) }),
    traySummary: () => apiRequest<TraySummaryEntry[]>('/api/plants/tray-summary'),
    groups: (status: string, trayOnly?: boolean) =>
      apiRequest<PlantGroupResponse[]>(`/api/plants/groups?status=${status}${trayOnly ? '&trayOnly=true' : ''}`),
    batchEvent: (data: {
      speciesId: number; bedId?: number; plantedDate?: string; status: string
      eventType: string; count: number; notes?: string; targetBedId?: number
    }) => apiRequest<{ updatedCount: number }>('/api/plants/batch-event', { method: 'POST', body: JSON.stringify(data) }),
    speciesSummary: () => apiRequest<SpeciesPlantSummary[]>('/api/plants/species-summary'),
    speciesLocations: (speciesId: number) =>
      apiRequest<PlantLocationGroup[]>(`/api/plants/species/${speciesId}/locations`),
  },

  species: {
    list: () => apiRequest<SpeciesResponse[]>('/api/species'),
    search: (q: string, limit = 20) => apiRequest<SpeciesResponse[]>(`/api/species?q=${encodeURIComponent(q)}&limit=${limit}`),
    create: (data: Record<string, unknown>) =>
      apiRequest<SpeciesResponse>('/api/species', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Record<string, unknown>) =>
      apiRequest<SpeciesResponse>(`/api/species/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/species/${id}`, { method: 'DELETE' }),
  },

  inventory: {
    list: (speciesId?: number) =>
      apiRequest<SeedInventoryResponse[]>(`/api/seed-inventory${speciesId ? `?speciesId=${speciesId}` : ''}`),
    create: (data: { speciesId: number; quantity: number; collectionDate?: string; expirationDate?: string }) =>
      apiRequest<SeedInventoryResponse>('/api/seed-inventory', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: { quantity?: number; collectionDate?: string; expirationDate?: string }) =>
      apiRequest<SeedInventoryResponse>(`/api/seed-inventory/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    decrement: (id: number, quantity: number) =>
      apiRequest<void>(`/api/seed-inventory/${id}/decrement`, { method: 'POST', body: JSON.stringify({ quantity }) }),
    delete: (id: number) => apiRequest<void>(`/api/seed-inventory/${id}`, { method: 'DELETE' }),
  },

  tasks: {
    list: () => apiRequest<ScheduledTaskResponse[]>('/api/tasks'),
    get: (id: number) => apiRequest<ScheduledTaskResponse>(`/api/tasks/${id}`),
    create: (data: {
      speciesId: number; activityType: string; deadline: string; targetCount: number; notes?: string
    }) => apiRequest<ScheduledTaskResponse>('/api/tasks', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: {
      speciesId?: number; activityType?: string; deadline?: string; targetCount?: number; notes?: string
    }) => apiRequest<ScheduledTaskResponse>(`/api/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    complete: (id: number, processedCount: number) =>
      apiRequest<void>(`/api/tasks/${id}/complete`, { method: 'POST', body: JSON.stringify({ processedCount }) }),
    delete: (id: number) => apiRequest<void>(`/api/tasks/${id}`, { method: 'DELETE' }),
  },

  comments: {
    list: () => apiRequest<FrequentCommentResponse[]>('/api/comments'),
    record: (text: string) =>
      apiRequest<void>('/api/comments', { method: 'POST', body: JSON.stringify({ text }) }),
  },

  stats: {
    harvests: () => apiRequest<HarvestStatRow[]>('/api/stats/harvests'),
  },
}
