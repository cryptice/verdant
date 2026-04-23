# Handoff — Verdant Dashboard (direction: **Fältet**)

## Overview

Verdant is a Swedish-first garden-planning tool for cut-flower growers. This handoff covers the **chosen visual direction, "Fältet" ("The Field")** — an editorial / magazine aesthetic — and the four primary screens designed in it:

1. **Dashboard / overview**
2. **Uppgifter** (tasks)
3. **Bädd** (bed detail — plant ledger for a single bed)
4. **Redigera art** (species-edit modal)

Each screen is delivered in **desktop (1280×800)** and **mobile (390×780)** artboards.

Two alternative directions — **Kretslopp** (data-ops / near-black surfaces) and **Växthuset** (organic bento) — are also in the design file for reference. Only Fältet is being shipped; the others are there to show rejected alternatives and help orient a developer who wasn't in the design conversation.

## About the design files

The HTML / JSX files in this bundle are **design references, not production code**. They are React-inside-Babel-standalone prototypes laid out on a custom `DesignCanvas` component; their only purpose is to show intended look, layout, and behaviour at pixel fidelity.

**The task is to recreate these designs in the Verdant codebase's real environment** (React + the project's existing component primitives, styling system, i18n and routing) — using the codebase's own patterns. Do **not** copy the JSX from `lib/` verbatim; the inline styles, the `FAL.*` token dictionary and the hand-rolled components there are scaffolding, not a component library. Extract the values (colors, sizes, copy), ignore the mechanics.

If the project has no styling system yet, pick sensibly — Tailwind with CSS custom properties for the tokens, or vanilla CSS modules — and lift the token dictionary from §"Design tokens" below.

## Fidelity

**High-fidelity.** Final colors, typography, copy, spacing, and interactions are locked. Recreate pixel-perfectly. The only things that may flex are:

- Photography — every `PhotoPlaceholder` is a placeholder; replace with real garden photos. Tone/aspect must stay.
- Icons — the designs deliberately avoid icon libraries; where glyphs appear (✕, →, ↵, ●, ▲) they are literal Unicode. Keep them Unicode unless the codebase has a strict icon system.
- Copy — Swedish is primary. English strings exist in the current i18n and can stay parallel.

## Screens / Views

### 1 — Dashboard (`FaltetDesktop` / `FaltetMobile`)

**Purpose.** Overview of the whole farm: season arc, tray inventory summary, upcoming tasks, bed status, recent harvest totals.

**Layout — desktop 1280×800.**

- **Left sidebar, 220 px wide**, full height, 1 px solid ink right border. Wordmark "Verdant" in Fraunces italic 26 px with a clay period; a tiny monospace "Est. 2026" below. Primary nav is a list of italic labels (Översikt, Trädgårdar, Uppgifter, Skörd, Arter, Frölager). Each item has a hairline bottom border at 20 % ink; the active item is italic, clay-colored, with a clay bullet on the right.
- **Main column** has a masthead bar (14 px padding, bottom ink border, monospace small-caps "Översikt" on the left, italic "— Fältliggaren —" centered, cream background).
- **Asymmetric hero grid** below the masthead: a massive stat on the left (`FalStat`, value at Fraunces 88 px, weight 300, opsz 144, SOFT 100, `letter-spacing -1.2`, with a small italic unit glyph in sage and a monospace small-caps label + clay delta marker), and a secondary visual on the right.
- Below: three content columns — tray ledger (table), tasks (ledger), bed status (chip list). Column rules are 1 px ink.

**Layout — mobile 390×780.** Single column, sidebar collapses into a masthead row; hero stat shrinks to ~56 px; sections stack with 1 px ink rules between them.

### 2 — Uppgifter / Tasks (`FaltetTasksDesktop` / `FaltetTasksMobile`)

**Purpose.** See everything that needs doing on the farm, grouped by today vs. upcoming, filterable by activity type.

**Layout.** Same sidebar + masthead. Main column contains:

- A **filter row** — five mono small-caps pill buttons in the five activity highlights: clay (skörd / harvest), mustard (sådd / sowing), sky (vattna / watering), sage (plantera / planting), berry (underhåll / maintenance). Active state = filled background + cream text; inactive = 1 px ring.
- **Idag section header** — Fraunces italic 30 px "Idag." with a clay period, a monospace small-caps task count on the right, and a flex rule.
- **Task rows** — grid `60px 1.5fr 140px 120px 80px` gap 18, padding 16 0, hairline ink/25 bottom border. The leading cell is an **oversized italic Fraunces № marker**, 26 px on desktop, weight 300, colored by the activity type. Then: Fraunces 20 px task title with a mono small-caps activity tag below it, bed reference (italic Fraunces + monospace coordinate), due time (monospace small-caps), and a right-aligned "→" action.
- **Kommande** section below, same structure, with italic Fraunces day headings ("Imorgon", "Onsdag", etc.) between row groups.

**Mobile.** Grid collapses to a stacked row with a smaller № (18 px), title + activity, small mono time at the bottom-right.

### 3 — Bädd / Bed detail (`FaltetBedDesktop` / `FaltetBedMobile`)

**Purpose.** Everything about one bed: conditions, stats, plant ledger, harvest history.

**Layout — desktop.**

- Sidebar + masthead with breadcrumbs ("Trädgårdar / Norra fältet / Bädd 02", last crumb in clay).
- **Hero row**, two columns: left is conditions chips row (Bädd № 02 mustard, Full sol sage, Droppbevattning sky, Upphöjd berry), then a giant two-line Fraunces title — "Bädd.02 —" on line 1 with a mustard period accent, "Dahliornas plats." in italic clay on line 2 (size 80 px, weight 300, SOFT 100, opsz 144, `letter-spacing -1.5`). A 15 px Fraunces Georgia paragraph describes soil. Right column is a sage PhotoPlaceholder 200 px tall with a 2×2 meta grid below it (Längd 8.0 m, Bredd 1.2 m, Orient S-SV, Yta 9.6 m² — 1 px ink borders, mono small-caps label + Fraunces 22 px value).
- **Stats band** — 5-up, top/bottom 1 px ink rules, 20 px padding. Each stat: Fraunces 56 px value, mono small-caps label with a colored dot (sage/clay/mustard/sky/berry).
- **Plantor section** — "Plantor." italic Fraunces 30 px with clay period, "7 arter · grupperat", flex rule, right-side italic actions "+ Så fröer", "+ Plantera".
- **Table header** grid `50px 1.5fr 1fr 70px 1fr 100px`, gap 18, mono 9 px small-caps at 0.7 opacity, 1 px ink bottom border: №, Art, Sort, Antal, Status, Sådd → Plant.
- **Plant rows.** № (italic Fraunces 22 px in the activity color), name (Fraunces 20 px + italic 9 px Latin below), sort (italic Fraunces clay), antal (tabular 20 px), status pill, timeline cell (sown date → planted date, mono 10 px).
- **Bottom row** — two columns. Left: dark ink card, cream text, butter decorative circle top-right at 20 % opacity. Fraunces italic 26 px "142 stjälkar skördade från denna bädd <span>säsong 2025</span>." (span in blush). Mono bottom row: sage "bästa vecka: v.32", blush "+24% vs 2024 ▲". Right: 1 px clay/40 bordered danger callout — "Farozon" heading in clay mono small-caps, italic Fraunces 15 px warning, mono "→ Radera bädd.02" in clay.

**Mobile.** Stacks to single column, hero title drops to ~40 px, stats band becomes a 2×3 ish flex wrap, plant ledger becomes row cards instead of a grid.

### 4 — Redigera art / Species-edit modal (`FaltetSpeciesModal` / `FaltetSpeciesModalMobile`)

**Purpose.** Edit a species (name, latin binomial, cultivation windows, notes, seed providers).

**Layout — desktop.**

- **Backdrop** — the bed screen rendered behind at filter `blur(1.5px)`, opacity 0.35. Over that a scrim at `rgba(30,36,29,0.55)`.
- **Modal** — centered, 760 px wide, max-height 86 %, cream paper background, 1 px ink border, a subtle offset shadow (`24px 24px 0 rgba(30,36,29,0.15)` — no blur, brutalist). No border-radius.
- **Masthead** — 14 px padding, cream background, 1 px ink bottom border. Left: mono small-caps "Art · Redigera". Center: italic Fraunces "— Artkortet —". Right: clay "✕ Stäng".
- **Hero** — `96px 1fr auto` grid. Photo placeholder (blush tone, 96 square) / chip row (Fleråriga berry, Knölväxt mustard, Full sol sage) + Fraunces 44 px title "Dahlia" with italic clay `'Café au Lait'`, Fraunces-italic 14 px sage subtitle "Dahlia pinnata · krämrosa, decorative · 15–20 cm" / right-aligned italic Fraunces 22 px mustard "№ 047".
- Horizontal 1 px ink rule.
- **Form grid** — 2 columns, 20/28 gap, 22 px padding top/bottom. Each field is custom (`Field` component): mono 9 px small-caps 0.7-opacity label, Fraunces 20 px value on a 1 px ink bottom border. Accent fields color the value (Sort · SV + Sort · EN in clay, cultivation windows in mustard/sage/sky/clay).
- Section divider: mono small-caps "§ Odling" at 0.6 opacity.
- **Textarea-like** — full-width `Odlingsanteckningar` block, 1 px ink border, cream fill, Fraunces italic 16 px copy, a forest/40 pipe as a caret glyph at the end.
- **Fröleverantörer ledger** — italic Fraunces 20 px heading with clay period, "3 länkade" mono count, flex rule, italic "+ Lägg till" action. Three rows grid `1fr 1fr 80px 28px`, 10 px padding, hairline ink/20 bottom border: colored dot + Fraunces name / mono code / italic Fraunces price in accent / mono "↗".
- **Footer** — cream background, 1 px ink top border. Left: mono small-caps "↵ radera art" in clay (destructive, text only). Right: two buttons. Avbryt = 1 px forest ring, transparent; Spara ändringar → = solid ink background, cream text. Both square (no radius), mono 10 px 1.8 letter-spaced small-caps, 10/18 padding (10/22 for primary).

**Mobile.** Becomes a **bottom sheet**. 44×3 pill drag handle at the top, condensed masthead, single-column fields (Sådd/Skörd as a 2-up at the bottom), condensed notes block. Footer is flex row with Avbryt 1/3, Spara 2/3.

## Interactions & behaviour

**Global.**

- Keyboard nav on lists — ↑/↓ to move, Enter to open, Esc to dismiss modals.
- The sidebar is persistent across all screens; the active item matches the route.
- Swedish is primary, English strings live parallel in existing i18n.

**Uppgifter.**

- Filter pills are a segmented multi-select by activity type (click toggles). At least one must stay active; clicking all active = noop. Filter state survives session via localStorage.
- Clicking a task row opens a side-drawer (not designed in this pass — use existing drawer primitive).
- The "→" glyph is a button hit area; hover = fill the row with `ink/04`.

**Bädd.**

- Conditions chips are editable (click → inline popover) but the read state is what's in the mock.
- Clicking any plant row in the ledger opens the **Redigera art modal** (the screen in §4).
- "Farozon" → Radera bädd.02 requires a second confirmation (standard destructive dialog).

**Redigera art modal.**

- Backdrop click dismisses (with unsaved-changes guard); Esc also dismisses.
- Fields are live-validated; on save, PATCH the species and close with an optimistic toast.
- Each seed-provider row has a hover state (underline name + show an edit glyph). "+ Lägg till" opens an inline row.
- The photo placeholder is a click target → opens an image picker.
- Save button has a loading state (replace "Spara ändringar →" with a spinner; disable Avbryt).

**Modal motion.** Backdrop fades 180 ms `ease-out`. Desktop modal scales from 0.97 → 1 with a 6 px upward translate, 220 ms `cubic-bezier(0.22, 1, 0.36, 1)`. Mobile bottom sheet translates Y 100 % → 0, 260 ms same curve; drag-down to dismiss beyond 80 px.

## State management

**Per screen.**

- **Dashboard** — reads `gardens`, `traySummary`, `tasks (today + next 7)`, `harvestStats`, `bedStatus`. All already in the current Verdant API.
- **Uppgifter** — reads `tasks` with activity-type filter; mutations are `complete(id)`, `snooze(id, days)`, `reassignBed(id, bedId)`.
- **Bädd** — reads `bed(id)` with embedded `plantings[]`, `harvestStats`, `conditions`. Mutations: update conditions, add sowing, add planting, delete bed.
- **Redigera art modal** — local form state mirrors the species record. On open, hydrate from `species(id)`. Diff on save and PATCH only changed keys.

## Design tokens

All tokens are Fältet-specific; extract them into the project's token layer as CSS custom properties or a theme object.

```
/* Palette — Fältet */
--cream:   #F5EFE2;  /* base backgrounds */
--paper:   #FBF7EC;  /* cards, modal body */
--ink:     #1E241D;  /* text, borders, dark surfaces */
--forest:  #2F3D2E;  /* secondary text, labels */
--sage:    #6B8F6A;  /* planting, positive */
--clay:    #B6553C;  /* primary accent, harvest, destructive */
--mustard: #C89A2B;  /* sowing, highlights */
--berry:   #7A2E44;  /* maintenance, perennials */
--sky:     #4A7A8C;  /* watering */
--butter:  #F2D27A;  /* decorative fills */
--blush:   #E9B8A8;  /* decorative fills, dahlia-tone photography */
--line:    #1E241D;  /* hairlines (alpha-mix for soft rules) */
```

**Typography.**

- **Display** — Fraunces variable (opsz 9..144, SOFT 0..100, wght 300..700, italic). Used 20–88 px, weight 300, `letter-spacing -0.3…-1.5`, italic for accents. Always pair with `font-variation-settings: 'SOFT' 100, 'opsz' 144` at large sizes for the softest optical cut.
- **Body** — Inter 400/500, 13–16 px, 1.5 line-height.
- **Body-serif (paragraphs)** — Fraunces Georgia fallback, 15 px, used inside editorial blurbs.
- **Meta / labels** — `ui-monospace, "SF Mono", Menlo, monospace`, 9–11 px, `letter-spacing 1.4–2`, uppercase.
- **Forbidden** — Inter Display, Roboto, Arial, emoji, and rounded corners > 0 except chip pills (`border-radius: 999`).

**Spacing.** 2 / 4 / 6 / 8 / 10 / 12 / 14 / 18 / 22 / 28 / 40. Don't invent new steps.

**Rules & borders.** Every boundary is a hairline `1px solid var(--ink)`. Soft internal rules use `#1E241D20` (12 % alpha) or `#1E241D30`. No shadows except the brutalist modal offset (`24px 24px 0 rgba(30,36,29,0.15)`).

**Shape.** Zero radius on every container. The only rounding is chip pills (`border-radius: 999`) and the mobile drag-handle (3 px tall pill).

## Assets

- **Photos.** All images in the mocks are rendered as `PhotoPlaceholder` (a cream-tinted rectangle with a tiny mono label and a subtle tone wash). Replace with real garden photography in production. Tones used: sage, blush, butter. Aspect is whatever the layout demands — keep it.
- **Icons.** None. The designs use Unicode glyphs only: ✕ ↵ → ← ▲ ● ↗ ※ § №. Don't introduce an icon library.
- **Fonts.** Loaded via Google Fonts in the prototype — Fraunces + Inter. Self-host in production.

## Files in this bundle

```
design_handoff_verdant_dashboard/
├── README.md                       ← this document
├── Verdant Dashboard.html          ← the canvas (open in a browser to see everything)
└── lib/
    ├── design-canvas.jsx           ← canvas primitive (ignore — scaffolding)
    ├── shared.jsx                  ← PhotoPlaceholder (reference for tone/placement)
    ├── direction-faltet.jsx        ← Dashboard screen (Fältet)
    ├── direction-kretslopp.jsx     ← rejected alternative (data-ops)
    ├── direction-vaxthuset.jsx     ← rejected alternative (organic bento)
    ├── faltet-tasks.jsx            ← Uppgifter screen
    ├── faltet-bed.jsx              ← Bädd screen
    └── faltet-species-modal.jsx    ← Redigera art modal
```

Open `Verdant Dashboard.html` locally (any static server, or just a file:// tab with a Babel/React-friendly browser) to pan around the canvas and see every screen at real size.

### How to read the JSX

The `FAL.*` dictionary at the top of `direction-faltet.jsx` is the source of truth for colors. Every other Fältet file re-references it. Sizes, gaps, letter-spacings, and font sizes live inline in each component's `style={{ ... }}` — copy those values into your token layer / component props as you port.

Ignore:

- `DesignCanvas`, `DCSection`, `DCArtboard` — canvas scaffolding, not part of the product.
- `FalChip`, `FalStat`, `FalRule`, `Field`, `PhotoPlaceholder` — design-file helpers. Reimplement idiomatically in your codebase's component system; they're here so the mock renders, not as an API contract.

Port order I'd recommend: tokens → typography → primitives (Chip, Rule, Stat, Field, PhotoPlaceholder) → Dashboard → Uppgifter → Bädd → Redigera art modal.
