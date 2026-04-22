# Android Fältet — Sub-Spec C3 (Activities) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port 6 Android activity / workflow screens (`ApplySupply`, `AddSeeds`, `SowActivity`, `BatchPotUp`, `BatchPlantOut`, `GenericActivity`) to the Fältet editorial aesthetic using 2 new primitives plus existing Spec A/B/C1/C2 primitives.

**Architecture:** Vertical slice. Phase 1 ships `FaltetScopeToggle` + `FaltetChecklistGroup`. Phase 2 ports `ApplySupplyScreen` as reference. Phase 3 batches the remaining 5 ports (`AddSeeds`, `SowActivity`, `BatchPotUp`, `BatchPlantOut`, `GenericActivity`). Phase 4 runs `assembleDebug` and creates an empty milestone commit. Two-stage batch flows preserve their in-composable state-driven list→form transitions. `GenericActivityScreen`'s shared `ActivityScaffold` / `PhotoSection` / `SubmitButton` / `CountField` / `FrequentCommentsField` are ported once — all 4–5 variants inherit automatically.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (for `AlertDialog` + `DatePickerDialog`), Hilt, existing `ui/faltet/` primitives (incl. C2's `Field`, `FaltetChipSelector`, `FaltetDropdown`, `FaltetDatePicker`, `FaltetImagePicker`, `FaltetCheckbox`, `FaltetSubmitButton`, `FaltetFormSubmitBar`, `FaltetScreenScaffold` with snackbarHost slot).

**Spec:** `docs/plans/2026-04-22-android-faltet-activities-design.md` — read this before starting.

**Reality-check notes:**
- `AddSpeciesScreen` (862 lines) is out of scope — future sub-spec C4.
- Preserve every ViewModel, state shape, navigation callback, existing dialog, Base64 image-encoding pipeline, post-submit upsell dialogs, task-prefill logic, auto-select-single-group behavior.
- Swedish copy is authored per-screen in this plan. Use what's specified.
- Where the plan references model field names (e.g., `Scope.BED`, `uiState.plantsInBed`), confirm the actual names by reading the current file. If a referenced field doesn't exist, adapt without inventing.

---

## File Structure

### New files (under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`)

- `FaltetScopeToggle.kt` — two-option segmented-control-style picker
- `FaltetChecklistGroup.kt` — multi-select list with hairline rows + optional select-all

### Modified files (6 screens)

- `android/app/src/main/kotlin/app/verdant/android/ui/activity/ApplySupplyScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSeedsScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/activity/SowActivityScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/activity/BatchPotUpScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/activity/BatchPlantOutScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/activity/GenericActivityScreen.kt`

---

## Phase 1 — Primitives

---

### Task 1: `FaltetScopeToggle`

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScopeToggle.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScopeToggle.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInkLine40

@Composable
fun <T : Any> FaltetScopeToggle(
    label: String,
    options: List<T>,
    selected: T,
    onSelectedChange: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
) {
    require(options.size == 2) { "FaltetScopeToggle requires exactly 2 options" }
    Column(modifier) {
        Text(
            text = buildAnnotatedString {
                append(label.uppercase())
                if (required) {
                    withStyle(SpanStyle(color = FaltetClay)) { append(" *") }
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .border(1.dp, FaltetInkLine40),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (index == 1) Modifier.drawBehind {
                                drawLine(
                                    color = FaltetInkLine40,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            } else Modifier,
                        )
                        .background(if (isSelected) FaltetClay else FaltetCream)
                        .clickable(enabled = !isSelected) { onSelectedChange(option) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelFor(option).uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = if (isSelected) FaltetCream else FaltetForest,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetScopeTogglePreview_SelectedLeft() {
    FaltetScopeToggle(
        label = "Omfattning",
        options = listOf("Hela bädden", "Enskilda plantor"),
        selected = "Hela bädden",
        onSelectedChange = {},
        labelFor = { it },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetScopeTogglePreview_SelectedRight() {
    FaltetScopeToggle(
        label = "Omfattning",
        options = listOf("Hela bädden", "Enskilda plantor"),
        selected = "Enskilda plantor",
        onSelectedChange = {},
        labelFor = { it },
        required = true,
    )
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScopeToggle.kt
git commit -m "feat: FaltetScopeToggle primitive"
```

---

### Task 2: `FaltetChecklistGroup`

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChecklistGroup.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChecklistGroup.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

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
) {
    Column(modifier) {
        Text(
            text = buildAnnotatedString {
                append(label.uppercase())
                if (required) {
                    withStyle(SpanStyle(color = FaltetClay)) { append(" *") }
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(6.dp))
        if (selectAllEnabled && options.isNotEmpty()) {
            val allSelected = selected.size == options.size
            Text(
                text = if (allSelected) "AVMARKERA ALLA" else "VÄLJ ALLA",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = FaltetClay,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectedChange(if (allSelected) emptySet() else options.toSet())
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        options.forEach { option ->
            val isSelected = option in selected
            val subtitle = subtitleFor?.invoke(option)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable {
                        onSelectedChange(
                            if (isSelected) selected - option else selected + option
                        )
                    }
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
                FaltetCheckbox(checked = isSelected, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = labelFor(option),
                        fontFamily = FaltetDisplay,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        color = FaltetInk,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = FaltetForest,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChecklistGroupPreview_Partial() {
    FaltetChecklistGroup(
        label = "Plantor",
        options = listOf("Cosmos #1", "Zinnia #3", "Dahlia #5"),
        selected = setOf("Cosmos #1"),
        onSelectedChange = {},
        labelFor = { it },
        subtitleFor = { "Status: Utplanterad" },
        selectAllEnabled = true,
        required = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChecklistGroupPreview_AllSelected() {
    FaltetChecklistGroup(
        label = "Plantor",
        options = listOf("Cosmos #1", "Zinnia #3"),
        selected = setOf("Cosmos #1", "Zinnia #3"),
        onSelectedChange = {},
        labelFor = { it },
        selectAllEnabled = true,
    )
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChecklistGroup.kt
git commit -m "feat: FaltetChecklistGroup primitive"
```

---

## Phase 2 — Reference port

---

### Task 3: Port `ApplySupplyScreen`

Validates both new primitives (`FaltetScopeToggle` + `FaltetChecklistGroup`), conditional rendering (plant list only when scope == plants), supply dropdown with Decimal quantity validation.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/ApplySupplyScreen.kt`

**Context for the agent:**
- Current file (~355 lines) has: `ApplySupplyState`, `ApplySupplyViewModel`, `Scaffold + TopAppBar + RadioButton + Checkbox list + OutlinedTextField + submit Button`. The VM may expose `scope` as a Boolean (e.g., `isBedScope`) or an enum — read first, adapt accordingly.
- Preserve VM, state, screen signature. Base64 n/a (no photo on this screen).

**Masthead:** `mastheadLeft = "§ Gödsling"`, `mastheadCenter = "Applicera förnödenhet"`.

**Bottom bar:** `FaltetFormSubmitBar(label = "Spara", onClick = viewModel::submit, enabled = canSubmit, submitting = uiState.isSubmitting)`.

**Field order:**

1. Scope toggle. The spec uses a local `enum class Scope { BED, PLANTS }`. If the VM already has such an enum, use it. If the VM uses `Boolean isBedScope`, wrap with a local enum mapping:

```kotlin
private enum class Scope { BED, PLANTS }

// in composable:
var scope by remember { mutableStateOf(if (uiState.isBedScope) Scope.BED else Scope.PLANTS) }
// on change:
onSelectedChange = { scope = it; viewModel.setBedScope(it == Scope.BED) }
```

Render:
```kotlin
item {
    FaltetScopeToggle(
        label = "Omfattning",
        options = listOf(Scope.BED, Scope.PLANTS),
        selected = scope,
        onSelectedChange = { scope = it; /* VM update here */ },
        labelFor = { if (it == Scope.BED) "Hela bädden" else "Enskilda plantor" },
        required = true,
    )
}
```

2. Conditional plant checklist when `scope == Scope.PLANTS`:
```kotlin
if (scope == Scope.PLANTS) {
    item {
        FaltetChecklistGroup(
            label = "Plantor",
            options = uiState.plantsInBed,
            selected = selectedPlantIds,
            onSelectedChange = { selectedPlantIds = it },
            labelFor = { plant -> plant.displayName ?: plant.name ?: "—" },
            subtitleFor = { plant -> plant.status?.let { "Status: ${statusLabelSv(it)}" } },
            selectAllEnabled = true,
            required = true,
        )
    }
}
```

Replace `plant.displayName / plant.name` with whatever the actual plant model field is. `statusLabelSv` — if already defined in `app.verdant.android.ui.plants` or elsewhere, import; otherwise inline at bottom of file:

```kotlin
private fun statusLabelSv(status: String?): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Krukad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    null -> "—"
    else -> status
}
```

3. Supply dropdown:
```kotlin
item {
    FaltetDropdown(
        label = "Förnödenhet",
        options = uiState.supplies,
        selected = selectedSupply,
        onSelectedChange = { selectedSupply = it },
        labelFor = { "${it.supplyTypeName} · ${formatQuantity(it.quantity, it.unit)}" },
        searchable = true,
        required = true,
    )
}
```

`formatQuantity` helper — reuse from `ui.supplies.SupplyInventoryScreen.kt` (it's marked `internal fun formatQuantity(...)`).

4. Quantity field with validation:
```kotlin
item {
    Field(
        label = "Mängd",
        value = quantityText,
        onValueChange = { quantityText = it; quantityError = null },
        keyboardType = KeyboardType.Decimal,
        required = true,
        error = quantityError,
    )
}
```

Compute `quantityError` in `submitAction` (on submit attempt): parse, check `> 0`, check `<= selectedSupply.quantity`. Error strings: "Mängd krävs" when empty, "Ogiltig mängd" when not a positive decimal, "Mängd överskrider tillgängligt" when above available.

5. Notes field:
```kotlin
item {
    Field(label = "Anteckningar (valfri)", value = notes, onValueChange = { notes = it })
}
```

**Validation (in composable, not VM):**

```kotlin
val canSubmit = (scope == Scope.BED || selectedPlantIds.isNotEmpty())
    && selectedSupply != null
    && quantityText.toDoubleOrNull()?.let { it > 0 && it <= (selectedSupply?.quantity ?: 0.0) } == true
    && !uiState.isSubmitting
```

**Snackbar + nav:**

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
LaunchedEffect(uiState.submitted) { if (uiState.submitted) onDone() }
```

Adapt `submitted` / `onDone` field names to what the VM exposes (`success`, `completed`, etc.).

**Steps:**

1. Read the full current file at `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/activity/ApplySupplyScreen.kt`. Identify:
   - Screen composable signature (callbacks: `onDone`, `onBack`, etc.)
   - `ApplySupplyState` field names (scope representation, plant list field, supply list, quantity, notes)
   - VM method names for updating scope / submitting
   - Existing InlineErrorBanner usage
   - Any post-submit dialog (ApplySupply spec says none, but confirm)
2. Read `CreateBedScreen.kt` (Spec C2 reference) for scaffold body + snackbar pattern.
3. Replace imports. Drop `TopAppBar`, `OutlinedTextField`, `RadioButton`, `Checkbox`, `Button`, `verticalScroll`, `rememberScrollState`, `RoundedCornerShape`, `verdantTopAppBarColors`, `stringResource`, `R`, `ArrowBack`. Add Fältet primitive imports, `PaddingValues`, `Arrangement`, `fillMaxSize`, `LazyColumn`, `SnackbarHost`, `SnackbarHostState`, `KeyboardType`.
4. Preserve state + VM + existing helpers (but add `statusLabelSv` at bottom if missing).
5. Replace the `@Composable fun ApplySupplyScreen(...)` body using the scaffold structure + field order above.
6. Add a `@Preview` showing populated state with scope=PLANTS + 2 plants selected.
7. `cd android && ./gradlew compileDebugKotlin --no-daemon -q` — must exit 0.
8. If green:
   ```
   git add android/app/src/main/kotlin/app/verdant/android/ui/activity/ApplySupplyScreen.kt
   git commit -m "feat: Fältet port — ApplySupply"
   ```
9. If red, BLOCKED with error output.

**Phase 2 checkpoint:**

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Open ApplySupplyScreen on emulator. Verify:
- Masthead `§ Gödsling / Applicera förnödenhet`.
- Scope toggle renders as two halves with clay-active, cream-inactive; tap switches.
- "Enskilda plantor" reveals the plant checklist below with working checkboxes.
- "Välj alla" / "Avmarkera alla" affordance works.
- Supply dropdown opens bottom sheet.
- Mängd validation errors show in clay below the field when exceeded.
- Submit disabled until all required fields valid; snackbar on submit error; closes on success.

If a primitive API feels wrong, amend in new commits before Phase 3.

---

## Phase 3 — Batch ports

---

### Task 4: Port `AddSeedsScreen`

Simplest — no special primitives.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSeedsScreen.kt`

**Context:**
- Current file (~259 lines). `Scaffold + OutlinedTextField + ExposedDropdownMenuBox + DatePickerDialog + Button`.
- Preserve VM + state + signature + Swedish labels.

**Masthead:** `§ Inventarie` / `"Lägg till frön"`.

**Bottom bar:** `FaltetFormSubmitBar(label = "Spara", ...)`.

**Field order:**

1. `FaltetDropdown(label = "Art", options = uiState.species, selected = selectedSpecies, onSelectedChange = ..., labelFor = { speciesDisplayName(it) }, searchable = true, required = true)`.
2. `Field(label = "Antal frön", value = quantityText, onValueChange = { quantityText = it.filter { c -> c.isDigit() }; quantityError = false }, keyboardType = KeyboardType.Number, required = true, error = if (quantityError) "Antal krävs" else null)`.
3. `FaltetDatePicker(label = "Skördedatum (valfri)", value = collectionDate, onValueChange = { collectionDate = it })`.
4. `FaltetDatePicker(label = "Utgångsdatum (valfri)", value = expirationDate, onValueChange = { expirationDate = it })`.
5. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = ...)`.

**Validation:** `canSubmit = selectedSpecies != null && quantityText.toIntOrNull()?.let { it > 0 } == true && !uiState.isSubmitting`.

**Date-string handling:** if VM stores dates as ISO strings, convert at field boundary:
- Prefill: `runCatching { LocalDate.parse(uiState.collectionDateString) }.getOrNull()`.
- Submit: `collectionDate?.toString()` (ISO format).

**Snackbar + nav:** same pattern as ApplySupply.

**Steps:**

1. Read current file. Note state fields, VM method signatures, species type + display helper name.
2. Rewrite imports (drop `ExposedDropdownMenuBox`, `DatePickerDialog`, `OutlinedTextField`, `Button`, `TopAppBar`, `stringResource`, `R`).
3. Preserve state + VM.
4. Replace body.
5. Add `@Preview` for populated form.
6. Compile + commit: `git add <file> && git commit -m "feat: Fältet port — AddSeeds"`

If VM field names differ, adapt.

---

### Task 5: Port `SowActivityScreen`

Searchable species dropdown + tray/bed toggle + task prefill + post-submit upsell dialog.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/SowActivityScreen.kt`

**Context:**
- Current file (~416 lines). Has existing `LaunchedEffect` for task prefill; preserve verbatim.
- Post-submit upsell `AlertDialog` offering supply usage recording; preserve with Swedish copy.
- Base64 photo pipeline; preserve.

**Masthead:** `§ Sådd` / `"Såaktivitet"`.

**Bottom bar:** `FaltetFormSubmitBar(label = "Så", ...)`.

**Field order:**

1. `FaltetDropdown(label = "Art", options = uiState.species, selected = selectedSpecies, onSelectedChange = { selectedSpecies = it }, labelFor = { speciesDisplayName(it) }, searchable = true, required = true)`.
2. `FaltetScopeToggle(label = "Destination", options = listOf(SowDestination.TRAY, SowDestination.BED), selected = destination, onSelectedChange = { destination = it }, labelFor = { if (it == SowDestination.TRAY) "Så i brätte" else "Så direkt i bädd" })`.

If the VM uses a `Boolean isTray` instead, wrap with local enum:
```kotlin
private enum class SowDestination { TRAY, BED }
```

3. Conditional bed dropdown when `destination == SowDestination.BED`:
```kotlin
if (destination == SowDestination.BED) {
    item {
        FaltetDropdown(
            label = "Bädd",
            options = uiState.beds,
            selected = selectedBed,
            onSelectedChange = { selectedBed = it },
            labelFor = { "${it.gardenName} · ${it.name}" },
            searchable = true,
            required = true,
        )
    }
}
```

4. Conditional seed-batch dropdown when `selectedSpecies != null`:
```kotlin
if (selectedSpecies != null) {
    val batches = uiState.seedBatches.filter { it.speciesId == selectedSpecies!!.id }
    if (batches.isNotEmpty()) {
        item {
            FaltetDropdown(
                label = "Frökälla (valfri)",
                options = batches,
                selected = selectedSeedBatch,
                onSelectedChange = { selectedSeedBatch = it },
                labelFor = { "${it.variantName ?: "—"} · ${it.quantity} frön" },
                searchable = false,
            )
        }
    }
}
```

5. `Field(label = "Antal frön", value = countText, onValueChange = { countText = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number, required = true, error = countError)`.

6. `FaltetImagePicker(label = "Foto (valfri)", value = photoBitmap, onValueChange = { photoBitmap = it; /* preserve existing Base64 side effects */ })`.

7. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = ...)`.

**Post-submit upsell dialog:** preserve the existing dialog. Example shape:
```kotlin
if (uiState.showSupplyUsagePrompt) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissSupplyPrompt() },
        title = { Text("Registrera förbrukning?") },
        text = { Text("Vill du registrera förbrukning av jord eller krukor?") },
        confirmButton = {
            TextButton(onClick = { viewModel.recordSupplyUsage() }) {
                Text("Ja", color = FaltetClay)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissSupplyPrompt() }) {
                Text("Nej")
            }
        },
    )
}
```

Adapt field names to actual VM state.

**Task prefill:** preserve `LaunchedEffect(uiState.task)` that populates `selectedSpecies` + `countText` from a scheduled task, verbatim.

**Validation:** `canSubmit = selectedSpecies != null && (destination == SowDestination.TRAY || selectedBed != null) && countText.toIntOrNull()?.let { it > 0 } == true && !uiState.isSubmitting`.

**Steps:**

1. Read current file. Note: task prefill `LaunchedEffect`, supply-usage upsell dialog shape, VM field names (destination representation, species/bed/seed-batch types).
2. Rewrite imports.
3. Preserve state + VM + task prefill + upsell dialog.
4. Replace body.
5. Add `@Preview` for populated state (destination=TRAY, species selected).
6. Compile + commit: `git add <file> && git commit -m "feat: Fältet port — SowActivity"`

---

### Task 6: Port `BatchPotUpScreen` — two-stage

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/BatchPotUpScreen.kt`

**Context:**
- Current file (~303 lines). Two-stage flow: list picker → detail form. Auto-select single group. Post-submit upsell dialog. Preserve all behaviors.

**Masthead:**
- List phase: `§ Kruka upp` / `"Välj grupp"`
- Detail phase: `§ Kruka upp` / `<selectedGroup.speciesName>`, with back-arrow IconButton in masthead-right.

**Bottom bar:**
- List phase: `{ }` (no bar)
- Detail phase: `FaltetFormSubmitBar(label = "Kruka upp", ...)`

**Two-stage state machine** (inside composable):

```kotlin
var selectedGroup by remember { mutableStateOf<PlantLocationGroup?>(null) }

LaunchedEffect(uiState.groups) {
    if (uiState.groups.size == 1 && selectedGroup == null) {
        selectedGroup = uiState.groups.first()
    }
}
```

Adapt `PlantLocationGroup` to the actual group type.

**List phase body:**

```kotlin
if (uiState.groups.isEmpty()) {
    FaltetEmptyState(
        headline = "Inga grupper att kruka upp",
        subtitle = "Så först några frön i brätten.",
        modifier = Modifier.padding(padding),
    )
} else {
    LazyColumn(Modifier.fillMaxSize().padding(padding)) {
        items(uiState.groups, key = { "${it.speciesId}_${it.sowDate}" }) { group ->
            FaltetListRow(
                title = group.speciesName,
                meta = "${formattedDate(group.sowDate)} · ${group.count} frön i brätte",
                onClick = { selectedGroup = group },
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
```

Adapt field names.

**Detail phase body:**

1. `Field(label = "Antal att kruka upp", value = countText, onValueChange = { countText = it.filter { c -> c.isDigit() }; countError = null }, keyboardType = KeyboardType.Number, required = true, error = countError)`.
   - Validate on submit: `countText.toIntOrNull()?.let { it in 1..selectedGroup!!.count } != true` → `countError = "Antal måste vara mellan 1 och ${selectedGroup!!.count}"`.
2. `FaltetImagePicker(label = "Foto (valfri)", value = photoBitmap, onValueChange = ...)`.
3. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = ...)`.

**Masthead right (detail phase only):**

```kotlin
mastheadRight = if (selectedGroup != null) {
    {
        IconButton(onClick = { selectedGroup = null }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "Tillbaka",
                tint = FaltetClay,
                modifier = Modifier.size(18.dp),
            )
        }
    }
} else null
```

**Post-submit upsell dialog:** preserve existing AlertDialog. Swedish copy.

**Helpers at bottom of file:**

```kotlin
private fun formattedDate(date: String?): String {
    if (date == null) return "—"
    return try {
        val parsed = java.time.LocalDate.parse(date.take(10))
        "${parsed.dayOfMonth} ${monthShortSv(parsed.monthValue)}"
    } catch (e: Exception) {
        date
    }
}

private fun monthShortSv(month: Int): String = arrayOf(
    "jan", "feb", "mar", "apr", "maj", "jun",
    "jul", "aug", "sep", "okt", "nov", "dec",
)[month - 1]
```

**Steps:**

1. Read current file.
2. Rewrite imports.
3. Preserve state + VM + auto-select effect + upsell dialog.
4. Replace body with two-stage shape.
5. Add helpers.
6. Add `@Preview` for list phase (3 mock groups) and one for detail phase.
7. Compile + commit: `git add <file> && git commit -m "feat: Fältet port — BatchPotUp"`

---

### Task 7: Port `BatchPlantOutScreen` — two-stage

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/BatchPlantOutScreen.kt`

**Context:**
- Current file (~231 lines). Same two-stage pattern as BatchPotUp. No post-submit upsell (unlike BatchPotUp). Preserve auto-select-single-group.

**Masthead:**
- List phase: `§ Plantera ut` / `"Välj grupp"`
- Detail phase: `§ Plantera ut` / `<selectedGroup.speciesName>`, back-arrow masthead-right.

**Bottom bar:**
- List phase: `{ }`
- Detail phase: `FaltetFormSubmitBar(label = "Plantera ut", ...)`

**Two-stage state machine:** same pattern as BatchPotUp.

**List phase body:**

```kotlin
if (uiState.groups.isEmpty()) {
    FaltetEmptyState(
        headline = "Inga plantor att plantera ut",
        subtitle = "Så eller kruka upp först.",
        modifier = Modifier.padding(padding),
    )
} else {
    LazyColumn(Modifier.fillMaxSize().padding(padding)) {
        items(uiState.groups, key = { "${it.speciesId}_${it.status}" }) { group ->
            FaltetListRow(
                title = group.speciesName,
                meta = "${statusLabelSv(group.status)} · ${group.count} plantor",
                onClick = { selectedGroup = group },
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
```

**Detail phase body:**

1. `FaltetDropdown(label = "Målbädd", options = uiState.beds, selected = selectedTargetBed, onSelectedChange = { selectedTargetBed = it }, labelFor = { "${it.gardenName} · ${it.name}" }, searchable = true, required = true)`.
2. `Field(label = "Antal att plantera ut", value = countText, onValueChange = { countText = it.filter { c -> c.isDigit() }; countError = null }, keyboardType = KeyboardType.Number, required = true, error = countError)` — validate `1..selectedGroup.count`.

**Detail phase masthead-right back-arrow:** same pattern as BatchPotUp.

**Helpers:** `formattedDate` / `monthShortSv` / `statusLabelSv` at bottom (copy from BatchPotUp or BedDetail).

**Steps:**

1. Read current file.
2. Rewrite imports.
3. Preserve state + VM + auto-select effect.
4. Replace body.
5. Add helpers.
6. Add `@Preview` for list + detail phases.
7. Compile + commit: `git add <file> && git commit -m "feat: Fältet port — BatchPlantOut"`

---

### Task 8: Port `GenericActivityScreen` — shared-scaffold rewrite

The shared `ActivityScaffold` / `PhotoSection` / `SubmitButton` / `CountField` / `FrequentCommentsField` composables get Fältet-ported once. All 4–5 variants (`PotUpActivityScreen`, `PlantActivityScreen`, `HarvestActivityScreen`, `RecoverActivityScreen`, `DiscardActivityScreen`) inherit automatically, with light per-variant edits for masthead + submit label.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/GenericActivityScreen.kt`

**Context for the agent:**
- Current file (~378 lines) contains:
  - `ActivityScaffold` — a private composable taking a content lambda, wrapping Material `Scaffold` + `TopAppBar`.
  - `PhotoSection` — camera/gallery picker + preview.
  - `CountField` — text input with ±/+ icon buttons.
  - `FrequentCommentsField` — text area + horizontal chip suggestions.
  - `SubmitButton` — `Button` at end of scroll.
  - 4–5 `@Composable fun *ActivityScreen(...)` variants that call `ActivityScaffold(...)` and compose the shared helpers.
- Preserve all VM wiring, callback signatures, Base64 image-encoding pipeline.

**Rewrite instructions:**

**`ActivityScaffold` rewrite:**

Change signature from `(topBarTitle: String, content: ...)` style to include a submit callback + label:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityScaffold(
    mastheadLeft: String,
    mastheadCenter: String,
    submitLabel: String,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    submitting: Boolean,
    snackbarHostState: SnackbarHostState,
    content: LazyListScope.() -> Unit,
) {
    FaltetScreenScaffold(
        mastheadLeft = mastheadLeft,
        mastheadCenter = mastheadCenter,
        bottomBar = {
            FaltetFormSubmitBar(
                label = submitLabel,
                onClick = onSubmit,
                enabled = submitEnabled,
                submitting = submitting,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}
```

Note: the content lambda receiver changes from a plain `@Composable` to `LazyListScope` so callers emit `item { ... }` blocks. Variant callers (`PotUpActivityScreen` etc.) must be updated accordingly — wrap each of their current composables in `item { ... }`.

**`PhotoSection` rewrite:**

```kotlin
@Composable
private fun PhotoSection(
    bitmap: Bitmap?,
    onBitmapChange: (Bitmap?) -> Unit,
) {
    FaltetImagePicker(
        label = "Foto (valfri)",
        value = bitmap,
        onValueChange = { newBitmap ->
            onBitmapChange(newBitmap)
            // Preserve existing Base64 side effects the original PhotoSection had — e.g., viewModel.setPhotoBase64(newBitmap?.toCompressedBase64())
            // If the original called a VM method here, keep that call in the variant's lambda instead.
        },
    )
}
```

If the Base64 side effect lives inside the original `PhotoSection`, relocate it to the variant's `onBitmapChange` lambda.

**`CountField` rewrite:**

```kotlin
@Composable
private fun CountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    required: Boolean = true,
    error: String? = null,
) {
    Field(
        label = label,
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
        keyboardType = KeyboardType.Number,
        required = required,
        error = error,
    )
}
```

(Drop the inline ±/+ icon buttons — out of scope.)

**`FrequentCommentsField` rewrite:**

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequentCommentsField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Field(
            label = "Anteckningar (valfri)",
            value = value,
            onValueChange = onValueChange,
        )
        if (suggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                suggestions.forEach { suggestion ->
                    Chip(
                        label = suggestion,
                        onClick = { onValueChange(suggestion) },
                    )
                }
            }
        }
    }
}
```

Chip primitive signature varies; check `app.verdant.android.ui.faltet.Chip` and adapt. If `Chip` doesn't accept `onClick` directly, wrap with `Modifier.clickable`.

**`SubmitButton` deprecated:** remove entirely. Variants that previously invoked it at the end of their scroll need to pass their submit callback up to `ActivityScaffold(onSubmit = ...)` instead.

**Per-variant masthead + submit copy:**

| Variant | `mastheadLeft` | Submit label |
|---|---|---|
| `PotUpActivityScreen` | § Kruka upp | Kruka upp |
| `PlantActivityScreen` | § Plantera ut | Plantera ut |
| `HarvestActivityScreen` | § Skörda | Skörda |
| `RecoverActivityScreen` | § Återhämta | Återhämta |
| `DiscardActivityScreen` | § Kassera | Kassera |

Each variant's `mastheadCenter` = whatever display-name helper the current code uses for the plant.

**Variant rewrites (minimal):**

Each variant's current body looks roughly like:

```kotlin
ActivityScaffold(topBarTitle = ...) {
    Column {
        PhotoSection(...)
        CountField(...)
        FrequentCommentsField(...)
        SubmitButton(onClick = { viewModel.submit() })
    }
}
```

Change to:

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(uiState.error) { uiState.error?.let { snackbarHostState.showSnackbar(it) } }
LaunchedEffect(uiState.submitted) { if (uiState.submitted) onDone() }

ActivityScaffold(
    mastheadLeft = "§ Kruka upp",  // per variant
    mastheadCenter = plant.commonName,  // or existing display-name
    submitLabel = "Kruka upp",  // per variant
    onSubmit = { viewModel.submit() },
    submitEnabled = canSubmit,
    submitting = uiState.isSubmitting,
    snackbarHostState = snackbarHostState,
) {
    item { PhotoSection(bitmap = photoBitmap, onBitmapChange = { photoBitmap = it; /* Base64 side effects */ }) }
    item { CountField(label = "Antal", value = countText, onValueChange = { countText = it }, required = true, error = countError) }
    item { FrequentCommentsField(value = notes, onValueChange = { notes = it }, suggestions = uiState.frequentComments) }
}
```

`canSubmit` per variant — typically `countText.toIntOrNull()?.let { it > 0 } == true && !uiState.isSubmitting`.

**HarvestActivityScreen additional fields:** the current harvest variant has weight + quantity fields beyond count. Preserve them:

```kotlin
item { Field(label = "Vikt g (valfri)", value = weightText, onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } }, keyboardType = KeyboardType.Decimal) }
item { Field(label = "Antal stjälkar (valfri)", value = quantityText, onValueChange = { quantityText = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number) }
```

Adapt field names to actual harvest VM state.

**Steps:**

1. Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/activity/GenericActivityScreen.kt` in full. Map out:
   - Signature of `ActivityScaffold`, `PhotoSection`, `CountField`, `FrequentCommentsField`, `SubmitButton`
   - Each variant's current body shape
   - HarvestActivityScreen's additional fields (weight, quantity, etc.)
   - Any Base64 side-effect location (likely inside `PhotoSection`)
   - Snackbar handling (probably via InlineErrorBanner currently)
2. Rewrite imports (drop `TopAppBar`, `OutlinedTextField`, `Button`, `Scaffold`, `verticalScroll`, `rememberScrollState`, etc. — add Fältet primitives, `LazyListScope`, `SnackbarHost`, `SnackbarHostState`, `FlowRow`).
3. Rewrite the 5 shared composables per the templates above.
4. Update each variant (4–5) to call the new `ActivityScaffold` signature, pass submit callback up, emit `item { ... }` blocks instead of a `Column`. Each variant gets the Swedish masthead/submit from the table above.
5. Compile.
6. Add one `@Preview` showing `HarvestActivityScreen` populated.
7. `cd android && ./gradlew compileDebugKotlin --no-daemon -q` — exit 0.
8. If green:
   ```
   git add android/app/src/main/kotlin/app/verdant/android/ui/activity/GenericActivityScreen.kt
   git commit -m "feat: Fältet port — GenericActivity + variants"
   ```
9. If red, BLOCKED.

**Adaptations OK** — document them. If the existing variants don't match the template closely (e.g., `HarvestActivityScreen` has conditional logic), preserve that logic and style per Fältet.

---

## Phase 4 — Verify + milestone

---

### Task 9: Verify + milestone

- [ ] **Step 1: Full Android build**

```bash
cd android && ./gradlew assembleDebug --no-daemon -q
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Install + manual smoke**

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Screens to open:

- [ ] ApplySupply — scope toggle + plant checklist + supply dropdown + quantity validation
- [ ] AddSeeds — species dropdown + dates + submit
- [ ] SowActivity — species + destination toggle + conditional bed dropdown + task prefill (launch from a scheduled sow task) + post-submit supply prompt
- [ ] BatchPotUp — list phase shows groups; tap opens detail phase with back-arrow; submit + supply prompt
- [ ] BatchPlantOut — list phase shows groups; tap opens detail phase with target bed dropdown
- [ ] GenericActivity — open any variant (PotUp, Plant, Harvest, Recover, Discard) from plant detail's event flows; verify scaffold + count field + suggestion chips

Verify:
- Masthead / bottom bar consistent across screens.
- Fraunces italic text-field values; mono uppercase labels; clay `*` on required.
- Scope toggle halves visually distinct; tapping active half is a no-op.
- Checklist select-all toggles every row; subtitle renders.
- Two-stage flows: list → detail on tap, back-arrow returns to list.
- Post-submit dialogs in Swedish.

- [ ] **Step 3: Milestone commit**

```bash
git commit --allow-empty -m "milestone: Android Fältet activities complete (Spec C3)"
```

---

## Verification summary

After Task 9:

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/` contains 2 new primitives (`FaltetScopeToggle`, `FaltetChecklistGroup`).
- 6 activity screens render Fältet-editorial.
- GenericActivity's 4–5 variants inherit Fältet styling via shared scaffold rewrite.
- `./gradlew assembleDebug` green.
- ViewModels, nav callbacks, task prefill, upsell dialogs, Base64 image pipelines unchanged.

**Follow-ups:**
- **Sub-spec C4** — `AddSpeciesScreen` with AI workflows.
- **Polish pass** — inline edit dialogs in GardenDetail + BedDetail.
- **AddPlantEvent AI restoration** — restore dropped AI identification from C2.
- **Sub-spec D** — analytics / account / auth + deferred MyWorld dashboard + SeasonSelector.

---

## Self-review notes

- **Spec §1 (goal):** Tasks 1–9 collectively port the 6 screens.
- **Spec §2 (scope):** 6 screens → Tasks 3, 4, 5, 6, 7, 8.
- **Spec §3 (decisions):** Split from C4 (AddSpecies deferred). 2 new primitives (Tasks 1, 2). Shared-scaffold port for GenericActivity (Task 8). Vertical slice strategy.
- **Spec §4 (primitives):** `FaltetScopeToggle` + `FaltetChecklistGroup` defined with full inline source (Tasks 1–2).
- **Spec §5 (port pattern):** Standard structure, two-stage pattern, shared-scaffold pattern — all laid out in Tasks 3, 6/7, and 8.
- **Spec §6 (per-screen):** Each screen's field order, Swedish copy, validation, state machine detailed in Tasks 3–8.
- **Spec §7 (phasing):** 2 + 1 + 5 + 1 = 9 tasks. Matches.
- **Spec §8 (testing):** `@Preview` per primitive + per screen; compile gate; manual smoke Phase 2 + Phase 4.
- **Spec §9 (follow-ups):** C4, polish pass, AI restoration, D all called out.
