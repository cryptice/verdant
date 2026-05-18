import type { PlantResponse } from '../api/client'

export const SOIL_TYPES = ['SANDY', 'LOAMY', 'CLAY', 'SILTY', 'PEATY', 'CHALKY'] as const
export const SUN_EXPOSURES = ['FULL_SUN', 'PARTIAL_SUN', 'PARTIAL_SHADE', 'FULL_SHADE'] as const
export const DRAINAGES = ['POOR', 'MODERATE', 'GOOD', 'SHARP'] as const
export const IRRIGATION_TYPES = ['DRIP', 'SPRINKLER', 'SOAKER_HOSE', 'MANUAL', 'NONE'] as const
export const PROTECTIONS = ['OPEN_FIELD', 'ROW_COVER', 'LOW_TUNNEL', 'HIGH_TUNNEL', 'GREENHOUSE', 'COLDFRAME'] as const

export function bedEventLabelSv(type: string): string {
  switch (type) {
    case 'WATERED': return 'Vattnade'
    case 'WEEDED': return 'Rensade ogräs'
    case 'APPLIED_SUPPLY': return 'Applicerade material'
    case 'NOTE': return 'Anteckning'
    default: return type[0] + type.slice(1).toLowerCase()
  }
}

export type PlantGroup = {
  key: string
  speciesId: number | null
  speciesName: string
  status: string
  plantedDate: string | null
  plants: PlantResponse[]
  totalSeeds: number
}

export function groupPlants(plants: PlantResponse[]): PlantGroup[] {
  const map = new Map<string, PlantGroup>()
  for (const p of plants) {
    const key = `${p.speciesId ?? 'null'}|${p.status}|${p.plantedDate ?? 'null'}`
    const existing = map.get(key)
    if (existing) {
      existing.plants.push(p)
      existing.totalSeeds += p.seedCount ?? 1
    } else {
      map.set(key, {
        key,
        speciesId: p.speciesId ?? null,
        speciesName: p.speciesName ?? p.name,
        status: p.status,
        plantedDate: p.plantedDate ?? null,
        plants: [p],
        totalSeeds: p.seedCount ?? 1,
      })
    }
  }
  return Array.from(map.values())
}
