# Android Fältet — Sub-Spec B: Lists (Design)

**Status:** design, awaiting implementation plan
**Scope:** 12 Android list screens
**Predecessor:** Spec A (Foundation — theme, typography, drawer/bottom bar, base primitives) shipped at commit `53f13fc`.
**Successors:** Sub-spec C (detail / forms / activity screens), Sub-spec D (analytics / account / auth).

---

## 1. Goal

Port 12 Android list screens from generic Material3 `LazyColumn` + `Card` to the Fältet editorial aesthetic using a small set of shared primitives, so the mobile app reads as the same product as the web admin.

**Non-goals:**

- No data-model changes.
- No API changes.
- No navigation changes.
- No new features.
- No new test infrastructure (no Paparazzi, no instrumented UI tests added).
- Existing pagination stays functional — picks up Fältet typography, no behavioral change.

**Success criteria:**

- All 12 list screens render with hairline-separated `FaltetListRow` cards on cream background.
- Typography: Fraunces italic for titles, mono uppercase for meta / stat labels, ink body for descriptive text.
- Empty states show editorial Swedish copy (headline + subtitle + optional action).
- Each screen has a `@Preview` composable exercising its main states.
- `./gradlew assembleDebug` green.
- Manual emulator smoke on all 12 screens shows no layout breaks.

---

## 2. Scope

### In scope — 12 screens

| Screen | File | Notes |
|---|---|---|
| PlantedSpeciesList | `plants/PlantedSpeciesListScreen.kt` | Simplest — Phase 2 reference port |
| TaskList | `task/TaskListScreen.kt` | Checkbox inline, grouped by due-date |
| SeedInventory | `inventory/SeedInventoryScreen.kt` | `FaltetStepper` inline |
| SupplyInventory | `supplies/SupplyInventoryScreen.kt` | Stepper + search + filter — Phase 2 complex reference port |
| SpeciesList | `activity/SpeciesListScreen.kt` | Category chips |
| SuccessionSchedules | `succession/SuccessionSchedulesScreen.kt` | Grouped by status |
| ProductionTargets | `targets/ProductionTargetsScreen.kt` | Stat-heavy rows |
| VarietyTrials | `trials/VarietyTrialsScreen.kt` | Multi-section rows |
| BouquetRecipes | `bouquet/BouquetRecipesScreen.kt` | Photo thumb + price stat |
| CustomerList | `customer/CustomerListScreen.kt` | Inline contact-action buttons |
| PestDiseaseLog | `pest/PestDiseaseLogScreen.kt` | Search + filter + 2-line meta |
| MyVerdantWorld | `world/MyVerdantWorldScreen.kt` | Garden list |

### Out of scope — deferred

- **MyWorld / Dashboard** — mixed-content dashboard, needs its own brainstorm (future sub-spec).
- **SeasonSelector** — picker/modal UX, not a standalone list page.
- **All detail screens, forms, activity screens** — sub-spec C.
- **Analytics, Account, Auth** — sub-spec D.

---

## 3. Design Decisions (summary of brainstorm)

| # | Decision | Chosen |
|---|---|---|
| 1 | Layout pattern on phone | Editorial card stack — one `FaltetListRow` primitive reused across all screens; not a literal Ledger table port. |
| 2 | Scope | 12 pure-list screens (above). MyWorld + SeasonSelector deferred. |
| 3 | Shared primitives | `FaltetListRow`, `FaltetEmptyState`, `FaltetSectionHeader`, `FaltetSearchField`, plus `FaltetCheckbox` + `FaltetStepper` for inline widgets, plus `FaltetLoadingState` to consolidate the current inline-spinner pattern. No `FaltetFilterBar` — compose filters as a `Row` of existing `Chip`. |
| 4 | `FaltetListRow` slot shape | Named semantic slots: `leading`, `title` (String), `meta` (String?), `stat` (composable?), `actions` (composable?). |
| 5 | Inline interactive widgets | Add `FaltetCheckbox` + `FaltetStepper`. Icon buttons on `CustomerList` compose directly from Material `IconButton`. |
| 6 | Empty state treatment | Editorial per-screen copy (headline + subtitle + optional action). No illustration. |
| 7 | Testing bar | Compile + manual smoke + `@Preview` composable per primitive and per screen. No snapshot testing this spec. |
| 8 | Implementation strategy | Vertical slice — primitives first, then two reference screens (PlantedSpeciesList + SupplyInventory) as milestone, then batch 10. |

---

## 4. New Primitives

All under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`.

### 4.1 `FaltetListRow.kt`

The workhorse primitive. Hairline-bottom-bordered clickable `Row` on cream background.

**Signature:**

```kotlin
@Composable
fun FaltetListRow(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    meta: String? = null,
    metaMaxLines: Int = 1,
    stat: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
)
```

**Visual grammar:**

```
┌──────────────────────────────────────────────────────────────────┐
│  [L]   Cosmos bipinnatus                          84  stk  [□]  │
│        COSMOS · BATCH 12 · SÅDD VECKA 14                         │
└──────────────────────────────────────────────────────────────────┘
       hairline (ink-line-20, 1dp) along bottom
```

**Slot rules:**

- **Leading**: 24dp × 24dp box. Conventions: filled color dot for status, `PhotoPlaceholder` 24dp mini for entities with photos, `Icon` for category. `null` when row has no mark.
- **Title**: Fraunces italic 18sp, `FaltetInk`, weight regular, `maxLines = 1`, `overflow = Ellipsis`.
- **Meta**: mono 10sp, letter-spacing 1.2sp, uppercase, `FaltetForest`. Default `maxLines = 1`, caller can pass higher (PestDiseaseLog uses 2). Multi-segment values joined with ` · ` (mid-dot) by caller.
- **Stat** (trailing-before-actions): composable slot so callers can mix label + value — e.g. `Text("84", mono 16sp FaltetInk) + Text(" STK", mono 9sp FaltetForest)`. Right-aligned.
- **Actions** (trailing): interactive widgets only. 48dp min touch target honored inside the slot.

**Layout:**

- `Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(...).padding(horizontal = 18.dp, vertical = 14.dp)`.
- `drawBehind` draws a 1dp `FaltetInkLine20` line at `y = size.height` — hairline bottom separator.
- No top separator; rows stack with bottom lines only.
- On press: ripple in `FaltetClay.copy(alpha = 0.08f)`.
- `Row` with three regions: `leading` (if present) → flexible center column (title + meta stacked) → `stat + actions` (inner `Row`, gap 12dp). Center column takes `weight(1f)`. 12dp spacing between leading ↔ center and center ↔ trailing.

**Preview:** one `@Preview` composable exercising all five slots filled + one minimal case (title + meta only).

### 4.2 `FaltetEmptyState.kt`

Centered column for empty-list states.

**Signature:**

```kotlin
@Composable
fun FaltetEmptyState(
    headline: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
)
```

**Visual:**

- `Column` filling available space, `verticalArrangement = Arrangement.Center`, `horizontalAlignment = Alignment.CenterHorizontally`, padding horizontal 32dp.
- Headline: Fraunces italic 22sp, `FaltetInk`, center-aligned.
- Subtitle: body 14sp `FaltetForest`, center-aligned, 16dp top margin.
- Action (optional): 24dp top margin — typically an `OutlinedButton` or similar.

### 4.3 `FaltetSectionHeader.kt`

Mono-uppercase label with clay underline rule.

**Signature:**

```kotlin
@Composable
fun FaltetSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
)
```

**Visual:**

- Padded 18dp horizontal, 12dp top, 6dp bottom.
- Label: mono 9sp, letter-spacing 1.4sp, uppercase, `FaltetForest`.
- Short clay underline: 24dp wide × 1.5dp tall `FaltetClay` rule beneath the label, 4dp below baseline.

### 4.4 `FaltetSearchField.kt`

Thin wrapper over existing `Field` primitive.

**Signature:**

```kotlin
@Composable
fun FaltetSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
)
```

**Visual:**

- Leading `Icon(Icons.Default.Search, tint = FaltetForest)`.
- Trailing `IconButton` with `Icons.Default.Close` rendered only when `value.isNotEmpty()`; click clears.
- Typography: body 14sp `FaltetInk`, placeholder mono 12sp `FaltetForest`.
- No debounce here; caller applies debounce in the view-model if needed. (Existing screens already handle this.)

### 4.5 `FaltetCheckbox.kt`

Fältet-styled checkbox wrapping Material `Checkbox`.

**Signature:**

```kotlin
@Composable
fun FaltetCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
)
```

**Visual:**

- Square 24dp, zero corner radius.
- Checked: fill `FaltetClay`, checkmark `FaltetCream`.
- Unchecked: 1dp border `FaltetInkLine40`, transparent fill.
- Uses `CheckboxDefaults.colors(...)` to map Fältet palette onto Material slots.

### 4.6 `FaltetStepper.kt`

Horizontal `[−] value [+]` cluster for inventory quantity adjustment.

**Signature:**

```kotlin
@Composable
fun FaltetStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = Int.MAX_VALUE,
)
```

**Visual:**

- `Row`, gap 8dp, `verticalAlignment = CenterVertically`.
- Each button: 32dp square, 1dp `FaltetInkLine40` border, zero radius, transparent background, ink icon (`Remove` / `Add`).
- Value text: mono 14sp `FaltetInk`, min-width 24dp, center-aligned.
- `onDecrement` disabled when `value <= min`; `onIncrement` disabled when `value >= max`. Disabled buttons dim to `FaltetInkLine20` border + `FaltetForest.copy(alpha = 0.3f)` icon.

### 4.7 `FaltetLoadingState.kt`

Shared loading spinner — every list screen currently rolls its own `Box + CircularProgressIndicator`; consolidate.

**Signature:**

```kotlin
@Composable
fun FaltetLoadingState(
    modifier: Modifier = Modifier,
)
```

**Visual:**

- `Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center)`.
- Single `CircularProgressIndicator(color = FaltetClay, strokeWidth = 2.dp)`, 28dp × 28dp.
- Cream background inherited from scaffold.

---

## 5. Screen Port Pattern

Every ported screen follows the same structural shape:

```kotlin
FaltetScreenScaffold(
    mastheadLeft = "§ Inventarie",
    mastheadCenter = "Fröförråd",
    mastheadRight = { /* optional right-side element */ },
    fab = { FaltetFab(onClick = ..., contentDescription = ...) },
) { padding ->
    when (val s = state) {
        Loading -> FaltetLoadingState(Modifier.padding(padding))
        is Error -> ConnectionErrorState(Modifier.padding(padding), s.message, retry)
        is Empty -> FaltetEmptyState(
            headline = "…",
            subtitle = "…",
            action = { /* optional */ },
            modifier = Modifier.padding(padding),
        )
        is Loaded -> LazyColumn(Modifier.padding(padding)) {
            if (searchable) stickyHeader { FaltetSearchField(...) }
            groupedItems.forEach { (section, rows) ->
                if (grouped) item { FaltetSectionHeader(label = section) }
                items(rows, key = { it.id }) { row ->
                    FaltetListRow(
                        leading = row.leading,
                        title = row.title,
                        meta = row.meta,
                        stat = row.stat,
                        actions = row.actions,
                        onClick = { navigate(row.id) },
                    )
                }
            }
            item { ExistingPaginationFooter(...) }
        }
    }
}
```

`FaltetLoadingState` is a shared primitive added in Phase 1 (§4.7).

### 5.1 Masthead copy per screen

Fixing Swedish copy now so the plan doesn't stall inventing it:

| Screen | `mastheadLeft` | `mastheadCenter` |
|---|---|---|
| PlantedSpeciesList | § Odling | Utplanterade |
| TaskList | § Arbete | Uppgifter |
| SeedInventory | § Inventarie | Fröförråd |
| SupplyInventory | § Inventarie | Förnödenheter |
| SpeciesList | § Sort | Sorter |
| SuccessionSchedules | § Plan | Successioner |
| ProductionTargets | § Plan | Produktionsmål |
| VarietyTrials | § Forskning | Sortförsök |
| BouquetRecipes | § Bukett | Recept |
| CustomerList | § Kund | Kunder |
| PestDiseaseLog | § Hälsa | Skadegörare |
| MyVerdantWorld | § Min värld | Trädgårdar |

### 5.2 Row-slot mapping per screen

Concretely what goes into each `FaltetListRow` slot. Plan tasks reference this table rather than re-deciding per screen:

| Screen | leading | title | meta | stat | actions |
|---|---|---|---|---|---|
| PlantedSpeciesList | color dot (species) | variety name | species · sådd v.X | batch count + "STK" | — |
| TaskList | tone dot (priority/status) | task title | due-date · species | — | `FaltetCheckbox` |
| SeedInventory | — | variety | species · sort | stock count + unit | `FaltetStepper` |
| SupplyInventory | category `Icon` | supply name | category · leverantör | stock + unit | `FaltetStepper` |
| SpeciesList | category dot | species | category | batches count + "BATCHAR" | — |
| SuccessionSchedules | status dot | species | vecka X · status | plant count + "ST" | — |
| ProductionTargets | — | species | period | `target` / `actual` (split-stat composable) | — |
| VarietyTrials | — | variety | år · species | result chip (existing `Chip`) | — |
| BouquetRecipes | `PhotoPlaceholder` (24dp) | recipe name | stems · säsong | price (Fraunces 16sp) | — |
| CustomerList | — | customer name | segment | — | 2 × Material `IconButton` (phone, email), tint `FaltetClay` |
| PestDiseaseLog | severity dot | observation | datum · species (2-line — `metaMaxLines = 2`) | — | — |
| MyVerdantWorld | — | garden name | plats | bed count + "BÄDDAR" | — |

### 5.3 Filter / search handling

- **SupplyInventory + PestDiseaseLog**: `stickyHeader` containing `FaltetSearchField` stacked above a horizontal scrolling `Row` of existing `Chip` toggles (the filter bar). No new primitive — inline composition.
- **TaskList**: groups rows by due-date section (Idag / I morgon / Denna vecka / Senare). `FaltetSectionHeader` emitted per group.
- **SuccessionSchedules**: groups by status (Planerad / Sådd / Utplanterad / Avslutad). `FaltetSectionHeader` emitted per group.
- No other screen has grouping or filtering in the current implementation — preserve that (don't add).

### 5.4 Loading / Error / Empty states

- **Loading**: centered spinner on cream background, masthead stays visible (no flicker).
- **Error**: existing `ConnectionErrorState` composable — masthead stays, content area shows it. No change.
- **Empty**: `FaltetEmptyState` — masthead stays, content area replaced. Each screen supplies its own Swedish `headline` + `subtitle` + optional `action`. Copy authored per-screen in the plan.

---

## 6. Phasing

### Phase 1 — Primitives (7 commits)

Ship the seven new primitives. Each commit: primitive + `@Preview`, compile-green gate.

1. `FaltetListRow.kt` (+ Preview)
2. `FaltetEmptyState.kt` (+ Preview)
3. `FaltetSectionHeader.kt` (+ Preview)
4. `FaltetSearchField.kt` (+ Preview)
5. `FaltetCheckbox.kt` (+ Preview)
6. `FaltetStepper.kt` (+ Preview)
7. `FaltetLoadingState.kt` (+ Preview)

At end of Phase 1 no screen consumes them; app still compiles and runs identical to Spec A.

### Phase 2 — Reference ports (2 commits + milestone check)

Validate the primitive API against real screens before batch porting:

8. Port `PlantedSpeciesListScreen.kt` — shortest, simplest. Validates basic slot pattern + empty state + pagination hook.
9. Port `SupplyInventoryScreen.kt` — most complex. Validates stepper + search + filter + sticky header + complex row.

After Phase 2: manual emulator smoke on both screens. If a primitive API turns out wrong, amend the primitive file in a new commit (don't rewrite Phase 1 history) and fix the reference screens before Phase 3.

### Phase 3 — Batch ports (10 commits)

Order chosen so grouped / inline-action screens come early (catch edge cases while pattern is fresh) and most-customized screens come last (when the pattern is stable):

10. `TaskListScreen.kt` — checkbox + grouped by due-date
11. `SeedInventoryScreen.kt` — stepper
12. `SpeciesListScreen.kt`
13. `SuccessionSchedulesScreen.kt` — grouped by status
14. `ProductionTargetsScreen.kt` — split stat (composed inline in the calling screen — not a new primitive)
15. `VarietyTrialsScreen.kt`
16. `BouquetRecipesScreen.kt` — photo thumb
17. `CustomerListScreen.kt` — inline action buttons
18. `PestDiseaseLogScreen.kt` — search + filter + 2-line meta
19. `MyVerdantWorldScreen.kt`

### Phase 4 — Verify + milestone (1 empty commit)

20. `./gradlew assembleDebug` green. Manual smoke on all 12 screens. Empty milestone commit:

```
milestone: Android Fältet lists complete (Spec B)
```

**Total: 20 tasks, 19 code commits + 1 milestone.**

---

## 7. Testing

Decided in brainstorm Q7.

### Per primitive (Phase 1)

- `@Preview` composable in the same file, exercising main slot permutations.
  - `FaltetListRow` preview permutations: minimal (title+meta), full (all 5 slots), with `FaltetStepper` actions, with `FaltetCheckbox` actions.
  - `FaltetEmptyState` preview: with action and without action.
  - `FaltetSectionHeader` preview: single header, three stacked.
  - `FaltetSearchField` preview: empty, populated.
  - `FaltetCheckbox` preview: checked and unchecked side by side.
  - `FaltetStepper` preview: middle, at min, at max.
  - `FaltetLoadingState` preview: default.
- `./gradlew compileDebugKotlin` green after each primitive commit.

### Per screen port (Phases 2–3)

- `@Preview` composable in the same file, exercising empty, loaded-short (3 rows), and loaded-grouped states where applicable.
- `./gradlew compileDebugKotlin` green after each screen commit.
- Manual emulator smoke after Phase 2 (reference ports) and after Phase 3 (complete batch).

### Not added this spec

- No Paparazzi / screenshot testing.
- No new instrumented UI tests.
- No changes to existing unit tests — view-model signatures don't change.

### Known non-issue

On first cold launch after install, the Downloadable Fonts API shows a brief system-serif flash before Fraunces arrives. Do not gate testing on eliminating this.

---

## 8. Follow-up (outside this spec)

- **Sub-spec C** — detail / forms / activity screens (GardenDetail, BedDetail, PlantDetail, CreatePlant, BatchPlantOut, AddPlantEvent, CreateGarden, CreateBed, CreateTask, EditTask, WorkflowProgress, plus activity screens).
- **Sub-spec D** — analytics / account / auth (Analytics, Account, Auth, Splash). Also picks up MyWorld dashboard and SeasonSelector deferred from this spec.

Each sub-spec brainstormed independently when ready.
