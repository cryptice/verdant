# Android Fältet — Sub-Spec C2: Form Screens (Design)

**Status:** design, awaiting implementation plan
**Scope:** 5 Android form (create / edit) screens
**Predecessor:** Spec C1 (Details) shipped at milestone `8112a75` + patch `3e8bf07`.
**Successors:** Sub-spec C3 (activities), D (analytics/account/auth).

---

## 1. Goal

Port 5 Android form screens (`CreateBed`, `CreateGarden`, `CreatePlant`, `TaskForm`, `AddPlantEvent`) from `OutlinedTextField` + Material idioms to the Fältet editorial aesthetic using 6 new primitives + an extension to the existing `Field.kt`.

**Non-goals:**

- No API or data-model changes.
- No new form features; no validation overhauls beyond what's needed to render errors under Fältet.
- CreateGarden's map boundary drawer is preserved as-is — only the surrounding form chrome is Fältet-styled.
- Activity screens (Sow, PotUp, PlantOut, ApplySupply, etc.) are out of scope → sub-spec C3.
- Inline edit dialogs in detail screens (GardenDetail, BedDetail) are out of scope — noted as a follow-up polish pass after C2 ships.

**Success criteria:**

- All 5 form screens render with Fältet masthead + cream body + hairline-separated form fields + fixed bottom submit bar.
- Text entry uses the extended `Field` primitive; short enums use `FaltetChipSelector`; long/searchable lists use `FaltetDropdown` (modal bottom sheet + search); dates use `FaltetDatePicker`; images use `FaltetImagePicker`.
- Submit button pinned to the bottom of the screen, `enabled` + `submitting` states wired.
- Each screen has a `@Preview`.
- `./gradlew assembleDebug` green. Manual emulator smoke on all 5 screens shows no layout breaks.

---

## 2. Scope

### In scope — 5 screens

| Screen | File | Current LOC | Notes |
|---|---|---|---|
| CreateBed | `ui/bed/CreateBedScreen.kt` | 431 | Reference port — text + numeric + chip selector + toggle + submit |
| CreateGarden | `ui/garden/CreateGardenScreen.kt` | 774 | Map boundary drawer stays as-is; form chrome ported |
| CreatePlant | `ui/plant/CreatePlantScreen.kt` | 273 | Minimal form + image picker |
| TaskForm | `ui/task/TaskFormScreen.kt` | 337 | Unified create/edit; dropdowns + date picker |
| AddPlantEvent | `ui/plant/AddPlantEventScreen.kt` | 454 | Complex: chip selector + conditional numeric fields + image picker + customer dropdown |

### Out of scope — later sub-specs / follow-ups

- **Activity screens** (SowActivity, BatchPotUp, BatchPlantOut, ApplySupply, AddSeeds, AddSpecies, GenericActivity) → **sub-spec C3**.
- **Inline edit dialogs** in detail screens (GardenDetail, BedDetail) — low-risk polish pass after C2 ships, not blocking C3.
- **MyWorld dashboard, SeasonSelector** → deferred to later standalone brainstorms.
- **Analytics, Account, Auth, Splash** → sub-spec D.

---

## 3. Design Decisions (summary of brainstorm)

| # | Decision | Chosen |
|---|---|---|
| 1 | CreateGarden map-drawing scope | Include CreateGarden in full; preserve the existing map boundary drawer component untouched inside the Fältet form chrome. |
| 2 | Enum selection pattern | Two primitives: `FaltetChipSelector` for short enums (soil type, aspect, etc.), `FaltetDropdown` for long/searchable lists (species, customer). |
| 3 | Primitive set | 6 new primitives + `Field` extension (see §4). |
| 4 | Submit button placement | Fixed bottom bar via `FaltetScreenScaffold.bottomBar` slot — always reachable. |
| 5 | `FaltetDropdown` presentation | `ModalBottomSheet` with `FaltetSearchField` + filtered `FaltetListRow` options. Not `ExposedDropdownMenu`. |
| 6 | Implementation strategy | Vertical slice — primitives first, CreateBed as reference port, batch the remaining 4. |

---

## 4. New primitives

All under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`.

### 4.1 Extend existing `Field.kt`

Add two parameters with defaults (no signature break):

```kotlin
@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    accent: FaltetTone? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,         // NEW
    required: Boolean = false,      // NEW
    modifier: Modifier = Modifier,
)
```

**Behavior:**
- When `error != null`: swap the 1dp bottom line color from `FaltetInk` to `FaltetClay`; render the error message as body-12sp `FaltetClay` Text 4dp below the underline.
- When `required == true`: append a subtle `*` in `FaltetClay` after the uppercased label, same font/size as the label.
- Existing callers (C1, prior code) unaffected — defaults preserve current visual.

### 4.2 `FaltetChipSelector.kt`

Horizontal flow row of `FilterChip`s, single-select, nullable.

```kotlin
@Composable
fun <T : Any> FaltetChipSelector(
    label: String,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T?) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String,
    required: Boolean = false,
)
```

**Visual:**
- Label rendered identically to `Field`'s label (mono 9sp letter-spacing 1.4sp uppercase `FaltetForest` alpha 0.7, optional required `*` in clay).
- `FlowRow` (`androidx.compose.foundation.layout.FlowRow`) of `FilterChip`s. Horizontal spacing 6dp, vertical 4dp.
- Chip colors — unselected: `FaltetInkLine40` 1dp border + `FaltetForest` label + transparent fill; selected: `FaltetClay` fill + `FaltetCream` label + 0dp border. Zero corner radius.
- Tapping the selected chip clears it → `onSelectedChange(null)`.

### 4.3 `FaltetDropdown.kt`

Field chrome that opens a `ModalBottomSheet` containing an optional `FaltetSearchField` + a `LazyColumn` of `FaltetListRow` options.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> FaltetDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String,
    searchable: Boolean = true,
    placeholder: String = "Välj…",
    required: Boolean = false,
)
```

**Visual — field chrome:**
- Same label pattern as `Field`.
- A clickable Row with: italic 20sp Fraunces value (`labelFor(selected)` in `FaltetInk` when selected, `placeholder` in `FaltetForest.copy(alpha = 0.4f)` when unselected) — 1dp `FaltetInk` underline — trailing `Icons.Default.ArrowDropDown` in `FaltetClay` at 18dp.

**Visual — bottom sheet:**
- `ModalBottomSheet(containerColor = FaltetCream, ...)` with a handle (Material default).
- If `searchable`: `FaltetSearchField(value = query, onValueChange = { query = it }, placeholder = "SÖK")` as first content item.
- `LazyColumn` below: one `FaltetListRow(title = labelFor(option), onClick = { onSelectedChange(option); closeSheet() })` per option.
- Filter predicate: case-insensitive `labelFor(option).contains(query, ignoreCase = true)`.

### 4.4 `FaltetDatePicker.kt`

Field chrome that opens Material3 `DatePickerDialog`, displays Swedish-formatted date.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltetDatePicker(
    label: String,
    value: java.time.LocalDate?,
    onValueChange: (java.time.LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Välj datum",
    required: Boolean = false,
)
```

**Visual:**
- Same field chrome as `FaltetDropdown` but trailing icon is `Icons.Default.CalendarMonth` in `FaltetClay`.
- Value format:
  - Current year: `"23 apr"`
  - Other years: `"23 apr 2025"`
- Month labels use the short-form helper: `"jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec"`.

**Dialog:**
- Material3 `DatePickerDialog` with `rememberDatePickerState(initialSelectedDateMillis = value?.toEpochDay()?.times(86_400_000))`.
- Confirm button: `"Välj"` in `FaltetClay`.
- Dismiss button: `"Avbryt"`.
- On confirm: `Instant.ofEpochMilli(state.selectedDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate()` → `onValueChange(...)`.

### 4.5 `FaltetImagePicker.kt`

Camera + gallery buttons with preview thumbnail.

```kotlin
@Composable
fun FaltetImagePicker(
    label: String,
    value: android.graphics.Bitmap?,
    onValueChange: (android.graphics.Bitmap?) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Visual — empty state (`value == null`):**
- Label (mono 9sp, standard `Field`-label style).
- Row (spacing 8dp) of two outlined `OutlinedButton`s:
  - `"KAMERA"` with leading `Icons.Default.PhotoCamera` (14dp, clay tint). Opens `rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview())`.
  - `"GALLERI"` with leading `Icons.Default.Image`. Opens `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())` with `"image/*"`.
- Button shape: zero radius, 1dp `FaltetInkLine40` border, `FaltetClay` text, mono 10sp.

**Visual — populated state (`value != null`):**
- Label.
- `Box(Modifier.fillMaxWidth().height(200.dp).border(1.dp, FaltetInk))` containing:
  - `Image(bitmap = value.asImageBitmap(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())`
  - Top-right overlay: small `IconButton(onClick = { onValueChange(null) })` with `Icons.Default.Close` in `FaltetClay`, 24dp square, cream background at 80% alpha.

**Gallery URI → Bitmap conversion:** use `BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))`. Base64 compression is done by the caller using the existing `toCompressedBase64()` utility — the primitive doesn't know about network payload.

### 4.6 `FaltetSubmitButton.kt`

Full-width ink button.

```kotlin
@Composable
fun FaltetSubmitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    submitting: Boolean = false,
)
```

**Visual:**
- `Button` — `containerColor = FaltetInk` when enabled, `FaltetInk.copy(alpha = 0.4f)` when disabled. `contentColor = FaltetCream`. Zero corner radius. `fillMaxWidth().height(56.dp)`.
- When `submitting == true`: render `CircularProgressIndicator(color = FaltetCream, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))` instead of text. Click is a no-op during submit; `onClick` only fires when `!submitting && enabled`.
- Otherwise: `Text(label.uppercase(), fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp)`.

### 4.7 `FaltetFormSubmitBar.kt`

Bottom-bar wrapper — `FaltetSubmitButton` on a cream background with 1dp `FaltetInkLine20` top border. Call in scaffold's `bottomBar` slot.

```kotlin
@Composable
fun FaltetFormSubmitBar(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    submitting: Boolean = false,
)
```

**Visual:**
- `Column(Modifier.fillMaxWidth().background(FaltetCream).drawBehind { drawLine(FaltetInkLine20, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(horizontal = 18.dp, vertical = 12.dp))`.
- Inside: single `FaltetSubmitButton(label, onClick, enabled = enabled, submitting = submitting)`.

### 4.8 `FaltetScreenScaffold` snackbar-host param

If `FaltetScreenScaffold` doesn't already accept a `snackbarHost: @Composable () -> Unit = {}` parameter, add one. Trivial addition (4 lines — add param, add `snackbarHost = snackbarHost` in the inner `Scaffold(...)`). Part of Phase 1 Task 7 commit.

---

## 5. Screen port pattern

Every form screen follows the same outer structure:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleFormScreen(
    onBack: () -> Unit,
    viewModel: ExampleFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show submit error via snackbar
    LaunchedEffect(uiState.formError) {
        uiState.formError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFormError()
        }
    }

    // Navigate back on successful submit
    LaunchedEffect(uiState.submitted) {
        if (uiState.submitted) onBack()
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Bädd",
        mastheadCenter = if (uiState.editMode) uiState.name else "Ny bädd",
        bottomBar = {
            FaltetFormSubmitBar(
                label = if (uiState.editMode) "Spara" else "Skapa",
                onClick = { viewModel.submit() },
                enabled = uiState.canSubmit && !uiState.isSubmitting,
                submitting = uiState.isSubmitting,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // form field items
                item { Field(label = "Namn", value = uiState.name, onValueChange = viewModel::onNameChange, required = true, error = uiState.nameError) }
                // ...
            }
        }
    }
}
```

### 5.1 Required-vs-optional labels

- **Required fields**: pass `required = true` to the primitive. A subtle `*` in clay appears after the label.
- **Optional fields**: include `(valfri)` inline in the Swedish label, e.g., `label = "Beskrivning (valfri)"`. Matches existing convention, keeps labels self-describing.

### 5.2 Validation

- **Per-field inline**: primitives accept `error: String?`. Screens track individual error states; compute on submit attempt (or as-you-type for trivial ones like numeric format).
- **Form-level**: submit errors from network go to `SnackbarHostState`. Don't double-render inline + snackbar — network failure = snackbar only.
- **Submit enablement**: `enabled = canSubmit && !isSubmitting`. `canSubmit` is derived in VM from "all required fields non-blank" — matches existing `CreateBedScreen` pattern.

### 5.3 Edit mode (TaskForm only)

- When editing, VM fetches the existing entity on init and populates form state.
- Masthead center shows entity title; submit label becomes `"Spara"`.
- `FaltetLoadingState` shown during initial fetch.

---

## 6. Per-screen structure

### 6.1 `CreateBedScreen` — reference port

**Masthead:** `§ Bädd` / `"Ny bädd"`.
**Bottom bar:** `FaltetFormSubmitBar(label = "Skapa", ...)`.

**Form fields (order):**

1. `Field(label = "Namn", required = true, error = uiState.nameError)`
2. `Field(label = "Beskrivning (valfri)")`
3. `FaltetChipSelector(label = "Jordtyp", options = BedSoilType.entries, labelFor = { bedSoilTypeLabel(it) })`
4. `Field(label = "pH (valfri)", keyboardType = KeyboardType.Decimal, error = if (phInvalid) "pH måste vara mellan 3.0 och 9.0" else null)`
5. `FaltetChipSelector(label = "Dränering", options = BedDrainage.entries, labelFor = { bedDrainageLabel(it) })`
6. `FaltetChipSelector(label = "Sol", options = BedSunExposure.entries, labelFor = { bedSunExposureLabel(it) })`
7. `FaltetChipSelector(label = "Väderstreck", options = BedAspect.entries, labelFor = { bedAspectLabel(it) })`
8. `FaltetChipSelector(label = "Bevattning", options = BedIrrigationType.entries, labelFor = { bedIrrigationTypeLabel(it) })`
9. `FaltetChipSelector(label = "Skydd", options = BedProtection.entries, labelFor = { bedProtectionLabel(it) })`
10. `FaltetChipSelector(label = "Upphöjd bädd (valfri)", options = listOf(true, false), labelFor = { if (it) "Ja" else "Nej" })` — boolean chip pair, no separate toggle primitive.

**Validation:**
- `name.isNotBlank()` required.
- `pH` (if non-blank) must parse to Double in `[3.0, 9.0]`.
- Submit enabled when name valid and pH (if present) valid.

### 6.2 `CreateGardenScreen`

**Masthead:** `§ Trädgård` / `"Ny trädgård"`.
**Bottom bar:** `FaltetFormSubmitBar(label = "Skapa", ...)`.

**Form fields (order):**

1. `Field(label = "Namn", required = true, error = uiState.nameError)`
2. `Field(label = "Emoji", placeholder = "🌱")` — single-grapheme entry; preserve any existing emoji-picker affordance without rebuilding it.
3. `Field(label = "Beskrivning (valfri)")`
4. **Location search field** (if present in current code): port as `Field` with trailing search icon; display resolved location as static text below. Don't restyle the map result dropdown — preserve existing pattern.
5. **Map boundary drawer:** render the existing map component as a single item in the `LazyColumn`. Wrap with `Modifier.padding(horizontal = 18.dp)` + hairline `FaltetInkLine20` bottom border for visual separation. No Fältet restyle of map internals.
6. **Inline beds (if present)**: port the current "add beds as part of garden creation" flow as a small repeated group using `Field`s — same pattern as other forms.

**Validation:** name required; other fields optional.

### 6.3 `CreatePlantScreen`

**Masthead:** `§ Planta` / `"Ny planta"`.
**Bottom bar:** `FaltetFormSubmitBar(label = "Skapa", ...)`.

**Form fields (order):**

1. `FaltetImagePicker(label = "Foto (valfri)")`
2. `Field(label = "Namn", required = true, error = uiState.nameError)`
3. `Field(label = "Art (valfri)")` — free-text (matches current code).
4. `Field(label = "Antal frön (valfri)", keyboardType = KeyboardType.Number)`

**Existing suggestion cards:** if the current screen shows species suggestions, render them as a flow row of `Chip`s below the species field. Tapping fills the species field. Preserve the data source.

### 6.4 `TaskFormScreen` (create + edit)

**Masthead:**
- Create: `§ Arbete` / `"Ny uppgift"`
- Edit: `§ Arbete` / `task.title` (fetched)

**Bottom bar:**
- Create: `FaltetFormSubmitBar(label = "Skapa", ...)`
- Edit: `FaltetFormSubmitBar(label = "Spara", ...)`

**Form fields (order):**

1. `FaltetDropdown(label = "Aktivitet", options = ActivityType.entries, labelFor = { activityTypeLabelSv(it) }, searchable = false, required = true)` — ~8 options.
2. `FaltetDropdown(label = "Art", options = uiState.species, labelFor = { it.commonName + (it.variantName?.let { v -> " $v" } ?: "") }, searchable = true, required = true)`
3. `FaltetDatePicker(label = "Deadline", required = true)`
4. `Field(label = "Målantal (valfri)", keyboardType = KeyboardType.Number)`
5. `Field(label = "Anteckningar (valfri)")`

**Edit mode:** when `taskId != null` in route args, VM fetches on init; show `FaltetLoadingState` until fetch completes.

### 6.5 `AddPlantEventScreen` — most complex

**Masthead:** `§ Händelse` / `plant.name`.
**Bottom bar:** `FaltetFormSubmitBar(label = "Spara", ...)`.

**Form fields (order):**

1. `FaltetImagePicker(label = "Foto (valfri)")`
2. `FaltetChipSelector(label = "Händelsetyp", options = eventTypeOptions, labelFor = { eventTypeLabelSv(it) }, required = true)` — multi-row flow.
3. **Conditional numeric fields by `eventType`:**
    - `HARVESTED` → plant count, weight (g), stem count, stem length (cm), vase life (days), `FaltetChipSelector` for quality grade, `FaltetDropdown` for customer.
    - `PLANTED_OUT` / `POTTED_UP` / `SEEDED` / `WATERED` / `FERTILIZED` → plant count only.
4. `Field(label = "Anteckningar (valfri)")`

**Conditional rendering:** `if (uiState.eventType == EventType.HARVESTED) { item { Field(...) } }` inline in the `LazyColumn`. No new primitive.

---

## 7. Phasing

### Phase 1 — Primitives (7 commits)

1. Extend `Field.kt` with `error` + `required` params (modification, not new file).
2. Create `FaltetChipSelector.kt` + `@Preview`.
3. Create `FaltetDropdown.kt` + `@Preview`.
4. Create `FaltetDatePicker.kt` + `@Preview`.
5. Create `FaltetImagePicker.kt` + `@Preview`.
6. Create `FaltetSubmitButton.kt` + `@Preview`.
7. Create `FaltetFormSubmitBar.kt` + `@Preview` (plus `FaltetScreenScaffold` `snackbarHost` param addition if missing).

Compile-green gate after each. No screen consumes them yet.

### Phase 2 — Reference port (1 commit + checkpoint)

8. Port `CreateBedScreen`. Validates: `Field` with required + error, `FaltetChipSelector` across 7 enum fields, numeric field with decimal validation, `FaltetFormSubmitBar` wiring, form-level snackbar on submit error.

Manual emulator smoke. If primitive APIs are wrong, amend primitives in new commits (no history rewrite) and fix CreateBed before Phase 3.

### Phase 3 — Batch ports (4 commits)

9. `CreatePlantScreen` — smallest; first use of `FaltetImagePicker`.
10. `TaskFormScreen` — `FaltetDropdown` (searchable + non-searchable) + `FaltetDatePicker` + edit-mode initial-load.
11. `CreateGardenScreen` — longest file; exercises map-wrapping inside the form.
12. `AddPlantEventScreen` — most complex; conditional fields + chip selector + image + dropdown.

### Phase 4 — Verify + milestone (1 empty commit)

13. `./gradlew assembleDebug` green. Manual smoke on all 5 screens. Empty milestone commit:

```
milestone: Android Fältet forms complete (Spec C2)
```

**Total: 13 tasks, 12 code commits + 1 milestone.**

---

## 8. Testing

### Per primitive (Phase 1)

- `@Preview` in the same file exercising main permutations:
  - `Field` (extended): required + optional + error.
  - `FaltetChipSelector`: selected + unselected + required.
  - `FaltetDropdown`: placeholder + selected; a second preview shows the bottom-sheet content.
  - `FaltetDatePicker`: empty + populated.
  - `FaltetImagePicker`: empty (both buttons visible) + populated (preview + × button).
  - `FaltetSubmitButton`: enabled + disabled + submitting.
  - `FaltetFormSubmitBar`: default.
- `./gradlew compileDebugKotlin` green after each commit.

### Per screen port (Phases 2–3)

- `@Preview` at end of each screen file exercising the populated form state (use hard-coded state values; no VM required).
- `./gradlew compileDebugKotlin` green after each screen commit.
- Manual emulator smoke after Phase 2 (CreateBed reference) and after Phase 3 (full batch).

### Not added this spec

- No snapshot testing.
- No new instrumented UI tests.
- No changes to existing view-model tests — view-model signatures unchanged.

### Known non-issue

First cold install flashes system-serif before Fraunces arrives via Downloadable Fonts — do not gate smoke on this.

---

## 9. Follow-up (outside this spec)

- **Inline edit dialogs in detail screens** (GardenDetail, BedDetail) still use `OutlinedTextField`s. After C2 ships, migrate those dialogs to the new `Field` + primitives — low-risk, rapid, 1 commit per detail screen. Not blocking C3.
- **Sub-spec C3** — activity / workflow screens.
- **Sub-spec D** — analytics, account, auth + deferred MyWorld dashboard and SeasonSelector.
