import { apiRequest } from './client'

export type SellableUnit = 'STEM' | 'FLOWER'
export interface LifecycleStep {
  key: string; name: string; activityType: string; daysBeforeHarvest: number | null
  quantityBasis: 'START' | 'PLANT' | 'OUTPUT'
}
export interface ProductionProfile {
  sellableUnit: SellableUnit; outputPerPlant: number | null; establishmentPercent: number | null
  startingUnit: string; steps: LifecycleStep[]
}
export interface PlanningSpecies {
  speciesId: number; speciesName: string; profile: ProductionProfile; customized: boolean
}
export interface HarvestPlanRequest {
  seasonId: number; speciesId?: number; groupId?: number; quantity: number
  sellableUnit: SellableUnit; harvestDate: string; allocations: Record<number, number>
}
export interface PlannedStep {
  key: string; name: string; activityType: string; date: string; quantity: number; unit: string
}
export interface SpeciesAllocation {
  speciesId: number; speciesName: string; outputQuantity: number; plantsNeeded: number
  startingQuantity: number; profile: ProductionProfile; steps: PlannedStep[]
}
export interface HarvestPlanPreview {
  request: HarvestPlanRequest; allocations: SpeciesAllocation[]
  issues: { speciesId: number | null; message: string }[]
  unallocatedQuantity: number; canSave: boolean; fingerprint: string
}
export interface HarvestPlanResponse {
  id: number; status: string; snapshot: HarvestPlanPreview; createdAt: string
  tasks: { id: number; speciesId: number; stepKey: string; remainingCount: number; status: string }[]
}
export const harvestPlans = {
  list: (offset = 0) => apiRequest<HarvestPlanResponse[]>(`/api/harvest-plans?limit=50&offset=${offset}`),
  get: (id: number) => apiRequest<HarvestPlanResponse>(`/api/harvest-plans/${id}`),
  species: (speciesId?: number, groupId?: number) => apiRequest<PlanningSpecies[]>(`/api/harvest-plans/species?${speciesId ? `speciesId=${speciesId}` : `groupId=${groupId}`}`),
  profile: (id: number, profile: ProductionProfile) => apiRequest<PlanningSpecies>(`/api/harvest-plans/species/${id}`, { method: 'PUT', body: JSON.stringify(profile) }),
  preview: (target: HarvestPlanRequest) => apiRequest<HarvestPlanPreview>('/api/harvest-plans/preview', { method: 'POST', body: JSON.stringify(target) }),
  create: (requestKey: string, preview: HarvestPlanPreview) => apiRequest<HarvestPlanResponse>('/api/harvest-plans', { method: 'POST', body: JSON.stringify({ requestKey, fingerprint: preview.fingerprint, target: preview.request }) }),
  complete: (id: number, taskId: number, expectedRemaining: number, processedCount: number) => apiRequest<HarvestPlanResponse>(`/api/harvest-plans/${id}/tasks/${taskId}/complete`, { method: 'POST', body: JSON.stringify({ expectedRemaining, processedCount }) }),
  cancel: (id: number) => apiRequest<HarvestPlanResponse>(`/api/harvest-plans/${id}/cancel`, { method: 'POST' }),
}
