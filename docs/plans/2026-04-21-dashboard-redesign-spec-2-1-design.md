# Verdant Dashboard Redesign — Spec 2.1: Ledger Pages

**Status:** approved for planning (2026-04-21)
**Target:** Verdant web admin (React + TypeScript + Tailwind v4 + Vite)
**Driver:** port the 14 ledger-shaped screens to Fältet, introducing shared `<Ledger>` + `<LedgerFilters>` primitives that cover the simple cases and leaving the structurally unusual pages as inline Fältet ports.

**Depends on:** Spec 1 (tokens, typography, primitives, sidebar, 4 screens) — already landed.
**Precedes:** Spec 2.2 (forms + details + settings + public, 16 screens), Spec 3 (CropCalendar + Analytics + WorkflowProgress, needs design input).

---

## 1. Scope

Fourteen pages visually inherit the Fältet chrome from spec 1 but still render their content in the pre-Fältet layout. This spec replaces their content rendering with the Fältet ledger aesthetic — mono small-caps header rows, hairline ink/20 body rows, № markers in tone-colored Fraunces italic, optional filter pill rows, Fraunces italic section titles with clay periods, Rule inline dividers.

**In scope (14 pages):**

*Batch A — `<Ledger>`-driven (7):*
GardenList, SpeciesList, PlantedSpeciesList, SeasonList, CustomerList, WorkflowTemplates, SpeciesGroups.

*Batch B — inline Fältet port (7):*
PestDiseaseLog, VarietyTrials, BouquetRecipes, SuccessionSchedules, ProductionTargets, SeedInventory, Supplies.

**Not in scope:**

- Forms, details, settings, public pages (spec 2.2).
- CropCalendar / Analytics / WorkflowProgress (spec 3).
- Real photography, self-hosted fonts, visual regression harness, accessibility audit.
- Any page's data layer or backend call shape — only rendering changes.
- Re-engineering of complex interaction patterns (Supplies' 7-dialog workflow, SeedInventory's batch expansion, etc.) — these keep their logic; only visuals change.

---

## 2. New primitives

Two additions under `web/src/components/faltet/`, both exported via the existing barrel.

### `Ledger.tsx`

Generic structural component for tabular ledger pages.

```ts
type LedgerColumn<T> = {
  key: string
  label: string
  width?: string
  align?: 'left' | 'right'
  render?: (row: T, index: number) => React.ReactNode
}

type LedgerProps<T> = {
  columns: LedgerColumn<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  onRowClick?: (row: T) => void
  emptyMessage?: string
  sectionHeaders?: (row: T, index: number, prev: T | null) => React.ReactNode | null
}
```

Rendering contract:

- Builds a CSS `grid-template-columns` track list from `width || '1fr'` for each column.
- **Header row** — same grid; each cell is mono 9px uppercase `letter-spacing 1.4` `color: forest @ 0.7` with `text-align: right` when `align === 'right'`. 1px solid ink bottom border.
- **Body rows** — same grid; 12–14px vertical padding; hairline ink/20 bottom border. Hover fills `color-mix(in srgb, var(--color-ink) 4%, transparent)` when `onRowClick` is defined. Row is a `<button>` in that case for accessibility, inline `div` otherwise.
- **Default cell render** — if `column.render` absent, renders `String(row[column.key])` in Fraunces 16 non-italic.
- **Empty state** — when `rows.length === 0`, renders a single cell spanning the full grid: mono 9 small-caps "Inga rader än" (or `emptyMessage` override) above an italic Fraunces 16 blank-slate blurb.
- **`sectionHeaders`** — called per row with `(row, i, prev)`; returning `null` means no header. Returning a ReactNode renders it as a full-width row above that data row (typically `§ PEST` / `§ DISEASE` style — use mono small-caps 0.7 opacity `padding: 16 0 6` with `<Rule variant="soft" />` below).

### `LedgerFilters.tsx`

Segmented pill filter row, extracted from TaskList's pattern.

```ts
type LedgerFilterOption<Id extends string> = {
  id: Id
  label: string
  tone?: 'clay' | 'mustard' | 'berry' | 'sky' | 'sage' | 'forest'
}

type LedgerFiltersProps<Id extends string> = {
  options: LedgerFilterOption<Id>[]
  value: Set<Id>
  onChange: (next: Set<Id>) => void
  atLeastOne?: boolean                      // default true
  storageKey?: string                        // when set, persist to localStorage
}
```

Rendering contract:

- Flex-wrap row, gap 8, margin-bottom 22.
- Each pill is a `<button>` — mono 10px 1.4ls uppercase, padding 6/12, `border-radius: 999`, 1px border in the option's tone color (defaulting `forest`). Active state: filled bg + cream text. Inactive: transparent bg, tone-colored text.
- Click toggles membership in `value`. If `atLeastOne` and the option is currently the only active one, click is a no-op.
- If `storageKey` is set, the component writes `value` to `localStorage[storageKey]` on change and reads initial value from it on mount (overriding `value` prop on first render). Caller owns the source of truth otherwise.

### Tests (vitest)

- `Ledger.test.tsx` — renders header labels, renders rows through default + custom `render`, calls `onRowClick` on click, shows empty state when `rows=[]`, renders section header between groups.
- `LedgerFilters.test.tsx` — toggling adds/removes id from set, `atLeastOne` prevents clearing last item, `storageKey` persists across reloads (mocked).

### Barrel update

`web/src/components/faltet/index.ts` gains `export { Ledger } from './Ledger'` and `export { LedgerFilters } from './LedgerFilters'`.

---

## 3. Screen ports

Every port replaces the page's top-level rendering but preserves:

- All existing data hooks (`api.xxx.yyy()` calls, TanStack Query keys).
- All existing mutations (create / update / delete flows).
- All existing routing (entry points from sidebar, navigation from other pages).
- i18n keys already defined for that page's content.

### Batch A — `<Ledger>`-driven (7)

Per-page spec is one paragraph + a column config table. Full implementation details land in the plan.

**`GardenList`** — Masthead `Trädgårdar / — Trädgårdsliggaren —`. No filters. Columns: № (italic Fraunces 22 sage, padded), Namn (Fraunces 20 + emoji prefix if present), Antal bäddar (tabular 20), → (right-aligned, clay). Row click → `/garden/:id`. Inline "+ Ny trädgård" dialog preserved (opens existing form).

**`SpeciesList`** — Masthead `Arter / — Artregister —`. Filters: `LedgerFilters` over `plantType` enum (ANNUAL/PERENNIAL/BULB/TUBER). Columns: № (clay italic), Art (Fraunces 20 common name + italic Fraunces 9 Latin below), Sort (italic Fraunces clay, variant name), Typ (small Chip with plantType tone), → (right). Row click → `/species/:id`. "+ Ny art" existing.

**`PlantedSpeciesList`** — Masthead `Plantor / — Planterade arter —`. Filters: status enum (SEEDED / POTTED_UP / PLANTED_OUT / HARVESTED / REMOVED). Columns: № (sage italic), Art (Fraunces 20), Antal plantor (tabular 20), Status (StatusBadge reused), → (right). Row click → `/species/:speciesId/plants`.

**`SeasonList`** — Masthead `Säsonger / — Säsongsliggaren —`. No filters. Columns: № (mustard italic), Säsong (Fraunces 20 name), År (tabular 20), Aktiv (● clay bullet when active, else empty), Frost-datum (mono 10 "start → slut"), → (right). Row click opens existing edit dialog. "+ Ny säsong" preserved.

**`CustomerList`** — Masthead `Kunder / — Kundliggaren —`. Filters: `Channel` enum (FLORIST/FARMERS_MARKET/CSA/WEDDING/WHOLESALE/DIRECT/OTHER) as `LedgerFilters`. Columns: № (berry italic), Namn (Fraunces 20), Kanal (Chip with channel tone), Kontakt (mono 10, truncated), → (right). Row click opens existing edit dialog.

**`WorkflowTemplates`** — Masthead `Arbetsflöden / — Mallregister —`. No filters. Columns: № (sage italic), Mall (Fraunces 20 name + italic description below), Steg (tabular 20 step count), → (right). Row click → `/workflows/:id/edit`. "+ Ny mall" preserved.

**`SpeciesGroups`** — Masthead `Artgrupper / — Grupper —`. No filters. Columns: № (forest italic), Grupp (Fraunces 20 name), Antal arter (tabular 20), → (right). Row click → `/species-groups/:id/edit`. "+ Ny grupp" preserved.

### Batch B — inline Fältet port (7)

Each page keeps its current structural logic (expand/collapse, grouping, nested dialogs) but replaces all rendering chrome with Fältet primitives.

**`PestDiseaseLog`** — Rows grouped by `PestCategory`; use inline section headers (`§ PEST` / `§ DISEASE` / `§ DEFICIENCY` / `§ OTHER` in mono small-caps with `<Rule variant="soft" />` below). Each row: № (category-toned italic Fraunces), Fraunces 20 title, severity chip + outcome chip on the right, mono date column. Filter pills over category via `LedgerFilters` optional toggle. Existing create + image-upload dialogs preserved.

**`VarietyTrials`** — Each trial renders as a card-style row (not a dense ledger): № marker clay, Fraunces 26 species name, italic Fraunces clay variant, quality-score as Stat-size small (Fraunces 32), verdict chip (KEEP/EXPAND/REDUCE/DROP/UNDECIDED) + reception chip (LOVED/LIKED/NEUTRAL/DISLIKED). Existing create dialog preserved.

**`BouquetRecipes`** — Outer row per recipe: № + name + price + item-count mono. Click expands an inline items list (existing expand state preserved) — items rendered as a nested mini-ledger (species name, quantity, role chip). `+ Lägg till buket` existing.

**`SuccessionSchedules`** — Each row: № clay, species Fraunces 20, first sow date mono, interval/cadence mono, total-successions tabular, "Generera uppgifter →" mono small-caps clay action button inline-right. Existing create dialog preserved.

**`ProductionTargets`** — Row: № sage, species Fraunces 20, stems/week tabular, delivery window mono. Expand state shows forecast panel (existing): mono totalStemsNeeded + plantsNeeded + seedsNeeded + italic suggestedSowDate, with warnings chipified in berry. Existing create dialog preserved.

**`SeedInventory`** — Grouped by species; each species row is a collapsible header (Fraunces 20 species + italic Latin + total-remaining tabular); expanded state reveals batch rows as a nested mini-ledger (variant, sown-year tabular, remaining tabular, cost mono). Existing "+ Lägg till utsäde" dialog preserved.

**`Supplies`** — The 1015-line page. Port with minimal structural change: existing three-section layout (supply types, inventory batches, maybe a usage log) stays; each section becomes a Fältet ledger. All seven existing dialogs keep their logic, restyled to Fältet chrome (cream paper, 1px ink, zero radius, mono header strip matching modal masthead pattern from spec 1). `CategoryPropertyFields` sub-component keeps its validation (mm/ml warnings) but fields render via `<Field>` primitive with `accent="clay"` on error.

### Row-click behaviour summary

| Page | Click target |
|---|---|
| GardenList | `/garden/:id` |
| SpeciesList | `/species/:id` |
| PlantedSpeciesList | `/species/:speciesId/plants` |
| SeasonList | inline edit dialog |
| CustomerList | inline edit dialog |
| WorkflowTemplates | `/workflows/:id/edit` |
| SpeciesGroups | `/species-groups/:id/edit` |
| PestDiseaseLog | inline edit dialog |
| VarietyTrials | inline edit dialog |
| BouquetRecipes | inline expand (items) |
| SuccessionSchedules | inline edit dialog |
| ProductionTargets | inline expand (forecast) |
| SeedInventory | inline expand (batches) |
| Supplies | inline expand / dialog (various) |

All are preserved from current behaviour — this spec doesn't touch navigation design.

---

## 4. i18n

No new translation work for page content — every page's copy already lives in `sv.json` / `en.json`.

**New keys:**

```json
"common": {
  "ledger": {
    "empty":       "Inga rader än" / "No rows yet",
    "filterClear": "Rensa filter" / "Clear filters"
  }
}
```

Existing keys are reused verbatim. Swedish-first.

---

## 5. Phasing

Single plan, seven tasks.

| # | Name | Scope |
|---|------|------|
| 1 | Ledger primitives | `Ledger.tsx`, `LedgerFilters.tsx`, barrel, vitest tests, common.ledger.* i18n |
| 2 | Batch A, simplest five | GardenList, SeasonList, SpeciesGroups, WorkflowTemplates, PlantedSpeciesList |
| 3 | Batch A, two with filters | SpeciesList, CustomerList |
| 4 | Batch B, smaller three | BouquetRecipes, SuccessionSchedules, ProductionTargets |
| 5 | Batch B, bigger three | PestDiseaseLog, VarietyTrials, SeedInventory |
| 6 | Supplies | The 1015-line page, its own task |
| 7 | Milestone | Empty commit marking spec 2.1 complete |

Each task ends with `npx tsc --noEmit` + `npm run build` green + its own commit. Rough sizing: tasks 2–5 are roughly equal (~2-3 pages each, pattern-driven). Task 6 is the largest because of Supplies. Task 1 is short but foundational.

---

## 6. Testing

- `<Ledger>` and `<LedgerFilters>` vitest tests shipped with task 1.
- No page-level vitest (rendering + data hooks are covered by Playwright).
- Playwright smoke: for each of the 14 routes, navigate + assert masthead renders + assert at least one ledger row OR the empty-state blurb is visible. No deep interaction tests.
- `npx tsc --noEmit` and `npm run build` green after every task commit.

---

## 7. Open questions

None as of 2026-04-21 — all scoping decisions recorded above.
