# Android Fältet — Spec A: Foundation

**Status:** approved for planning (2026-04-22)
**Target:** Verdant Android app (Kotlin + Jetpack Compose + Hilt + Retrofit)
**Driver:** establish the Fältet aesthetic foundation on Android — theme, palette, typography, primitives, drawer + bottom-nav restyle, shared scaffold. Once landed, all ~44 existing screens inherit the new palette and typography via Material token mapping; per-screen editorial layouts come in subsequent sub-specs (B/C/D).

**Depends on:** Web Fältet rollout (specs 1 / 2.1 / 2.2 / 3) — established the palette, typography choices, and primitive vocabulary this spec mirrors.
**Precedes:** Android sub-specs B (lists), C (detail/forms/activities), D (analytics/account/auth).

---

## 1. Scope

This is the first of four Android sub-specs. It ships the foundation that every subsequent screen builds on.

**In scope:**

- Palette tokens (Fältet 11-color set) replacing the current `GreenPrimary`/`Cream`/`TextPrimary` constants.
- `MaterialTheme.colorScheme` mapping so every Material-native Compose component inherits Fältet colors automatically.
- `Shapes` override to zero radius globally (chips and FABs override locally).
- Fraunces + Inter typography via `androidx.compose.ui:ui-text-google-fonts` Downloadable Fonts API.
- `Typography` mapped to Material slots so existing screens pick up the new type.
- Shared primitives under `ui/faltet/`: `Chip`, `Rule`, `Stat`, `Field`, `PhotoPlaceholder`, `Masthead`.
- `FaltetScreenScaffold` composable as the shared shell (top bar + masthead + content + optional bottom bar/FAB).
- Drawer restyle: Fältet wordmark header, grouped nav sections under mono small-caps headers, italic Fraunces 18sp labels with clay active bullet.
- Bottom bar restyle: cream bg, 1dp ink top border, mono small-caps labels, clay active icon, no pill indicator.
- FAB style preset (ink circle, clay glyph, 0dp elevation).
- Find-replace across ~44 existing screen files removing deleted color constants.

**Not in scope — deferred to sub-specs B/C/D:**

- Per-screen layout ports (editorial hero, hairline ledger rows, 5-column grids). Screens keep their current Material compact-card layouts until individually ported.
- Replacing `Icons.Default.*` with Unicode glyphs — accepted platform concession per Q4(d).
- Adding `Masthead` to each screen — the primitive exists; screens adopt it when ported individually.
- Bottom-sheet migration of existing dialogs.
- New screens that don't exist today (GardenList, pure SpeciesList) — future separate work, not a visual redesign task.

---

## 2. Token layer

### `ui/theme/Color.kt` — replace entirely

```kotlin
package app.verdant.android.ui.theme

import androidx.compose.ui.graphics.Color

// Fältet palette — matches web tokens in web/src/index.css
val FaltetCream   = Color(0xFFF5EFE2)
val FaltetPaper   = Color(0xFFFBF7EC)
val FaltetInk     = Color(0xFF1E241D)
val FaltetForest  = Color(0xFF2F3D2E)
val FaltetSage    = Color(0xFF6B8F6A)
val FaltetClay    = Color(0xFFB6553C)
val FaltetMustard = Color(0xFFC89A2B)
val FaltetBerry   = Color(0xFF7A2E44)
val FaltetSky     = Color(0xFF4A7A8C)
val FaltetButter  = Color(0xFFF2D27A)
val FaltetBlush   = Color(0xFFE9B8A8)

// Hairline alphas
val FaltetInkLine20 = FaltetInk.copy(alpha = 0.20f)
val FaltetInkLine40 = FaltetInk.copy(alpha = 0.40f)
val FaltetInkFill04 = FaltetInk.copy(alpha = 0.04f)
```

The old `GreenPrimary`, `GreenDark`, `GreenLight`, `Cream`, `CreamDark`, `White`, `TextPrimary`, `TextSecondary`, `ErrorRed` are deleted. A find-replace (Task 2) updates the ~44 call sites.

### `ui/theme/Theme.kt` — replace

`FaltetColorScheme` maps Fältet colors onto Material token names:

| Material slot | Fältet color | Usage |
|---|---|---|
| `primary` | `FaltetInk` | filled buttons, main text, drawer active bg |
| `onPrimary` | `FaltetCream` | text on ink buttons |
| `secondary` | `FaltetClay` | accent — periods, destructive text, active-nav bullet |
| `onSecondary` | `FaltetCream` | text on clay fills |
| `tertiary` | `FaltetSage` | positive tone — harvest stats, success states |
| `background` | `FaltetCream` | global page bg |
| `onBackground` | `FaltetInk` | primary text on page bg |
| `surface` | `FaltetPaper` | cards, sheets, dialogs |
| `onSurface` | `FaltetInk` | text on cards |
| `surfaceVariant` | `FaltetCream` | drawer, masthead, secondary surfaces |
| `onSurfaceVariant` | `FaltetForest` | secondary text |
| `outline` | `FaltetInk` | 1dp borders |
| `outlineVariant` | `FaltetInkLine20` | hairline rules |
| `error` | `FaltetClay` | errors / destructive — Fältet uses clay, not red |
| `onError` | `FaltetCream` | text on error fills |

`FaltetShapes` sets every corner radius to `0.dp`. Components that need pill radius (Chip, FAB) apply `CircleShape` locally.

`verdantTopAppBarColors()` keeps its signature (call sites don't change) and returns Fältet colors:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun verdantTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = FaltetCream,
    titleContentColor = FaltetInk,
    navigationIconContentColor = FaltetInk,
    actionIconContentColor = FaltetInk,
)
```

---

## 3. Typography

### Dependency

```kotlin
// android/app/build.gradle.kts
dependencies {
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.5")  // version matches the Compose BOM
}
```

### `ui/theme/Typography.kt` (new file)

`GoogleFont.Provider` wired to `com.google.android.gms.fonts` with standard AndroidX cert array (`R.array.com_google_android_gms_fonts_certs` — bundled with the library).

Two `FontFamily` values built from `GoogleFont` descriptors:

- `FaltetDisplay` — Fraunces, weights 300 + 400 + 500, roman and italic.
- `FaltetBody` — Inter, weights 400 + 500 + 600.
- `FaltetMono` — `FontFamily.Monospace` (platform default: Android ships Noto Sans Mono / Droid Sans Mono).

Material `Typography` mapping fills every slot with Fältet-appropriate size/weight/spacing. Key styles:

- `displayLarge` — Fraunces 300 80sp, `letter-spacing: -1.5`. Hero titles.
- `displaySmall` — Fraunces 300 44sp, `letter-spacing: -0.8`. Species / bed detail heros.
- `headlineMedium` — Fraunces 300 italic 26sp. Editorial strips, card totals.
- `titleLarge` — Fraunces 400 20sp. Row titles, species names in ledgers.
- `bodyLarge` — Inter 400 15sp `line-height: 22sp`. Description paragraphs.
- `labelMedium` — mono 10sp `letter-spacing: 1.4sp`. Small-caps labels (callers uppercase the string).
- `labelSmall` — mono 9sp `letter-spacing: 1.4sp`. Field labels, section headers.

Callers apply uppercase via `String.uppercase()`; Compose's `textTransform` is not idiomatic.

### Fallback

Downloadable Fonts fetch asynchronously. If the font isn't cached locally on first run, text briefly renders in `FontFamily.Default` then swaps once Fraunces arrives (Android-standard behavior). On devices without Google Play Services the fetch silently fails and the fallback sticks — acceptable degradation.

---

## 4. Primitives

All under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`. No barrel file — Compose imports directly from the package.

- **`Chip.kt`** — `FaltetTone` enum (Clay / Mustard / Berry / Sky / Sage / Forest) + `Chip(text, tone, filled)` composable. Pill shape (`CircleShape`), 1dp tone border, mono 10sp uppercase label, 10dp/4dp padding. Filled variant for active-state filter pills.
- **`Rule.kt`** — `Rule(variant: RuleVariant)`. Ink variant = solid `FaltetInk`; Soft variant = `FaltetInkLine20`. Full-width 1dp horizontal hairline.
- **`Stat.kt`** — `Stat(value, label, unit?, delta?, hue, size)` with `StatSize.Large` (88sp) / `Medium` (56sp) / `Small` (32sp) presets. Value in Fraunces 300 with `letter-spacing: -1.2sp`, unit glyph in italic hue-colored, label in mono 11sp 1.8 letter-spacing with 6dp colored dot prefix, delta in clay with ▲ prefix.
- **`Field.kt`** — `Field(label, value, onValueChange?, accent?, placeholder?, keyboardType)`. Read-only mode shows label + value + 1dp ink bottom rule. Editable mode uses `BasicTextField` with the same visual treatment. Accent tone recolors the value text.
- **`PhotoPlaceholder.kt`** — `PhotoPlaceholder(tone, label, aspectRatio)`. Radial-gradient tone wash (Sage / Blush / Butter) inside a 1dp ink border with mono label bottom-left. Reserves a future `src: Painter?` param.
- **`Masthead.kt`** — `Masthead(left, center, right?)`. Three-column row: mono 10sp uppercase `left` label, italic Fraunces 14sp `center` editorial tagline, optional `right` slot. 22dp horizontal + 14dp vertical padding, cream bg, custom `drawBehind` bottom 1dp ink line (full `border` paints all four edges).

### Testing

No unit tests for primitives in this spec. The project has no Compose UI test harness; adding one is its own infrastructure project. Primitives are smoke-tested end-to-end when screens adopt them in later sub-specs.

---

## 5. Scaffold, drawer, bottom bar, FAB

### `ui/faltet/FaltetScreenScaffold.kt` (new)

Shared screen shell that stacks `topBar` + `Masthead` in the Scaffold's top slot:

```kotlin
@Composable
fun FaltetScreenScaffold(
    topBar: @Composable () -> Unit = {},
    mastheadLeft: String,
    mastheadCenter: String,
    mastheadRight: @Composable (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    fab: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
)
```

Wraps `Scaffold` with `containerColor = FaltetCream`. `topBar` slot contains a `Column { topBar(); Masthead(...) }` so screens with a back-arrow `TopAppBar` can still surface the editorial masthead right below it. Ported screens adopt `FaltetScreenScaffold` one at a time; the composable itself is installed in this spec.

### Drawer (`ui/navigation/NavGraph.kt` changes)

Preserve the existing `ModalNavigationDrawer` structure. Replace the drawer content's sheet with a Fältet-styled column:

1. **Header** — 24dp top padding + 16dp horizontal: Fraunces italic 26sp "Verdant" + clay "." + mono 10sp 0.08em uppercase "Est. 2026 — Småland" below. 1dp ink hairline.
2. **Five grouped sections**, four top-docked and the last bottom-docked via `Modifier.weight(1f)` spacer:
   - `§ ODLING` — MyWorld, PlantedSpeciesList (Plantor), Workflows progress links per species
   - `§ UPPGIFTER` — TaskList, SeedInventory, Supplies, Successions, ProductionTargets
   - `§ SKÖRD & FÖRSÄLJNING` — HarvestStats (Skörd), Customers, Bouquets
   - `§ ANALYS` — VarietyTrials, PestDisease, Analytics
   - `§ KONTO` (bottom-docked) — Seasons, Account
3. **Section headers** — mono 9sp 1.4 letter-spacing uppercase forest 0.7 opacity, 14dp top padding, 6dp bottom padding, 18dp horizontal padding.
4. **Item rows** — full-width buttons, 48dp min height, italic Fraunces 18sp, 12dp vertical + 18dp horizontal padding, 1dp `FaltetInkLine20` bottom border. Active: clay text + trailing `●` clay bullet right-aligned. Ripple color = `FaltetInkFill04`.

Drawer entries that correspond to screens missing from today's Android app (GardenList as a first-class screen, pure Workflows landing) are **not added** — the drawer stays honest. If users want those screens later, that's a separate build task.

### Bottom bar

Current `NavigationBar` lives in `NavGraph.kt` gated by `showBottomBar`. Restyle:

- `containerColor = FaltetCream`
- `drawBehind` paints a 1dp ink line along the top edge only
- `NavigationBarItem` overrides:
  - `selectedIcon` + `unselectedIcon` — current Material icons kept
  - `indicatorColor = Color.Transparent` — no pill backdrop
  - `colors = NavigationBarItemDefaults.colors(selectedIconColor = FaltetClay, unselectedIconColor = FaltetForest, selectedTextColor = FaltetClay, unselectedTextColor = FaltetForest, indicatorColor = Color.Transparent)`
  - Label text style = mono 9sp 1.4 letter-spacing uppercase

### FAB style preset

A helper composable in `ui/faltet/FaltetFab.kt`:

```kotlin
@Composable
fun FaltetFab(
    onClick: () -> Unit,
    contentDescription: String?,
    icon: ImageVector = Icons.Default.Add,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = FaltetInk,
        contentColor = FaltetClay,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
```

Adopted on list screens (CustomerList, PestDiseaseLog, etc.) when those screens are ported in sub-spec B.

---

## 6. Existing-screen migration strategy

When Tasks 1–3 land, every screen that uses `MaterialTheme.colorScheme.*` or `MaterialTheme.typography.*` inherits Fältet tokens automatically.

**Direct references to deleted constants** (~44 screens affected — find-replace in Task 2):

| Deleted | Replacement |
|---|---|
| `GreenPrimary` | `FaltetClay` |
| `GreenDark` | `FaltetInk` |
| `GreenLight` | `FaltetSage` |
| `Cream` | `FaltetCream` |
| `CreamDark` | `FaltetPaper` |
| `White` | `FaltetCream` |
| `TextPrimary` | `FaltetInk` |
| `TextSecondary` | `FaltetForest` |
| `ErrorRed` | `FaltetClay` |

Task 2 runs a shell find-replace then opens each touched file for a quick visual review to ensure no replacement semantics are off (e.g., a `White` used as a color on an icon in a filled button context might semantically be `onPrimary`, not `cream`). Usually the blanket replacement is right; edge cases get one-off edits.

**What screens look like immediately after the foundation lands** (before any per-screen port):

- Cards: paper-colored with ink text, zero corner radius, still Material-pattern `Card { … }`.
- Top bars: cream background, ink icons, no mono/italic center strip yet.
- Buttons: ink-filled with cream text, mono uppercase labels. Material `Button { Text("Save") }` automatically picks this up.
- Chips: use the new `Chip` primitive only if the screen is ported; old `AssistChip` / `FilterChip` usages render with Fältet colors via `colorScheme` but keep their rounded pill shape (Material default) until replaced.
- Icons: `Icons.Default.*` still Material. Per Q4(d), this is an accepted platform concession.

This is the expected transitional state. Sub-specs B/C/D migrate screens individually.

---

## 7. Testing

- Compile gate: `cd android && ./gradlew compileDebugKotlin --no-daemon -q` green after each task.
- Final task runs `./gradlew assembleDebug` to confirm APK builds with the Downloadable Fonts dep.
- No unit/instrumentation tests added — matching the project's current Compose test situation.
- Manual smoke: install debug build on emulator, open every bottom-nav tab and drawer entry, look for obvious color/layout regressions.

---

## 8. Phasing

Single plan, eight tasks.

| Task | Name |
|---|---|
| 1 | Replace `Color.kt` + `Theme.kt` |
| 2 | Find-replace old color constants across 44 screens |
| 3 | Typography (Google Fonts dep + `Typography.kt` + Material `Typography` mapping) |
| 4 | Primitives (Chip, Rule, Stat, Field, PhotoPlaceholder, Masthead under `ui/faltet/`) |
| 5 | Drawer restyle in `NavGraph.kt` |
| 6 | Bottom bar restyle in `NavGraph.kt` |
| 7 | `FaltetScreenScaffold` + `FaltetFab` primitives |
| 8 | Milestone (compile + manual smoke) |

Rough sizing: 1-1.5 solo-dev days.

**Follow-up specs:**

- **Spec B (lists)** — SeasonSelector, CustomerList, PestDiseaseLog, VarietyTrials, BouquetRecipes, SuccessionSchedules, ProductionTargets, SupplyInventory.
- **Spec C (detail + forms + activities)** — BedDetail, PlantDetail, GardenDetail, CreateBedScreen, CreatePlantScreen, AddPlantEventScreen, SowActivity, PotUp, PlantOut, Harvest, Recover, Discard, BatchPotUp, ApplySupplyScreen, task forms.
- **Spec D (analytics + account + auth)** — Dashboard, MyVerdantWorldScreen, HarvestStatsScreen, AnalyticsScreen, AccountScreen, AuthScreen, SplashScreen, WorkflowProgressScreen, PlantPicker, PlantedSpeciesListScreen + Detail.

---

## 9. Open questions

None as of 2026-04-22 — all decisions recorded in sections 1–8.
