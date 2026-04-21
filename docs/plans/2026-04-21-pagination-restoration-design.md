# Pagination Restoration — Design

**Status:** approved for planning (2026-04-21)
**Target:** Verdant web admin (React + TypeScript).
**Driver:** restore pagination on the ledger pages that lost it during the spec 2.1 Fältet port. Client-side only. Fältet-styled controls.

---

## 1. Scope

Spec 2.1 dropped `<Pagination>` from 14 ledger pages when porting them to `<Ledger>`. Most pages fetch full lists client-side (50–500 rows typically), so rendering everything is currently fine — but six pages realistically grow to scale where users will want pagination.

**In scope — 10 pages:**

*Ledger-driven (4):*
SpeciesList, CustomerList, PlantedSpeciesList, ProductionTargets.

*Inline-rendered (6):*
SuccessionSchedules, PestDiseaseLog, VarietyTrials, BouquetRecipes, SeedInventory, Supplies.

**Not in scope:**
- The 4 genuinely small pages: SeasonList, SpeciesGroups, WorkflowTemplates, GardenList. These rarely exceed 20 rows; pagination controls would be visual clutter.
- Server-side pagination. All endpoints keep their current "list everything" shape. If a single endpoint's response hits a real performance ceiling (5000+ rows), swap that one endpoint to server-side per-request.
- User-configurable rows-per-page selector.
- Jump-to-page input.
- `"N of M pages"` indicator.

---

## 2. API extensions

### `<Ledger>` (`web/src/components/faltet/Ledger.tsx`)

New props:

```ts
type LedgerProps<T> = {
  // existing props untouched …
  paginated?: boolean     // default false — no pagination, renders all rows
  pageSize?: number       // default 50
}
```

Behavior:

- `paginated = false` → renders every row as today.
- `paginated = true` → component manages `useState<number>` `page`. Rows are sliced after any `sectionHeaders` logic: `rows.slice(page * pageSize, (page + 1) * pageSize)`. Controls render beneath the ledger body.
- Controls always render when `paginated = true`, even when only one page exists. Disabled arrows + "1–N AV N" label. Keeps page layout stable during data loads.
- When `rows` changes (search, filter, etc.), page index resets to 0 via a `useEffect` comparing the row IDs.

### New primitive: `<LedgerPagination>` (`web/src/components/faltet/LedgerPagination.tsx`)

For pages that render inline rather than through `<Ledger>`:

```ts
type LedgerPaginationProps = {
  page: number
  pageSize: number
  total: number
  onChange: (nextPage: number) => void
}
```

Renders the same centered mono control row with a `<Rule variant="soft" />` divider above. Stateless — parent owns the page index. Hides itself when `total === 0` (no empty-state noise).

### Shared control layout

Both `<Ledger paginated>` and `<LedgerPagination>` render:

```
─────────────────────  (Rule variant="soft")

         ← 1–50 AV 237 →
```

- Flex row, centered, `padding: 14px 0`, `gap: 18px`.
- `←` and `→` are `<button>` elements: `background: transparent; border: none; color: var(--color-clay); font-family: var(--font-mono); font-size: 14px; cursor: pointer;` with `opacity: 0.3; cursor: default;` when disabled.
- Counter: mono 10 `letter-spacing 1.4` uppercase `var(--color-forest)`, content `{from}–{to} {pagination.of} {total}`.
- `from = page * pageSize + 1`, `to = Math.min((page + 1) * pageSize, total)`.

### i18n additions

`pagination.of` → "av" / "of" (joining word — reuse `pagination.showing` key's pattern if it already uses a template string; otherwise add `pagination.of`).

---

## 3. Page ports

### Ledger-driven — add `paginated` prop

These use `<Ledger>` directly. Change is one prop:

- `SpeciesList.tsx` — `<Ledger paginated pageSize={50} ...>`.
- `CustomerList.tsx` — same.
- `PlantedSpeciesList.tsx` — same.
- `ProductionTargets.tsx` — the existing expand-collapse forecast panels sit inside the row-rendering callback; they stay local to whichever 50 rows are on the current page. Per-row `<ForecastPanel>` queries continue to fire on demand.

### Inline-rendered — use `<LedgerPagination>`

Manual pagination state lives in each page:

```ts
const [page, setPage] = useState(0)
const pageSize = 50
const rows = filtered          // whatever the page's existing rows array is
const slice = rows.slice(page * pageSize, (page + 1) * pageSize)
// render slice instead of rows …
<LedgerPagination page={page} pageSize={pageSize} total={rows.length} onChange={setPage} />
```

Pages and notes:

- **`SuccessionSchedules.tsx`** — replace the existing custom `<Pagination>` call. Consolidates to the shared primitive.
- **`PestDiseaseLog.tsx`** — category-grouped rows. The flat sorted `logs` array is what gets paginated; `§ CATEGORY` section headers still render inline where category changes within the current slice. If a category group spans two pages, each page just shows the portion it contains with its own header re-rendered at the top. Acceptable.
- **`VarietyTrials.tsx`** — card-style rows. Paginate the flat list.
- **`BouquetRecipes.tsx`** — expand-collapse inline items. Expansion state is per recipe id; when page changes, expanded recipes on other pages stay expanded in state but aren't visible. Acceptable — returning to their page preserves expansion.
- **`SeedInventory.tsx`** — grouped by species, each outer row expands to show batches. Paginate the outer species-group count.
- **`Supplies.tsx`** — three internal sections (types / inventory / usage log or similar per current file). The longest section gets paginated; smaller sections ignore pagination. If only one section is long in practice, only that one is paginated.

### Page-reset on filter/search changes

Ledger-driven pages that also use `<LedgerFilters>` (SpeciesList, CustomerList) already reset via the internal `useEffect` in `<Ledger>` (watches row identity). Inline pages with filters need `setPage(0)` wired into filter/search `onChange` handlers explicitly.

---

## 4. Testing

- `Ledger.test.tsx` gains two tests:
  - `paginated prop slices rows and shows N–N OF total`.
  - `previous/next buttons change the visible slice`.
- `LedgerPagination.test.tsx` — new file:
  - renders buttons and counter.
  - disables `←` on page 0, `→` on last page.
  - `onChange` fires on click.
  - hides itself when `total === 0`.

No per-page tests; the existing Playwright smokes continue to validate that each route renders.

---

## 5. Phasing

Three tasks.

| Task | Scope |
|---|---|
| 1 | `<Ledger>` + `<LedgerPagination>` + vitest tests + i18n |
| 2 | Port 4 Ledger pages (add `paginated` prop) |
| 3 | Port 6 inline pages + milestone |

Each task ends with `cd web && npx tsc --noEmit && npm run test && npm run build 2>&1 | tail -3` green + its own commit.

---

## 6. Open questions

None as of 2026-04-21.
