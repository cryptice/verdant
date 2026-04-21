# Verdant Dashboard Redesign — Spec 3: CropCalendar, Analytics, WorkflowProgress

**Status:** approved for planning (2026-04-21)
**Target:** Verdant web admin (React + TypeScript + Tailwind v4 + Vite)
**Driver:** port the three pages that specs 1 + 2 deferred because they required genuine design decisions beyond applying established Fältet patterns. Each of the three has a unique layout shape with no handoff mock — calendar/Gantt, charts, and workflow-progress ledger.

**Depends on:** Spec 1 (tokens, primitives, sidebar), Spec 2.1 (Ledger + LedgerFilters + 14 ledger pages), Spec 2.2 (16 form/detail/settings/public pages). All landed.
**Concludes:** the web-wide Fältet rollout.

---

## 1. Scope

Three pages:

- `CropCalendar.tsx` (249 lines) — species × months Gantt grid.
- `Analytics.tsx` (243 lines) — three analytics sections (season overview, species comparison, yield per bed).
- `WorkflowProgress.tsx` (263 lines) — per-species workflow step progress with per-step plant drill-down.

**Not in scope:**

- New chart primitive library. Analytics uses div-width bars in the established Fältet palette. Flagged as future work only if multiple pages need richer charts.
- CropCalendar month-header change from single letters (J F M A M J J A S O N D) — keep single letters; they match the Fältet mono-small-caps aesthetic at the intended density.
- Real harvest-aggregate analytics endpoint. Current endpoints are consumed as-is; hardcoded zeros / placeholder stats carried forward from specs 1 + 2.2 stay flagged.
- Pagination restoration — spec 2.1 carry-forward.
- Real photography, self-hosted fonts, visual regression harness, accessibility audit.

---

## 2. CropCalendar

### Layout

- `<Masthead left="Kalender" center="— Odlingskalender —">`.
- Season selector row below masthead (separate from Masthead right slot because it affects page data, not chrome): mono small-caps label `SÄSONG` + native `<select>` styled with `selectStyle` helper from `BedForm.tsx` (bottom-rule Fraunces 20).
- Legend strip above the calendar: three `<Chip>` pills `<Chip tone="mustard">Sådd</Chip>` `<Chip tone="sage">Blomning</Chip>` `<Chip tone="clay">Skörd</Chip>`. Gap 10, margin-bottom 22.
- Calendar grid — CSS `grid-template-columns: 200px repeat(12, 1fr)`. Full-width within page padding.
  - **Header row.** 200px empty cell + 12 month cells. Each month cell: mono 9 uppercase `letter-spacing 1.4` centered, color `forest @ 0.7`. 1px ink bottom border across the entire header row. Vertical hairlines between cells: 1px `ink/20`.
  - **Species rows.** Each row's 200px cell contains the species name in Fraunces 20 weight 300 with italic Latin below in Fraunces 9 sage when present. The 12-cell month strip is a single relative-positioned container where three absolute-positioned 6px hairline bars (sow/bloom/harvest) render at `top: 14px / 22px / 30px` respectively. Each bar uses `left` + `width` computed from the range's month indices — preserve the existing `barStyle()` math. Bars use the tone variables: `mustard` for sow, `sage` for bloom, `clay` for harvest. Row height 44px. Row bottom border 1px `ink/20`. Row hover fills `color-mix(in srgb, var(--color-ink) 4%, transparent)`.
- Empty state (no season selected or no rows): single full-width cell spanning grid with mono 9 small-caps "Inga data ännu" + italic Fraunces 16 blurb "Välj en säsong ovan för att se kalendern."

### Data / behavior

Preserve all existing hooks (`api.seasons.list()`, `api.species.list()`, `api.plants.list()`, `api.plants.events(id)`) and the `rows` `useMemo` computation.

---

## 3. Analytics

### Layout

- `<Masthead left="Analys" center="— Siffrorna —">`.
- Three vertically stacked sections. Each section heading = italic Fraunces 30 title with clay period + optional right-aligned control + `<Rule inline variant="ink" />`. Gap 40 between sections.

**§ Säsongsöversikt**

Horizontal flex row of season cards, overflow-x auto, gap 20, scroll-snap.

Each card: 280px wide, `background: var(--color-paper)`, 1px ink border, padding 22 28, zero radius, no shadow.
- Top row: mono 10 small-caps season name + mono 10 year on the right (both forest color).
- Big stat: Fraunces 44 weight 300 `totalStemsHarvested.toLocaleString()` with mono small-caps "stjälkar" below.
- 1px ink/20 divider.
- 2×2 mono meta grid: Plantor / Arter / Bästa art / (empty), each cell = mono 9 small-caps label + Fraunces 14 value. Top-species gets a Chip tone=clay rendering.

**§ Artjämförelse**

Above: `<SpeciesAutocomplete>` component preserved. On a side-by-side row with a mono label.

Below when a species is selected: per-season horizontal bar rows. Each row is a 4-column grid `100px 80px 1fr 60px`:
- Mono 9 small-caps season label (left).
- Tabular Fraunces 20 stem count.
- Bar container: 1px ink border, paper bg, 8px tall, with an inner `div` filled `var(--color-clay)` and width computed as `(stems / maxStems) * 100%`.
- Mono 10 small-caps percentage (right).

Row bottom border 1px `ink/20`. Empty state mono 9 small-caps when no species picked.

**§ Skörd per bädd**

Ledger-style table with seasons as columns. Header row: 1px ink bottom, mono 9 small-caps. First column 180px width, remaining columns equal `1fr`.

Body rows: bed name in Fraunces 20 + bed emoji or name. Stems cells tabular Fraunces 18 weight 300. Empty cells show `—` in mono 10 `clay @ 0.4`. Row bottom 1px `ink/20`.

### Data / behavior

Preserve all existing hooks: `api.analytics.seasonSummaries()`, `api.analytics.speciesComparison(id)`, `api.analytics.yieldPerBed()`. Preserve `compSpecies` state, `maxStems` memo, `allSeasons` memo.

---

## 4. WorkflowProgress

### Layout

- `<Masthead left={Breadcrumb} center="— Arbetsflöde —">`. Breadcrumb: `Arter / <species name>` with the last crumb in clay.
- Compact hero strip (not the full detail-pattern hero): Fraunces 44 common name (with optional italic clay variant) + mono 10 meta `"{activePlants} aktiva plantor · {stepsComplete} steg klara av {totalSteps}"`.
- Step ledger: 5-column CSS grid `40px 1.5fr 100px 140px 120px`:
  1. № marker — italic Fraunces 22, color picked by completion:
     - all plants completed this step → `sage`
     - some but not all → `mustard`
     - none yet → `forest`
  2. Step name — Fraunces 20.
  3. `daysAfterPrevious` — mono 10 small-caps "DAG +N" or blank when null.
  4. Event-type `<Chip>` — tone picked by event category (SOWN=mustard, HARVESTED=clay, others=forest). When `eventType` is null or `NOTE`, no chip.
  5. Completion progress — mono 10 `{done}/{total}` + 60px 1px-ink bordered paper strip with inner clay fill sized by percentage.

Row bottom border 1px `ink/20`. Row click toggles inline expansion in `useState<Set<number>>` (step IDs).

**Inline expansion** renders beneath the row in a nested mini-ledger:
- Padding 10px 78px (left indent past the № column).
- Each plant row — 3-column grid `1fr 200px 100px`: plant display name (Fraunces 16), bed reference (mono 10), date-step-reached (mono 10).
- Row bottom border 1px `color-mix(in srgb, var(--color-ink) 15%, transparent)`.

**Side branches** — if the current page handles side branches, render a second `§ Sidspår` block below the main step list. Header = italic Fraunces 20 `Sidspår: {name}` + clay period. Same ledger shape beneath.

### Data / behavior

Preserve `useQuery` hooks for species + workflow template + per-step plant grouping. Preserve reorder / add / delete if this screen has them (spec says no — it's a read-only progress view, but double-check the current file).

---

## 5. i18n

Editorial taglines to add where missing (grep first):

```json
"calendar": {
  "masthead": { "center": "— Odlingskalender —" / "— Cultivation Calendar —" },
  "legend":   {
    "sow":     "Sådd"      / "Sowing",
    "bloom":   "Blomning"  / "Bloom",
    "harvest": "Skörd"     / "Harvest"
  },
  "seasonLabel": "SÄSONG" / "SEASON",
  "emptyTitle":  "Inga data ännu" / "No data yet",
  "emptyBody":   "Välj en säsong ovan för att se kalendern." / "Choose a season above to view the calendar."
},
"analytics": {
  "masthead": { "center": "— Siffrorna —" / "— The Numbers —" },
  "section": {
    "seasonOverview":    "Säsongsöversikt"  / "Season overview",
    "speciesComparison": "Artjämförelse"    / "Species comparison",
    "yieldPerBed":       "Skörd per bädd"   / "Yield per bed"
  },
  "card": {
    "stems":    "stjälkar" / "stems",
    "plants":   "Plantor"  / "Plants",
    "species":  "Arter"    / "Species",
    "topSpecies": "Bästa art" / "Top species"
  }
},
"workflows": {
  "progress": {
    "masthead":      { "center": "— Arbetsflöde —" / "— Workflow —" },
    "activePlants":  "{{count}} aktiva plantor" / "{{count}} active plants",
    "stepsComplete": "{{done}} steg klara av {{total}}" / "{{done}} of {{total}} steps complete",
    "sideBranch":    "Sidspår" / "Side branch",
    "day":           "DAG +{{n}}" / "DAY +{{n}}"
  }
}
```

Existing keys reused.

---

## 6. Phasing

Single plan, 4 tasks.

| Task | Name | Page |
|---|---|---|
| 1 | CropCalendar port | CropCalendar.tsx |
| 2 | Analytics port | Analytics.tsx |
| 3 | WorkflowProgress port | WorkflowProgress.tsx |
| 4 | Milestone | empty commit |

Each task ends with `cd web && npx tsc --noEmit && npm run build 2>&1 | tail -3` green + commit.

---

## 7. Testing

- No new primitives → no new vitest.
- Playwright smoke per route (`/calendar`, `/analytics`, `/workflows/progress/:speciesId`) — navigate, assert masthead renders, no console errors.
- Typecheck + build after every task commit.

---

## 8. Open questions

None as of 2026-04-21 — all scoping decisions recorded above.
