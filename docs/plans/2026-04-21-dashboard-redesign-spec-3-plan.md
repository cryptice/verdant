# Verdant Dashboard Redesign Spec 3 — CropCalendar, Analytics, WorkflowProgress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the three remaining pages to Fältet, concluding the web-wide redesign.

**Architecture:** No new primitives. CropCalendar keeps its existing species × months grid layout, restyled with Fältet tokens (mustard/sage/clay hairline bars, mono small-caps month headers, single-letter month labels, tone-coded rows). Analytics replaces Tailwind cards and bars with paper/ink paper blocks + div-width bars in clay. WorkflowProgress becomes a 5-column step ledger with inline row expansion to show plants at each step. Design spec: `docs/plans/2026-04-21-dashboard-redesign-spec-3-design.md`.

**Tech Stack:** React 19 + TypeScript + Vite + Tailwind v4 (@theme + @utility), TanStack Query, react-i18next. No new dependencies.

**Important notes:**
- Solo-dev, commits to `main`.
- Data layer untouched. Preserve all existing hooks, mutations, and routing.
- i18n: most keys already exist. Add only the editorial taglines listed in §5 of the spec.
- Every task: `cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3` green + its own commit.

---

## Phasing overview

| Task | Name | File |
|---|---|---|
| 1 | CropCalendar port | `web/src/pages/CropCalendar.tsx` |
| 2 | Analytics port | `web/src/pages/Analytics.tsx` |
| 3 | WorkflowProgress port | `web/src/pages/WorkflowProgress.tsx` |
| 4 | Milestone | empty commit |

---

## Task 1 — CropCalendar port

**Goal:** Replace Tailwind-styled species × months grid with Fältet layout.
**Deliverable:** `CropCalendar.tsx` rendering the Masthead + season selector + legend + 13-column grid + empty state.
**Dependencies:** none.

**Files:**
- Modify: `web/src/pages/CropCalendar.tsx`
- Modify: `web/src/i18n/sv.json` (add `calendar.masthead.center`, `calendar.legend.*`, `calendar.seasonLabel`, `calendar.emptyTitle`, `calendar.emptyBody` if missing)
- Modify: `web/src/i18n/en.json` (same)

### Step 1: Read the current file

```bash
cat /Users/erik/development/verdant/web/src/pages/CropCalendar.tsx
```

Note:
- `api.seasons.list()`, `api.species.list()`, `api.plants.list()`, `api.plants.events(id)` queries.
- `rows` memo that produces `SpeciesRow[]` with optional `sowRange / bloomRange / harvestRange` month-index tuples.
- `barStyle()` helper computing `left` + `width` percentages from a `[startMonth, endMonth]` tuple.
- `MONTHS` array (single letters).
- `seasonId` state + season selector.

All of this is preserved. Only the rendering chrome changes.

### Step 2: Rewrite the file

Replace the file content with:

```tsx
import { useQuery } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse, type PlantEventResponse } from '../api/client'
import { Masthead, Chip } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'

const MONTHS = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D']

interface SpeciesRow {
  speciesId: number
  name: string
  scientificName?: string
  sowRange?: [number, number]
  bloomRange?: [number, number]
  harvestRange?: [number, number]
}

function monthIndex(dateStr: string): number {
  return new Date(dateStr).getMonth()
}

function parseBloomMonths(bloomMonths: string | undefined): [number, number] | undefined {
  if (!bloomMonths) return undefined
  const parts = bloomMonths.split(',').map(s => parseInt(s.trim(), 10)).filter(n => !isNaN(n))
  if (parts.length === 0) return undefined
  return [Math.min(...parts) - 1, Math.max(...parts) - 1]
}

function barStyle(range: [number, number], color: string, top: number): React.CSSProperties {
  const left = `${(range[0] / 12) * 100}%`
  const width = `${(((range[1] - range[0]) + 1) / 12) * 100}%`
  return {
    position: 'absolute',
    left,
    width,
    top,
    height: 6,
    background: color,
  }
}

const selectStyle: React.CSSProperties = {
  background: 'transparent',
  border: 'none',
  borderBottom: '1px solid var(--color-ink)',
  borderRadius: 0,
  padding: '4px 0',
  fontFamily: 'var(--font-display)',
  fontSize: 20,
  fontWeight: 300,
  color: 'var(--color-ink)',
  outline: 'none',
  minWidth: 200,
}

export function CropCalendar() {
  const { t, i18n } = useTranslation()
  const [seasonId, setSeasonId] = useState<number | undefined>(undefined)

  const { data: seasons } = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  const { data: speciesList } = useQuery({
    queryKey: ['species'],
    queryFn: () => api.species.list(),
  })

  const { data: plants, error, isLoading, refetch } = useQuery({
    queryKey: ['plants-for-calendar', seasonId],
    queryFn: () => api.plants.list(),
    enabled: seasonId !== undefined,
  })

  const plantIds = plants?.map(p => p.id) ?? []
  const { data: allEvents } = useQuery({
    queryKey: ['plant-events-calendar', plantIds.join(',')],
    queryFn: async () => {
      if (!plants || plants.length === 0) return [] as PlantEventResponse[]
      const results = await Promise.all(plants.map(p => api.plants.events(p.id)))
      return results.flat()
    },
    enabled: !!plants && plants.length > 0,
  })

  const rows = useMemo<SpeciesRow[]>(() => {
    if (!speciesList) return []
    const speciesMap = new Map<number, SpeciesResponse>()
    for (const s of speciesList) speciesMap.set(s.id, s)

    const rowsBySpecies = new Map<number, SpeciesRow>()
    const langSv = i18n.language === 'sv'

    plants?.forEach(plant => {
      if (!plant.speciesId) return
      const species = speciesMap.get(plant.speciesId)
      if (!species) return
      if (!rowsBySpecies.has(plant.speciesId)) {
        rowsBySpecies.set(plant.speciesId, {
          speciesId: plant.speciesId,
          name: (langSv ? species.commonNameSv : null) ?? species.commonName,
          scientificName: species.scientificName ?? undefined,
          bloomRange: parseBloomMonths(species.bloomMonths ?? undefined),
        })
      }
    })

    allEvents?.forEach(ev => {
      const plant = plants?.find(p => p.id === ev.plantId)
      if (!plant || !plant.speciesId) return
      const row = rowsBySpecies.get(plant.speciesId)
      if (!row) return
      const m = monthIndex(ev.eventDate)
      if (ev.eventType === 'SEEDED') {
        row.sowRange = row.sowRange
          ? [Math.min(row.sowRange[0], m), Math.max(row.sowRange[1], m)]
          : [m, m]
      } else if (ev.eventType === 'HARVESTED') {
        row.harvestRange = row.harvestRange
          ? [Math.min(row.harvestRange[0], m), Math.max(row.harvestRange[1], m)]
          : [m, m]
      }
    })

    return Array.from(rowsBySpecies.values())
  }, [speciesList, plants, allEvents, i18n.language])

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 64 }}>
        <div style={{ width: 32, height: 32, border: '2px solid var(--color-ink)', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
      </div>
    )
  }
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const MUSTARD = 'var(--color-mustard)'
  const SAGE = 'var(--color-sage)'
  const CLAY = 'var(--color-clay)'

  return (
    <div>
      <Masthead left={t('nav.calendar')} center={t('calendar.masthead.center')} />

      <div style={{ padding: '28px 40px' }}>
        {/* Season selector */}
        <div style={{ marginBottom: 28, display: 'flex', alignItems: 'flex-end', gap: 14 }}>
          <div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 4 }}>
              {t('calendar.seasonLabel')}
            </div>
            <select
              value={seasonId ?? ''}
              onChange={(e) => setSeasonId(e.target.value ? Number(e.target.value) : undefined)}
              style={selectStyle}
            >
              <option value="">—</option>
              {seasons?.map((s) => (
                <option key={s.id} value={s.id}>{s.name} · {s.year}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Legend */}
        <div style={{ display: 'flex', gap: 10, marginBottom: 22 }}>
          <Chip tone="mustard">{t('calendar.legend.sow')}</Chip>
          <Chip tone="sage">{t('calendar.legend.bloom')}</Chip>
          <Chip tone="clay">{t('calendar.legend.harvest')}</Chip>
        </div>

        {/* Empty state */}
        {(seasonId === undefined || rows.length === 0) && (
          <div style={{ padding: '60px 22px', textAlign: 'center', borderTop: '1px solid var(--color-ink)', borderBottom: '1px solid var(--color-ink)' }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 8 }}>
              {t('calendar.emptyTitle')}
            </div>
            <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, color: 'var(--color-forest)' }}>
              {t('calendar.emptyBody')}
            </div>
          </div>
        )}

        {/* Calendar grid */}
        {rows.length > 0 && (
          <div>
            {/* Header row */}
            <div style={{ display: 'grid', gridTemplateColumns: '200px repeat(12, 1fr)', borderBottom: '1px solid var(--color-ink)' }}>
              <div />
              {MONTHS.map((m, i) => (
                <div
                  key={i}
                  style={{
                    textAlign: 'center',
                    padding: '10px 0',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.4,
                    textTransform: 'uppercase',
                    color: 'var(--color-forest)',
                    opacity: 0.7,
                    borderLeft: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  }}
                >
                  {m}
                </div>
              ))}
            </div>

            {/* Species rows */}
            {rows.map((row) => (
              <div
                key={row.speciesId}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '200px repeat(12, 1fr)',
                  alignItems: 'stretch',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  minHeight: 44,
                }}
              >
                <div style={{ padding: '10px 14px 10px 0' }}>
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 20, fontWeight: 300 }}>{row.name}</div>
                  {row.scientificName && (
                    <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 9, color: 'var(--color-sage)' }}>
                      {row.scientificName}
                    </div>
                  )}
                </div>
                <div style={{ gridColumn: '2 / -1', position: 'relative', borderLeft: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)' }}>
                  {row.sowRange && <div style={barStyle(row.sowRange, MUSTARD, 14)} />}
                  {row.bloomRange && <div style={barStyle(row.bloomRange, SAGE, 22)} />}
                  {row.harvestRange && <div style={barStyle(row.harvestRange, CLAY, 30)} />}
                  {/* Vertical month hairlines */}
                  {Array.from({ length: 11 }).map((_, i) => (
                    <div
                      key={i}
                      style={{
                        position: 'absolute',
                        left: `${((i + 1) / 12) * 100}%`,
                        top: 0,
                        bottom: 0,
                        width: 1,
                        background: 'color-mix(in srgb, var(--color-ink) 12%, transparent)',
                      }}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
```

Adapt `row.scientificName` / `species.bloomMonths` references if actual field names differ — grep `web/src/api/client.ts` for `SpeciesResponse`.

### Step 3: i18n

Grep first:

```bash
grep -n "calendar" /Users/erik/development/verdant/web/src/i18n/sv.json | head
```

Add missing keys inside an existing `calendar` block (or create it):

```json
"calendar": {
  "masthead":     { "center": "— Odlingskalender —" / "— Cultivation Calendar —" },
  "legend":       {
    "sow":     "Sådd"     / "Sowing",
    "bloom":   "Blomning" / "Bloom",
    "harvest": "Skörd"    / "Harvest"
  },
  "seasonLabel":  "Säsong"             / "Season",
  "emptyTitle":   "Inga data ännu"     / "No data yet",
  "emptyBody":    "Välj en säsong ovan för att se kalendern." / "Choose a season above to view the calendar."
}
```

Reuse existing `calendar.*` keys where they already match.

### Step 4: Verify + commit

```bash
cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3
```

Both green.

```bash
git add web/src/pages/CropCalendar.tsx web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat: Fältet port — CropCalendar"
```

---

## Task 2 — Analytics port

**Goal:** Replace Tailwind cards + bars with Fältet chrome (paper/ink cards, div-width clay bars, ledger-style yield table).
**Dependencies:** none.

**Files:**
- Modify: `web/src/pages/Analytics.tsx`
- Modify: `web/src/i18n/sv.json` (add `analytics.masthead.center`, `analytics.section.*`, `analytics.card.*` if missing)
- Modify: `web/src/i18n/en.json` (same)

### Step 1: Read the current file

Preserve: `api.analytics.seasonSummaries()`, `api.analytics.speciesComparison(id)`, `api.analytics.yieldPerBed()`, `compSpecies` state, `maxStems` memo, `allSeasons` memo.

### Step 2: Rewrite the file

```tsx
import { useQuery } from '@tanstack/react-query'
import { useState, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { Masthead, Chip, Rule } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

export function Analytics() {
  const { t } = useTranslation()

  const { data: summaries, error: summariesError, isLoading: summariesLoading, refetch: summariesRefetch } = useQuery({
    queryKey: ['analytics-season-summaries'],
    queryFn: () => api.analytics.seasonSummaries(),
  })

  const [compSpecies, setCompSpecies] = useState<SpeciesResponse | null>(null)
  const { data: comparison, isLoading: compLoading } = useQuery({
    queryKey: ['analytics-species-comparison', compSpecies?.id],
    queryFn: () => api.analytics.speciesComparison(compSpecies!.id),
    enabled: compSpecies !== null,
  })

  const maxStems = useMemo(() => {
    if (!comparison?.seasons) return 1
    return Math.max(1, ...comparison.seasons.map(s => s.stemsHarvested))
  }, [comparison])

  const { data: yieldData } = useQuery({
    queryKey: ['analytics-yield-per-bed'],
    queryFn: () => api.analytics.yieldPerBed(),
  })

  const allSeasons = useMemo(() => {
    if (!yieldData) return [] as { seasonId: number; seasonName: string }[]
    const map = new Map<number, string>()
    yieldData.forEach(bed => {
      bed.seasons.forEach(s => {
        if (!map.has(s.seasonId)) map.set(s.seasonId, s.seasonName)
      })
    })
    return Array.from(map.entries()).map(([seasonId, seasonName]) => ({ seasonId, seasonName }))
  }, [yieldData])

  if (summariesLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 64 }}>
        <div style={{ width: 32, height: 32, border: '2px solid var(--color-ink)', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
      </div>
    )
  }
  if (summariesError) return <ErrorDisplay error={summariesError} onRetry={summariesRefetch} />

  return (
    <div>
      <Masthead left={t('nav.analytics')} center={t('analytics.masthead.center')} />

      <div style={{ padding: '28px 40px', display: 'flex', flexDirection: 'column', gap: 40 }}>

        {/* § Season overview */}
        <section>
          <SectionHeading title={t('analytics.section.seasonOverview')} />
          {(!summaries || summaries.length === 0) ? (
            <EmptyBlock label={t('analytics.noData')} />
          ) : (
            <div style={{ display: 'flex', gap: 20, overflowX: 'auto', paddingBottom: 8 }}>
              {summaries.map(s => (
                <div
                  key={s.seasonId}
                  style={{
                    flex: '0 0 280px',
                    background: 'var(--color-paper)',
                    border: '1px solid var(--color-ink)',
                    padding: '22px 28px',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)' }}>
                    <span>{s.seasonName}</span>
                    <span>{s.year}</span>
                  </div>
                  <div style={{
                    fontFamily: 'var(--font-display)', fontSize: 44, fontWeight: 300, letterSpacing: -0.6, marginTop: 14,
                    fontVariationSettings: '"SOFT" 100, "opsz" 144',
                  }}>
                    {s.totalStemsHarvested.toLocaleString()}
                  </div>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
                    {t('analytics.card.stems')}
                  </div>
                  <div style={{ marginTop: 14, borderTop: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)', paddingTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                    <Meta label={t('analytics.card.plants')} value={s.totalPlants.toString()} />
                    <Meta label={t('analytics.card.species')} value={s.speciesCount.toString()} />
                    {s.topSpeciesName && (
                      <div style={{ gridColumn: '1 / -1' }}>
                        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 4 }}>
                          {t('analytics.card.topSpecies')}
                        </div>
                        <Chip tone="clay">{s.topSpeciesName}</Chip>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* § Species comparison */}
        <section>
          <SectionHeading title={t('analytics.section.speciesComparison')} />
          <div style={{ marginBottom: 22, maxWidth: 360 }}>
            <SpeciesAutocomplete value={compSpecies} onChange={setCompSpecies} />
          </div>

          {compSpecies === null ? (
            <EmptyBlock label={t('analytics.selectSpecies')} />
          ) : compLoading ? (
            <EmptyBlock label={t('analytics.loading')} />
          ) : !comparison?.seasons || comparison.seasons.length === 0 ? (
            <EmptyBlock label={t('analytics.noData')} />
          ) : (
            <div>
              {comparison.seasons.map(season => {
                const pct = Math.round((season.stemsHarvested / maxStems) * 100)
                return (
                  <div
                    key={season.seasonId}
                    style={{
                      display: 'grid',
                      gridTemplateColumns: '120px 100px 1fr 70px',
                      gap: 18,
                      alignItems: 'center',
                      padding: '14px 0',
                      borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    }}
                  >
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)' }}>
                      {season.seasonName}
                    </span>
                    <span style={{ fontFamily: 'var(--font-display)', fontSize: 20, fontVariantNumeric: 'tabular-nums' }}>
                      {season.stemsHarvested.toLocaleString()}
                    </span>
                    <div style={{ height: 8, background: 'var(--color-paper)', border: '1px solid var(--color-ink)' }}>
                      <div style={{ width: `${pct}%`, height: '100%', background: 'var(--color-clay)' }} />
                    </div>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, textAlign: 'right' }}>
                      {pct}%
                    </span>
                  </div>
                )
              })}
            </div>
          )}
        </section>

        {/* § Yield per bed */}
        <section>
          <SectionHeading title={t('analytics.section.yieldPerBed')} />

          {(!yieldData || yieldData.length === 0) ? (
            <EmptyBlock label={t('analytics.noData')} />
          ) : (
            <div>
              {/* Header */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: `180px repeat(${allSeasons.length}, 1fr)`,
                gap: 18,
                padding: '10px 0',
                borderBottom: '1px solid var(--color-ink)',
                fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
              }}>
                <span>{t('analytics.card.bed')}</span>
                {allSeasons.map(col => <span key={col.seasonId} style={{ textAlign: 'right' }}>{col.seasonName}</span>)}
              </div>

              {/* Rows */}
              {yieldData.map(bed => (
                <div
                  key={bed.bedId}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: `180px repeat(${allSeasons.length}, 1fr)`,
                    gap: 18,
                    padding: '12px 0',
                    borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    alignItems: 'center',
                  }}
                >
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{bed.bedName}</span>
                  {allSeasons.map(col => {
                    const season = bed.seasons.find(s => s.seasonId === col.seasonId)
                    return (
                      <span
                        key={col.seasonId}
                        style={{
                          textAlign: 'right',
                          fontFamily: 'var(--font-display)',
                          fontSize: 18,
                          fontVariantNumeric: 'tabular-nums',
                          color: season ? 'var(--color-ink)' : 'color-mix(in srgb, var(--color-clay) 40%, transparent)',
                        }}
                      >
                        {season ? season.stemsHarvested.toLocaleString() : '—'}
                      </span>
                    )
                  })}
                </div>
              ))}
            </div>
          )}
        </section>

      </div>
    </div>
  )
}

function SectionHeading({ title }: { title: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 14 }}>
      <h2 style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 30, fontWeight: 300, margin: 0, fontVariationSettings: '"SOFT" 100, "opsz" 144' }}>
        {title}<span style={{ color: 'var(--color-clay)' }}>.</span>
      </h2>
      <Rule inline variant="ink" />
    </div>
  )
}

function EmptyBlock({ label }: { label: string }) {
  return (
    <div style={{ padding: '40px 22px', textAlign: 'center', borderTop: '1px solid var(--color-ink)', borderBottom: '1px solid var(--color-ink)' }}>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
        {label}
      </div>
    </div>
  )
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
        {label}
      </div>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
        {value}
      </div>
    </div>
  )
}
```

Adapt `bed.bedId`, `bed.bedName`, `bed.seasons`, `season.seasonId`, `season.seasonName`, `season.stemsHarvested`, `s.topSpeciesName` to real field names — grep `yieldPerBed` + `speciesComparison` + `seasonSummaries` response types in `web/src/api/client.ts`.

### Step 3: i18n

```json
"analytics": {
  "masthead": { "center": "— Siffrorna —" / "— The Numbers —" },
  "section": {
    "seasonOverview":    "Säsongsöversikt"  / "Season overview",
    "speciesComparison": "Artjämförelse"    / "Species comparison",
    "yieldPerBed":       "Skörd per bädd"   / "Yield per bed"
  },
  "card": {
    "stems":       "stjälkar"  / "stems",
    "plants":      "Plantor"   / "Plants",
    "species":     "Arter"     / "Species",
    "topSpecies":  "Bästa art" / "Top species",
    "bed":         "Bädd"      / "Bed"
  },
  "selectSpecies": "Välj en art för att jämföra" / "Pick a species to compare",
  "loading":       "Laddar…"   / "Loading…"
}
```

Preserve any existing `analytics.noData`, `analytics.title`, etc.

### Step 4: Verify + commit

```bash
cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3
git add web/src/pages/Analytics.tsx web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat: Fältet port — Analytics"
```

---

## Task 3 — WorkflowProgress port

**Goal:** Rebuild the page as a 5-column step ledger with tone-coded № markers, event-type chips, completion progress, and inline row expansion listing plants at each step.
**Dependencies:** none.

**Files:**
- Modify: `web/src/pages/WorkflowProgress.tsx`
- Modify: `web/src/i18n/sv.json` (add `workflows.progress.*` if missing)
- Modify: `web/src/i18n/en.json` (same)

### Step 1: Read the current file

Note: existing queries for species, workflow template steps, per-step plant grouping; side-branch handling if present; existing completion mutation (if row clicks call `completeStep` rather than expanding). The plan shifts row-click semantics to inline expansion; the existing completion flow lives in `WorkflowTemplateEdit.tsx`, not here.

### Step 2: Rewrite the file

```tsx
import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesWorkflowStepResponse } from '../api/client'
import { Masthead, Chip, Rule } from '../components/faltet'

type StepTone = 'sage' | 'mustard' | 'forest'

function numberTone(done: number, total: number): StepTone {
  if (total === 0) return 'forest'
  if (done === total) return 'sage'
  if (done > 0) return 'mustard'
  return 'forest'
}

function eventTypeTone(eventType: string | null | undefined): 'mustard' | 'clay' | 'forest' {
  if (eventType === 'SEEDED' || eventType === 'POTTED_UP') return 'mustard'
  if (eventType === 'HARVESTED' || eventType === 'FIRST_BLOOM' || eventType === 'PEAK_BLOOM') return 'clay'
  return 'forest'
}

export function WorkflowProgress() {
  const { speciesId } = useParams<{ speciesId: string }>()
  const { t } = useTranslation()
  const spId = Number(speciesId)

  const { data: species } = useQuery({
    queryKey: ['species', spId],
    queryFn: () => api.species.get(spId),
    enabled: !isNaN(spId),
  })

  const { data: progress } = useQuery({
    queryKey: ['workflow-progress', spId],
    queryFn: () => api.workflows.progressForSpecies(spId),
    enabled: !isNaN(spId),
  })

  const [expanded, setExpanded] = useState<Set<number>>(new Set())

  const toggle = (stepId: number) => {
    setExpanded(prev => {
      const next = new Set(prev)
      next.has(stepId) ? next.delete(stepId) : next.add(stepId)
      return next
    })
  }

  const mainSteps = useMemo(() => progress?.steps?.filter(s => !s.isSideBranch) ?? [], [progress])
  const sideBranches = useMemo(() => {
    const branches = new Map<string, SpeciesWorkflowStepResponse[]>()
    progress?.steps?.filter(s => s.isSideBranch && s.sideBranchName).forEach(s => {
      const list = branches.get(s.sideBranchName!) ?? []
      list.push(s)
      branches.set(s.sideBranchName!, list)
    })
    return branches
  }, [progress])

  const activePlants = progress?.totalActivePlants ?? 0
  const done = mainSteps.filter(s => s.plantCountAtStep === 0 && s.hasCompletedPlants).length
  const total = mainSteps.length

  return (
    <div>
      <Masthead
        left={<span>{t('nav.species')} / <span style={{ color: 'var(--color-clay)' }}>{species?.commonName ?? '…'}</span></span>}
        center={t('workflows.progress.masthead.center')}
      />

      <div style={{ padding: '28px 40px' }}>
        {/* Compact hero */}
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 44, fontWeight: 300, letterSpacing: -0.8, margin: 0, fontVariationSettings: '"SOFT" 100, "opsz" 144' }}>
          {species?.commonName}
          {species?.variantName && (
            <span style={{ fontStyle: 'italic', color: 'var(--color-clay)' }}> '{species.variantName}'</span>
          )}
        </h1>
        <div style={{ marginTop: 6, fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
          {t('workflows.progress.activePlants', { count: activePlants })} · {t('workflows.progress.stepsComplete', { done, total })}
        </div>

        {/* Step ledger */}
        <div style={{ marginTop: 40 }}>
          <StepLedger steps={mainSteps} expanded={expanded} onToggle={toggle} />
        </div>

        {/* Side branches */}
        {Array.from(sideBranches.entries()).map(([branchName, steps]) => (
          <section key={branchName} style={{ marginTop: 40 }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 14 }}>
              <h2 style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 20, fontWeight: 300, margin: 0 }}>
                {t('workflows.progress.sideBranch')}: {branchName}<span style={{ color: 'var(--color-clay)' }}>.</span>
              </h2>
              <Rule inline variant="soft" />
            </div>
            <StepLedger steps={steps} expanded={expanded} onToggle={toggle} />
          </section>
        ))}
      </div>
    </div>
  )
}

function StepLedger({
  steps, expanded, onToggle,
}: {
  steps: SpeciesWorkflowStepResponse[]
  expanded: Set<number>
  onToggle: (id: number) => void
}) {
  const { t } = useTranslation()

  if (steps.length === 0) {
    return (
      <div style={{ padding: '40px 22px', textAlign: 'center', borderTop: '1px solid var(--color-ink)', borderBottom: '1px solid var(--color-ink)' }}>
        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
          {t('workflows.progress.empty')}
        </div>
      </div>
    )
  }

  return (
    <div>
      {/* Header */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '40px 1.5fr 100px 140px 120px',
        gap: 18,
        padding: '10px 0',
        borderBottom: '1px solid var(--color-ink)',
        fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
      }}>
        <span>№</span>
        <span>{t('workflows.stepName')}</span>
        <span>{t('workflows.daysAfterPrevious')}</span>
        <span>{t('workflows.eventType')}</span>
        <span style={{ textAlign: 'right' }}>{t('workflows.progress.completion')}</span>
      </div>

      {steps.map((step, i) => {
        const done = step.completedPlantCount ?? 0
        const totalPlants = (step.plantCountAtStep ?? 0) + done
        const tone = numberTone(done, totalPlants)
        const toneVar = tone === 'sage' ? 'var(--color-sage)' : tone === 'mustard' ? 'var(--color-mustard)' : 'var(--color-forest)'
        const pct = totalPlants > 0 ? Math.round((done / totalPlants) * 100) : 0
        const isExpanded = expanded.has(step.id)

        return (
          <div key={step.id}>
            <button
              onClick={() => onToggle(step.id)}
              style={{
                display: 'grid',
                gridTemplateColumns: '40px 1.5fr 100px 140px 120px',
                gap: 18,
                padding: '14px 0',
                borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                width: '100%',
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                alignItems: 'center',
                textAlign: 'left',
              }}
            >
              <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: toneVar }}>
                {String(i + 1).padStart(2, '0')}
              </span>
              <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{step.name}</span>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)' }}>
                {step.daysAfterPrevious != null ? t('workflows.progress.day', { n: step.daysAfterPrevious }) : ''}
              </span>
              <span>
                {step.eventType && step.eventType !== 'NOTE' && (
                  <Chip tone={eventTypeTone(step.eventType)}>{t(`eventType.${step.eventType}`, { defaultValue: step.eventType })}</Chip>
                )}
              </span>
              <span style={{ textAlign: 'right', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 10 }}>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase' }}>
                  {done}/{totalPlants}
                </span>
                <div style={{ width: 60, height: 6, background: 'var(--color-paper)', border: '1px solid var(--color-ink)' }}>
                  <div style={{ width: `${pct}%`, height: '100%', background: 'var(--color-clay)' }} />
                </div>
              </span>
            </button>

            {isExpanded && step.plantsAtStep && step.plantsAtStep.length > 0 && (
              <div style={{ padding: '10px 0 10px 78px' }}>
                {step.plantsAtStep.map(plant => (
                  <div
                    key={plant.id}
                    style={{
                      display: 'grid',
                      gridTemplateColumns: '1fr 200px 100px',
                      gap: 18,
                      padding: '8px 0',
                      borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 15%, transparent)',
                      alignItems: 'center',
                    }}
                  >
                    <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>{plant.name ?? plant.speciesName}</span>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-forest)' }}>{plant.bedName ?? '—'}</span>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, textAlign: 'right' }}>{plant.startedAt ?? '—'}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
```

**Field-adaptation note:** Adapt `step.plantCountAtStep`, `step.completedPlantCount`, `step.plantsAtStep`, `progress.totalActivePlants`, `plant.startedAt` to real API shapes — grep `workflows.progressForSpecies` or equivalent in `web/src/api/client.ts`. The existing `WorkflowProgress.tsx` has authoritative field accesses; use them.

### Step 3: i18n

```json
"workflows": {
  "progress": {
    "masthead":       { "center": "— Arbetsflöde —" / "— Workflow —" },
    "activePlants":   "{{count}} aktiva plantor" / "{{count}} active plants",
    "stepsComplete":  "{{done}} steg klara av {{total}}" / "{{done}} of {{total}} steps complete",
    "sideBranch":     "Sidspår" / "Side branch",
    "day":            "DAG +{{n}}" / "DAY +{{n}}",
    "completion":     "Framsteg" / "Progress",
    "empty":          "Inga steg" / "No steps"
  }
}
```

### Step 4: Verify + commit

```bash
cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3
git add web/src/pages/WorkflowProgress.tsx web/src/i18n/sv.json web/src/i18n/en.json
git commit -m "feat: Fältet port — WorkflowProgress"
```

---

## Task 4 — Milestone

### Step 1: Final verification

```bash
cd web && npx tsc --noEmit && npm run test && npm run build 2>&1 | tail -3
```

All green.

### Step 2: Milestone commit

```bash
git commit --allow-empty -m "milestone: Fältet dashboard redesign spec 3 (web rollout complete)"
```

---

## Verification summary

After task 4:
- Three pages ported (CropCalendar, Analytics, WorkflowProgress).
- No new primitives, no new chart library, no new dependencies.
- `npx tsc --noEmit` green.
- `npm run test` green.
- `npm run build` green.
- Full web-wide Fältet rollout: specs 1 + 2.1 + 2.2 + 3 = ~37 pages ported.

**Still flagged from earlier specs:**
- Real photography replacing PhotoPlaceholder.
- Self-hosted fonts (Fraunces + Inter `.woff2` instead of Google CDN).
- Visual regression harness.
- Accessibility audit.
- Pagination restoration on spec 2.1 ledger pages.
- Hardcoded harvest aggregate stats on Dashboard / BedDetail / GardenDetail (need a real analytics endpoint).
- Android parity — the Fältet rollout is web-only.
