# Pagination Restoration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore pagination on 10 ledger pages lost during the spec 2.1 Fältet port, client-side only.

**Architecture:** Extend `<Ledger>` with a `paginated` + `pageSize` prop so Ledger-driven pages opt in via one flag. Add a standalone `<LedgerPagination>` primitive for the 6 pages that render rows inline (not through `<Ledger>`). Both render identical Fältet-styled controls — two clay arrows around a mono small-caps "from–to AV total" label, preceded by a soft rule. Design spec: `docs/plans/2026-04-21-pagination-restoration-design.md`.

**Tech Stack:** React 19 + TypeScript + Vite + Tailwind v4, TanStack Query, react-i18next, vitest + @testing-library/react.

**Important notes:**
- Solo-dev, commits to `main`.
- Client-side only. No backend / endpoint changes.
- Page size default 50. Any page can override via `pageSize` prop.
- Four genuinely small pages (SeasonList, SpeciesGroups, WorkflowTemplates, GardenList) stay un-paginated.

---

## Phasing overview

| Task | Name | Scope |
|---|---|---|
| 1 | Primitives | Extend `Ledger`, add `LedgerPagination`, vitest tests, i18n |
| 2 | Ledger-driven ports | SpeciesList, CustomerList, PlantedSpeciesList, ProductionTargets |
| 3 | Inline ports + milestone | SuccessionSchedules, PestDiseaseLog, VarietyTrials, BouquetRecipes, SeedInventory, Supplies, milestone |

---

## Task 1 — Primitives

**Goal:** Extend `<Ledger>` with pagination support; add `<LedgerPagination>`; tests; i18n.

**Files:**
- Modify: `web/src/components/faltet/Ledger.tsx`
- Create: `web/src/components/faltet/LedgerPagination.tsx`
- Modify: `web/src/components/faltet/index.ts`
- Modify: `web/src/components/faltet/Ledger.test.tsx`
- Create: `web/src/components/faltet/LedgerPagination.test.tsx`
- Modify: `web/src/i18n/sv.json`, `web/src/i18n/en.json`

### Step 1: Add shared pagination-controls helper

Create the new component first — `<Ledger>` will reuse it internally.

```tsx
// web/src/components/faltet/LedgerPagination.tsx
import { useTranslation } from 'react-i18next'
import { Rule } from './Rule'

export type LedgerPaginationProps = {
  page: number
  pageSize: number
  total: number
  onChange: (nextPage: number) => void
}

export function LedgerPagination({ page, pageSize, total, onChange }: LedgerPaginationProps) {
  const { t } = useTranslation()
  if (total === 0) return null

  const lastPage = Math.max(0, Math.ceil(total / pageSize) - 1)
  const from = page * pageSize + 1
  const to = Math.min((page + 1) * pageSize, total)
  const atStart = page === 0
  const atEnd = page >= lastPage

  const btn = (disabled: boolean): React.CSSProperties => ({
    background: 'transparent',
    border: 'none',
    color: 'var(--color-clay)',
    fontFamily: 'var(--font-mono)',
    fontSize: 14,
    cursor: disabled ? 'default' : 'pointer',
    opacity: disabled ? 0.3 : 1,
    padding: '4px 8px',
  })

  return (
    <div>
      <Rule variant="soft" />
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 18, padding: '14px 0' }}>
        <button
          type="button"
          onClick={() => onChange(Math.max(0, page - 1))}
          disabled={atStart}
          style={btn(atStart)}
          aria-label={t('pagination.previous')}
        >←</button>
        <span
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
          }}
        >
          {from}–{to} {t('pagination.of')} {total}
        </span>
        <button
          type="button"
          onClick={() => onChange(Math.min(lastPage, page + 1))}
          disabled={atEnd}
          style={btn(atEnd)}
          aria-label={t('pagination.next')}
        >→</button>
      </div>
    </div>
  )
}
```

### Step 2: Extend `Ledger.tsx` with pagination

Replace the component body with:

```tsx
// web/src/components/faltet/Ledger.tsx
import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { LedgerPagination } from './LedgerPagination'

export type LedgerColumn<T> = {
  key: string
  label: string
  width?: string
  align?: 'left' | 'right'
  render?: (row: T, index: number) => ReactNode
}

type Props<T> = {
  columns: LedgerColumn<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  onRowClick?: (row: T) => void
  emptyMessage?: string
  sectionHeaders?: (row: T, index: number, prev: T | null) => ReactNode | null
  paginated?: boolean
  pageSize?: number
}

export function Ledger<T>({
  columns, rows, rowKey, onRowClick, emptyMessage, sectionHeaders,
  paginated = false, pageSize = 50,
}: Props<T>) {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)

  // Reset page when row identity-set changes (filter/search/data refetch).
  const rowKeysFingerprint = rows.map(rowKey).join('|')
  useEffect(() => { setPage(0) }, [rowKeysFingerprint])

  const template = columns.map((c) => c.width ?? '1fr').join(' ')

  if (rows.length === 0) {
    return (
      <div
        style={{
          padding: '40px 22px',
          textAlign: 'center',
          borderBottom: '1px solid var(--color-ink)',
          borderTop: '1px solid var(--color-ink)',
        }}
      >
        <div
          style={{
            fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
            textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 6,
          }}
        >
          {emptyMessage ?? t('common.ledger.empty')}
        </div>
      </div>
    )
  }

  const visibleRows = paginated
    ? rows.slice(page * pageSize, (page + 1) * pageSize)
    : rows

  return (
    <div>
      {/* Header */}
      <div
        style={{
          display: 'grid', gridTemplateColumns: template, gap: 18,
          padding: '10px 0', borderBottom: '1px solid var(--color-ink)',
        }}
      >
        {columns.map((col) => (
          <div
            key={col.key}
            style={{
              fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
              textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
              textAlign: col.align === 'right' ? 'right' : 'left',
            }}
          >
            {col.label}
          </div>
        ))}
      </div>

      {/* Body */}
      {visibleRows.map((row, i) => {
        const globalIndex = paginated ? page * pageSize + i : i
        const prevRow = globalIndex > 0 ? rows[globalIndex - 1] : null
        // When paginated, force a section header on the first row of each page
        // even if the preceding row (off-page) shares the same group.
        const prevForHeader = paginated && i === 0 ? null : prevRow
        const sectionNode = sectionHeaders?.(row, globalIndex, prevForHeader)

        const rowStyle: React.CSSProperties = {
          display: 'grid', gridTemplateColumns: template, gap: 18,
          padding: '12px 0',
          borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
          alignItems: 'center',
          textAlign: 'left',
          background: 'transparent',
          cursor: onRowClick ? 'pointer' : 'default',
          width: '100%',
        }
        const RowComponent = onRowClick ? 'button' : 'div'

        return (
          <div key={rowKey(row)}>
            {sectionNode}
            <RowComponent
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              style={{
                ...rowStyle,
                borderWidth: 0,
                borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              }}
              className={onRowClick ? 'ledger-row' : undefined}
            >
              {columns.map((col) => (
                <div
                  key={col.key}
                  style={{
                    textAlign: col.align === 'right' ? 'right' : 'left',
                    fontFamily: 'var(--font-display)', fontSize: 16, fontWeight: 300,
                    color: 'var(--color-ink)',
                  }}
                >
                  {col.render ? col.render(row, globalIndex) : String((row as any)[col.key] ?? '')}
                </div>
              ))}
            </RowComponent>
          </div>
        )
      })}

      {paginated && (
        <LedgerPagination
          page={page}
          pageSize={pageSize}
          total={rows.length}
          onChange={setPage}
        />
      )}
    </div>
  )
}
```

### Step 3: Extend i18n

Append to `web/src/i18n/sv.json` inside the `pagination` block (already exists with `showing`):

```json
"pagination": {
  "showing": "{{from}}–{{to}} av {{total}}",
  "of": "av",
  "previous": "Föregående sida",
  "next": "Nästa sida"
}
```

And `web/src/i18n/en.json`:

```json
"pagination": {
  "showing": "{{from}}–{{to}} of {{total}}",
  "of": "of",
  "previous": "Previous page",
  "next": "Next page"
}
```

Merge with existing `pagination` block — don't duplicate `showing`.

### Step 4: Update barrel

Append to `web/src/components/faltet/index.ts`:

```ts
export { LedgerPagination } from './LedgerPagination'
export type { LedgerPaginationProps } from './LedgerPagination'
```

### Step 5: Extend `Ledger.test.tsx`

Open the existing file. Append these two tests inside the existing `describe('Ledger', ...)` block:

```tsx
test('paginated prop slices rows', () => {
  const rows = Array.from({ length: 7 }, (_, i) => ({ id: i, name: `Row ${i}`, count: i }))
  render(withI18n(<Ledger columns={columns} rows={rows} rowKey={(r) => r.id} paginated pageSize={5} />))
  expect(screen.getByText('Row 0')).toBeInTheDocument()
  expect(screen.getByText('Row 4')).toBeInTheDocument()
  expect(screen.queryByText('Row 5')).not.toBeInTheDocument()
})

test('next button advances the page slice', async () => {
  const rows = Array.from({ length: 7 }, (_, i) => ({ id: i, name: `Row ${i}`, count: i }))
  render(withI18n(<Ledger columns={columns} rows={rows} rowKey={(r) => r.id} paginated pageSize={5} />))
  const nextBtn = screen.getByLabelText('Next page')
  fireEvent.click(nextBtn)
  expect(screen.getByText('Row 5')).toBeInTheDocument()
  expect(screen.getByText('Row 6')).toBeInTheDocument()
  expect(screen.queryByText('Row 0')).not.toBeInTheDocument()
})
```

Add `import { fireEvent } from '@testing-library/react'` at the top if not already present. Also extend the i18next init in that file to include the new `pagination.*` keys:

```ts
i18n.use(initReactI18next).init({
  lng: 'en',
  resources: {
    en: {
      translation: {
        common: { ledger: { empty: 'No rows yet' } },
        pagination: { of: 'of', previous: 'Previous page', next: 'Next page' },
      },
    },
  },
  interpolation: { escapeValue: false },
})
```

### Step 6: Create `LedgerPagination.test.tsx`

```tsx
// web/src/components/faltet/LedgerPagination.test.tsx
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, test, vi } from 'vitest'
import { I18nextProvider } from 'react-i18next'
import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { LedgerPagination } from './LedgerPagination'

i18n.use(initReactI18next).init({
  lng: 'en',
  resources: {
    en: {
      translation: {
        pagination: { of: 'of', previous: 'Previous page', next: 'Next page' },
      },
    },
  },
  interpolation: { escapeValue: false },
})

function withI18n(node: React.ReactNode) {
  return <I18nextProvider i18n={i18n}>{node}</I18nextProvider>
}

describe('LedgerPagination', () => {
  test('renders counter and arrows', () => {
    render(withI18n(<LedgerPagination page={0} pageSize={10} total={25} onChange={() => {}} />))
    expect(screen.getByText('1–10 of 25')).toBeInTheDocument()
    expect(screen.getByLabelText('Previous page')).toBeInTheDocument()
    expect(screen.getByLabelText('Next page')).toBeInTheDocument()
  })

  test('disables previous on first page', () => {
    render(withI18n(<LedgerPagination page={0} pageSize={10} total={25} onChange={() => {}} />))
    expect(screen.getByLabelText('Previous page')).toBeDisabled()
  })

  test('disables next on last page', () => {
    render(withI18n(<LedgerPagination page={2} pageSize={10} total={25} onChange={() => {}} />))
    expect(screen.getByLabelText('Next page')).toBeDisabled()
  })

  test('onChange fires on click', () => {
    const onChange = vi.fn()
    render(withI18n(<LedgerPagination page={0} pageSize={10} total={25} onChange={onChange} />))
    fireEvent.click(screen.getByLabelText('Next page'))
    expect(onChange).toHaveBeenCalledWith(1)
  })

  test('returns null when total is 0', () => {
    const { container } = render(withI18n(<LedgerPagination page={0} pageSize={10} total={0} onChange={() => {}} />))
    expect(container.firstChild).toBeNull()
  })
})
```

### Step 7: Verify + commit

```bash
cd /Users/erik/development/verdant/web && npx tsc --noEmit && npm run test && npm run build 2>&1 | tail -3
```

All green.

```bash
git add web/src/components/faltet/Ledger.tsx \
        web/src/components/faltet/LedgerPagination.tsx \
        web/src/components/faltet/index.ts \
        web/src/components/faltet/Ledger.test.tsx \
        web/src/components/faltet/LedgerPagination.test.tsx \
        web/src/i18n/sv.json \
        web/src/i18n/en.json
git commit -m "feat: pagination support for Fältet Ledger + new LedgerPagination primitive"
```

---

## Task 2 — Ledger-driven ports

**Goal:** Add `paginated` to the four pages that use `<Ledger>` directly.

**Files:**
- Modify: `web/src/pages/SpeciesList.tsx`
- Modify: `web/src/pages/CustomerList.tsx`
- Modify: `web/src/pages/PlantedSpeciesList.tsx`
- Modify: `web/src/pages/ProductionTargets.tsx`

### Step 1: SpeciesList

Find the `<Ledger ...>` invocation in `web/src/pages/SpeciesList.tsx`. Add two props:

```tsx
<Ledger
  paginated
  pageSize={50}
  columns={[...]}
  rows={filtered}
  rowKey={...}
  onRowClick={...}
/>
```

Place `paginated` + `pageSize` before existing props or after — ordering doesn't matter.

### Step 2: CustomerList

Same change in `web/src/pages/CustomerList.tsx`:

```tsx
<Ledger
  paginated
  pageSize={50}
  columns={[...]}
  rows={filtered}
  rowKey={...}
  onRowClick={...}
/>
```

### Step 3: PlantedSpeciesList

Same in `web/src/pages/PlantedSpeciesList.tsx`:

```tsx
<Ledger
  paginated
  pageSize={50}
  columns={[...]}
  rows={filteredList}
  rowKey={...}
  onRowClick={...}
/>
```

Rows variable name depends on current file — grep `<Ledger` to find the exact invocation.

### Step 4: ProductionTargets

`web/src/pages/ProductionTargets.tsx` uses `<Ledger>` per spec 2.1 Task 4. Same change:

```tsx
<Ledger
  paginated
  pageSize={50}
  columns={[...]}
  rows={filteredTargets}
  rowKey={...}
/>
```

Note: ProductionTargets has inline expand-collapse for forecast panels. When a page change hides an expanded row, its `expanded: Set<number>` state still contains the id; returning to that page rehydrates the expansion visually. Acceptable.

### Step 5: Verify + commit

```bash
cd /Users/erik/development/verdant/web && npx tsc --noEmit && npm run build 2>&1 | tail -3
```

Green.

```bash
git add web/src/pages/SpeciesList.tsx \
        web/src/pages/CustomerList.tsx \
        web/src/pages/PlantedSpeciesList.tsx \
        web/src/pages/ProductionTargets.tsx
git commit -m "feat: pagination on SpeciesList, CustomerList, PlantedSpeciesList, ProductionTargets"
```

---

## Task 3 — Inline ports + milestone

**Goal:** Add `<LedgerPagination>` to the six pages that render rows inline. Finish with milestone commit.

**Files:**
- Modify: `web/src/pages/SuccessionSchedules.tsx`
- Modify: `web/src/pages/PestDiseaseLog.tsx`
- Modify: `web/src/pages/VarietyTrials.tsx`
- Modify: `web/src/pages/BouquetRecipes.tsx`
- Modify: `web/src/pages/SeedInventory.tsx`
- Modify: `web/src/pages/Supplies.tsx`

### Pattern for all six pages

Each page renders an array of rows inline (not through `<Ledger>`). The transformation:

1. Find the array that gets iterated with `.map(...)` (e.g. `filtered`, `logs`, `trials`, `recipes`, `sortedSpecies`, `groupedTypes`).
2. Add local pagination state:
   ```tsx
   const [page, setPage] = useState(0)
   const pageSize = 50
   ```
3. Add a `useEffect` that resets `page` when the array identity changes:
   ```tsx
   useEffect(() => { setPage(0) }, [JSON.stringify((filtered as any[]).map((x: any) => x.id))])
   ```
   (Use the array variable name and row id for the page. If the array doesn't have `id` per row, use the row index or row name.)
4. Slice for rendering:
   ```tsx
   const visible = filtered.slice(page * pageSize, (page + 1) * pageSize)
   ```
5. Render `visible` instead of the full array inside the `.map(...)`.
6. Reset page on filter / search handler changes:
   ```tsx
   const toggleFilter = (x: ...) => { setFilters(...); setPage(0) }
   ```
7. Add `<LedgerPagination page={page} pageSize={pageSize} total={filtered.length} onChange={setPage} />` immediately after the rows container.

Import from `../components/faltet`.

### Step 1: SuccessionSchedules

Open `web/src/pages/SuccessionSchedules.tsx`. There is an existing manual prev/next prev + next from spec 2.1 Task 4 — **remove it** and replace with `<LedgerPagination>`.

Grep for `setPage` / `LedgerPagination` / `page <` in the file to locate the existing prev/next UI; delete it and insert the new primitive.

```tsx
// at top of file
import { LedgerPagination, Masthead, Chip, Rule } from '../components/faltet'
import { useEffect, useState } from 'react'

// inside component
const [page, setPage] = useState(0)
const pageSize = 50

const { data: seasons } = useQuery({...})
const { data: schedules = [] } = useQuery({...})

const filtered = seasonId
  ? schedules.filter((s: any) => s.seasonId === seasonId)
  : schedules
const visible = filtered.slice(page * pageSize, (page + 1) * pageSize)

useEffect(() => { setPage(0) }, [seasonId])

// in the JSX — replace the existing rows.map with:
{visible.map((s, i) => (/* existing row JSX using global index = page * pageSize + i */))}

// immediately after the rows block, replace the manual prev/next controls with:
<LedgerPagination page={page} pageSize={pageSize} total={filtered.length} onChange={setPage} />
```

If the current SuccessionSchedules row JSX uses `i` from `.map((s, i) => …)` as a display number — e.g. `String(i + 1).padStart(2, '0')` — adjust so the displayed № reflects the *global* index across pages, not the per-page index:

```tsx
{visible.map((s, i) => {
  const globalIndex = page * pageSize + i
  // use globalIndex wherever i was used before for display numbers
  return (<div ...>
    <span>{String(globalIndex + 1).padStart(2, '0')}</span>
    ...
  </div>)
})}
```

### Step 2: PestDiseaseLog

Open `web/src/pages/PestDiseaseLog.tsx`. The file sorts logs by `(category, observedDate)` and renders with `§ CATEGORY` headers when category changes. Pagination slices the flat sorted list; when a category spans two pages, each page renders its own header for the portion it contains.

```tsx
import { LedgerPagination, ... } from '../components/faltet'

const [page, setPage] = useState(0)
const pageSize = 50

const sortedLogs = /* existing sort logic */
const visible = sortedLogs.slice(page * pageSize, (page + 1) * pageSize)

useEffect(() => { setPage(0) }, [seasonId /* and whatever filter dimensions exist */])

// In the rendering loop, use `visible` instead of `sortedLogs`:
let prevCategory: string | null = null
{visible.map((log, i) => {
  const globalIndex = page * pageSize + i
  const needsHeader = log.category !== prevCategory
  prevCategory = log.category
  return (
    <React.Fragment key={log.id}>
      {needsHeader && (
        <div style={{...}}>
          § {t('pestDisease.category.' + log.category)}
        </div>
      )}
      <button ...>
        <span>{String(globalIndex + 1).padStart(2, '0')}</span>
        ...
      </button>
    </React.Fragment>
  )
})}

<LedgerPagination page={page} pageSize={pageSize} total={sortedLogs.length} onChange={setPage} />
```

Reset `prevCategory = null` before the map so the first row of each page gets its header regardless of what was on the previous page. Wrap the map body in an `{(() => { let prevCategory: string | null = null; return visible.map(...) })()}` IIFE so the `prevCategory` variable doesn't leak.

### Step 3: VarietyTrials

Open `web/src/pages/VarietyTrials.tsx`. Cards rendered via `.map(...)` over `trials`:

```tsx
import { LedgerPagination, ... } from '../components/faltet'

const [page, setPage] = useState(0)
const pageSize = 50

const filtered = /* existing season filter */
const visible = filtered.slice(page * pageSize, (page + 1) * pageSize)

useEffect(() => { setPage(0) }, [seasonId])

{visible.map((trial, i) => {
  const globalIndex = page * pageSize + i
  return (
    /* existing card JSX using globalIndex where i was used for display */
  )
})}

<LedgerPagination page={page} pageSize={pageSize} total={filtered.length} onChange={setPage} />
```

The file renamed `t` to `tr` for the translation function (to avoid shadowing `trial`); keep that convention.

### Step 4: BouquetRecipes

`web/src/pages/BouquetRecipes.tsx`. Uses expand-collapse for items. `expanded: Set<number>` state of recipe ids:

```tsx
import { LedgerPagination, ... } from '../components/faltet'

const [page, setPage] = useState(0)
const pageSize = 50

const visible = recipes.slice(page * pageSize, (page + 1) * pageSize)

useEffect(() => { setPage(0) }, [recipes.length])

{visible.map((r, i) => {
  const globalIndex = page * pageSize + i
  return (
    <div key={r.id}>
      <button onClick={() => toggle(r.id)}>
        <span>{String(globalIndex + 1).padStart(2, '0')}</span>
        ...
      </button>
      {expanded.has(r.id) && (/* expanded items list */)}
    </div>
  )
})}

<LedgerPagination page={page} pageSize={pageSize} total={recipes.length} onChange={setPage} />
```

Expansion state `expanded: Set<number>` is by id, so recipes on other pages stay "expanded" internally but aren't rendered. When user navigates back, they reappear already expanded. Acceptable per spec §3.

### Step 5: SeedInventory

`web/src/pages/SeedInventory.tsx`. Grouped-by-species rendering. Paginate the outer species-group count:

```tsx
import { LedgerPagination, ... } from '../components/faltet'

const [page, setPage] = useState(0)
const pageSize = 50

const groupedEntries = Object.entries(groupedBySpecies)  // or whatever the current grouping produces
const visibleGroups = groupedEntries.slice(page * pageSize, (page + 1) * pageSize)

useEffect(() => { setPage(0) }, [groupedEntries.length])

{visibleGroups.map(([speciesId, batches], i) => {
  const globalIndex = page * pageSize + i
  return (
    /* existing species-group row JSX with globalIndex */
  )
})}

<LedgerPagination page={page} pageSize={pageSize} total={groupedEntries.length} onChange={setPage} />
```

Batches within an expanded species group stay together (don't paginate nested batches). Page size 50 species groups is generous.

### Step 6: Supplies

`web/src/pages/Supplies.tsx` has multiple sections. Find the largest-scaling section (typically the inventory batches list). Add pagination only to that section:

```tsx
import { LedgerPagination, ... } from '../components/faltet'

const [inventoryPage, setInventoryPage] = useState(0)
const inventoryPageSize = 50

const visibleInventory = inventoryList.slice(
  inventoryPage * inventoryPageSize,
  (inventoryPage + 1) * inventoryPageSize
)

useEffect(() => { setInventoryPage(0) }, [inventoryList.length])

{/* in the inventory rendering block */}
{visibleInventory.map(...)}
<LedgerPagination
  page={inventoryPage}
  pageSize={inventoryPageSize}
  total={inventoryList.length}
  onChange={setInventoryPage}
/>
```

Grep the file for the existing top-level state declarations to place the new page state alongside. The other sections (supply types, usage log) stay unpaginated.

### Step 7: Verify

```bash
cd /Users/erik/development/verdant/web && npx tsc --noEmit && npm run test && npm run build 2>&1 | tail -3
```

All green.

### Step 8: Commit + milestone

```bash
git add web/src/pages/SuccessionSchedules.tsx \
        web/src/pages/PestDiseaseLog.tsx \
        web/src/pages/VarietyTrials.tsx \
        web/src/pages/BouquetRecipes.tsx \
        web/src/pages/SeedInventory.tsx \
        web/src/pages/Supplies.tsx
git commit -m "feat: pagination on SuccessionSchedules, PestDiseaseLog, VarietyTrials, BouquetRecipes, SeedInventory, Supplies"

git commit --allow-empty -m "milestone: pagination restoration complete"
```

---

## Verification summary

After task 3:
- `<Ledger paginated>` on 4 pages.
- `<LedgerPagination>` on 6 pages.
- Vitest tests cover both primitives' pagination behavior.
- `npx tsc --noEmit`, `npm run test`, `npm run build` all green.
- Four small pages (SeasonList, SpeciesGroups, WorkflowTemplates, GardenList) remain unpaginated by design.
