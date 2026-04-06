const API_BASE = ''

function getToken(): string | null {
  return localStorage.getItem('admin_token')
}

export class ApiError extends Error {
  status?: number
  isNetworkError: boolean

  constructor(message: string, status?: number, isNetworkError = false) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.isNetworkError = isNetworkError
  }
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...((options.headers as Record<string, string>) || {})
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  } catch {
    throw new ApiError('Unable to connect to the server. Is the backend running?', undefined, true)
  }

  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem('admin_token')
    window.location.href = '/login'
    throw new ApiError('Unauthorized', response.status)
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    if (!body) {
      throw new ApiError('Unable to connect to the server. Is the backend running?', undefined, true)
    }
    throw new ApiError(body.message || `HTTP ${response.status}`, response.status)
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
  germinationTimeDaysMin: number | null
  germinationTimeDaysMax: number | null
  daysToHarvestMin: number | null
  daysToHarvestMax: number | null
  sowingDepthMm: number | null
  growingPositions: string[]
  soils: string[]
  heightCmMin: number | null
  heightCmMax: number | null
  bloomMonths: number[]
  sowingMonths: number[]
  germinationRate: number | null
  groups: { id: number; name: string }[]
  tags: SpeciesTag[]
  providers: SpeciesProvider[]
  costPerSeedSek: number | null
  expectedStemsPerPlant: number | null
  expectedVaseLifeDays: number | null
  plantType: string | null
  defaultUnitType: string | null
  workflowTemplateId: number | null
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
  germinationTimeDaysMin?: number | null
  germinationTimeDaysMax?: number | null
  daysToHarvestMin?: number | null
  daysToHarvestMax?: number | null
  sowingDepthMm?: number | null
  growingPositions?: string[]
  soils?: string[]
  heightCmMin?: number | null
  heightCmMax?: number | null
  bloomMonths?: number[]
  sowingMonths?: number[]
  germinationRate?: number | null
  tagIds?: number[]
  costPerSeedSek?: number | null
  expectedStemsPerPlant?: number | null
  expectedVaseLifeDays?: number | null
  plantType?: string | null
  defaultUnitType?: string | null
  workflowTemplateId?: number | null
}

export interface UpdateSpeciesRequest {
  commonName?: string | null
  commonNameSv?: string | null
  variantName?: string | null
  variantNameSv?: string | null
  scientificName?: string | null
  imageFrontBase64?: string | null
  imageBackBase64?: string | null
  germinationTimeDaysMin?: number | null
  germinationTimeDaysMax?: number | null
  daysToHarvestMin?: number | null
  daysToHarvestMax?: number | null
  sowingDepthMm?: number | null
  growingPositions?: string[] | null
  soils?: string[] | null
  heightCmMin?: number | null
  heightCmMax?: number | null
  bloomMonths?: number[]
  sowingMonths?: number[]
  germinationRate?: number | null
  tagIds?: number[] | null
  costPerSeedSek?: number | null
  expectedStemsPerPlant?: number | null
  expectedVaseLifeDays?: number | null
  plantType?: string | null
  defaultUnitType?: string | null
  workflowTemplateId?: number | null
}

export interface ExtractedFrontInfo {
  commonName: string | null
  commonNameSv: string | null
  variantName: string | null
  variantNameSv: string | null
  scientificName: string | null
}

export interface ExtractedSpeciesInfo {
  commonName: string | null
  scientificName: string | null
  germinationTimeDaysMin: number | null
  germinationTimeDaysMax: number | null
  sowingDepthMm: number | null
  heightCmMin: number | null
  heightCmMax: number | null
  bloomMonths: number[] | null
  sowingMonths: number[] | null
  germinationRate: number | null
  growingPositions: string[] | null
  soils: string[] | null
  daysToHarvestMin: number | null
  daysToHarvestMax: number | null
}

export interface SpeciesExportProvider {
  providerName: string
  providerIdentifier: string
  imageFrontUrl: string | null
  imageBackUrl: string | null
  productUrl: string | null
}

export interface SpeciesExportEntry {
  commonName: string
  variantName: string | null
  commonNameSv: string | null
  variantNameSv: string | null
  scientificName: string | null
  imageFrontUrl: string | null
  imageBackUrl: string | null
  germinationTimeDaysMin: number | null
  germinationTimeDaysMax: number | null
  daysToHarvestMin: number | null
  daysToHarvestMax: number | null
  sowingDepthMm: number | null
  growingPositions: string[]
  soils: string[]
  heightCmMin: number | null
  heightCmMax: number | null
  bloomMonths: number[]
  sowingMonths: number[]
  germinationRate: number | null
  groupNames: string[]
  tagNames: string[]
  providers: SpeciesExportProvider[]
  workflowTemplateId: number | null
}

export interface ImportResult {
  created: number
  skipped: number
}

export interface Provider {
  id: number
  name: string
  identifier: string
}

export interface AddSpeciesProviderRequest {
  providerId: number
  imageFrontBase64?: string | null
  imageBackBase64?: string | null
  productUrl?: string | null
}

export interface UpdateSpeciesProviderRequest {
  imageFrontBase64?: string | null
  imageBackBase64?: string | null
  productUrl?: string | null
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
    searchSpecies: (q: string, limit = 20) => apiRequest<Species[]>(`/api/admin/species?q=${encodeURIComponent(q)}&limit=${limit}`),
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

    extractFront: (imageBase64: string) =>
      apiRequest<ExtractedFrontInfo>('/api/admin/species/extract-front', {
        method: 'POST',
        body: JSON.stringify({ imageBase64 })
      }),
    extractBack: (imageBase64: string) =>
      apiRequest<ExtractedSpeciesInfo>('/api/admin/species/extract-back', {
        method: 'POST',
        body: JSON.stringify({ imageBase64 })
      }),
    exportSpecies: () => apiRequest<SpeciesExportEntry[]>('/api/admin/species/export'),
    importSpecies: (entries: SpeciesExportEntry[]) =>
      apiRequest<ImportResult>('/api/admin/species/import', {
        method: 'POST',
        body: JSON.stringify(entries)
      }),

    getSpeciesTags: () => apiRequest<SpeciesTag[]>('/api/admin/species/tags'),

    // Providers
    getProviders: () => apiRequest<Provider[]>('/api/admin/providers'),
    createProvider: (req: { name: string; identifier: string }) =>
      apiRequest<Provider>('/api/admin/providers', { method: 'POST', body: JSON.stringify(req) }),
    updateProvider: (id: number, req: { name?: string; identifier?: string }) =>
      apiRequest<Provider>(`/api/admin/providers/${id}`, { method: 'PUT', body: JSON.stringify(req) }),
    deleteProvider: (id: number) =>
      apiRequest<void>(`/api/admin/providers/${id}`, { method: 'DELETE' }),

    // Species Providers
    addSpeciesProvider: (speciesId: number, req: AddSpeciesProviderRequest) =>
      apiRequest<SpeciesProvider>(`/api/admin/species/${speciesId}/providers`, {
        method: 'POST', body: JSON.stringify(req)
      }),
    updateSpeciesProvider: (speciesId: number, spId: number, req: UpdateSpeciesProviderRequest) =>
      apiRequest<SpeciesProvider>(`/api/admin/species/${speciesId}/providers/${spId}`, {
        method: 'PUT', body: JSON.stringify(req)
      }),
    deleteSpeciesProvider: (speciesId: number, spId: number) =>
      apiRequest<void>(`/api/admin/species/${speciesId}/providers/${spId}`, { method: 'DELETE' }),
  }
}
