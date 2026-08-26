import type { MaintenanceRuleResponse } from '../api/client'

export const MAINTENANCE_ACTIVITIES = [
  'WATER', 'WEED', 'FERTILIZE',
  'MOW', 'RAKE', 'PRUNE', 'EDGE', 'SWEEP', 'TOP_UP', 'CLEAN', 'INSPECT',
] as const

export type MaintenanceActivity = (typeof MAINTENANCE_ACTIVITIES)[number]
export type MaintenanceTarget = 'BED' | 'AREA'

/**
 * Mirrors MaintenanceActivity's `targets` set in the backend. Sending an
 * activity the server rejects for that target is a 400, so the pickers must
 * filter by this rather than offering everything.
 */
const BED_ACTIVITIES: MaintenanceActivity[] = ['WATER', 'WEED', 'FERTILIZE']

export function activitiesForTarget(target: MaintenanceTarget): MaintenanceActivity[] {
  return target === 'BED'
    ? [...BED_ACTIVITIES]
    : MAINTENANCE_ACTIVITIES.filter((a) => a !== 'FERTILIZE')
}

export function maintenanceActivityLabelSv(activity: string): string {
  switch (activity) {
    case 'WATER': return 'Vattna'
    case 'WEED': return 'Rensa ogräs'
    case 'FERTILIZE': return 'Gödsla'
    case 'MOW': return 'Klippa gräs'
    case 'RAKE': return 'Kratta'
    case 'PRUNE': return 'Beskära'
    case 'EDGE': return 'Kantskära'
    case 'SWEEP': return 'Sopa'
    case 'TOP_UP': return 'Fylla på'
    case 'CLEAN': return 'Rensa'
    case 'INSPECT': return 'Inspektera'
    default: return activity
  }
}

export type DueState =
  | { kind: 'inactive' }
  | { kind: 'overdue'; days: number }
  | { kind: 'due' }
  | { kind: 'upcoming'; days: number }

/** Whole days between two ISO yyyy-mm-dd strings, ignoring time and zone. */
function daysBetween(fromIso: string, toIso: string): number {
  const from = Date.parse(`${fromIso}T00:00:00Z`)
  const to = Date.parse(`${toIso}T00:00:00Z`)
  return Math.round((to - from) / 86_400_000)
}

/**
 * `nextDueDate` is computed server-side; this only turns it into something to
 * render. An inactive rule is never overdue — pausing a rule must stop it
 * nagging, not freeze it in a red state.
 */
export function dueState(rule: MaintenanceRuleResponse, todayIso: string): DueState {
  if (!rule.active) return { kind: 'inactive' }
  const delta = daysBetween(todayIso, rule.nextDueDate)
  if (delta < 0) return { kind: 'overdue', days: -delta }
  if (delta === 0) return { kind: 'due' }
  return { kind: 'upcoming', days: delta }
}

/**
 * Swedish ordinal suffix keys off the last digit (1st/2nd → :a, 3rd+ → :e),
 * except the teens (11, 12) which always take :e regardless of last digit —
 * so 21:a (tjugoförsta) but 11:e (elfte).
 */
function ordinalSv(n: number): string {
  const lastTwo = n % 100
  const last = n % 10
  const useA = (last === 1 || last === 2) && lastTwo !== 11 && lastTwo !== 12
  return `${n}:${useA ? 'a' : 'e'}`
}

export function formatInterval(days: number): string {
  if (days === 1) return 'Varje dag'
  if (days === 7) return 'Varje vecka'
  if (days % 7 === 0) return `Var ${ordinalSv(days / 7)} vecka`
  return `Var ${ordinalSv(days)} dag`
}

const MONTHS_SV_SHORT = [
  'jan', 'feb', 'mar', 'apr', 'maj', 'jun',
  'jul', 'aug', 'sep', 'okt', 'nov', 'dec',
]

export function hasSeasonWindow(rule: MaintenanceRuleResponse): boolean {
  return rule.seasonStartMonth != null && rule.seasonStartDay != null &&
    rule.seasonEndMonth != null && rule.seasonEndDay != null
}

/**
 * Wrap-around windows (Nov 1 – Mar 31) need no special case: the pair is
 * rendered in the order stored, and "1 nov – 31 mar" reads correctly as a
 * winter season.
 */
export function formatSeasonWindow(rule: MaintenanceRuleResponse): string | null {
  if (!hasSeasonWindow(rule)) return null
  const start = `${rule.seasonStartDay} ${MONTHS_SV_SHORT[rule.seasonStartMonth! - 1]}`
  const end = `${rule.seasonEndDay} ${MONTHS_SV_SHORT[rule.seasonEndMonth! - 1]}`
  return `${start} – ${end}`
}
