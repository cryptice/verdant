# Garden Areas — Web Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface garden areas ("Platser") and recurring maintenance rules in the React web app, against the backend API as it actually shipped.

**Architecture:** Areas get a detail page and a create/edit form modelled on the existing bed pages, listed under their garden. Maintenance rules live in ONE shared component used by both the area detail page and the bed detail page — that shared component is what makes "beds get rules too" nearly free. All derived state (due/overdue, season window text) is computed in pure `lib/` helpers so it is unit-testable without rendering.

**Tech Stack:** React 19, TypeScript, Vite, TanStack Query v5, react-i18next, Tailwind v4, Vitest + Testing Library.

**Spec:** `docs/plans/2026-08-26-garden-areas-design.md`
**Backend plan (shipped):** `docs/plans/2026-08-26-garden-areas-backend-plan.md`

## Global Constraints

- **Write against the API as shipped, not as the design doc describes it.** The shapes below were read out of the merged backend. Where this plan and the design doc disagree, this plan is right.
- Swedish is the primary UI language. Every user-facing string goes through `react-i18next`, and `i18n/sv.json` and `i18n/en.json` must be kept in lockstep — same keys, same nesting, both files edited in the same commit.
- The UI label for a `GardenArea` is **"Plats" / "Platser"**. The word `Location` never appears in user-facing copy (`TrayLocation` already owns "plats" in a different sense — see the naming note below).
- API calls go through `api` in `web/src/api/client.ts`. Never call `fetch` directly from a component.
- Data fetching is TanStack Query. Mutations invalidate the query keys they affect.
- Follow the existing `components/faltet` primitives (`Masthead`, `Chip`, `Stat`, `Field`, `PhotoPlaceholder`, `Rule`) and `components/Dialog`, `components/Snackbar` + `useSnackbar`. Do not introduce a new UI kit, a form library, or a date library.
- No map/polygon work. `boundaryJson` is carried by the API but no web client renders it. Do not add a map dependency.
- Tests are Vitest + Testing Library, colocated as `*.test.ts` / `*.test.tsx` next to the file under test — see `components/faltet/Chip.test.tsx` and `pages/Supplies.test.ts`.

### Naming collision to be careful about

`TrayLocation` already exists in this app as `pages/TrayLocations.tsx` / `pages/TrayLocationDetail.tsx`, routed at `/tray-locations`, and its Swedish label is also placement-flavoured. Areas are a different concept: a tray location is where portable trays sit; an area is a fixed part of a garden. Keep the code names distinct (`area*` vs `trayLocation*`) and route areas under `/area/:id`, never `/locations`.

### The shipped API

```
GET  POST        /api/gardens/{gardenId}/areas
GET  PUT  DELETE /api/areas/{id}
GET  POST        /api/areas/{id}/events        ?limit= on GET
GET  POST        /api/areas/{id}/photos
DELETE           /api/areas/{id}/photos/{photoId}

GET              /api/maintenance-rules?bedId=&areaId=    (at most one; both = 400)
POST             /api/maintenance-rules
PUT  DELETE      /api/maintenance-rules/{id}
```

Response shapes, verbatim from the merged backend:

- `GardenAreaResponse`: `id, gardenId, gardenName, name, description, category, boundaryJson, sizeSqm, createdAt, updatedAt`
- `GardenAreaEventResponse`: `id, gardenAreaId, eventType, eventDate, notes, createdAt`
- `GardenAreaPhotoResponse`: `id, gardenAreaId, photoUrl, reason, description, capturedAt, createdAt`
- `MaintenanceRuleResponse`: `id, bedId, bedName, gardenAreaId, gardenAreaName, activityType, intervalDays, anchorDate, seasonStartMonth, seasonStartDay, seasonEndMonth, seasonEndDay, active, notes, lastDoneDate, nextDueDate, createdAt, updatedAt`
- `ScheduledTaskResponse` additionally carries `gardenAreaId, gardenAreaName, maintenanceRuleId`

Request shapes:

- `CreateGardenAreaRequest`: `name` (required), `description?`, `category` (required), `boundaryJson?`, `sizeSqm?`
- `UpdateGardenAreaRequest`: all optional; **omitted fields keep their value** (`?:` coalescing server-side)
- `CreateGardenAreaEventRequest`: `activityType` (required), `eventDate?`, `notes?`
- `CreateGardenAreaPhotoRequest`: `imageBase64` (required), `reason` (required), `description?`, `capturedAt?`
- `CreateMaintenanceRuleRequest`: exactly one of `bedId`/`gardenAreaId`, `activityType`, `intervalDays` (≥1), `anchorDate?`, four `season*?`, `notes?`
- `UpdateMaintenanceRuleRequest`: all optional, plus **`clearSeasonWindow: boolean`**

### Three server behaviours the UI must respect

1. **`clearSeasonWindow` is the only way to remove a season window.** Sending `seasonStartMonth: null` does nothing — the server cannot distinguish omitted from null. Sending `clearSeasonWindow: true` alongside any `season*` value is a **400**.
2. **Rule-backed tasks reject `activityType` and `targetCount` edits.** A task with `maintenanceRuleId != null` will 400 if the edit form sends a changed activity or count. The existing `TaskForm` sends both on every save; unchanged echoes are accepted, changes are not. Task 8 handles this.
3. **Deleting a pending maintenance task does not dismiss it.** The scheduler recreates it the next morning, because the underlying work is still undone. Never label that action "dismiss" or "skip" — it is "delete this occurrence", and the copy must not promise otherwise.

### Enums

`GardenAreaCategory`: `WALKWAY, LAWN, HEDGE, COMPOST, GREENHOUSE, WATER_FEATURE, STRUCTURE, OTHER`

`MaintenanceActivity`, with which targets accept each:

| Activity | Beds | Areas |
|---|---|---|
| `WATER` | yes | yes |
| `WEED` | yes | yes |
| `FERTILIZE` | yes | no |
| `MOW`, `RAKE`, `PRUNE`, `EDGE`, `SWEEP`, `TOP_UP`, `CLEAN`, `INSPECT` | no | yes |

Area event types are those activity names plus `NOTE`.

### Running tests

```bash
cd web && npm test                                    # full suite
cd web && npx vitest run src/lib/maintenance.test.ts   # one file
```
Or in Docker without a local Node: `./scripts/run-tests.sh web`

---

### Task 1: API types, client namespaces, and area helpers

**Files:**
- Modify: `web/src/api/client.ts`
- Create: `web/src/lib/area.ts`
- Test: `web/src/lib/area.test.ts`

**Interfaces:**
- Consumes: `apiRequest` and the existing `BedPhotoReason` type in `api/client.ts`.
- Produces: exported types `GardenAreaResponse`, `GardenAreaEventResponse`, `GardenAreaPhotoResponse`, `MaintenanceRuleResponse`; `api.areas.*` and `api.maintenanceRules.*` namespaces; from `lib/area.ts`: `AREA_CATEGORIES`, `areaCategoryLabelSv(category)`, `areaEventLabelSv(eventType)`, `sortAreasByNaturalName(areas)`.

- [ ] **Step 1: Write the failing test**

Create `web/src/lib/area.test.ts`:

```ts
import { describe, it, expect } from 'vitest'
import {
  AREA_CATEGORIES,
  areaCategoryLabelSv,
  areaEventLabelSv,
  sortAreasByNaturalName,
} from './area'
import type { GardenAreaResponse } from '../api/client'

function area(name: string, id = 1): GardenAreaResponse {
  return {
    id, gardenId: 1, gardenName: 'Trädgården', name,
    description: null, category: 'WALKWAY', boundaryJson: null, sizeSqm: null,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
  }
}

describe('AREA_CATEGORIES', () => {
  it('matches the backend enum exactly', () => {
    expect(AREA_CATEGORIES).toEqual([
      'WALKWAY', 'LAWN', 'HEDGE', 'COMPOST',
      'GREENHOUSE', 'WATER_FEATURE', 'STRUCTURE', 'OTHER',
    ])
  })

  it('has a Swedish label for every category', () => {
    for (const c of AREA_CATEGORIES) {
      const label = areaCategoryLabelSv(c)
      expect(label).toBeTruthy()
      expect(label).not.toBe(c)
    }
  })
})

describe('areaEventLabelSv', () => {
  it('translates known activities', () => {
    expect(areaEventLabelSv('WEED')).toBe('Rensade ogräs')
    expect(areaEventLabelSv('MOW')).toBe('Klippte gräs')
    expect(areaEventLabelSv('NOTE')).toBe('Anteckning')
  })

  it('falls back to a readable form for anything unknown', () => {
    expect(areaEventLabelSv('SOMETHING_NEW')).toBe('Something_new')
  })
})

describe('sortAreasByNaturalName', () => {
  it('orders digit runs numerically, so #10 follows #9', () => {
    const sorted = sortAreasByNaturalName([
      area('Gång #10', 1), area('Gång #9', 2), area('Gång #1', 3),
    ])
    expect(sorted.map((a) => a.name)).toEqual(['Gång #1', 'Gång #9', 'Gång #10'])
  })

  it('does not mutate its input', () => {
    const input = [area('B', 1), area('A', 2)]
    sortAreasByNaturalName(input)
    expect(input.map((a) => a.name)).toEqual(['B', 'A'])
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd web && npx vitest run src/lib/area.test.ts`
Expected: FAIL — cannot resolve `./area`.

- [ ] **Step 3: Add the types to the API client**

In `web/src/api/client.ts`, in the Types section near `BedPhotoResponse`:

```ts
export type GardenAreaCategory =
  | 'WALKWAY' | 'LAWN' | 'HEDGE' | 'COMPOST'
  | 'GREENHOUSE' | 'WATER_FEATURE' | 'STRUCTURE' | 'OTHER'

export interface GardenAreaResponse {
  id: number
  gardenId: number
  gardenName: string | null
  name: string
  description: string | null
  category: string
  boundaryJson: string | null
  sizeSqm: number | null
  createdAt: string
  updatedAt: string
}

export interface GardenAreaEventResponse {
  id: number
  gardenAreaId: number
  eventType: string
  eventDate: string
  notes: string | null
  createdAt: string
}

export interface GardenAreaPhotoResponse {
  id: number
  gardenAreaId: number
  photoUrl: string
  reason: BedPhotoReason
  description: string | null
  capturedAt: string
  createdAt: string
}

export interface MaintenanceRuleResponse {
  id: number
  bedId: number | null
  bedName: string | null
  gardenAreaId: number | null
  gardenAreaName: string | null
  activityType: string
  intervalDays: number
  anchorDate: string | null
  seasonStartMonth: number | null
  seasonStartDay: number | null
  seasonEndMonth: number | null
  seasonEndDay: number | null
  active: boolean
  notes: string | null
  /** Derived server-side from the event log. Null when never done. */
  lastDoneDate: string | null
  /** Derived server-side: when the next task will be created. */
  nextDueDate: string
  createdAt: string
  updatedAt: string
}
```

Extend the existing `ScheduledTaskResponse` interface with:

```ts
  gardenAreaId?: number | null
  gardenAreaName?: string | null
  maintenanceRuleId?: number | null
```

- [ ] **Step 4: Add the client namespaces**

In the `api` object, after the `beds` namespace:

```ts
  areas: {
    listByGarden: (gardenId: number) =>
      apiRequest<GardenAreaResponse[]>(`/api/gardens/${gardenId}/areas`),
    get: (id: number) => apiRequest<GardenAreaResponse>(`/api/areas/${id}`),
    create: (
      gardenId: number,
      data: {
        name: string; category: string
        description?: string; boundaryJson?: string; sizeSqm?: number
      },
    ) =>
      apiRequest<GardenAreaResponse>(`/api/gardens/${gardenId}/areas`, {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    update: (
      id: number,
      data: {
        name?: string; category?: string
        description?: string; boundaryJson?: string; sizeSqm?: number
      },
    ) =>
      apiRequest<GardenAreaResponse>(`/api/areas/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
      }),
    delete: (id: number) => apiRequest<void>(`/api/areas/${id}`, { method: 'DELETE' }),

    events: (id: number, limit = 50) =>
      apiRequest<GardenAreaEventResponse[]>(`/api/areas/${id}/events?limit=${limit}`),
    logEvent: (
      id: number,
      data: { activityType: string; eventDate?: string; notes?: string },
    ) =>
      apiRequest<GardenAreaEventResponse>(`/api/areas/${id}/events`, {
        method: 'POST',
        body: JSON.stringify(data),
      }),

    photos: (id: number) => apiRequest<GardenAreaPhotoResponse[]>(`/api/areas/${id}/photos`),
    addPhoto: (
      id: number,
      data: { imageBase64: string; reason: BedPhotoReason; description?: string; capturedAt?: string },
    ) =>
      apiRequest<GardenAreaPhotoResponse>(`/api/areas/${id}/photos`, {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    deletePhoto: (id: number, photoId: number) =>
      apiRequest<void>(`/api/areas/${id}/photos/${photoId}`, { method: 'DELETE' }),
  },

  maintenanceRules: {
    // At most one filter — supplying both is a 400 server-side.
    list: (filter?: { bedId?: number; areaId?: number }) => {
      const qs = filter?.bedId != null ? `?bedId=${filter.bedId}`
        : filter?.areaId != null ? `?areaId=${filter.areaId}`
        : ''
      return apiRequest<MaintenanceRuleResponse[]>(`/api/maintenance-rules${qs}`)
    },
    create: (data: {
      bedId?: number; gardenAreaId?: number
      activityType: string; intervalDays: number; anchorDate?: string
      seasonStartMonth?: number; seasonStartDay?: number
      seasonEndMonth?: number; seasonEndDay?: number
      notes?: string
    }) =>
      apiRequest<MaintenanceRuleResponse>('/api/maintenance-rules', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    update: (id: number, data: {
      activityType?: string; intervalDays?: number; anchorDate?: string
      seasonStartMonth?: number; seasonStartDay?: number
      seasonEndMonth?: number; seasonEndDay?: number
      /** The ONLY way to remove a season window. Cannot be combined with season* values. */
      clearSeasonWindow?: boolean
      active?: boolean; notes?: string
    }) =>
      apiRequest<MaintenanceRuleResponse>(`/api/maintenance-rules/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
      }),
    delete: (id: number) =>
      apiRequest<void>(`/api/maintenance-rules/${id}`, { method: 'DELETE' }),
  },
```

- [ ] **Step 5: Write `lib/area.ts`**

```ts
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
```

`compareNaturalNames` is already exported from `web/src/lib/bed.ts` — reuse it rather than writing a second comparator.

- [ ] **Step 6: Run test to verify it passes**

Run: `cd web && npx vitest run src/lib/area.test.ts`
Expected: PASS, 6 tests.

- [ ] **Step 7: Typecheck**

Run: `cd web && npx tsc -b`
Expected: no errors. This is the real check on the client additions, which have no direct test.

- [ ] **Step 8: Commit**

```bash
git add web/src/api/client.ts web/src/lib/area.ts web/src/lib/area.test.ts
git commit -m "feat(web): garden area types, API client namespaces, and label helpers"
```

---

### Task 2: Maintenance presentation helpers

Pure functions, no rendering. This is where the fiddly derived state lives, so it is worth testing hard.

**Files:**
- Create: `web/src/lib/maintenance.ts`
- Test: `web/src/lib/maintenance.test.ts`

**Interfaces:**
- Consumes: `MaintenanceRuleResponse` from `api/client`.
- Produces: `MAINTENANCE_ACTIVITIES`, `activitiesForTarget(target)`, `maintenanceActivityLabelSv(activity)`, `dueState(rule, today)`, `formatInterval(days)`, `formatSeasonWindow(rule)`, `hasSeasonWindow(rule)`.

- [ ] **Step 1: Write the failing test**

Create `web/src/lib/maintenance.test.ts`:

```ts
import { describe, it, expect } from 'vitest'
import {
  MAINTENANCE_ACTIVITIES,
  activitiesForTarget,
  maintenanceActivityLabelSv,
  dueState,
  formatInterval,
  formatSeasonWindow,
  hasSeasonWindow,
} from './maintenance'
import type { MaintenanceRuleResponse } from '../api/client'

function rule(over: Partial<MaintenanceRuleResponse> = {}): MaintenanceRuleResponse {
  return {
    id: 1, bedId: null, bedName: null, gardenAreaId: 5, gardenAreaName: 'Gången',
    activityType: 'WEED', intervalDays: 21, anchorDate: null,
    seasonStartMonth: null, seasonStartDay: null,
    seasonEndMonth: null, seasonEndDay: null,
    active: true, notes: null,
    lastDoneDate: null, nextDueDate: '2026-06-25',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

describe('activitiesForTarget', () => {
  it('gives beds exactly the three activities the backend accepts', () => {
    expect(activitiesForTarget('BED')).toEqual(['WATER', 'WEED', 'FERTILIZE'])
  })

  it('excludes FERTILIZE from areas and includes the area-only work', () => {
    const areaActivities = activitiesForTarget('AREA')
    expect(areaActivities).not.toContain('FERTILIZE')
    expect(areaActivities).toContain('MOW')
    expect(areaActivities).toContain('WEED')
  })

  it('covers every activity across the two targets', () => {
    const union = new Set([...activitiesForTarget('BED'), ...activitiesForTarget('AREA')])
    expect([...union].sort()).toEqual([...MAINTENANCE_ACTIVITIES].sort())
  })

  it('has a Swedish label for every activity', () => {
    for (const a of MAINTENANCE_ACTIVITIES) {
      expect(maintenanceActivityLabelSv(a)).not.toBe(a)
    }
  })
})

describe('dueState', () => {
  const today = '2026-06-25'

  it('reports overdue when the due date has passed', () => {
    const s = dueState(rule({ nextDueDate: '2026-06-20' }), today)
    expect(s.kind).toBe('overdue')
    expect(s.days).toBe(5)
  })

  it('reports due today on the due date itself', () => {
    expect(dueState(rule({ nextDueDate: today }), today).kind).toBe('due')
  })

  it('reports upcoming with days remaining', () => {
    const s = dueState(rule({ nextDueDate: '2026-07-02' }), today)
    expect(s.kind).toBe('upcoming')
    expect(s.days).toBe(7)
  })

  it('reports inactive regardless of date, so a paused rule never nags', () => {
    expect(dueState(rule({ active: false, nextDueDate: '2026-01-01' }), today).kind)
      .toBe('inactive')
  })
})

describe('formatInterval', () => {
  it('uses days below a fortnight', () => {
    expect(formatInterval(1)).toBe('Varje dag')
    expect(formatInterval(3)).toBe('Var 3:e dag')
  })

  it('uses weeks for exact multiples', () => {
    expect(formatInterval(7)).toBe('Varje vecka')
    expect(formatInterval(21)).toBe('Var 3:e vecka')
  })

  it('falls back to days when not a whole number of weeks', () => {
    expect(formatInterval(10)).toBe('Var 10:e dag')
  })
})

describe('season windows', () => {
  it('detects presence from all four bounds', () => {
    expect(hasSeasonWindow(rule())).toBe(false)
    expect(hasSeasonWindow(rule({
      seasonStartMonth: 4, seasonStartDay: 1, seasonEndMonth: 10, seasonEndDay: 15,
    }))).toBe(true)
  })

  it('formats a summer window', () => {
    expect(formatSeasonWindow(rule({
      seasonStartMonth: 4, seasonStartDay: 1, seasonEndMonth: 10, seasonEndDay: 15,
    }))).toBe('1 apr – 15 okt')
  })

  it('formats a wrap-around winter window without special-casing it', () => {
    expect(formatSeasonWindow(rule({
      seasonStartMonth: 11, seasonStartDay: 1, seasonEndMonth: 3, seasonEndDay: 31,
    }))).toBe('1 nov – 31 mar')
  })

  it('returns null when there is no window', () => {
    expect(formatSeasonWindow(rule())).toBeNull()
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd web && npx vitest run src/lib/maintenance.test.ts`
Expected: FAIL — cannot resolve `./maintenance`.

- [ ] **Step 3: Write the implementation**

Create `web/src/lib/maintenance.ts`:

```ts
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

function ordinalSv(n: number): string {
  // Swedish ordinals for intervals: 1:a, 2:a, 3:e, 4:e …
  return n === 1 || n === 2 ? `${n}:a` : `${n}:e`
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd web && npx vitest run src/lib/maintenance.test.ts`
Expected: PASS, 15 tests.

- [ ] **Step 5: Commit**

```bash
git add web/src/lib/maintenance.ts web/src/lib/maintenance.test.ts
git commit -m "feat(web): maintenance rule presentation helpers"
```

---

### Task 3: The shared MaintenanceRules component

Used by BOTH the area detail page and the bed detail page. Building it once here is what makes Task 7 (beds get rules) small.

**Files:**
- Create: `web/src/components/maintenance/MaintenanceRules.tsx`
- Create: `web/src/components/maintenance/MaintenanceRuleDialog.tsx`
- Test: `web/src/components/maintenance/MaintenanceRules.test.tsx`
- Modify: `web/src/i18n/sv.json`, `web/src/i18n/en.json`

**Interfaces:**
- Consumes: `api.maintenanceRules.*` (Task 1); everything from `lib/maintenance` (Task 2); `Dialog`, `Chip`, `useSnackbar`.
- Produces: `<MaintenanceRules target={{ kind: 'AREA' | 'BED', id: number }} />` — self-contained: fetches its own rules, renders them, and owns create/edit/delete.

- [ ] **Step 1: Write the failing test**

Create `web/src/components/maintenance/MaintenanceRules.test.tsx`:

```tsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MaintenanceRules } from './MaintenanceRules'
import type { MaintenanceRuleResponse } from '../../api/client'

vi.mock('../../api/client', async (orig) => {
  const actual = await orig<typeof import('../../api/client')>()
  return {
    ...actual,
    api: {
      ...actual.api,
      maintenanceRules: { list: vi.fn(), create: vi.fn(), update: vi.fn(), delete: vi.fn() },
    },
  }
})

// i18n `t` is identity-ish in tests; assert on Swedish helper output instead.
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

import { api } from '../../api/client'

function rule(over: Partial<MaintenanceRuleResponse> = {}): MaintenanceRuleResponse {
  return {
    id: 1, bedId: null, bedName: null, gardenAreaId: 5, gardenAreaName: 'Gången',
    activityType: 'WEED', intervalDays: 21, anchorDate: null,
    seasonStartMonth: null, seasonStartDay: null,
    seasonEndMonth: null, seasonEndDay: null,
    active: true, notes: null,
    lastDoneDate: null, nextDueDate: '2099-01-01',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

function renderRules() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <MaintenanceRules target={{ kind: 'AREA', id: 5 }} />
    </QueryClientProvider>,
  )
}

beforeEach(() => vi.clearAllMocks())

describe('MaintenanceRules', () => {
  it('renders an activity and its interval in Swedish', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([rule()])
    renderRules()
    expect(await screen.findByText('Rensa ogräs')).toBeInTheDocument()
    expect(screen.getByText(/Var 3:e vecka/)).toBeInTheDocument()
  })

  it('shows the season window when the rule has one', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([
      rule({ seasonStartMonth: 4, seasonStartDay: 1, seasonEndMonth: 10, seasonEndDay: 15 }),
    ])
    renderRules()
    expect(await screen.findByText(/1 apr – 15 okt/)).toBeInTheDocument()
  })

  it('marks an overdue rule', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([
      rule({ nextDueDate: '2020-01-01' }),
    ])
    renderRules()
    expect(await screen.findByTestId('rule-due-1')).toHaveAttribute('data-due-kind', 'overdue')
  })

  it('marks a paused rule inactive rather than overdue', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([
      rule({ active: false, nextDueDate: '2020-01-01' }),
    ])
    renderRules()
    expect(await screen.findByTestId('rule-due-1')).toHaveAttribute('data-due-kind', 'inactive')
  })

  it('queries scoped to the area, never with both filters', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([])
    renderRules()
    await screen.findByTestId('maintenance-rules')
    expect(api.maintenanceRules.list).toHaveBeenCalledWith({ areaId: 5 })
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd web && npx vitest run src/components/maintenance/MaintenanceRules.test.tsx`
Expected: FAIL — cannot resolve `./MaintenanceRules`.

- [ ] **Step 3: Write `MaintenanceRules.tsx`**

The list container. It must:

- Take `target: { kind: 'AREA' | 'BED'; id: number }`.
- Query `['maintenance-rules', target.kind, target.id]` calling `api.maintenanceRules.list(target.kind === 'AREA' ? { areaId: target.id } : { bedId: target.id })` — **never both filters**, which is a 400.
- Render a wrapper with `data-testid="maintenance-rules"`.
- For each rule render: `maintenanceActivityLabelSv(rule.activityType)`, `formatInterval(rule.intervalDays)`, `formatSeasonWindow(rule)` when non-null, and a due badge with `data-testid={`rule-due-${rule.id}`}` and `data-due-kind={dueState(rule, todayIso).kind}`.
- Compute `todayIso` once as `new Date().toISOString().slice(0, 10)`.
- Style the due badge by kind using the existing CSS custom properties: overdue → `var(--color-berry)`, due → `var(--color-mustard)`, upcoming → `var(--color-sage)`, inactive → muted. Follow how `TaskList.tsx` maps tones to `var(--color-*)`.
- Offer "Lägg till underhåll" opening `MaintenanceRuleDialog` in create mode, and per-rule edit and delete. Delete confirms through the existing `Dialog`.
- Invalidate `['maintenance-rules', target.kind, target.id]` after every mutation, and surface failures through `useSnackbar`.
- Render an empty state when there are no rules, explaining that nothing recurs until a rule is added — this is where "nothing is seeded" becomes visible to the user.

- [ ] **Step 4: Write `MaintenanceRuleDialog.tsx`**

The create/edit form, in a `Dialog`. It must:

- Offer activities from `activitiesForTarget(target.kind)` only — offering `FERTILIZE` on an area is a guaranteed 400.
- Take an interval in days, minimum 1, with `formatInterval` shown live as a hint so the user sees "Var 3:e vecka" while typing 21.
- Offer an optional "I did this last on…" anchor date, explained in the copy: leaving it blank makes the rule due immediately.
- Offer an optional season window as four inputs (start month/day, end month/day), all-or-none. Explain in the copy that a window may wrap the new year — Nov 1 – Mar 31 is a valid winter season, not an error.
- On **create**: post exactly one of `bedId`/`gardenAreaId` from the target.
- On **edit**, handle the season window carefully:
  - Window present before and after → send the four `season*` values.
  - Window present before, user cleared it → send `clearSeasonWindow: true` and **no** `season*` values. Sending both is a 400.
  - No window before, user added one → send the four values, `clearSeasonWindow` absent or false.
- Include an "Aktiv" toggle mapping to `active`, described as pausing the rule rather than deleting it.
- Disable submit while a required field is empty or the interval is below 1.

- [ ] **Step 5: Add the i18n keys**

Add a `maintenance` block to BOTH `web/src/i18n/sv.json` and `web/src/i18n/en.json`, same keys in both, covering: section title, add/edit/delete rule, activity, interval, anchor date and its "leave blank = due now" hint, season window and its wrap-around hint, active toggle, the empty state, and the delete confirmation. Follow the nesting style of the existing `bed` and `garden` blocks.

- [ ] **Step 6: Run test to verify it passes**

Run: `cd web && npx vitest run src/components/maintenance/MaintenanceRules.test.tsx`
Expected: PASS, 5 tests.

- [ ] **Step 7: Typecheck and full suite**

Run: `cd web && npx tsc -b && npm test`
Expected: no type errors; all tests pass.

- [ ] **Step 8: Commit**

```bash
git add web/src/components/maintenance web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat(web): shared maintenance rules component for beds and areas"
```

---

### Task 4: Area detail page

**Files:**
- Create: `web/src/pages/AreaDetail.tsx`
- Modify: `web/src/App.tsx` (route)
- Modify: `web/src/i18n/sv.json`, `web/src/i18n/en.json`

**Interfaces:**
- Consumes: `api.areas.get/events/logEvent` (Task 1), `lib/area` helpers (Task 1), `<MaintenanceRules>` (Task 3).
- Produces: route `/area/:id` rendering `<AreaDetail />`.

- [ ] **Step 1: Add the route**

In `web/src/App.tsx`, beside the existing `bed/:id` route:

```tsx
          <Route path="area/:id" element={<AreaDetail />} />
```

with the matching import. Place it directly after the `bed/:id` line so the file's grouping stays legible.

- [ ] **Step 2: Build the page**

Model it on `web/src/pages/BedDetail.tsx` — read that file first; it is the pattern for masthead, meta cells, section headers, and snackbar wiring. `AreaDetail` is simpler because areas have no plants and no harvest stats.

It must contain:

- `useParams` → `areaId`, and `useQuery(['area', areaId], () => api.areas.get(areaId))`.
- A `Masthead` showing the area name, with `areaCategoryLabelSv(area.category)` as a `Chip`, and a breadcrumb link back to its garden via `area.gardenId` / `area.gardenName`.
- Meta: category, size (`sizeSqm` with `m²`, or `—`), description.
- **`<MaintenanceRules target={{ kind: 'AREA', id: areaId }} />`** — the shared component from Task 3.
- A "Logga underhåll" action opening a small dialog: pick an activity from `activitiesForTarget('AREA')` plus `NOTE`, an optional date defaulting to today, optional notes. Posts `api.areas.logEvent`. On success invalidate `['area-events', areaId]` **and** `['maintenance-rules', 'AREA', areaId]` — logging work moves the derived clock, so the rules section must refetch or it will show a stale next-due date.
- Event history from `useQuery(['area-events', areaId], () => api.areas.events(areaId, 20))`, rendered with `areaEventLabelSv` and the event date, newest first.
- A danger section with delete, confirming through `Dialog`, navigating back to the garden on success.

- [ ] **Step 3: Add the i18n keys**

Add an `area` block to both `sv.json` and `en.json`: masthead, meta labels, log-maintenance dialog copy, event history heading, empty state, delete confirmation. Same keys in both files.

- [ ] **Step 4: Verify**

Run: `cd web && npx tsc -b && npm test`
Expected: no type errors, suite green.

Then run the app (`cd web && npm run dev`), create an area through the API or the form from Task 5, and confirm: the page renders, logging maintenance appends to history, and the rules section's next-due date changes after logging.

- [ ] **Step 5: Commit**

```bash
git add web/src/pages/AreaDetail.tsx web/src/App.tsx web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat(web): area detail page with event history and maintenance rules"
```

---

### Task 5: Area photos

**Files:**
- Create: `web/src/components/area/AreaPhotosSection.tsx`
- Modify: `web/src/pages/AreaDetail.tsx`

**Interfaces:**
- Consumes: `api.areas.photos/addPhoto/deletePhoto` (Task 1), the existing `PhotoPicker`.
- Produces: `<AreaPhotosSection areaId={number} />`.

- [ ] **Step 1: Build the component**

Copy `web/src/components/bed/BedPhotosSection.tsx` and substitute throughout:

| From | To |
|---|---|
| `bedId` prop and variable | `areaId` |
| `api.beds.photos/addPhoto/deletePhoto` | `api.areas.photos/addPhoto/deletePhoto` |
| query key `['bed-photos', bedId]` | `['area-photos', areaId]` |
| `BedPhotoResponse` | `GardenAreaPhotoResponse` |

Everything else — the `PhotoPicker` integration, the base64 stripping (`photoDataUrl.replace(/^data:image\/\w+;base64,/, '')`), the reason picker, the lightbox `Dialog`, the delete confirmation — carries over unchanged. `BedPhotoReason` is shared between both, so its labels need no duplicate.

**The base64 stripping matters.** The API takes `imageBase64` (raw bytes, no data-URL prefix) and mints the URL server-side. It does **not** accept a URL — an earlier draft of the backend did, and that was a cross-tenant security hole. Do not add a "paste an image URL" affordance.

- [ ] **Step 2: Mount it**

Add `<AreaPhotosSection areaId={areaId} />` to `AreaDetail`, in the same position `BedDetail` places `<BedPhotosSection>`.

- [ ] **Step 3: Verify**

Run: `cd web && npx tsc -b && npm test`

Then in the running app: upload a photo to an area, confirm it renders, open the lightbox, delete it. Confirm the stored URL is a `storage.googleapis.com` path the server minted, not anything the client supplied.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/area web/src/pages/AreaDetail.tsx
git commit -m "feat(web): area photo gallery and upload"
```

---

### Task 6: Create and edit areas, and list them under their garden

**Files:**
- Create: `web/src/pages/AreaForm.tsx`
- Modify: `web/src/App.tsx` (route)
- Modify: `web/src/pages/GardenDetail.tsx`
- Modify: `web/src/i18n/sv.json`, `web/src/i18n/en.json`

**Interfaces:**
- Consumes: `api.areas.create/update/listByGarden`, `AREA_CATEGORIES`, `areaCategoryLabelSv`, `sortAreasByNaturalName`.
- Produces: route `/garden/:gardenId/area/new` rendering `<AreaForm />`; an areas section on `GardenDetail`.

- [ ] **Step 1: Add the route**

In `App.tsx`, beside `garden/:gardenId/bed/new`:

```tsx
          <Route path="garden/:gardenId/area/new" element={<AreaForm />} />
```

- [ ] **Step 2: Build the form**

Model on `web/src/pages/BedForm.tsx`. Fields: name (required), category (required — a `<select>` over `AREA_CATEGORIES` labelled with `areaCategoryLabelSv`), description (optional), size in m² (optional, positive).

`boundaryJson` is **not** a form field. The API carries it, but no web client draws polygons and this plan adds no map. Omit it from the request entirely.

On success, invalidate `['garden-areas', gardenId]` and navigate to the new area's detail page.

For editing, follow whichever pattern `BedDetail` uses for `BedEditDialog` — an inline dialog on the detail page is consistent with beds and avoids a second route. If you add `AreaEditDialog`, put it in `components/area/`.

- [ ] **Step 3: List areas on the garden page**

In `GardenDetail.tsx`, add below the existing beds section:

- `useQuery({ queryKey: ['garden-areas', gardenId], queryFn: () => api.areas.listByGarden(gardenId) })`
- A section header matching the beds one ("Platser"), with a "Ny plats" button routing to `garden/${gardenId}/area/new`.
- Rows sorted with `sortAreasByNaturalName`, each linking to `/area/${id}`, showing the name and its category chip.
- An empty state when the garden has no areas.

Mirror the beds section's markup so the two read as siblings — that visual equivalence is the point of areas being a first-class place.

- [ ] **Step 4: i18n**

Add the new keys to both `sv.json` and `en.json`: "Platser", "Ny plats", form labels, the empty state.

- [ ] **Step 5: Verify**

Run: `cd web && npx tsc -b && npm test`

In the app: create an area from a garden, confirm it appears in the garden's list and its detail page loads, then edit it and confirm the change persists.

- [ ] **Step 6: Commit**

```bash
git add web/src/pages/AreaForm.tsx web/src/pages/GardenDetail.tsx web/src/App.tsx \
        web/src/components/area web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat(web): create/edit areas and list them under their garden"
```

---

### Task 7: Beds get maintenance rules

The payoff for building Task 3 as a shared component: this task is one import and one line of JSX.

**Files:**
- Modify: `web/src/pages/BedDetail.tsx`

- [ ] **Step 1: Mount the component**

Add to `BedDetail`, below the bed's meta section and above the plants section:

```tsx
<MaintenanceRules target={{ kind: 'BED', id: bedId }} />
```

with `import { MaintenanceRules } from '../components/maintenance/MaintenanceRules'`.

- [ ] **Step 2: Keep the existing weed/water buttons honest**

`BedDetail` already has "Rensa ogräs" and "Vattna" buttons calling `api.beds.weed`/`api.beds.water`. Those write the very `bed_event` rows the backend derives "last done" from — so pressing them moves any matching rule's clock.

Extend both mutations' `onSuccess` to also invalidate `['maintenance-rules', 'BED', bedId]`, so the rules section updates its next-due date immediately rather than showing a stale one until the next refetch. This is the visible payoff of the derived-clock design and it should be obvious in the UI.

- [ ] **Step 3: Verify**

Run: `cd web && npx tsc -b && npm test`

In the app: add a WEED rule to a bed, note its next-due date, press "Rensa ogräs", and confirm the rule's next due date jumps forward by the interval without a page reload.

- [ ] **Step 4: Commit**

```bash
git add web/src/pages/BedDetail.tsx
git commit -m "feat(web): maintenance rules on bed detail"
```

---

### Task 8: Area-scoped tasks in the task list

**Files:**
- Modify: `web/src/pages/TaskList.tsx`
- Modify: `web/src/pages/TaskForm.tsx`
- Modify: `web/src/i18n/sv.json`, `web/src/i18n/en.json`

- [ ] **Step 1: Fix the task title**

`TaskList.tsx` currently has:

```ts
function taskTitle(task: ScheduledTaskResponse): string {
  return task.speciesName ?? task.activityType
}
```

A maintenance task carries no species, so today it would render as the raw enum `"MOW"`. Change it to fall back through the place before the activity:

```ts
function taskTitle(task: ScheduledTaskResponse): string {
  if (task.speciesName) return task.speciesName
  const place = task.gardenAreaName ?? task.bedName
  const activity = maintenanceActivityLabelSv(task.activityType)
  return place ? `${activity} · ${place}` : activity
}
```

importing `maintenanceActivityLabelSv` from `../lib/maintenance`.

- [ ] **Step 2: Route the new activities to the maintenance filter**

`ACTIVITY_TO_FILTER` maps activity types to the five filter chips, with `?? 'maintenance'` as the fallback. The new activities therefore already land under "maintenance" — verify that reads sensibly, and explicitly map `WATER` to `'watering'` if it is not already, so watering an area files with watering rather than generic maintenance.

- [ ] **Step 3: Make rule-backed tasks non-editable where the server forbids it**

The backend rejects `activityType` and `targetCount` changes on any task with `maintenanceRuleId != null` — the activity belongs to the rule, and the count is always 1.

`TaskForm.tsx` seeds both fields from the existing task and sends both on every save. An unchanged echo is accepted, so this does not break today — but a user who edits the activity dropdown on a maintenance task gets an opaque 400.

In `TaskForm`, when the loaded task has `maintenanceRuleId != null`: disable the activity select and the target-count input, and show a short line explaining that this task comes from a maintenance rule and that the activity and count are set by the rule. Link to the rule's place (`/area/${gardenAreaId}` or `/bed/${bedId}`) so the user can go change the rule itself.

- [ ] **Step 4: Do not call deletion "dismiss"**

Deleting a pending rule-backed task does not stop it coming back — the scheduler recreates it the next morning, because the work is still undone. Wherever the task list offers delete on a task with `maintenanceRuleId != null`, the confirmation copy must say the task will reappear while the rule is active, and offer pausing the rule as the way to actually stop it. Do not label it "dismiss", "skip", or "done for now".

- [ ] **Step 5: i18n**

Add the new keys to both files: the rule-backed task explanation, the delete-reappears warning, and any new activity labels the filter chips need.

- [ ] **Step 6: Verify**

Run: `cd web && npx tsc -b && npm test`

In the app: let the scheduler create a maintenance task (or insert one directly), confirm it renders with a readable title rather than a raw enum, confirm the edit form disables activity and count with an explanation, and confirm the delete copy warns that it will return.

- [ ] **Step 7: Commit**

```bash
git add web/src/pages/TaskList.tsx web/src/pages/TaskForm.tsx \
        web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat(web): render and guard area-scoped maintenance tasks"
```

---

## What this plan deliberately leaves out

- **Any map or polygon work.** `boundaryJson` round-trips through the API untouched; no web client draws it, and no map dependency is added.
- **The Android client.** That is the third plan.
- **Dashboard surfacing.** Maintenance tasks are ordinary `ScheduledTask` rows, so they already appear wherever tasks appear. A dedicated "due maintenance" dashboard card is a follow-up, not part of parity.
- **Backfilling the deferred backend minors** — `DataExportService` omitting areas, rules and events is a real gap, but it is a backend change, not a web one.
