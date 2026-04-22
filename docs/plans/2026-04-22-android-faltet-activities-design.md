# Android Fältet — Sub-Spec C3: Activity Screens (Design)

**Status:** design, awaiting implementation plan
**Scope:** 6 Android activity / workflow screens
**Predecessor:** Spec C2 (Forms) shipped at milestone `3f700d8`.
**Successors:** Sub-spec C4 (AddSpecies with AI), Sub-spec D (analytics / account / auth + deferred MyWorld dashboard and SeasonSelector).

---

## 1. Goal

Port 6 Android activity / workflow screens (`ApplySupply`, `AddSeeds`, `SowActivity`, `BatchPotUp`, `BatchPlantOut`, `GenericActivity`) to the Fältet editorial aesthetic using 2 new primitives plus existing Spec A/B/C1/C2 primitives.

**Non-goals:**

- No API, data-model, navigation, or feature changes.
- No Fältet restyle of `AddSpeciesScreen` (deferred to a future C4 spec — 862 lines with AI workflows warrant dedicated design).
- No refactor of `GenericActivityScreen` into a parameterized single screen — port the shared scaffold instead; all variants inherit.

**Success criteria:**

- All 6 activity screens render Fältet: masthead + cream body + hairline-separated fields + fixed bottom submit bar.
- `FaltetScopeToggle` replaces `ApplySupply`'s `RadioButton` scope picker and `SowActivity`'s tray/bed picker.
- `FaltetChecklistGroup` replaces `ApplySupply`'s inline `Row + Checkbox` plant list.
- The 4–5 `GenericActivityScreen` sub-variants pick up Fältet styling via the shared scaffold rewrite — no per-variant rewrites.
- Two-stage batch flows (`BatchPotUp`, `BatchPlantOut`) preserve their in-composable state-driven list→form transitions; both phases Fältet-styled.
- Post-submit upsell dialogs (`SowActivity`, `BatchPotUp`) stay as `AlertDialog`s with Swedish copy — no Fältet restyle of dialog chrome.
- Each screen has a `@Preview`.
- `./gradlew assembleDebug` green. Manual emulator smoke on all 6 screens shows no layout breaks.

---

## 2. Scope

### In scope — 6 screens

| Screen | File | Current LOC | Notes |
|---|---|---|---|
| ApplySupply | `ui/activity/ApplySupplyScreen.kt` | 355 | Reference port. Scope toggle + multi-select plant list + supply dropdown. |
| AddSeeds | `ui/activity/AddSeedsScreen.kt` | 259 | Simplest. Species dropdown + quantity + optional dates. |
| SowActivity | `ui/activity/SowActivityScreen.kt` | 416 | Searchable species dropdown + tray/bed toggle + count + post-submit upsell dialog. Can be launched from a scheduled task (prefill). |
| BatchPotUp | `ui/activity/BatchPotUpScreen.kt` | 303 | Two-stage: tray-group picker → detail form. Post-submit upsell dialog. |
| BatchPlantOut | `ui/activity/BatchPlantOutScreen.kt` | 231 | Two-stage: tray-group picker → detail form + target bed. |
| GenericActivity | `ui/activity/GenericActivityScreen.kt` | 378 | Shared `ActivityScaffold` + `PhotoSection` + `CountField` + `FrequentCommentsField` + `SubmitButton` used by 4–5 variants (PotUp, Plant, Harvest, Recover, Discard). Port shared scaffolds once; variants inherit. |

### Out of scope

- **`AddSpeciesScreen`** (862 lines) — deferred to sub-spec C4 (AI workflows + multi-chip grids + create+edit diffing warrant dedicated design).
- **Inline edit dialogs** in C1 detail screens (`GardenDetail`, `BedDetail`) — separate polish pass.
- **Sub-spec D** — analytics, account, auth, deferred MyWorld dashboard, SeasonSelector.

---

## 3. Design decisions (summary of brainstorm)

| # | Decision | Chosen |
|---|---|---|
| 1 | Spec scope | Split sub-spec C activities into C3 (6 screens above) + C4 (AddSpecies alone). |
| 2 | New primitives | `FaltetScopeToggle` (two-option exhaustive exclusive picker) + `FaltetChecklistGroup` (multi-select list with optional select-all). No count-picker primitive — existing `Field` with `KeyboardType.Number` covers all counts in current code. |
| 3 | GenericActivity port | Port the shared `ActivityScaffold` / `PhotoSection` / `CountField` / `FrequentCommentsField` / `SubmitButton` private composables once. All 4–5 variants inherit automatically. |
| 4 | Implementation strategy | Vertical slice: 2 primitives → ApplySupply reference port (checkpoint) → 5 batch ports → verify. |

---

## 4. New primitives

Both under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`.

### 4.1 `FaltetScopeToggle.kt`

Two-option segmented-control-style picker. Not nullable — both options are exhaustive and exclusive.

**Signature:**

```kotlin
@Composable
fun <T : Any> FaltetScopeToggle(
    label: String,
    options: List<T>,       // exactly 2
    selected: T,
    onSelectedChange: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
)
```

**Visual:**
- Label row identical to `Field` (mono 9sp, letter-spacing 1.4sp, uppercase, `FaltetForest` alpha 0.7) + optional required `*` in clay.
- Horizontal `Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).border(1.dp, FaltetInkLine40))`.
- Two tappable halves, each `Modifier.weight(1f).fillMaxHeight()`, separated by a 1dp `FaltetInkLine40` vertical rule (drawn with `drawBehind`).
- Active half: `FaltetClay` background fill, `FaltetCream` label (mono 11sp, letter-spacing 1.4sp, uppercase).
- Inactive half: `FaltetCream` background, `FaltetForest` label.
- Zero corner radius.
- Tapping the active half is a no-op (distinguishes from `FaltetChipSelector` which deselects).
- `require(options.size == 2) { "FaltetScopeToggle requires exactly 2 options" }` — fail-fast in debug builds.

**Preview permutations:** selected-left, selected-right, required.

### 4.2 `FaltetChecklistGroup.kt`

Multi-select list with hairline rows + Fältet-styled checkboxes. Optional select-all affordance.

**Signature:**

```kotlin
@Composable
fun <T : Any> FaltetChecklistGroup(
    label: String,
    options: List<T>,
    selected: Set<T>,
    onSelectedChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String,
    subtitleFor: ((T) -> String?)? = null,
    selectAllEnabled: Boolean = false,
    required: Boolean = false,
)
```

**Visual:**
- Label row identical to other primitives.
- When `selectAllEnabled`: a row above the list containing `"Välj alla"` (when `selected.size < options.size`) or `"Avmarkera alla"` (when `selected.size == options.size`), right-aligned, mono 10sp letter-spacing 1.4sp uppercase `FaltetClay`, padded 18dp horizontal 8dp vertical, tap toggles. No hairline.
- A `Column` of rows. Each row:
  - `Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { toggle(option) }.drawBehind { hairline bottom }.padding(horizontal = 18.dp, vertical = 10.dp))`.
  - Leading: `FaltetCheckbox` (from Spec B primitives) — 24dp.
  - 12dp spacer.
  - Column (weight 1f): primary label Fraunces italic 16sp `FaltetInk`; optional subtitle below as mono 10sp letter-spacing 1.2sp `FaltetForest`.
- Full-row tap toggles selection; checkbox is visual indicator only (not independently tappable to avoid double-tap-target confusion — use `onCheckedChange = null` on the inner `FaltetCheckbox`).

**Preview permutations:** empty selection, partial selection, all-selected, with subtitle, with select-all affordance.

**Other primitives reused unchanged:**
- `FaltetDropdown` (C2) — for species / supply / bed pickers.
- `FaltetImagePicker` (C2) — for all photo slots.
- `FaltetDatePicker` (C2) — for optional collection/expiration dates in AddSeeds.
- `Field` (C2 extended) — for all text + numeric entry (counts, notes).
- `FaltetFormSubmitBar` (C2) — fixed bottom submit.
- `FaltetScreenScaffold` (C1/C2 with snackbarHost) — all screens.
- `FaltetListRow` (Spec B) — batch group picker rows in BatchPotUp / BatchPlantOut.
- `FaltetEmptyState` (Spec B) — batch list empty states.
- `Chip` (Spec A) — quick-pick suggestion chips inside `FrequentCommentsField`.
- `AlertDialog` (Material) — post-submit upsell dialogs; chrome stays Material, copy Swedish.

No count-picker primitive. Existing code uses plain text fields for counts; preserve that.

---

## 5. Screen port pattern + shared rules

### 5.1 Standard structure

Every activity screen:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleActivityScreen(
    // preserve current signature
    viewModel: ExampleActivityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.submitted) {
        if (uiState.submitted) onDone()
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ <entity category>",
        mastheadCenter = "<activity label>",
        bottomBar = {
            FaltetFormSubmitBar(
                label = "<verb>",
                onClick = viewModel::submit,
                enabled = uiState.canSubmit && !uiState.isSubmitting,
                submitting = uiState.isSubmitting,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // field items
        }
    }
}
```

**Shared rules:**
- Preserve VM method names + state shape + callback signature. Adapt field names to VM reality; do not invent.
- Drop `TopAppBar`, `OutlinedTextField`, `FilterChip`, `ExposedDropdownMenu*`, `RadioButton`, Material `Checkbox`, `verticalScroll` / `rememberScrollState`.
- Replace `InlineErrorBanner` usage (if present) with the scaffold's `SnackbarHost`.
- Base64 image encoding — preserve existing `toCompressedBase64()` wiring; route new `Bitmap?` through the same utility.
- Post-submit upsell dialogs (SowActivity, BatchPotUp) stay as `AlertDialog` with Swedish copy.
- Preserve all `LaunchedEffect`s for task prefill, auto-select-single-group, nav-on-done.

### 5.2 Two-stage flow pattern (BatchPotUp, BatchPlantOut)

Preserve the in-composable state machine:

```kotlin
var selectedGroup by remember { mutableStateOf<PlantGroup?>(null) }

FaltetScreenScaffold(
    mastheadLeft = "§ <category>",
    mastheadCenter = if (selectedGroup == null) "Välj grupp" else selectedGroup!!.speciesName,
    mastheadRight = if (selectedGroup != null) {
        {
            IconButton(onClick = { selectedGroup = null }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, "Tillbaka", tint = FaltetClay, modifier = Modifier.size(18.dp))
            }
        }
    } else null,
    bottomBar = if (selectedGroup != null) {
        { FaltetFormSubmitBar(...) }
    } else { {} },
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { padding ->
    if (selectedGroup == null) {
        // list phase
    } else {
        // detail phase
    }
}
```

- List phase: `LazyColumn` of `FaltetListRow(title = ..., meta = ..., onClick = { selectedGroup = group })`. Empty state via `FaltetEmptyState`.
- Detail phase: standard form `LazyColumn` with `Field`s / `FaltetDropdown` / `FaltetImagePicker`.
- Preserve the `LaunchedEffect(uiState.groups) { if (uiState.groups.size == 1) selectedGroup = uiState.groups.first() }` auto-select behavior.

### 5.3 `GenericActivityScreen` shared-scaffold port

Single file port. Rewrites inside the file:

- `ActivityScaffold` (private composable) → wrap `FaltetScreenScaffold` + `FaltetFormSubmitBar` via its `bottomBar` slot. Same content-lambda signature, so variants don't change.
- `PhotoSection` → call `FaltetImagePicker(label = "Foto (valfri)", value = bitmap, onValueChange = ...)`. Preserve Base64 side-effect wiring.
- `SubmitButton` → deprecate (replaced by `FaltetFormSubmitBar` in the scaffold's `bottomBar` slot). Callers that invoked `SubmitButton()` at the bottom of their scroll need the submit action routed through the scaffold instead. Refactor callers minimally to pass their submit callback up to `ActivityScaffold(onSubmit = ...)`.
- `CountField` → `Field(label = "Antal", value = countText, onValueChange = ..., keyboardType = KeyboardType.Number, required = true, error = countError)`. Drop inline ±/+ icon buttons (out of scope; stepper pattern not needed here).
- `FrequentCommentsField` → `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = ...)` + below it an inline `FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp))` of `Chip(label = suggestion, onClick = { notes = suggestion })` for quick-pick suggestions.

Per-variant masthead + submit copy:

| Variant | `mastheadLeft` | Submit label |
|---|---|---|
| `PotUpActivityScreen` | § Kruka upp | Kruka upp |
| `PlantActivityScreen` | § Plantera ut | Plantera ut |
| `HarvestActivityScreen` | § Skörda | Skörda |
| `RecoverActivityScreen` | § Återhämta | Återhämta |
| `DiscardActivityScreen` | § Kassera | Kassera |

Every variant's `mastheadCenter = plant.commonName` (or the current screen's display-name logic).

Variants don't change their own field orderings — only the shared scaffold rewrite touches them indirectly.

---

## 6. Per-screen structure

### 6.1 `ApplySupplyScreen` — reference port

**Masthead:** `§ Gödsling` / `"Applicera förnödenhet"`.
**Bottom bar:** `FaltetFormSubmitBar(label = "Spara", ...)`.

**Field order:**

1. `FaltetScopeToggle(label = "Omfattning", options = listOf(Scope.BED, Scope.PLANTS), selected = scope, onSelectedChange = { scope = it }, labelFor = { if (it == Scope.BED) "Hela bädden" else "Enskilda plantor" }, required = true)`.
2. If `scope == Scope.PLANTS`: `FaltetChecklistGroup(label = "Plantor", options = uiState.plantsInBed, selected = selectedPlantIds, onSelectedChange = { selectedPlantIds = it }, labelFor = { it.displayName }, subtitleFor = { "Status: ${statusLabelSv(it.status)}" }, selectAllEnabled = true, required = true)`.
3. `FaltetDropdown(label = "Förnödenhet", options = uiState.supplies, selected = selectedSupply, onSelectedChange = ..., labelFor = { "${it.supplyTypeName} · ${formatQuantity(it.quantity, it.unit)}" }, searchable = true, required = true)`.
4. `Field(label = "Mängd", value = quantityText, onValueChange = { quantityText = it; quantityError = null }, keyboardType = KeyboardType.Decimal, required = true, error = quantityError)` — error set to `"Mängd överskrider tillgängligt"` when the parsed value exceeds `selectedSupply.quantity`.
5. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = ...)`.

**Validation:**

```kotlin
val canSubmit = (scope == Scope.BED || selectedPlantIds.isNotEmpty())
    && selectedSupply != null
    && quantityText.toDoubleOrNull()?.let { it > 0 && it <= (selectedSupply?.quantity ?: 0.0) } == true
    && !uiState.isSubmitting
```

Adapt `Scope` enum to whatever the current code uses. If the model has a `Boolean isBedScope` instead of an enum, wrap it in a local `enum class Scope { BED, PLANTS }` mapping.

### 6.2 `AddSeedsScreen`

**Masthead:** `§ Inventarie` / `"Lägg till frön"`.
**Bottom bar:** `FaltetFormSubmitBar(label = "Spara", ...)`.

**Field order:**

1. `FaltetDropdown(label = "Art", options = uiState.species, selected = selectedSpecies, onSelectedChange = ..., labelFor = { speciesDisplayName(it) }, searchable = true, required = true)`.
2. `Field(label = "Antal frön", value = quantityText, onValueChange = ..., keyboardType = KeyboardType.Number, required = true, error = if (quantityError) "Antal krävs" else null)`.
3. `FaltetDatePicker(label = "Skördedatum (valfri)", value = collectionDate, onValueChange = ...)`.
4. `FaltetDatePicker(label = "Utgångsdatum (valfri)", value = expirationDate, onValueChange = ...)`.
5. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = ...)`.

**Validation:** `canSubmit = selectedSpecies != null && quantityText.toIntOrNull()?.let { it > 0 } == true && !uiState.isSubmitting`.

### 6.3 `SowActivityScreen`

**Masthead:** `§ Sådd` / `"Såaktivitet"`.
**Bottom bar:** `FaltetFormSubmitBar(label = "Så", ...)`.

**Preserve:** `LaunchedEffect(uiState.task) { /* prefill species + count from task */ }` — task-launched flow must still prefill fields from the scheduled task.

**Field order:**

1. `FaltetDropdown(label = "Art", options = uiState.species, selected = selectedSpecies, onSelectedChange = ..., labelFor = { speciesDisplayName(it) }, searchable = true, required = true)`.
2. `FaltetScopeToggle(label = "Destination", options = listOf(SowDestination.TRAY, SowDestination.BED), selected = destination, onSelectedChange = ..., labelFor = { if (it == SowDestination.TRAY) "Så i brätte" else "Så direkt i bädd" })`.
3. If `destination == SowDestination.BED`: `FaltetDropdown(label = "Bädd", options = uiState.beds, selected = selectedBed, onSelectedChange = ..., labelFor = { bedDisplayName(it) }, searchable = true, required = true)`.
4. If `selectedSpecies != null`: `FaltetDropdown(label = "Frökälla (valfri)", options = availableSeedBatches, selected = selectedSeedBatch, onSelectedChange = ..., labelFor = { seedBatchDisplayName(it) }, searchable = false)`.
5. `Field(label = "Antal frön", value = countText, onValueChange = ..., keyboardType = KeyboardType.Number, required = true, error = countError)`.
6. `FaltetImagePicker(label = "Foto (valfri)", value = photoBitmap, onValueChange = { photoBitmap = it; /* preserve existing Base64 side effects */ })`.
7. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = ...)`.

**Post-submit upsell:** preserve the existing `AlertDialog` offering supply-usage recording (jord / krukor). Translate any `stringResource` references to Swedish literals.

### 6.4 `BatchPotUpScreen` — two-stage

**List phase masthead:** `§ Kruka upp` / `"Välj grupp"`.
**Detail phase masthead:** `§ Kruka upp` / `<selectedGroup.speciesName>`, with back-arrow `IconButton` in masthead-right.

**List phase body:**

- `LazyColumn` of `FaltetListRow(title = group.speciesName, meta = "${formattedDate(group.sowDate)} · ${group.count} frön i brätte", onClick = { selectedGroup = group })`.
- Empty state: `FaltetEmptyState(headline = "Inga grupper att kruka upp", subtitle = "Så först några frön i brätten.")`.
- Preserve `LaunchedEffect(uiState.groups) { if (uiState.groups.size == 1) selectedGroup = uiState.groups.first() }`.

**Detail phase body:**

1. `Field(label = "Antal att kruka upp", value = countText, onValueChange = ..., keyboardType = KeyboardType.Number, required = true, error = countError)` — validate `1..selectedGroup.count`, error `"Antal måste vara mellan 1 och ${selectedGroup.count}"` otherwise.
2. `FaltetImagePicker(label = "Foto (valfri)", value = photoBitmap, onValueChange = ...)`.
3. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = ...)`.

**Detail phase bottom bar:** `FaltetFormSubmitBar(label = "Kruka upp", ...)`.

**Post-submit upsell:** preserve existing `AlertDialog` offering supply-usage recording.

### 6.5 `BatchPlantOutScreen` — two-stage

**List phase masthead:** `§ Plantera ut` / `"Välj grupp"`.
**Detail phase masthead:** `§ Plantera ut` / `<selectedGroup.speciesName>`, back-arrow in masthead-right.

**List phase body:**

- `LazyColumn` of `FaltetListRow(title = group.speciesName, meta = "${statusLabelSv(group.status)} · ${group.count} plantor", onClick = ...)`.
- Empty state: `FaltetEmptyState(headline = "Inga plantor att plantera ut", subtitle = "Så eller kruka upp först.")`.
- Preserve auto-select-single behavior.

**Detail phase body:**

1. `FaltetDropdown(label = "Målbädd", options = uiState.beds, selected = selectedTargetBed, onSelectedChange = ..., labelFor = { bedDisplayName(it) }, searchable = true, required = true)`.
2. `Field(label = "Antal att plantera ut", value = countText, onValueChange = ..., keyboardType = KeyboardType.Number, required = true, error = countError)`.

**Detail phase bottom bar:** `FaltetFormSubmitBar(label = "Plantera ut", ...)`.

### 6.6 `GenericActivityScreen` — shared-scaffold port

Single file port. Only the shared private composables get rewritten; the 4–5 variant entrypoints (`PotUpActivityScreen`, `PlantActivityScreen`, `HarvestActivityScreen`, `RecoverActivityScreen`, `DiscardActivityScreen`) stay untouched beyond masthead/submit-label wiring.

**Rewrites inside the file:**

- `ActivityScaffold(...)` — wrap `FaltetScreenScaffold` + bind `FaltetFormSubmitBar` to the `bottomBar` slot. Expose a submit callback in its signature so variants provide their own `onSubmit` and submit-label string.
- `PhotoSection(...)` — call `FaltetImagePicker(label = "Foto (valfri)", value = bitmap, onValueChange = ...)`. Preserve Base64 side effects.
- `SubmitButton(...)` — deprecate. Variants should pass their submit callback to the scaffold instead of rendering an inline button at the end of their content. Minimal caller refactor.
- `CountField(...)` — `Field(label = "Antal", value = countText, onValueChange = ..., keyboardType = KeyboardType.Number, required = true, error = countError)`. Drop the ±/+ buttons.
- `FrequentCommentsField(...)` — `Field(label = "Anteckningar (valfri)", ...)` + inline `FlowRow` of `Chip` quick-picks below.

**Per-variant copy:**

| Variant | `mastheadLeft` | Submit label |
|---|---|---|
| `PotUpActivityScreen` | § Kruka upp | Kruka upp |
| `PlantActivityScreen` | § Plantera ut | Plantera ut |
| `HarvestActivityScreen` | § Skörda | Skörda |
| `RecoverActivityScreen` | § Återhämta | Återhämta |
| `DiscardActivityScreen` | § Kassera | Kassera |

Each variant's `mastheadCenter = plant.commonName` (or whatever display-name helper the current code uses).

---

## 7. Phasing

### Phase 1 — Primitives (2 commits)

1. `FaltetScopeToggle.kt` + `@Preview`.
2. `FaltetChecklistGroup.kt` + `@Preview`.

Compile-green gate after each. No screen consumes them yet.

### Phase 2 — Reference port (1 commit + checkpoint)

3. Port `ApplySupplyScreen.kt`. Validates: `FaltetScopeToggle`, `FaltetChecklistGroup`, conditional rendering (plant list only when `scope == PLANTS`), `FaltetDropdown` for supplies, quantity-exceeds-available validation error, snackbar wiring.

Manual emulator smoke after Phase 2. If either primitive's API feels wrong, amend in new commits before Phase 3.

### Phase 3 — Batch ports (5 commits)

4. `AddSeedsScreen` — simplest; no special primitives.
5. `SowActivityScreen` — scope toggle (tray vs bed) + conditional bed dropdown + task prefill + post-submit upsell.
6. `BatchPotUpScreen` — two-stage flow + post-submit upsell.
7. `BatchPlantOutScreen` — two-stage flow.
8. `GenericActivityScreen` — shared-scaffold rewrite; 4–5 variants inherit.

### Phase 4 — Verify + milestone (1 empty commit)

9. `./gradlew assembleDebug` green. Manual smoke on all 6 screens (plus each GenericActivity variant). Empty milestone commit:

```
milestone: Android Fältet activities complete (Spec C3)
```

**Total: 9 tasks, 8 code commits + 1 milestone.**

---

## 8. Testing

### Per primitive (Phase 1)

- `@Preview` in the same file:
  - `FaltetScopeToggle`: selected-left, selected-right, required.
  - `FaltetChecklistGroup`: empty, partial, all-selected, with subtitle, with select-all.
- `./gradlew compileDebugKotlin` green after each commit.

### Per screen port (Phases 2–3)

- `@Preview` at end of each file exercising populated state (hard-coded values, no VM).
- For `BatchPotUp` / `BatchPlantOut`: two previews — list phase + detail phase.
- For `GenericActivityScreen`: one preview of any variant is sufficient — shared scaffold guarantees visual consistency across all 4–5.
- `./gradlew compileDebugKotlin` green after each commit.
- Manual emulator smoke after Phase 2 (ApplySupply) and after Phase 3 (full batch, sample one variant per GenericActivity).

### Not added this spec

- No snapshot testing.
- No new instrumented UI tests.
- No changes to existing view-model tests — VM signatures unchanged.

### Known non-issue

First cold install flashes system-serif before Fraunces arrives via Downloadable Fonts — do not gate smoke on this.

---

## 9. Follow-up (outside this spec)

- **Sub-spec C4** — `AddSpeciesScreen` (862 lines) with AI workflows + multi-chip grids.
- **Polish pass** — inline edit dialogs in `GardenDetail` + `BedDetail` still use `OutlinedTextField` (from C2 follow-up list).
- **AddPlantEvent AI restoration** — restore dropped AI plant identification from C2.
- **Sub-spec D** — analytics, account, auth + deferred MyWorld dashboard + SeasonSelector.
