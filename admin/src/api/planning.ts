import { apiRequest } from './client'

export type StartingUnit = 'SEED' | 'PLUG' | 'BULB' | 'TUBER' | 'PLANT'
export interface LifecycleStep {
  key: string
  name: string
  activityType: string
  daysBeforeHarvest: number | null
  quantityBasis: 'START' | 'PLANT' | 'OUTPUT'
}
export interface SharedSchedule {
  key: string
  name: string
  priority: number
  revision: number
  autoMatch: boolean
  useSpeciesTiming: boolean
  version: string
  climate: string
  description: string
  reviewRequired: boolean
  harvestMonths: number[]
  plantingMonths: number[]
  sources: { title: string; url: string }[]
  genera: string[]
  scientificPrefixes: string[]
  plantTypes: string[]
  startingUnits: StartingUnit[]
  profile: {
    startingUnit: StartingUnit
    sellableUnit: 'STEM' | 'FLOWER'
    outputPerPlant: number | null
    establishmentPercent: number | null
    steps: LifecycleStep[]
  }
}
export interface PlanningSpecies {
  id: number
  name: string
  scientificName: string | null
  startingUnit: StartingUnit
  assignedScheduleKey: string | null
  resolvedScheduleKey: string
  reviewRequired: boolean
  workflowTemplateId: number | null
}
export interface SharedGroup { id: number; name: string; speciesIds: number[] }

const base = '/api/admin/planning'
const json = (method: string, data: unknown) => ({ method, body: JSON.stringify(data) })
export const planning = {
  groups: () => apiRequest<SharedGroup[]>(`${base}/groups`),
  createGroup: (name: string) => apiRequest<SharedGroup>(`${base}/groups`, json('POST', { name })),
  renameGroup: (id: number, name: string) => apiRequest<SharedGroup>(`${base}/groups/${id}`, json('PUT', { name })),
  deleteGroup: (id: number) => apiRequest<void>(`${base}/groups/${id}`, { method: 'DELETE' }),
  addMembers: (id: number, speciesIds: number[]) => apiRequest<void>(`${base}/groups/${id}/members`, json('POST', { speciesIds })),
  removeMember: (id: number, speciesId: number) => apiRequest<void>(`${base}/groups/${id}/members/${speciesId}`, { method: 'DELETE' }),
  species: () => apiRequest<PlanningSpecies[]>(`${base}/species`),
  assign: (speciesIds: number[], scheduleKey: string | null) => apiRequest<void>(`${base}/assignments`, json('PUT', { speciesIds, scheduleKey })),
  schedules: () => apiRequest<SharedSchedule[]>(`${base}/schedules`),
  schedule: (key: string) => apiRequest<SharedSchedule>(`${base}/schedules/${encodeURIComponent(key)}`),
  createSchedule: (schedule: SharedSchedule) => apiRequest<SharedSchedule>(`${base}/schedules`, json('POST', schedule)),
  updateSchedule: (schedule: SharedSchedule) => apiRequest<SharedSchedule>(`${base}/schedules/${encodeURIComponent(schedule.key)}`, json('PUT', schedule)),
  deleteSchedule: (key: string, revision: number) => apiRequest<void>(`${base}/schedules/${encodeURIComponent(key)}?revision=${revision}`, { method: 'DELETE' }),
}

export const inputClass = 'w-full border border-[#E9E9E7] rounded-md px-3 py-2 text-sm bg-[#FBFBFA]'
export const buttonClass = 'border border-[#E9E9E7] rounded-md px-3 py-2 text-sm hover:bg-[#F0F0EE] disabled:opacity-40 disabled:cursor-not-allowed'
export const linkClass = 'text-[#2EAADC] hover:underline'
export const units: StartingUnit[] = ['SEED', 'PLUG', 'BULB', 'TUBER', 'PLANT']
export const activities = ['PURCHASE', 'SOW', 'POT_UP', 'PLANT', 'PINCH', 'SUPPORT', 'WATER', 'FERTILIZE', 'HARVEST', 'TODO']
