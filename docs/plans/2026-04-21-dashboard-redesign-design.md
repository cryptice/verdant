# Verdant Dashboard Redesign — Design (Spec 1 of 3)

**Status:** approved for planning (2026-04-21)
**Target:** Verdant web admin (React + TypeScript + Tailwind v4 + Vite)
**Driver:** port the four high-fidelity Fältet screens from `docs/design_handoff_verdant_dashboard/` to the real codebase, and lay the token / primitive / chrome foundation that spec 2 will use to port the remaining ~30 screens.

---

## 1. Scope

The handoff covers four pixel-locked screens in the Fältet visual direction — Dashboard, Uppgifter (tasks), Bädd (bed detail), Redigera art (species edit modal) — plus the shared token system, typography, and sidebar chrome. This spec ships those four screens and the foundation; spec 2 ports the other ~25 list + form screens; spec 3 handles the three screens the handoff doesn't cover (CropCalendar, Analytics, WorkflowProgress).

**User chose a full-app visual redesign** (not just 4 isolated screens): therefore all chrome changes (sidebar, mastheads, tokens, typography, button/card/input primitives) land globally in spec 1. Screens not yet individually ported will render on cream + Fraunces + new sidebar but keep their legacy layouts until spec 2.

**Not in scope for spec 1:**
- Porting screens other than the four designed ones.
- Real photography (every mock uses `PhotoPlaceholder`).
- Visual regression harness.
- Self-hosted fonts (Google CDN now, self-host as a follow-up).
- IA restructuring (navigation items preserved; only grouping + styling change).

**Integration decisions recorded from brainstorming:**
- **Sidebar nav** keeps every current destination reachable at one click, grouped under mono small-caps headers (`§ ODLING`, `§ UPPGIFTER`, `§ SKÖRD & FÖRSÄLJNING`, `§ ANALYS`, `§ KONTO`) in the Fältet style.
- **Species editing** happens in both the modal (from Bed-row clicks) and the full-page route (deep links, admin). Both surfaces share one `<SpeciesEditForm />` component.
- **"Skörd"** sidebar item maps to `/analytics?tab=harvest` (minimal scope; no new view).
- **Token strategy:** replace the `@theme` block in `index.css`; keep compatibility aliases for existing utilities so every page automatically takes the new chrome.
- **Motion:** add `framer-motion` dependency for the modal + bottom-sheet transitions and drag-to-dismiss.

---

## 2. Token layer

### Palette (replaces current `@theme` block)

Primary tokens:

| Token | Hex | Usage |
|---|---|---|
| `--color-cream` | `#F5EFE2` | base backgrounds |
| `--color-paper` | `#FBF7EC` | cards, modal body |
| `--color-ink` | `#1E241D` | text, borders, dark surfaces |
| `--color-forest` | `#2F3D2E` | secondary text, labels |
| `--color-sage` | `#6B8F6A` | planting, positive |
| `--color-clay` | `#B6553C` | primary accent, harvest, destructive |
| `--color-mustard` | `#C89A2B` | sowing |
| `--color-berry` | `#7A2E44` | maintenance, perennials |
| `--color-sky` | `#4A7A8C` | watering |
| `--color-butter` | `#F2D27A` | decorative fills |
| `--color-blush` | `#E9B8A8` | decorative fills |

Compatibility aliases (so existing utilities keep compiling against the new palette):

```css
--color-bg:             var(--color-cream);
--color-surface:        var(--color-cream);
--color-sidebar:        var(--color-cream);
--color-divider:        color-mix(in srgb, var(--color-ink) 20%, transparent);
--color-text-primary:   var(--color-ink);
--color-text-secondary: var(--color-forest);
--color-text-muted:     color-mix(in srgb, var(--color-forest) 60%, transparent);
--color-accent:         var(--color-clay);
--color-accent-hover:   color-mix(in srgb, var(--color-clay) 85%, var(--color-ink));
--color-accent-light:   var(--color-paper);
--color-warm:           var(--color-paper);
--color-error:          var(--color-clay);
```

### Typography

```css
--font-display: "Fraunces", Georgia, serif;
--font-body:    "Inter", -apple-system, BlinkMacSystemFont, sans-serif;
--font-mono:    ui-monospace, "SF Mono", Menlo, monospace;
```

Fonts loaded via Google CDN at the top of `index.css`:
- Fraunces variable (opsz 9..144, SOFT 0..100, wght 300..700, italic 0,1)
- Inter (wght 400..700)

Self-host as a follow-up task outside this spec.

### Updated `@utility` blocks

- `btn-primary`: ink bg, cream text, `font-family: var(--font-mono)`, uppercase, `letter-spacing: 1.8`, `padding: 10px 22px`, `border-radius: 0`, no shadow.
- `btn-secondary`: transparent bg, 1px solid ink ring, same mono label style.
- `card`: paper bg, 1px solid ink border, `border-radius: 0`, no shadow.
- `input`: paper bg, **1px solid ink bottom border only** (no full box), Fraunces display 20px, no radius.
- Chip styling lives in the `<Chip>` primitive, not as a utility — chip pills keep `border-radius: 999`.

### Shape & spacing

- **Zero radius everywhere** except chip pills (999) and the mobile bottom-sheet drag handle (3px pill).
- **No shadows** except the brutalist modal offset `24px 24px 0 rgba(30,36,29,0.15)`.
- Spacing steps: 2 / 4 / 6 / 8 / 10 / 12 / 14 / 18 / 22 / 28 / 40. No invented steps.

---

## 3. Primitives

All under `web/src/components/faltet/`, barrel-exported via `index.ts`.

### `Chip.tsx`

```ts
<Chip tone?="clay"|"mustard"|"berry"|"sky"|"sage"|"forest">{children}</Chip>
```

Inline-flex, mono 10px `letter-spacing 1.4` uppercase, `4px 8px` padding, 1px border in the tone color (defaulting to `forest`), `border-radius: 999`.

### `Rule.tsx`

```ts
<Rule variant?="ink"|"soft" inline?=boolean />
```

`ink` = 1px solid `--color-ink`. `soft` = 1px solid `color-mix(in srgb, var(--color-ink) 20%, transparent)`. `inline` makes it a horizontally flex-growing rule used in section headings between title and right-aligned meta.

### `Stat.tsx`

```ts
<Stat
  value={number|string}
  unit?={string}
  label={string}
  delta?={string}
  hue?="sage"|"clay"|"mustard"|"sky"|"berry"
  size?="large"|"medium"|"small"
/>
```

Value: Fraunces, weight 300, `letter-spacing -1.2`, `font-variation-settings: "SOFT" 100, "opsz" 144`. Sizes: 88 / 56 / 32 px. Unit glyph: italic Fraunces 28/18/14 in the hue color, inline after the value. Label: mono 11px `letter-spacing 1.8` uppercase with a 6×6 colored dot. Delta: clay with ▲/▼ glyph.

### `Field.tsx`

```ts
<Field label={string} value={string} accent?="clay"|"mustard"|"sage"|"sky" />
<Field label editable value onChange={(v)=>…} accent?/>
```

Label: mono 9px uppercase 0.7 opacity. Value: Fraunces 20px on a 1px ink bottom border only. Accent recolors the value.

### `PhotoPlaceholder.tsx`

```ts
<PhotoPlaceholder tone="sage"|"blush"|"butter" label={string} aspect?="wide"|"tall"|"square" />
```

Cream-tinted rectangle with a radial tone wash + mono label. Dimensions flow from the parent; `aspect` hints default. Reserves a future `src?` prop.

### `Masthead.tsx`

```ts
<Masthead left={ReactNode} center={ReactNode} right?={ReactNode} />
```

14px padding, cream background, 1px ink bottom border. Left: mono small-caps. Center: italic Fraunces 14px. Right: optional slot (breadcrumbs on Bed, garden chip on Dashboard). Each screen renders its own `<Masthead />` as the first element under the layout outlet.

---

## 4. Shell — Sidebar + Layout

### `components/faltet/Sidebar.tsx`

220px fixed, cream bg, 1px solid ink right border, full viewport height. Replaces the navigation-list portion of the current `Layout.tsx`.

**Header block:**
- `Verdant` wordmark — Fraunces italic 26px wght 300, trailing `.` in `--color-clay`.
- Subtitle — mono 10px `0.08em` uppercase: `Est. 2026` (via i18n `app.subtitle`).
- 1px ink hairline beneath.

**Nav groups** (see Q2 option c from brainstorming):

| Section header (mono small-caps) | Items (route → label) |
|---|---|
| `§ Odling` | `/` → Översikt · `/gardens` → Trädgårdar · `/species` → Arter · `/species-groups` → Artgrupper · `/plants` → Plantor · `/workflows` → Arbetsflöden · `/successions` → Successioner |
| `§ Uppgifter` | `/tasks` → Uppgifter · `/calendar` → Kalender · `/targets` → Målmål · `/seed-stock` → Frölager · `/supplies` → Förbrukning |
| `§ Skörd & Försäljning` | `/analytics?tab=harvest` → Skörd · `/customers` → Kunder · `/bouquets` → Buketter |
| `§ Analys` | `/trials` → Försök · `/pest-disease` → Skadedjur · `/analytics` → Analys |
| `§ Konto` (bottom-docked `margin-top: auto`) | `/guide` → Guide · `/org/settings` → Org-inställningar · `/account` → Konto |

**Item style.** Italic Fraunces 16px, 1px ink/20% hairline bottom border, 10/16 padding, hover = 1px ink/40%. Active = italic, `color: --color-clay`, with a `●` clay bullet at the right edge. Section headers: mono 9px 0.7 opacity, 12px top padding.

**Language switcher.** Two mono labels `SV EN` at the very bottom; active one clay.

**Mobile.** Sidebar collapses at `<768px` to a hamburger `≡` in the masthead-left slot, opening a slide-out drawer with identical nav styling.

### `components/Layout.tsx`

Left column = `<Sidebar />`. Right column = `<Outlet />`. Page components own their own `<Masthead />` as the first child of the outlet.

---

## 5. Screens

### 5.1 Dashboard — `pages/Dashboard.tsx`

Replaces the current 170-line file. Data layer untouched (`api.dashboard.get()`, `api.plants.traySummary()`, `api.tasks.list(...)`, `api.beds.list()`, harvest stats).

- `<Masthead left="Översikt" center="— Fältliggaren —" />`.
- **Asymmetric hero grid** (`grid-template-columns: 2fr 1fr` desktop, stacked mobile):
  - Left: `<Stat size="large" value={activeBedsCount} unit="×" label="aktiva bäddar" hue="sage" />`.
  - Right: `<PhotoPlaceholder tone="sage" aspect="wide" label="…" />`.
- `<Rule variant="ink" />`.
- **Three content columns** (`repeat(3, 1fr)`, vertical `Rule` between):
  - Tray ledger: ledger table over `traySummary`. Header mono 9 small-caps 0.7 opacity. Rows hairline ink/20.
  - Uppgifter: the next ≤6 tasks (today+7). Row format mirrors the full Task list row. `→` link to `/tasks`.
  - Bäddar status: one `<Chip tone="sage|mustard|forest">` per bed (fully planted / partial / empty).
- **Harvest totals band** — dark ink card, cream text, Fraunces italic 26 stems total + blush "vs 2024" delta.

Mobile: single column; hero stat drops to `size="medium"` (Fraunces 56); columns stack with `<Rule variant="ink" />` between.

### 5.2 Uppgifter — `pages/TaskList.tsx`

Replaces the current 300-line file.

- `<Masthead left="Uppgifter" center="— Dagens rader —" />`.
- **Filter row** — `<Chip tone>` clickable pills for Skörd (clay), Sådd (mustard), Vattna (sky), Plantera (sage), Underhåll (berry). Multi-select persisted to `localStorage` under `verdant-task-filters`. At-least-one rule enforced (clicking the sole active = no-op, optional first-time toast). Active = filled bg + cream text; inactive = 1px ring only.
- **`Idag.` section** — italic Fraunces 30 + clay period, `<Rule inline />`, mono task count.
- Task rows — grid `60px 1.5fr 140px 120px 80px`, gap 18, padding 16/0, hairline ink/25 bottom:
  - Italic Fraunces 26 № marker, tone-colored by activity type.
  - Task title Fraunces 20 + mono 9 small-caps activity tag below.
  - Bed reference: italic Fraunces species + mono `№ 02`.
  - Due time mono small-caps.
  - Right-aligned `→` as explicit button; whole row also clickable; both open the task side drawer (reuse `components/Dialog.tsx` positioned right).
  - Hover: fill with `ink/04`.
- **`Kommande` section** below — same structure with italic Fraunces day headings (`Imorgon`, `Onsdag 23 april`) between row groups.

Mobile: grid collapses to stacked block — 18px №, title + activity stacked right, mono time bottom-right.

### 5.3 Bädd — `pages/BedDetail.tsx`

Replaces the current 365-line file.

- `<Masthead left={<Breadcrumb />} center="— Bäddliggaren —" />` — breadcrumb `Trädgårdar / Norra fältet / Bädd 02` with last crumb clay.
- **Hero row** — grid `1.2fr 1fr` desktop, stacked mobile:
  - Left: chips row over the bed's conditions columns (soilType, sunExposure, irrigationType, raisedBed → `<Chip tone="…">`). Giant title — `Bädd.02 —` Fraunces 80 wght 300 `letter-spacing -1.5` with mustard period, then `Dahliornas plats.` italic clay on line 2. Fraunces Georgia 15 paragraph from `bed.description`.
  - Right: `<PhotoPlaceholder tone="sage" aspect="tall" />` 200px, 2×2 meta grid beneath (Längd, Bredd, Orient, Yta).
- **Stats band** — 5-up flex, 1px ink top + bottom rules, `padding: 20 0`. Each cell is `<Stat size="medium" hue>`: aktiva plantor, årets skörd, dagar till skörd, + two trend stats.
- **Plantor section** — heading `Plantor.` italic Fraunces 30 + clay period, `"N arter · grupperat"` mono, `<Rule inline />`, right-side italic `+ Så fröer` (link `/sow?bedId=X`) and `+ Plantera` actions.
  - Table header — grid `50px 1.5fr 1fr 70px 1fr 100px`, gap 18, mono 9 0.7 opacity, 1px ink bottom.
  - Rows from `api.beds.plants(bedId)`, grouped by species per existing logic. № italic Fraunces 22 tone-colored, name Fraunces 20 + italic 9 Latin below, sort italic Fraunces clay, antal tabular 20, status pill, timeline `sown → planted` mono 10.
  - **Row click opens the Redigera art modal (5.4) for the row's species.**
- **Bottom row** — two columns:
  - Left dark ink card, cream text, butter decorative circle top-right at 20% opacity. Fraunces italic 26: `"142 stjälkar skördade från denna bädd <span blush>säsong 2025</span>."` Mono row: sage `bästa vecka: v.32`, blush `+24 % vs 2024 ▲`. Values from `api.beds.history(bedId)` + analytics.
  - Right 1px clay/40 danger callout — `Farozon` in clay mono small-caps, italic Fraunces 15 warning, mono `→ Radera bädd.02` clay button triggering the existing delete confirmation dialog.

Mobile: single column; title 40px; stats 2×3 wrap; plant ledger flattens to row cards.

### 5.4 Redigera art — `components/faltet/SpeciesEditModal.tsx` + updated `pages/SpeciesDetail.tsx`

Per brainstorming Q3 decision (ii), modal and page share one `<SpeciesEditForm />`.

**`components/faltet/SpeciesEditForm.tsx`.**

- Hero row — `96px 1fr auto` grid: `<PhotoPlaceholder tone="blush" aspect="square" />` 96. Chip row (Fleråriga berry, Knölväxt mustard, Full sol sage) + Fraunces 44 title (common name) with italic clay variant name. Fraunces italic 14 sage subtitle (Latin binomial + description). Right: italic Fraunces 22 mustard `№ 047` (species ID).
- `<Rule variant="ink" />`.
- Form grid — 2 cols, `gap: 20 28`, `padding: 22 0`. `<Field editable>` per column (Sort · SV + EN in clay, cultivation windows in mustard/sage/sky/clay).
- `§ Odling` divider (mono small-caps 0.6 opacity).
- **Full-width notes block** — `Odlingsanteckningar`. 1px ink border, paper fill, Fraunces italic 16, forest/40 pipe caret at end on focus.
- **Fröleverantörer ledger** — italic Fraunces 20 heading + clay period, `"N länkade"` mono, `<Rule inline />`, italic `+ Lägg till` action. Rows grid `1fr 1fr 80px 28px`, padding 10, hairline ink/20 bottom: colored dot + Fraunces name, mono code, italic Fraunces price in accent, mono `↗`.

**`components/faltet/SpeciesEditModal.tsx`.**

- Scrim `rgba(30,36,29,0.55)` + sibling `position: fixed` layer with `backdrop-filter: blur(1.5px)` over the viewport.
- Modal container — 760px wide, `max-height: 86vh`, paper bg, 1px ink border, offset shadow `24px 24px 0 rgba(30,36,29,0.15)`, zero radius, centered.
- Masthead strip (14/padding, cream, 1px ink bottom): mono `Art · Redigera` left, italic Fraunces `— Artkortet —` center, clay `✕ Stäng` button right.
- Body = `<SpeciesEditForm speciesId={…} onSaved={onClose} />`.
- Footer — cream, 1px ink top. Left: mono `↵ radera art` in clay (text button, triggers existing destructive dialog). Right: `<button class="btn-secondary">Avbryt</button>` + `<button class="btn-primary">Spara ändringar →</button>`. Save → PATCH diff-only.

**Motion (framer-motion `AnimatePresence`):**
- Backdrop: `opacity 0 → 1`, `duration: 0.18, ease: "easeOut"`.
- Desktop modal: `scale 0.97 → 1`, `y +6 → 0`, `duration: 0.22, ease: [0.22, 1, 0.36, 1]`.
- Mobile bottom sheet: `y "100%" → "0%"`, `duration: 0.26, ease: [0.22, 1, 0.36, 1]`. `drag="y"` + `dragConstraints={{ top: 0, bottom: 0 }}` with `onDragEnd` dismiss if `info.offset.y > 80`.

**`pages/SpeciesDetail.tsx`** — thin wrapper: Layout chrome + `<SpeciesEditForm speciesId={params.id} />` + sticky footer with Avbryt / Spara buttons. No modal shell.

**`BedDetail.tsx` integration:** plant-row click sets `modalSpeciesId`; renders `<SpeciesEditModal speciesId={modalSpeciesId} onClose={…} />` at end of JSX when truthy.

---

## 6. Phasing

Six tasks, single plan:

1. **Tokens + fonts + utility overhaul.** Replace `@theme`, update `@utility` (`btn-primary`, `btn-secondary`, `card`, `input`), Google Fonts imports, add `framer-motion` dependency.
2. **Primitives.** `Chip`, `Rule`, `Stat`, `Field`, `PhotoPlaceholder`, `Masthead` + barrel + one vitest test per component.
3. **Sidebar + Layout.** Replace `Layout.tsx` with the new `<Sidebar />` + `<Outlet />` shell; mobile drawer; language switcher; bottom-docked account section.
4. **Dashboard.** Replace `Dashboard.tsx` per §5.1.
5. **Uppgifter.** Replace `TaskList.tsx` per §5.2, filter pills + localStorage + side drawer.
6. **Bädd + Redigera art.** Replace `BedDetail.tsx` per §5.3; extract `SpeciesEditForm`; add `SpeciesEditModal`; update `SpeciesDetail.tsx` as thin wrapper; wire framer-motion transitions and mobile bottom-sheet drag-to-dismiss.

Each task ends with its own commit. Milestone commit after task 6.

---

## 7. i18n

New namespace keys in both `web/src/i18n/sv.json` and `web/src/i18n/en.json`:

- `app.subtitle` — "Est. 2026" / "Est. 2026" (sv=en for this one)
- `dashboard.hero.label` — "aktiva bäddar" / "active beds"
- `dashboard.masthead.center` — "— Fältliggaren —" / "— The Field Ledger —"
- `dashboard.trays.title` — "Brickor" / "Trays"
- `dashboard.tasks.title` — "Uppgifter" / "Tasks"
- `dashboard.beds.title` — "Bäddar" / "Beds"
- `tasks.masthead.center` — "— Dagens rader —" / "— Today's rows —"
- `tasks.today` — "Idag." / "Today."
- `tasks.upcoming` — "Kommande" / "Upcoming"
- `tasks.filters.harvest|sowing|watering|planting|maintenance` — (sv/en pairs)
- `bed.masthead.center` — "— Bäddliggaren —" / "— The Bed Ledger —"
- `bed.danger.title` — "Farozon" / "Danger zone"
- `species.masthead.center` — "— Artkortet —" / "— The Species Card —"
- `species.section.cultivation` — "§ Odling" / "§ Cultivation"
- `species.notes.label` — "Odlingsanteckningar" / "Cultivation notes"
- `species.providers.title` — "Fröleverantörer" / "Seed providers"
- `species.providers.count` — "{{count}} länkade" / "{{count}} linked"
- `species.providers.add` — "+ Lägg till" / "+ Add"
- `common.cancel` — existing
- `common.save` — keep existing; new compound key `species.edit.save` = "Spara ändringar →" / "Save changes →"
- `species.edit.delete` — "↵ radera art" / "↵ delete species"

Existing keys reused where the intent matches.

---

## 8. Testing

- **Primitives.** `vitest` + `@testing-library/react` — one test per component verifying tone/variant/size prop affects rendered class or inline style. No exhaustive snapshots.
- **Screens.** Playwright smoke in `web/e2e/` — one spec per route (`/`, `/tasks`, `/bed/:id`, `/species/:id`) that navigates and asserts no console errors + a known element is present. Plus one modal spec: BedDetail → click plant row → modal opens → `✕ Stäng` closes.
- **Type safety.** `npx tsc --noEmit` green after every task.
- **Out of scope.** Visual regression, accessibility audit, performance profile.

---

## 9. Open questions

None as of 2026-04-21 — all scoping decisions recorded above.
