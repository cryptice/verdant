# Android Fältet — Sub-Spec C4: AddSpecies (Design)

**Status:** design, awaiting implementation plan
**Scope:** 1 Android screen (AddSpeciesScreen, 862 lines)
**Predecessor:** Spec C3 (Activities) shipped at milestone `da3b1f1`.
**Successors:** Sub-spec D (analytics / account / auth + deferred MyWorld dashboard + SeasonSelector).

---

## 1. Goal

Port `AddSpeciesScreen.kt` (862 lines, 19 fields, dual AI photo workflows, dirty detection, inline create-tag dialog, create + edit modes) to the Fältet editorial aesthetic. Introduce one new primitive (`FaltetChipMultiSelector`). Port in three compile-green chunks to reduce the risk of silent feature loss.

**Non-goals:**

- No API or data-model changes.
- No new features — do NOT add the missing group dropdown (that's a gap in the original; handle separately).
- No AI-specific primitive — inline suggestion cards with existing primitives. Defer `FaltetSuggestionCard` until we revisit C2's AddPlantEvent AI restoration.

**Success criteria:**

- Screen renders Fältet: masthead + cream body + hairline-separated fields + side-by-side photo pickers + fixed bottom submit bar.
- AI identify + extract workflows preserved: suggestion cards, auto-populate, auto-crop, both photo slots.
- Dirty detection + BackHandler + discard-confirmation dialog work identically to current code.
- Inline create-tag dialog works; newly created tag appears in the chip list after VM refresh.
- Create + edit modes both work: edit prefills all fields (including photo URLs); submit label changes from "Skapa" to "Spara".
- All labels in Swedish.
- AI errors surface via snackbar (new; current code silently swallows).
- `@Preview` + compile green + manual emulator smoke on 6 scenarios.

---

## 2. Scope

### In scope — 1 screen

| Screen | File | Current LOC | Notes |
|---|---|---|---|
| AddSpecies | `ui/activity/AddSpeciesScreen.kt` | 862 | The only remaining large un-ported screen. Dual AI workflows, 19 fields (10 mandatory), 4 multi-chip grids + tag multi-chip, dirty detection, inline create-tag dialog, create+edit modes, cropping logic. |

### Out of scope

- **Missing group dropdown** — `createGroup()` VM method exists but has no UI entry. Adding the UI is a new-feature decision, not part of this port.
- **Polish-pass items** — inline edit dialogs in `GardenDetail` / `BedDetail` still use `OutlinedTextField` (carry-over from C2).
- **`AddPlantEvent` AI restoration** — separate follow-up (carry-over from C2).
- **`ApplySupply` "show all categories" toggle** — minor regression carry-over from C3.
- **Sub-spec D** — analytics, account, auth, MyWorld dashboard, SeasonSelector.

---

## 3. Design decisions (summary of brainstorm)

| # | Decision | Chosen |
|---|---|---|
| 1 | Primitives needed | Only `FaltetChipMultiSelector` (multi-select chip grid). AI suggestion cards composed inline. Inline create-tag dialog uses Material `AlertDialog` + `Field` primitive with Swedish copy. |
| 2 | Implementation strategy | Chunked 3-commit port: shell → form fields → AI photos. Each commit compile-green. Reduces risk of silent feature loss (seen 3× in prior specs). |
| 3 | AI workflow | Preserve identify + extract + auto-populate + auto-crop. Add snackbar error surfacing (VM catch blocks gain `error = "Kunde inte identifiera bilden"` etc.). |
| 4 | Dirty detection | Preserve verbatim (hasData + hasChanges + BackHandler + discard dialog). |
| 5 | Photo layout | Side-by-side (preserve current UX). |
| 6 | Language | Swedish labels throughout. |

---

## 4. New primitive

One new file under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`.

### 4.1 `FaltetChipMultiSelector.kt`

Multi-select version of `FaltetChipSelector`. Flow-row of filter chips; tap toggles membership in `Set<T>`.

**Signature:**

```kotlin
@Composable
fun <T : Any> FaltetChipMultiSelector(
    label: String,
    options: List<T>,
    selected: Set<T>,
    onSelectedChange: (Set<T>) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
)
```

**Visual:**

- Label row identical to existing primitives (mono 9sp letter-spacing 1.4sp uppercase `FaltetForest` alpha 0.7) + optional `*` in clay.
- `FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp))`.
- Each chip: `FilterChip` with zero radius, 1dp `FaltetInkLine40` border (unselected) → `FaltetClay` fill + `FaltetCream` label (selected). Same color scheme as `FaltetChipSelector`.
- Tap toggles: `if (option in selected) selected - option else selected + option`.

**Preview permutations:**

- Month grid — 12 chips with ~4 selected (bloom/sowing-month use case).
- Small enum — 3 growing positions with 1 selected.
- Required with empty selection — shows `*` treatment.

**Reuse rationale:**

- `FaltetChipSelector` (C2): single-select nullable. Wrong semantics.
- `FaltetChecklistGroup` (C3): multi-select but vertical rows + checkboxes. Wrong layout.
- `FaltetChipMultiSelector` fills the gap: multi-select + chip-grid layout. Five consumers on this screen alone (months × 2, positions, soils, tags).

**Other primitives reused unchanged:**

- `Field` (C2 extended) — all text + numeric + in-dialog input.
- `FaltetImagePicker` (C2) — both photo slots.
- `FaltetFormSubmitBar` (C2) — fixed bottom submit.
- `FaltetScreenScaffold` (C2) — masthead + snackbarHost.
- `Chip` (Spec A) — not used here; AI suggestions inline.
- Material `AlertDialog` — create-tag + discard-confirm. Chrome stays Material; copy Swedish.

### 4.2 Inline AI suggestion row (no new primitive)

```kotlin
@Composable
private fun SuggestionRow(
    suggestion: PlantSuggestion,
    onTap: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.commonName,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = FaltetInk,
            )
            Text(
                text = suggestion.species.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        }
        Text(
            text = "${(suggestion.confidence * 100).toInt()}%",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = FaltetClay,
        )
    }
}
```

---

## 5. Screen port pattern

### 5.1 Standard structure

Same scaffold + snackbar + LaunchedEffect(created/error) pattern from C2/C3:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpeciesScreen(
    onBack: () -> Unit,
    viewModel: AddSpeciesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEdit = viewModel.speciesId != null

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.created) {
        if (uiState.created) onBack()
    }

    // Dirty detection + discard dialog — §5.2

    FaltetScreenScaffold(
        mastheadLeft = "§ Art",
        mastheadCenter = if (isEdit) uiState.existingSpecies?.commonName ?: "Redigera art" else "Ny art",
        mastheadRight = {
            IconButton(onClick = { tryBack() }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Tillbaka",
                    tint = FaltetClay,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        bottomBar = {
            FaltetFormSubmitBar(
                label = if (isEdit) "Spara" else "Skapa",
                onClick = submitAction,
                enabled = !uiState.isLoading,
                submitting = uiState.isLoading,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading && isEdit && !prefilled ->
                FaltetLoadingState(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // items: AI photo section, then form fields
            }
        }
    }
}
```

### 5.2 Dirty detection + discard confirmation

Preserve existing `hasData` + `hasChanges` logic verbatim. Wrap back paths with `tryBack()`:

```kotlin
val hasData = remember(/* all form state */) { /* boolean from current code */ }
val hasChanges = remember(/* all state */) {
    if (!isEdit) hasData else /* diff check per current code */
}
var showDiscardDialog by remember { mutableStateOf(false) }

val tryBack: () -> Unit = {
    if (hasData && (!isEdit || hasChanges)) {
        showDiscardDialog = true
    } else {
        onBack()
    }
}

BackHandler(enabled = hasData) { tryBack() }

if (showDiscardDialog) {
    AlertDialog(
        onDismissRequest = { showDiscardDialog = false },
        title = { Text("Avbryt ändringar?") },
        text = { Text("Dina ändringar kommer att gå förlorade.") },
        confirmButton = {
            TextButton(onClick = {
                showDiscardDialog = false
                onBack()
            }) { Text("Avbryt ändringar", color = FaltetClay) }
        },
        dismissButton = {
            TextButton(onClick = { showDiscardDialog = false }) { Text("Fortsätt redigera") }
        },
    )
}
```

### 5.3 Create-new-tag inline dialog

Preserve the current `AlertDialog` pattern with `Field` primitive inside instead of `OutlinedTextField`. Swedish copy:

```kotlin
var showNewTagDialog by remember { mutableStateOf(false) }
var newTagName by remember { mutableStateOf("") }

if (showNewTagDialog) {
    AlertDialog(
        onDismissRequest = { showNewTagDialog = false; newTagName = "" },
        title = { Text("Ny tagg") },
        text = {
            Field(
                label = "Namn",
                value = newTagName,
                onValueChange = { newTagName = it },
                required = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newTagName.isNotBlank()) {
                        viewModel.createTag(newTagName.trim())
                        showNewTagDialog = false
                        newTagName = ""
                    }
                },
                enabled = newTagName.isNotBlank(),
            ) { Text("Skapa", color = FaltetClay) }
        },
        dismissButton = {
            TextButton(onClick = { showNewTagDialog = false; newTagName = "" }) { Text("Avbryt") }
        },
    )
}
```

VM's `createTag(name)` already calls `loadData()` to refresh. Current UX doesn't auto-select the new tag — preserve that.

### 5.4 AI photo + suggestion + extraction

- Preserve both `LaunchedEffect`s for auto-populate (`suggestions` + `extractedInfo`) verbatim.
- Preserve `cropToBox()` + `toCompressedBase64()` utility calls verbatim.
- Preserve the `identifying || extracting` inline spinner below the photo row.
- **New:** add snackbar error setters to VM catch blocks in `identifyPlant()` and `extractSpeciesInfo()`:

```kotlin
// In AddSpeciesViewModel
fun identifyPlant(imageBase64: String) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(identifying = true, error = null)
        try {
            val suggestions = repo.identifyPlant(imageBase64)
            _uiState.value = _uiState.value.copy(identifying = false, suggestions = suggestions)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(identifying = false, error = "Kunde inte identifiera bilden")
        }
    }
}

fun extractSpeciesInfo(imageBase64: String) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(extracting = true, error = null)
        try {
            val info = repo.extractSpeciesInfo(imageBase64)
            _uiState.value = _uiState.value.copy(extracting = false, extractedInfo = info)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(extracting = false, error = "Kunde inte extrahera information")
        }
    }
}
```

Adapt to the actual VM method bodies (method names + error state field). If the VM uses a different error-reset strategy, conform.

### 5.5 Imports & preservations

**Preserve verbatim:**

- Package, `AddSpeciesState` data class, `AddSpeciesViewModel` and all methods (`loadData`, `createSpecies`, `updateSpecies`, `createGroup`, `createTag`, `identifyPlant`, `extractSpeciesInfo`) — with the minimal error-setter additions above.
- `PhotoPicker` helpers at `ui/activity/PhotoPicker.kt` (or wherever they live): `cropToBox()`, `toCompressedBase64()`, `ensurePortrait()`. Not moved or renamed.
- Data types: `PlantSuggestion`, `ExtractedSpeciesInfo`, `CropBox`, `SpeciesGroupResponse`, `SpeciesTagResponse`, `CreateSpeciesRequest`, `UpdateSpeciesRequest`.

**Drop after port:** `TopAppBar`, `OutlinedTextField`, `FilterChip`, `Card`, `CardDefaults`, `Button`, `RoundedCornerShape`, `verdantTopAppBarColors`, `stringResource`, `R`, the old `PhotoPicker` composable call (replaced by `FaltetImagePicker`), `verticalScroll`, `rememberScrollState`.

---

## 6. Field structure

### 6.1 Photo + AI section (top of form)

```kotlin
item {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            FaltetImagePicker(
                label = "Framsida *",
                value = frontBitmap,
                onValueChange = { bitmap ->
                    frontBitmap = bitmap
                    if (bitmap != null) {
                        imageFrontBase64 = bitmap.toCompressedBase64()
                        viewModel.identifyPlant(imageFrontBase64!!)
                    } else {
                        imageFrontBase64 = null
                    }
                },
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            FaltetImagePicker(
                label = "Baksida (valfri)",
                value = backBitmap,
                onValueChange = { bitmap ->
                    backBitmap = bitmap
                    if (bitmap != null) {
                        imageBackBase64 = bitmap.toCompressedBase64()
                        viewModel.extractSpeciesInfo(imageBackBase64!!)
                    } else {
                        imageBackBase64 = null
                    }
                },
            )
        }
    }
}

if (uiState.identifying || uiState.extracting) {
    item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            CircularProgressIndicator(
                color = FaltetClay,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (uiState.identifying) "Identifierar…" else "Extraherar information…",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                color = FaltetForest,
            )
        }
    }
}

if (uiState.suggestions.isNotEmpty()) {
    item { FaltetSectionHeader(label = "Förslag") }
    items(uiState.suggestions, key = { it.species }) { suggestion ->
        SuggestionRow(suggestion = suggestion, onTap = { fillFromSuggestion(suggestion) })
    }
}
```

`fillFromSuggestion` sets `commonName` + `scientificName` from the tapped suggestion — same semantics as the auto-populate `LaunchedEffect`, but gated on user tap for manual override. Existing auto-populate `LaunchedEffect` stays in place (picks top suggestion automatically).

### 6.2 Form fields (below photo section)

| # | Item | Primitive + args |
|---|---|---|
| 1 | Common name | `Field(label = "Artnamn", value = commonName, onValueChange = ..., required = true, error = if (showErrors && commonName.isBlank()) "Artnamn krävs" else null)` |
| 2 | Scientific name | `Field(label = "Vetenskapligt namn", value = scientificName, onValueChange = ..., required = true, error = if (showErrors && scientificName.isBlank()) "Vetenskapligt namn krävs" else null)` |
| 3 | Variant EN | `Field(label = "Variant (engelska, valfri)", value = variantName, onValueChange = ...)` |
| 4 | Variant SV | `Field(label = "Variant (svenska, valfri)", value = variantNameSv, onValueChange = ...)` |
| 5 | Germination min | `Field(label = "Grobarhet dagar min", value = germinationMinText, onValueChange = { germinationMinText = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number, required = true, error = if (showErrors && germinationMinText.toIntOrNull() == null) "Heltal krävs" else null)` |
| 6 | Germination max | `Field(label = "Grobarhet dagar max (valfri)", value = germinationMaxText, onValueChange = ..., keyboardType = KeyboardType.Number)` |
| 7 | Harvest min | `Field(label = "Dagar till skörd min", value = harvestMinText, onValueChange = ..., keyboardType = KeyboardType.Number, required = true, error = ...)` |
| 8 | Harvest max | `Field(label = "Dagar till skörd max (valfri)", value = harvestMaxText, ..., keyboardType = KeyboardType.Number)` |
| 9 | Sowing depth | `Field(label = "Sådjup mm", value = sowingDepthText, ..., keyboardType = KeyboardType.Number, required = true, error = ...)` |
| 10 | Height min | `Field(label = "Höjd cm min", value = heightMinText, ..., keyboardType = KeyboardType.Number, required = true, error = ...)` |
| 11 | Height max | `Field(label = "Höjd cm max (valfri)", value = heightMaxText, ..., keyboardType = KeyboardType.Number)` |
| 12 | Sowing months | `FaltetChipMultiSelector(label = "Såmånader (valfri)", options = (1..12).toList(), selected = sowingMonths, onSelectedChange = { sowingMonths = it }, labelFor = { monthShortSv(it) })` |
| 13 | Bloom months | `FaltetChipMultiSelector(label = "Blomningsmånader (valfri)", options = (1..12).toList(), selected = bloomMonths, onSelectedChange = ..., labelFor = { monthShortSv(it) })` |
| 14 | Germination rate | `Field(label = "Grobarhetsprocent", value = germinationRateText, ..., keyboardType = KeyboardType.Number, required = true, error = ...)` |
| 15 | Growing positions | `FaltetChipMultiSelector(label = "Växtplats", options = listOf("SUNNY", "PARTIALLY_SUNNY", "SHADOWY"), selected = positions, onSelectedChange = ..., labelFor = { positionLabelSv(it) }, required = true)` |
| 16 | Soil types | `FaltetChipMultiSelector(label = "Jordtyp", options = listOf("CLAY", "SANDY", "LOAMY", "CHALKY", "PEATY", "SILTY"), selected = soils, onSelectedChange = ..., labelFor = { soilLabelSv(it) }, required = true)` |
| 17 | Tags | `FaltetChipMultiSelector(label = "Taggar (valfri)", options = uiState.tags, selected = selectedTags, onSelectedChange = ..., labelFor = { it.name })` |

**"Ny tagg" affordance** — right after the tag multi-selector, a right-aligned mono-clay text row opens the create-tag dialog:

```kotlin
item {
    Text(
        text = "+ NY TAGG",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
        color = FaltetClay,
        textAlign = TextAlign.End,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showNewTagDialog = true }
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}
```

### 6.3 Helpers (at bottom of file)

```kotlin
private fun monthShortSv(month: Int): String = arrayOf(
    "jan", "feb", "mar", "apr", "maj", "jun",
    "jul", "aug", "sep", "okt", "nov", "dec",
)[month - 1]

private fun positionLabelSv(code: String): String = when (code) {
    "SUNNY" -> "Sol"
    "PARTIALLY_SUNNY" -> "Halvskugga"
    "SHADOWY" -> "Skugga"
    else -> code
}

private fun soilLabelSv(code: String): String = when (code) {
    "CLAY" -> "Lera"
    "SANDY" -> "Sand"
    "LOAMY" -> "Mylla"
    "CHALKY" -> "Kalk"
    "PEATY" -> "Torv"
    "SILTY" -> "Silt"
    else -> code
}
```

### 6.4 Validation

On submit click, set `showValidationErrors = true`. Submit proceeds only when all mandatory fields valid:

- `commonName.isNotBlank()`
- `scientificName.isNotBlank()`
- `germinationMinText.toIntOrNull() != null`
- `harvestMinText.toIntOrNull() != null`
- `sowingDepthText.toIntOrNull() != null`
- `heightMinText.toIntOrNull() != null`
- `germinationRateText.toIntOrNull() != null`
- `positions.isNotEmpty()`
- `soils.isNotEmpty()`
- `imageFrontBase64 != null || (isEdit && uiState.existingSpecies?.imageFrontUrl != null)`

If validation fails: stay on form with inline errors shown. If passes: call `viewModel.createSpecies(...)` or `viewModel.updateSpecies(...)` per mode.

---

## 7. Phasing

### Phase 1 — Primitive (1 commit)

1. `FaltetChipMultiSelector.kt` + `@Preview`s.

Compile-green. No screen consumes it yet.

### Phase 2 — AddSpecies chunk 1: Shell (1 commit)

2. Rewrite `AddSpeciesScreen.kt` outer scaffold + imports + all state declarations + dirty detection + discard dialog + submit/back logic + create-tag dialog scaffolding + masthead-right back-arrow. The `LazyColumn` body is empty (or contains only a placeholder comment). Preserve VM, data classes, all helpers.

At end of chunk 1: compile green; app builds; screen opens with masthead + empty body + bottom bar; back-press triggers discard dialog when dirty.

### Phase 3 — AddSpecies chunk 2: Form fields (1 commit)

3. Fill in the `LazyColumn` body with all 17 non-photo fields (§6.2 table): text, numeric, 4 `FaltetChipMultiSelector` grids, tag multi-chip, "+ NY TAGG" affordance, Swedish labels, validation error wiring. Add `monthShortSv` / `positionLabelSv` / `soilLabelSv` helpers.

At end of chunk 2: the screen looks complete except for the photo/AI section at the top.

### Phase 4 — AddSpecies chunk 3: AI photo section (1 commit)

4. Prepend the photo row + AI spinner + suggestion cards + `SuggestionRow` private composable + both `LaunchedEffect`s for auto-populate + crop logic. Add VM catch-block error setters for `identifyPlant` / `extractSpeciesInfo`.

At end of chunk 3: full port complete.

### Phase 5 — Verify + milestone (1 empty commit)

5. `./gradlew assembleDebug` green. Manual smoke on 6 scenarios (§8). Empty milestone commit:

```
milestone: Android Fältet AddSpecies complete (Spec C4)
```

**Total: 5 tasks, 4 code commits + 1 milestone.**

---

## 8. Testing

### Per primitive (Phase 1)

- `@Preview`s for `FaltetChipMultiSelector`: 12-month grid with ~4 selected; 3-option set with 1 selected; required with empty selection.
- `./gradlew compileDebugKotlin` green.

### Per chunk (Phases 2–4)

- Chunk 1 (shell): compile green; manually verify screen opens with masthead + empty body + bottom bar; BackHandler triggers nothing without data.
- Chunk 2 (fields): compile green; manually verify all 17 fields render; multi-chip selections toggle; validation errors show after failed submit; "+ NY TAGG" dialog opens/creates.
- Chunk 3 (AI): compile green; manually verify full AI flow with real photos (front → suggestions → auto-populate; back → field extraction).

### Manual smoke at Phase 5 — 6 scenarios

1. **Create mode**: fill required fields, submit, returns to previous screen.
2. **Edit mode**: launch with an existing `speciesId`; all fields (including photo URLs) prefill; change a field, save.
3. **AI identify**: take/pick a front photo; spinner shows; suggestion cards render; tap a suggestion → common+scientific name fill.
4. **AI extract**: take/pick a back photo; fields auto-populate from extracted info.
5. **Dirty detection**: start typing; press back → discard dialog appears; "Fortsätt redigera" returns to form; "Avbryt ändringar" exits.
6. **Create tag**: tap "+ NY TAGG"; enter name; confirm; tag appears in the chip list.

### Not added this spec

- No snapshot testing.
- No new instrumented UI tests.
- Minimal VM changes (add error setters in AI catch blocks) — no new tests.

### Known non-issue

First cold install flashes system-serif before Fraunces arrives via Downloadable Fonts.

---

## 9. Follow-up (not this spec)

- **AddPlantEvent AI restoration** — reuse the `identifyPlant` endpoint pattern from this port.
- **Inline edit dialogs in GardenDetail + BedDetail** — carryover polish pass.
- **ApplySupply "show all categories" toggle** — minor regression restoration.
- **Missing group dropdown in AddSpecies** — feature gap in original; separate decision.
- **Sub-spec D** — analytics, account, auth, MyWorld dashboard, SeasonSelector.
