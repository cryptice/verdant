const API_BASE = ''

function getToken(): string | null {
  return localStorage.getItem('verdant_token')
}

function getActiveOrgId(): string | null {
  return localStorage.getItem('verdant_org_id')
}

export function setActiveOrgId(orgId: number | null) {
  if (orgId) localStorage.setItem('verdant_org_id', String(orgId))
  else localStorage.removeItem('verdant_org_id')
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
  const orgId = getActiveOrgId()
  if (orgId) headers['X-Organization-Id'] = orgId

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
  role: string; language?: string; onboarding?: string; advancedMode: boolean
  organizations: UserOrgMembership[]
  createdAt: string
}

export interface UserOrgMembership {
  orgId: number; orgName: string; orgEmoji?: string; role: string
}

export interface OrganizationResponse {
  id: number; name: string; emoji?: string; role: string; createdAt: string
}

export interface OrgMemberResponse {
  id: number; userId: number; email: string; displayName: string
  avatarUrl?: string; role: string; joinedAt: string
}

export interface OrgInviteResponse {
  id: number; orgId: number; orgName: string; email: string
  invitedByName: string; status: string; createdAt: string
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
  lengthMeters?: number; widthMeters?: number
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
  stemCount?: number; stemLengthCm?: number; qualityGrade?: string
  vaseLifeDays?: number; harvestDestinationId?: number; customerName?: string
  notes?: string; imageUrl?: string; createdAt: string
}

export interface SpeciesResponse {
  id: number; commonName: string; commonNameSv?: string
  variantName?: string; variantNameSv?: string; scientificName?: string
  daysToSprout?: number; daysToHarvest?: number; sowingDepthMm?: number
  heightCm?: number; germinationRate?: number
  bloomMonths?: string; sowingMonths?: string
  costPerSeedSek?: number; expectedStemsPerPlant?: number
  expectedVaseLifeDays?: number; plantType?: string
  photos: { id: number; imageUrl: string; sortOrder: number }[]
  tags: { id: number; name: string }[]
  providers: { id: number; providerId: number; providerName: string }[]
  custom: boolean
}

export interface SeedInventoryResponse {
  id: number; speciesId: number; speciesName: string
  quantity: number; collectionDate?: string; expirationDate?: string
  costPerUnitSek?: number; unitType?: string; seasonId?: number
  speciesProviderId?: number; providerName?: string
  createdAt: string
}

export interface ScheduledTaskResponse {
  id: number; speciesId: number; speciesName: string; activityType: string
  deadline: string; targetCount: number; remainingCount: number
  status: string; notes?: string; seasonId?: number; successionScheduleId?: number
  createdAt: string; updatedAt: string
}

export interface HarvestStatRow {
  species: string; totalWeightGrams: number; totalQuantity: number; totalStems: number; harvestCount: number
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

// Season
export interface SeasonResponse {
  id: number; name: string; year: number
  startDate?: string; endDate?: string
  lastFrostDate?: string; firstFrostDate?: string
  growingDegreeBaseC?: number; notes?: string
  isActive: boolean; createdAt: string; updatedAt: string
}

// Customer
export interface CustomerResponse {
  id: number; name: string; channel: string
  contactInfo?: string; notes?: string; createdAt: string
}

// Pest/Disease
export interface PestDiseaseLogResponse {
  id: number; seasonId?: number; bedId?: number; speciesId?: number
  observedDate: string; category: string; name: string
  severity: string; treatment?: string; outcome?: string
  notes?: string; imageUrl?: string; createdAt: string
}

// Variety Trial
export interface VarietyTrialResponse {
  id: number; seasonId: number; speciesId: number; bedId?: number
  plantCount?: number; stemYield?: number; avgStemLengthCm?: number
  avgVaseLifeDays?: number; qualityScore?: number
  customerReception?: string; verdict: string
  notes?: string; createdAt: string
}

// Bouquet Recipe
export interface BouquetRecipeResponse {
  id: number; name: string; description?: string
  imageUrl?: string; priceSek?: number
  items: BouquetRecipeItemResponse[]
  createdAt: string; updatedAt: string
}
export interface BouquetRecipeItemResponse {
  id: number; speciesId: number; speciesName: string
  stemCount: number; role: string; notes?: string
}

// Succession Schedule
export interface SuccessionScheduleResponse {
  id: number; seasonId: number; speciesId: number; speciesName: string
  bedId?: number; firstSowDate: string; intervalDays: number
  totalSuccessions: number; seedsPerSuccession: number
  notes?: string; createdAt: string
}

// Production Target
export interface ProductionTargetResponse {
  id: number; seasonId: number; speciesId: number; speciesName: string
  stemsPerWeek: number; startDate: string; endDate: string
  notes?: string; createdAt: string
}
export interface ProductionForecastResponse {
  targetId: number; speciesName: string
  totalStemsNeeded: number; plantsNeeded: number
  seedsNeeded: number; suggestedSowDate?: string
  weeksOfDelivery: number; warnings: string[]
}

// Analytics
export interface SeasonSummaryResponse {
  seasonId: number; seasonName: string; year: number
  totalPlants: number; totalStemsHarvested: number
  totalHarvestWeightGrams: number; speciesCount: number
  topSpecies: SpeciesYieldSummary[]
}
export interface SpeciesYieldSummary {
  speciesId: number; speciesName: string
  plantCount: number; stemsHarvested: number
  avgStemLength?: number; avgVaseLife?: number
  qualityBreakdown: Record<string, number>
}
export interface SpeciesComparisonResponse {
  speciesId: number; speciesName: string
  seasons: SpeciesSeasonData[]
}
export interface SpeciesSeasonData {
  seasonId: number; seasonName: string; year: number
  plantCount: number; stemsHarvested: number
  stemsPerPlant?: number; avgStemLength?: number
  avgVaseLife?: number
}
export interface YieldPerBedResponse {
  bedId: number; bedName: string; gardenName: string
  areaM2?: number; seasons: BedSeasonYield[]
}
export interface BedSeasonYield {
  seasonId: number; seasonName: string
  stemsHarvested: number; stemsPerM2?: number
}

// Market
export interface ListingResponse {
  id: number; sellerName: string; producerName: string;
  speciesId: number; speciesName: string; speciesNameSv?: string;
  title: string; description?: string;
  quantityAvailable: number; pricePerStemSek: number;
  availableFrom: string; availableUntil: string;
  imageUrl?: string; isActive: boolean; createdAt: string;
}

export interface MarketOrderResponse {
  id: number; purchaserId: number; purchaserName: string;
  producerId: number; producerName: string;
  status: string; deliveryDate?: string;
  totalSek: number; notes?: string;
  items: OrderItemResponse[];
  createdAt: string; updatedAt: string;
}

export interface OrderItemResponse {
  id: number; listingId: number;
  speciesId: number; speciesName: string;
  quantity: number; pricePerStemSek: number;
}

// Bed History
export interface BedHistoryEntry {
  seasonId?: number; seasonName?: string; year?: number
  species: BedHistorySpecies[]
}
export interface BedHistorySpecies {
  speciesId: number; speciesName: string
  plantCount: number; totalStemsHarvested: number; status: string
}

// ── API ──

export const api = {
  auth: {
    google: (idToken: string) =>
      apiRequest<AuthResponse>('/api/auth/google', { method: 'POST', body: JSON.stringify({ idToken }) }),
  },

  user: {
    me: () => apiRequest<UserResponse>('/api/users/me'),
    update: (data: { displayName?: string; language?: string; advancedMode?: boolean }) =>
      apiRequest<UserResponse>('/api/users/me', { method: 'PUT', body: JSON.stringify(data) }),
    delete: () => apiRequest<void>('/api/users/me', { method: 'DELETE' }),
    updateOnboarding: (data: { completedSteps?: string[]; dismissed?: boolean }) =>
      apiRequest<UserResponse>('/api/users/me/onboarding', { method: 'PUT', body: JSON.stringify(data) }),
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
    history: (id: number) => apiRequest<BedHistoryEntry[]>(`/api/beds/${id}/history`),
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
      stemCount?: number; stemLengthCm?: number; qualityGrade?: string; harvestDestinationId?: number
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
      apiRequest<SeedInventoryResponse[]>(`/api/seed-stock${speciesId ? `?speciesId=${speciesId}` : ''}`),
    create: (data: { speciesId: number; quantity: number; collectionDate?: string; expirationDate?: string; costPerUnitSek?: number; unitType?: string; speciesProviderId?: number }) =>
      apiRequest<SeedInventoryResponse>('/api/seed-stock', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: { quantity?: number; collectionDate?: string; expirationDate?: string; costPerUnitSek?: number; unitType?: string; speciesProviderId?: number }) =>
      apiRequest<SeedInventoryResponse>(`/api/seed-stock/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    decrement: (id: number, quantity: number) =>
      apiRequest<void>(`/api/seed-stock/${id}/decrement`, { method: 'POST', body: JSON.stringify({ quantity }) }),
    delete: (id: number) => apiRequest<void>(`/api/seed-stock/${id}`, { method: 'DELETE' }),
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

  seasons: {
    list: () => apiRequest<SeasonResponse[]>('/api/seasons'),
    get: (id: number) => apiRequest<SeasonResponse>(`/api/seasons/${id}`),
    create: (data: Record<string, unknown>) => apiRequest<SeasonResponse>('/api/seasons', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Record<string, unknown>) => apiRequest<SeasonResponse>(`/api/seasons/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/seasons/${id}`, { method: 'DELETE' }),
  },

  customers: {
    list: () => apiRequest<CustomerResponse[]>('/api/customers'),
    get: (id: number) => apiRequest<CustomerResponse>(`/api/customers/${id}`),
    create: (data: Record<string, unknown>) => apiRequest<CustomerResponse>('/api/customers', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Record<string, unknown>) => apiRequest<CustomerResponse>(`/api/customers/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/customers/${id}`, { method: 'DELETE' }),
  },

  pestDisease: {
    list: (seasonId?: number) => apiRequest<PestDiseaseLogResponse[]>(`/api/pest-disease-logs${seasonId ? `?seasonId=${seasonId}` : ''}`),
    get: (id: number) => apiRequest<PestDiseaseLogResponse>(`/api/pest-disease-logs/${id}`),
    create: (data: Record<string, unknown>) => apiRequest<PestDiseaseLogResponse>('/api/pest-disease-logs', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Record<string, unknown>) => apiRequest<PestDiseaseLogResponse>(`/api/pest-disease-logs/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/pest-disease-logs/${id}`, { method: 'DELETE' }),
  },

  varietyTrials: {
    list: (seasonId?: number) => apiRequest<VarietyTrialResponse[]>(`/api/variety-trials${seasonId ? `?seasonId=${seasonId}` : ''}`),
    get: (id: number) => apiRequest<VarietyTrialResponse>(`/api/variety-trials/${id}`),
    create: (data: Record<string, unknown>) => apiRequest<VarietyTrialResponse>('/api/variety-trials', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Record<string, unknown>) => apiRequest<VarietyTrialResponse>(`/api/variety-trials/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/variety-trials/${id}`, { method: 'DELETE' }),
  },

  bouquetRecipes: {
    list: () => apiRequest<BouquetRecipeResponse[]>('/api/bouquet-recipes'),
    get: (id: number) => apiRequest<BouquetRecipeResponse>(`/api/bouquet-recipes/${id}`),
    create: (data: Record<string, unknown>) => apiRequest<BouquetRecipeResponse>('/api/bouquet-recipes', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Record<string, unknown>) => apiRequest<BouquetRecipeResponse>(`/api/bouquet-recipes/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/bouquet-recipes/${id}`, { method: 'DELETE' }),
  },

  successionSchedules: {
    list: (seasonId?: number) => apiRequest<SuccessionScheduleResponse[]>(`/api/succession-schedules${seasonId ? `?seasonId=${seasonId}` : ''}`),
    get: (id: number) => apiRequest<SuccessionScheduleResponse>(`/api/succession-schedules/${id}`),
    create: (data: Record<string, unknown>) => apiRequest<SuccessionScheduleResponse>('/api/succession-schedules', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Record<string, unknown>) => apiRequest<SuccessionScheduleResponse>(`/api/succession-schedules/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/succession-schedules/${id}`, { method: 'DELETE' }),
    generateTasks: (id: number) => apiRequest<number[]>(`/api/succession-schedules/${id}/generate-tasks`, { method: 'POST' }),
  },

  productionTargets: {
    list: (seasonId?: number) => apiRequest<ProductionTargetResponse[]>(`/api/production-targets${seasonId ? `?seasonId=${seasonId}` : ''}`),
    get: (id: number) => apiRequest<ProductionTargetResponse>(`/api/production-targets/${id}`),
    create: (data: Record<string, unknown>) => apiRequest<ProductionTargetResponse>('/api/production-targets', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: Record<string, unknown>) => apiRequest<ProductionTargetResponse>(`/api/production-targets/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/production-targets/${id}`, { method: 'DELETE' }),
    forecast: (id: number) => apiRequest<ProductionForecastResponse>(`/api/production-targets/${id}/forecast`),
  },

  market: {
    listings: () => apiRequest<ListingResponse[]>('/api/market/listings'),
    myListings: () => apiRequest<ListingResponse[]>('/api/market/listings/mine'),
    getListing: (id: number) => apiRequest<ListingResponse>(`/api/market/listings/${id}`),
    createListing: (data: Record<string, unknown>) => apiRequest<ListingResponse>('/api/market/listings', { method: 'POST', body: JSON.stringify(data) }),
    updateListing: (id: number, data: Record<string, unknown>) => apiRequest<ListingResponse>(`/api/market/listings/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    deleteListing: (id: number) => apiRequest<void>(`/api/market/listings/${id}`, { method: 'DELETE' }),
    placeOrder: (data: Record<string, unknown>) => apiRequest<MarketOrderResponse>('/api/market/orders', { method: 'POST', body: JSON.stringify(data) }),
    myOrders: () => apiRequest<MarketOrderResponse[]>('/api/market/orders/mine'),
    incomingOrders: () => apiRequest<MarketOrderResponse[]>('/api/market/orders/incoming'),
    getOrder: (id: number) => apiRequest<MarketOrderResponse>(`/api/market/orders/${id}`),
    updateOrderStatus: (id: number, status: string) => apiRequest<MarketOrderResponse>(`/api/market/orders/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) }),
  },

  analytics: {
    seasonSummaries: () => apiRequest<SeasonSummaryResponse[]>('/api/analytics/seasons'),
    speciesComparison: (speciesId: number) => apiRequest<SpeciesComparisonResponse>(`/api/analytics/species/${speciesId}/compare`),
    yieldPerBed: (seasonId?: number) => apiRequest<YieldPerBedResponse[]>(`/api/analytics/yield-per-bed${seasonId ? `?seasonId=${seasonId}` : ''}`),
  },

  organizations: {
    list: () => apiRequest<OrganizationResponse[]>('/api/organizations'),
    create: (data: { name: string; emoji?: string }) =>
      apiRequest<OrganizationResponse>('/api/organizations', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: number, data: { name?: string; emoji?: string }) =>
      apiRequest<OrganizationResponse>(`/api/organizations/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: number) => apiRequest<void>(`/api/organizations/${id}`, { method: 'DELETE' }),
    members: (id: number) => apiRequest<OrgMemberResponse[]>(`/api/organizations/${id}/members`),
    invite: (id: number, email: string) =>
      apiRequest<OrgInviteResponse>(`/api/organizations/${id}/invite`, { method: 'POST', body: JSON.stringify({ email }) }),
    removeMember: (orgId: number, userId: number) =>
      apiRequest<void>(`/api/organizations/${orgId}/members/${userId}`, { method: 'DELETE' }),
  },

  invites: {
    pending: () => apiRequest<OrgInviteResponse[]>('/api/invites'),
    accept: (id: number) => apiRequest<OrganizationResponse>(`/api/invites/${id}/accept`, { method: 'POST' }),
    decline: (id: number) => apiRequest<void>(`/api/invites/${id}/decline`, { method: 'POST' }),
  },
}

export async function downloadDataExport(): Promise<void> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch('/api/users/me/export', { headers })
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

  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'verdant-data-export.json'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
