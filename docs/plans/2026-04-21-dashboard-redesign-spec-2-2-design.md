# Verdant Dashboard Redesign — Spec 2.2: Forms, Details, Settings, Public Pages

**Status:** approved for planning (2026-04-21)
**Target:** Verdant web admin (React + TypeScript + Tailwind v4 + Vite)
**Driver:** port the remaining 16 pages to Fältet by applying established patterns from specs 1 and 2.1. No new primitives; three existing rendering patterns (form, detail, editorial) plus one new editorial chrome for the public/unauth pages.

**Depends on:** Spec 1 (tokens, primitives, sidebar, 4 screens), Spec 2.1 (Ledger + 14 ledger pages) — both landed.
**Concludes:** the web-wide Fältet rollout except the three design-dependent screens in spec 3.

---

## 1. Scope

Sixteen pages still render legacy layouts despite inheriting the Fältet palette + typography via the token overhaul in spec 1. This spec replaces their layouts to finish the visual rollout.

**In scope (16 pages):**

*Forms (8):*
BedForm, GardenForm, TaskForm, SpeciesGroupEdit, WorkflowTemplateEdit, ApplySupply, SowActivity, OrgSetup.

*Detail pages (3):*
GardenDetail, PlantedSpeciesDetail, PlantDetail.

*Settings / meta (3):*
Account, OrgSettings, Guide.

*Public / unauth (2):*
LandingPage, PrivacyPolicy.

**Not in scope:**
- CropCalendar, Analytics, WorkflowProgress — spec 3, needs design input (no handoff mocks).
- Real photography replacing `PhotoPlaceholder`.
- Self-hosted font swap from Google CDN.
- Visual regression harness.
- Accessibility audit.
- Any page's data layer / backend call shape — rendering only.
- Restoring pagination on pages where spec 2.1 removed it — separate follow-up.

---

## 2. Rendering patterns

No new primitives. Four existing patterns cover every page.

### Form pattern — used for all 8 forms + all 3 settings pages + Guide

From `components/faltet/SpeciesEditForm.tsx` (spec 1).

1. **Masthead** — `left = nav label`, `center = editorial tagline`, `right = optional cancel / close link`.
2. **Hero row** (when applicable) — `grid-template-columns: 96px 1fr auto`. `PhotoPlaceholder` square + chip row + Fraunces 44 title + italic 14 Fraunces sage subtitle + right-aligned italic Fraunces 22 mustard identifier (e.g. `№ 047`). Hero is omitted for pages that don't have an entity to showcase (OrgSetup, Account, Guide).
3. **Form grid** — 2 cols desktop (`1fr 1fr, gap: 20px 28px`), 1 col mobile. Each field is `<Field editable>` with an optional `accent` tone.
4. **Section dividers** — mono 9 small-caps `§ Sektion` at 0.6 opacity + `<Rule variant="soft" />`.
5. **Notes block** — full-width `<textarea>` inside a 1px-ink-bordered paper box with Fraunces italic 16 body.
6. **Sticky footer** — cream bg, 1px ink top border. Left: mono `↵ radera …` clay text button (destructive, when applicable). Right: `<button class="btn-secondary">Avbryt</button>` + `<button class="btn-primary">Spara →</button>`.

### Detail pattern — used for all 3 detail pages

From `pages/BedDetail.tsx` (spec 1).

1. **Masthead** with breadcrumb `left` (last crumb clay), italic Fraunces tagline `center`.
2. **Hero row** — grid `1.2fr 1fr` desktop, stacked mobile.
   - Left: chip row over the entity's attribute columns, giant Fraunces title (60–80 px, weight 300, `letter-spacing: -1.5`, `font-variation-settings: "SOFT" 100, "opsz" 144`) with tone-colored period accent, Fraunces Georgia 15 description paragraph.
   - Right: `<PhotoPlaceholder tone aspect="tall">` 200 px tall, 2×2 meta grid beneath (each cell: mono 9 small-caps label + Fraunces 22 value, 1 px ink borders).
3. **Stats band** — 5-up flex, 20 px padding, 1 px ink top + bottom rules. Each cell is `<Stat size="medium" hue>`.
4. **Content section** — italic Fraunces 30 heading + clay period + mono count + `<Rule inline />` + right-aligned italic action links.
5. **Optional bottom row** — dark-ink editorial card (Fraunces italic 26 stat + mono meta) on the left, 1 px clay/40 Farozon callout on the right.

### Settings pattern — used for Account, OrgSettings

Lighter variant of form pattern. No hero. Simpler field list. Destructive actions (sign out / delete account) live in a Farozon-style callout at the bottom.

### Editorial pattern — new, used for LandingPage, PrivacyPolicy, Guide, OrgSetup

These pages render outside the Fältet `<Layout>` shell (no sidebar).

1. **Top strip** — Verdant wordmark Fraunces italic 32 + clay period, `Est. 2026` mono subtitle on the left. Right-aligned: login link on LandingPage; language switcher on the others. 1 px ink bottom border. Full-bleed cream.
2. **Content area** — max-width 860 px centered, cream bg. Fraunces-Georgia 16 (`font-family: Georgia, var(--font-display); font-size: 15–16; line-height: 1.6; color: var(--color-forest)`) for prose paragraphs. Hairline rules between sections. Editorial headlines Fraunces 48–80, weight 300, letter-spacing -1.
3. **No emoji icons.** Unicode glyphs only (per spec 1 handoff rule).
4. **Footer** — mono small-caps privacy / language / contact links on cream, 1 px ink top border.

**LandingPage specifics:**
- Hero: Fraunces 80 headline "Odla vackert. Skörda mer." / "Grow beautifully. Harvest more." with clay period.
- Features: 4-up grid (CSS `repeat(4, 1fr)` desktop, stack mobile). Each feature cell: mono 9 small-caps "№ 01" marker + Fraunces 22 label + italic Fraunces 14 description.
- Login CTA: Google button, 1 px ink border, mono small-caps, zero radius.

**PrivacyPolicy specifics:**
- Simple top strip.
- Body sections with mono small-caps `§ §1 Lagring` style headers + Fraunces-Georgia body.

**Guide specifics:**
- Masthead (uses `<Layout>` since Guide is auth-gated) rather than editorial top strip.
- Body sections with mono small-caps `§ Tema` headers + Fraunces-Georgia paragraphs.

**OrgSetup specifics:**
- Pre-shell (renders before `<Layout>` because the user has no org yet).
- Editorial top strip without login link.
- Centered form (single column, narrower than the standard form grid) with Fraunces 44 headline "Din första trädgård.", Fraunces-Georgia intro paragraph, two `<Field editable>` inputs (org name, slug), `<button class="btn-primary">Skapa →</button>`.

---

## 3. Page-by-page notes

### Forms

- **`BedForm`** (180 lines) — already has a conditions section added by the bed-conditions feature. Wrap it in Fältet form chrome: Masthead + form grid (name, description, dimensions) + collapsible `§ Villkor` section holding the existing soil/sun/drainage/aspect/irrigation/protection/raisedBed/pH fields + sticky footer with save.
- **`GardenForm`** (73 lines) — simple: name, description, emoji picker. Single-column form grid is fine.
- **`TaskForm`** (224 lines) — task type, species/bed pickers, due date, notes. Preserve existing species and bed picker components; wrap in Fältet chrome.
- **`SpeciesGroupEdit`** (168 lines) — group name + species membership checklist. Keep checklist functionality; restyle as a Fältet `<Field>` + nested ledger-style species list.
- **`WorkflowTemplateEdit`** (304 lines) — complex ordered step editor. Each step row is a Fältet mini-card: mono step № + italic Fraunces name + `daysAfterPrevious` mono + event-type chip + drag handle. Preserve reorder / add / remove / delete. `suggestedSupplyTypeId` + `suggestedQuantity` conditional fields from the fertilizer feature stay.
- **`ApplySupply`** (173 lines) — already Fältet-aware (it was created recently). Verify chrome matches the spec's form pattern (some of the initial implementation used inline Tailwind classes that predate the Fältet primitives); migrate to `<Field>` / `<Chip>` / `<Masthead>` where it doesn't already.
- **`SowActivity`** (286 lines) — multi-section: species picker → bed picker → seed count + date. Use `§ Sektion` dividers between the three sections. Preserve any autocomplete / picker sub-components.
- **`OrgSetup`** (149 lines) — editorial pattern (pre-shell). Centered form with the welcoming headline.

### Detail pages

- **`GardenDetail`** (416 lines, largest) — Masthead with breadcrumb `Trädgårdar / <name>`, hero (emoji + Fraunces garden name + description), stats band (aktiva bäddar / aktiva plantor / årets skörd), `Bäddar` section as a ledger-like list of bed cards (each card: `<Chip>` strip of bed attributes + bed name + plant count + chevron-right), bottom row with harvest stat card + Farozon callout (mono "→ Radera trädgård" in clay).
- **`PlantedSpeciesDetail`** (64 lines) — light page: Masthead with breadcrumb, Fraunces hero with species common name + italic Latin, simple ledger list of plants in this species. Keep existing plant-row logic.
- **`PlantDetail`** (284 lines) — already has the GDD strip (from spec 1 / fertilizer). Finish porting: Masthead with breadcrumb `Plantor / <species> / <plant name>`, hero with species + variant + status chip + bed reference, event timeline section (already Fältet-styled for APPLIED_SUPPLY events; restyle other event types to match), add-event floating action.

### Settings / meta

- **`Account`** (104 lines) — Fältet form pattern: advanced mode toggle, language preference, danger zone (delete account) at bottom.
- **`OrgSettings`** (190 lines) — Fältet form pattern: org name, slug, members section, leave-org destructive action.
- **`Guide`** (80 lines) — editorial pattern within Masthead shell (auth-gated). Fraunces-Georgia body with `§ Tema` section headers per topic.

### Public / unauth

- **`LandingPage`** (236 lines) — editorial pattern. Editorial top strip + Fraunces 80 headline + 4-up features + Google login CTA. No sidebar shell; component renders at the root. Preserve `useAuth().login` flow, Google credential response handler, error surface.
- **`PrivacyPolicy`** (36 lines) — editorial pattern, minimal. Top strip + Fraunces-Georgia body sections + mono section headers.

---

## 4. i18n

Most copy already exists. New keys needed:

```json
"landing": {
  "masthead": { "subtitle": "Est. 2026" },
  "hero": {
    "headline":   "Odla vackert. Skörda mer." / "Grow beautifully. Harvest more.",
    "sub":        "Trädgårdsplanering för kommersiell snittblomsodling." / "Garden planning for commercial cut-flower growing."
  },
  "cta": {
    "login":   "Logga in med Google" / "Sign in with Google"
  }
},
"privacy": {
  "masthead": {
    "left":   "Integritet" / "Privacy",
    "center": "— Regelverk —" / "— Rulebook —"
  }
},
"guide": {
  "masthead": { "center": "— Handbok —" / "— Handbook —" }
},
"orgSetup": {
  "hero":      { "headline": "Din första trädgård." / "Your first garden." },
  "intro":     "Välj ett namn för din organisation så kommer vi igång." / "Choose a name for your organisation and we'll get started.",
  "submit":    "Skapa →" / "Create →"
},
"form": {
  "sections": {
    "conditions": "§ Villkor" / "§ Conditions",
    "scheduling": "§ Schema"  / "§ Schedule",
    "details":    "§ Detaljer" / "§ Details"
  }
}
```

Existing per-page strings reused verbatim. Swedish-first.

---

## 5. Phasing

Single plan, 6 tasks per Section 2.

| Task | Name | Pages |
|---|---|---|
| 1 | Simple forms | BedForm, GardenForm, SpeciesGroupEdit, OrgSetup |
| 2 | Activity forms | TaskForm, ApplySupply, SowActivity |
| 3 | Workflow template editor | WorkflowTemplateEdit |
| 4 | Detail pages | GardenDetail, PlantedSpeciesDetail, PlantDetail |
| 5 | Settings + meta + public | Account, OrgSettings, Guide, LandingPage, PrivacyPolicy |
| 6 | Milestone | Empty commit |

Each task ends with `npx tsc --noEmit` + `npm run build` green + its own commit. No new vitest tests (no new primitives). Rough sizing: task 4 largest (GardenDetail 416 lines), task 5 broadest (5 pages), others moderate.

---

## 6. Testing

- No new primitives → no new vitest files.
- Playwright smoke per route (16 routes total):
  - Navigate, assert masthead or editorial top strip renders.
  - No console errors.
  - LandingPage smoke: renders with no authenticated user, login button visible.
- `npx tsc --noEmit` and `npm run build` green after every task commit.

---

## 7. Open questions

None as of 2026-04-21 — all scoping decisions recorded above.
