import type { GardenAreaResponse } from '../api/client'
import { compareNaturalNames } from './bed'

export const AREA_CATEGORIES = [
  'WALKWAY', 'LAWN', 'HEDGE', 'COMPOST',
  'GREENHOUSE', 'WATER_FEATURE', 'STRUCTURE', 'OTHER',
] as const

export type AreaCategory = (typeof AREA_CATEGORIES)[number]

export function areaCategoryLabelSv(category: string): string {
  switch (category) {
    case 'WALKWAY': return 'Gång'
    case 'LAWN': return 'Gräsmatta'
    case 'HEDGE': return 'Häck'
    case 'COMPOST': return 'Kompost'
    case 'GREENHOUSE': return 'Växthus'
    case 'WATER_FEATURE': return 'Vatten'
    case 'STRUCTURE': return 'Byggnad'
    case 'OTHER': return 'Övrigt'
    default: return category
  }
}

/** Area event log labels. Past tense, matching `bedEventLabelSv`'s register. */
export function areaEventLabelSv(eventType: string): string {
  switch (eventType) {
    case 'WATER': return 'Vattnade'
    case 'WEED': return 'Rensade ogräs'
    case 'MOW': return 'Klippte gräs'
    case 'RAKE': return 'Krattade'
    case 'PRUNE': return 'Beskar'
    case 'EDGE': return 'Kantskar'
    case 'SWEEP': return 'Sopade'
    case 'TOP_UP': return 'Fyllde på'
    case 'CLEAN': return 'Rensade'
    case 'INSPECT': return 'Inspekterade'
    case 'NOTE': return 'Anteckning'
    default: return eventType[0] + eventType.slice(1).toLowerCase()
  }
}

/** Sort areas within a garden by name, digit runs compared numerically. */
export function sortAreasByNaturalName<T extends GardenAreaResponse>(areas: readonly T[]): T[] {
  return areas.slice().sort((a, b) => compareNaturalNames(a.name, b.name))
}
