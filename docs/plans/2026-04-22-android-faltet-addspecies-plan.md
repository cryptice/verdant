# Android Fältet — Sub-Spec C4 (AddSpecies) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port `AddSpeciesScreen.kt` (862 LOC, 19 fields, dual AI photo workflows, dirty detection, inline create-tag dialog, create+edit modes) to Fältet editorial aesthetic using one new primitive (`FaltetChipMultiSelector`). Port in three compile-green chunks to reduce silent feature loss.

**Architecture:** Phase 1 ships the new primitive. Phases 2–4 port AddSpecies in three chunks (shell → fields → AI photos), each compile-green so the screen is usable after each commit. Phase 5 verifies + milestone. Dirty detection, discard dialog, inline create-tag dialog, AI identify + extract + auto-crop + auto-populate flows all preserved. VM gets minimal additions: error setters in AI catch blocks for snackbar surfacing.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (`AlertDialog` for create-tag + discard), Hilt, existing `ui/faltet/` primitives (incl. C2's `Field` + `FaltetImagePicker` + `FaltetFormSubmitBar` + `FaltetScreenScaffold` with snackbarHost), existing `ui/activity/PhotoPicker.kt` helpers (`cropToBox()`, `toCompressedBase64()`, `ensurePortrait()`).

**Spec:** `docs/plans/2026-04-22-android-faltet-addspecies-design.md` — read before starting.

**Reality-check notes:**
- The current `AddSpeciesViewModel.identifyPlant()` and `.extractSpeciesInfo()` silently swallow errors (no `error = ...` in catch blocks). Task 5 (Phase 4) adds the error setters.
- Current state holds `selectedTagIds: Set<Long>` — the port keeps the same shape; the primitive works directly on `Set<SpeciesTagResponse>` via a derived set, converted back to IDs at submit time.
- Current file references `R.string.*` for month/position/soil labels. The port drops `R.string.*` and uses inline Swedish helpers (`monthShortSv`, `positionLabelSv`, `soilLabelSv`).
- `PhotoPicker` composable (old one) is replaced with `FaltetImagePicker`; the `PhotoPicker.kt` file still owns `cropToBox()` / `toCompressedBase64()` / `ensurePortrait()` extensions — keep the file but don't call the old composable.

---

## File Structure

### New file

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipMultiSelector.kt`

### Modified file

- `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt` — touched 3× in Phases 2–4.

### Preserved (unchanged)

- `android/app/src/main/kotlin/app/verdant/android/ui/activity/PhotoPicker.kt` — the file stays, its helper extensions (`cropToBox()`, `toCompressedBase64()`, `ensurePortrait()`) remain used. Only the `PhotoPicker` composable entry-point call is removed from AddSpeciesScreen.

---

## Phase 1 — Primitive

---

### Task 1: `FaltetChipMultiSelector`

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipMultiSelector.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipMultiSelector.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> FaltetChipMultiSelector(
    label: String,
    options: List<T>,
    selected: Set<T>,
    onSelectedChange: (Set<T>) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val isSelected = option in selected
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelectedChange(
                            if (isSelected) selected - option else selected + option
                        )
                    },
                    label = { Text(labelFor(option), fontSize = 12.sp) },
                    shape = RectangleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = FaltetCream,
                        labelColor = FaltetForest,
                        selectedContainerColor = FaltetClay,
                        selectedLabelColor = FaltetCream,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = FaltetInkLine40,
                        selectedBorderColor = FaltetClay,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 0.dp,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChipMultiSelectorPreview_MonthGrid() {
    FaltetChipMultiSelector(
        label = "Blomningsmånader",
        options = (1..12).toList(),
        selected = setOf(5, 6, 7, 8),
        onSelectedChange = {},
        labelFor = {
            arrayOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec")[it - 1]
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChipMultiSelectorPreview_SmallEnum() {
    FaltetChipMultiSelector(
        label = "Växtplats",
        options = listOf("Sol", "Halvskugga", "Skugga"),
        selected = setOf("Sol"),
        onSelectedChange = {},
        labelFor = { it },
        required = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChipMultiSelectorPreview_RequiredEmpty() {
    FaltetChipMultiSelector(
        label = "Jordtyp",
        options = listOf("Lera", "Sand", "Mylla"),
        selected = emptySet(),
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
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipMultiSelector.kt
git commit -m "feat: FaltetChipMultiSelector primitive"
```

---

## Phase 2 — AddSpecies port, chunk 1: Shell

---

### Task 2: Rewrite scaffold + state + dialogs (empty body)

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt`

**Goal:** Replace the `Scaffold + TopAppBar + Column + OutlinedTextField...` shell with `FaltetScreenScaffold + snackbar + LazyColumn (empty body) + FaltetFormSubmitBar`. Preserve `AddSpeciesState`, `AddSpeciesViewModel`, all state declarations, the pre-fill `LaunchedEffect(uiState.existingSpecies)`, dirty detection (`hasData` + `hasChanges` logic), `BackHandler`, discard dialog, create-tag dialog scaffolding (state only; dialog rendered in Task 2 already), validation state (`showValidationErrors`), `LaunchedEffect(uiState.created) { onBack() }`.

The `LazyColumn` body contains only a placeholder `item { Spacer(Modifier.height(0.dp)) }` at this stage. Compile green, screen loads with masthead + empty body + bottom bar.

**Context for the agent:**
- Current VM signature + state shape (`AddSpeciesViewModel`, `AddSpeciesState`) — verbatim preserve; see lines 39–145 of the current file.
- Current composable state + pre-fill logic (lines 147–268ish) — verbatim preserve with only minor adaptations:
  - Change the internal tag selection model from `selectedTagIds: Set<Long>` to `selectedTags: Set<SpeciesTagResponse>` (simpler primitive API). Pre-fill maps from `s.tags.toSet()` (not `s.tags.map { it.id }.toSet()`). Submit maps back to `selectedTags.map { it.id }`.
- Current dirty-detection logic — verbatim preserve.

- [ ] **Step 1: Read the current file**

Open `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt` in full. Note:
- The exact `hasData` / `hasChanges` boolean expressions (if they're inlined or separate `remember` blocks).
- All 20+ `var ... by remember { mutableStateOf(...) }` declarations.
- Pre-fill mapping from `existingSpecies` to form state.
- Submit composition (the `CreateSpeciesRequest(...)` / `UpdateSpeciesRequest(...)` builder).
- Any helpers defined at the bottom of the file (unlikely but confirm).

- [ ] **Step 2: Replace imports block**

Replace everything from `package app.verdant.android.ui.activity` through the last import with:

```kotlin
package app.verdant.android.ui.activity

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.CreateSpeciesGroupRequest
import app.verdant.android.data.model.CreateSpeciesRequest
import app.verdant.android.data.model.CreateSpeciesTagRequest
import app.verdant.android.data.model.ExtractSpeciesInfoRequest
import app.verdant.android.data.model.ExtractedSpeciesInfo
import app.verdant.android.data.model.IdentifyPlantRequest
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.data.model.SpeciesGroupResponse
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SpeciesTagResponse
import app.verdant.android.data.model.UpdateSpeciesRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.faltet.FaltetChipMultiSelector
import app.verdant.android.ui.faltet.FaltetFormSubmitBar
import app.verdant.android.ui.faltet.FaltetImagePicker
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
```

Drop any old imports that become unused (`TopAppBar`, `OutlinedTextField`, `Card`, `RoundedCornerShape`, `verdantTopAppBarColors`, `stringResource`, `R`, `FilterChip`, `FontWeight`, `FlowRow` from foundation.layout which we don't need here, `horizontalScroll`, `rememberScrollState`, `verticalScroll`).

- [ ] **Step 3: Preserve state + VM verbatim (lines 39–145 of current file)**

Do NOT modify `AddSpeciesState`, `AddSpeciesViewModel`, or any VM method. Task 5 (Phase 4) will add error setters to AI catch blocks.

- [ ] **Step 4: Replace the composable body**

Replace from `@OptIn(ExperimentalMaterial3Api::class)` through the end of the file with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSpeciesScreen(
    onBack: () -> Unit,
    viewModel: AddSpeciesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEdit = viewModel.speciesId != null

    // Form state — exact names match original file
    var commonName by remember { mutableStateOf("") }
    var variantName by remember { mutableStateOf("") }
    var variantNameSv by remember { mutableStateOf("") }
    var scientificName by remember { mutableStateOf("") }
    var imageFrontBase64 by remember { mutableStateOf<String?>(null) }
    var imageBackBase64 by remember { mutableStateOf<String?>(null) }
    var imageFrontUrl by remember { mutableStateOf<String?>(null) }
    var imageBackUrl by remember { mutableStateOf<String?>(null) }
    var frontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var backBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var germinationTimeDaysMin by remember { mutableStateOf("") }
    var germinationTimeDaysMax by remember { mutableStateOf("") }
    var daysToHarvestMin by remember { mutableStateOf("") }
    var daysToHarvestMax by remember { mutableStateOf("") }
    var sowingDepthMm by remember { mutableStateOf("") }
    var heightCmMin by remember { mutableStateOf("") }
    var heightCmMax by remember { mutableStateOf("") }
    var selectedBloomMonths by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedSowingMonths by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var germinationRate by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<SpeciesTagResponse>>(emptySet()) }
    var selectedPositions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSoils by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var prefilled by remember { mutableStateOf(false) }

    // Pre-fill from existing species for edit mode
    LaunchedEffect(uiState.existingSpecies) {
        val s = uiState.existingSpecies
        if (s != null && !prefilled) {
            commonName = s.commonName
            variantName = s.variantName ?: ""
            variantNameSv = s.variantNameSv ?: ""
            scientificName = s.scientificName ?: ""
            imageFrontUrl = s.imageFrontUrl
            imageBackUrl = s.imageBackUrl
            germinationTimeDaysMin = s.germinationTimeDaysMin?.toString() ?: ""
            germinationTimeDaysMax = s.germinationTimeDaysMax?.toString() ?: ""
            daysToHarvestMin = s.daysToHarvestMin?.toString() ?: ""
            daysToHarvestMax = s.daysToHarvestMax?.toString() ?: ""
            sowingDepthMm = s.sowingDepthMm?.toString() ?: ""
            heightCmMin = s.heightCmMin?.toString() ?: ""
            heightCmMax = s.heightCmMax?.toString() ?: ""
            selectedBloomMonths = s.bloomMonths.toSet()
            selectedSowingMonths = s.sowingMonths.toSet()
            germinationRate = s.germinationRate?.toString() ?: ""
            selectedTags = s.tags.toSet()
            selectedPositions = s.growingPositions.toSet()
            selectedSoils = s.soils.toSet()
            prefilled = true
        }
    }

    // Dirty detection — preserve exact expressions from original
    val hasData = commonName.isNotBlank() || variantName.isNotBlank() || variantNameSv.isNotBlank() ||
        scientificName.isNotBlank() || imageFrontBase64 != null || imageBackBase64 != null ||
        germinationTimeDaysMin.isNotBlank() || germinationTimeDaysMax.isNotBlank() ||
        daysToHarvestMin.isNotBlank() || daysToHarvestMax.isNotBlank() ||
        sowingDepthMm.isNotBlank() || heightCmMin.isNotBlank() || heightCmMax.isNotBlank() ||
        selectedBloomMonths.isNotEmpty() || selectedSowingMonths.isNotEmpty() ||
        germinationRate.isNotBlank() || selectedTags.isNotEmpty() ||
        selectedPositions.isNotEmpty() || selectedSoils.isNotEmpty()

    val hasChanges = if (!isEdit) hasData else {
        val s = uiState.existingSpecies
        s == null || commonName != s.commonName ||
            (variantName.takeIf { it.isNotBlank() }) != s.variantName ||
            (variantNameSv.takeIf { it.isNotBlank() }) != s.variantNameSv ||
            (scientificName.takeIf { it.isNotBlank() }) != s.scientificName ||
            imageFrontBase64 != null || imageBackBase64 != null ||
            germinationTimeDaysMin.toIntOrNull() != s.germinationTimeDaysMin ||
            germinationTimeDaysMax.toIntOrNull() != s.germinationTimeDaysMax ||
            daysToHarvestMin.toIntOrNull() != s.daysToHarvestMin ||
            daysToHarvestMax.toIntOrNull() != s.daysToHarvestMax ||
            sowingDepthMm.toIntOrNull() != s.sowingDepthMm ||
            heightCmMin.toIntOrNull() != s.heightCmMin ||
            heightCmMax.toIntOrNull() != s.heightCmMax ||
            selectedBloomMonths != s.bloomMonths.toSet() ||
            selectedSowingMonths != s.sowingMonths.toSet() ||
            germinationRate.toIntOrNull() != s.germinationRate ||
            selectedTags.map { it.id }.toSet() != s.tags.map { it.id }.toSet() ||
            selectedPositions != s.growingPositions.toSet() ||
            selectedSoils != s.soils.toSet()
    }

    val tryBack: () -> Unit = {
        if (hasData && (!isEdit || hasChanges)) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = hasData) { tryBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.created) {
        if (uiState.created) onBack()
    }

    // Discard dialog
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

    // Create-tag dialog
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

    val submitAction: () -> Unit = {
        showValidationErrors = true
        val germMin = germinationTimeDaysMin.toIntOrNull()
        val harvestMin = daysToHarvestMin.toIntOrNull()
        val depth = sowingDepthMm.toIntOrNull()
        val heightMin = heightCmMin.toIntOrNull()
        val germRate = germinationRate.toIntOrNull()
        val valid = commonName.isNotBlank() &&
            scientificName.isNotBlank() &&
            germMin != null && harvestMin != null &&
            depth != null && heightMin != null && germRate != null &&
            selectedPositions.isNotEmpty() &&
            selectedSoils.isNotEmpty() &&
            (imageFrontBase64 != null || imageFrontUrl != null)

        if (valid) {
            if (isEdit) {
                viewModel.updateSpecies(
                    UpdateSpeciesRequest(
                        commonName = commonName,
                        variantName = variantName.takeIf { it.isNotBlank() },
                        variantNameSv = variantNameSv.takeIf { it.isNotBlank() },
                        scientificName = scientificName.takeIf { it.isNotBlank() },
                        imageFrontBase64 = imageFrontBase64,
                        imageBackBase64 = imageBackBase64,
                        germinationTimeDaysMin = germMin,
                        germinationTimeDaysMax = germinationTimeDaysMax.toIntOrNull(),
                        daysToHarvestMin = harvestMin,
                        daysToHarvestMax = daysToHarvestMax.toIntOrNull(),
                        sowingDepthMm = depth,
                        heightCmMin = heightMin,
                        heightCmMax = heightCmMax.toIntOrNull(),
                        bloomMonths = selectedBloomMonths.toList().sorted(),
                        sowingMonths = selectedSowingMonths.toList().sorted(),
                        germinationRate = germRate,
                        tagIds = selectedTags.map { it.id },
                        growingPositions = selectedPositions.toList(),
                        soils = selectedSoils.toList(),
                    )
                )
            } else {
                viewModel.createSpecies(
                    CreateSpeciesRequest(
                        commonName = commonName,
                        variantName = variantName.takeIf { it.isNotBlank() },
                        variantNameSv = variantNameSv.takeIf { it.isNotBlank() },
                        scientificName = scientificName.takeIf { it.isNotBlank() },
                        imageFrontBase64 = imageFrontBase64,
                        imageBackBase64 = imageBackBase64,
                        germinationTimeDaysMin = germMin,
                        germinationTimeDaysMax = germinationTimeDaysMax.toIntOrNull(),
                        daysToHarvestMin = harvestMin,
                        daysToHarvestMax = daysToHarvestMax.toIntOrNull(),
                        sowingDepthMm = depth,
                        heightCmMin = heightMin,
                        heightCmMax = heightCmMax.toIntOrNull(),
                        bloomMonths = selectedBloomMonths.toList().sorted(),
                        sowingMonths = selectedSowingMonths.toList().sorted(),
                        germinationRate = germRate,
                        tagIds = selectedTags.map { it.id },
                        growingPositions = selectedPositions.toList(),
                        soils = selectedSoils.toList(),
                    )
                )
            }
        }
    }

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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Fields added in Task 3 (Phase 3)
            // AI photo section added in Task 5 (Phase 4)
            item { Spacer(Modifier.height(0.dp)) }
        }
    }
}
```

**Critical adaptations to watch for:**
- `UpdateSpeciesRequest` / `CreateSpeciesRequest` exact field names: the above uses `tagIds`, `growingPositions`, `soils`, `bloomMonths`, `sowingMonths`. If the actual model uses different names (e.g., `tags: List<Long>`), adapt.
- If `bloomMonths` / `sowingMonths` on `SpeciesResponse` are nullable or default to empty lists, handle in pre-fill.
- Keep `selectedTags: Set<SpeciesTagResponse>` as the internal state shape (not `Set<Long>`).

- [ ] **Step 5: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt
git commit -m "feat: Fältet AddSpecies chunk 1 — scaffold + state + dialogs"
```

---

## Phase 3 — AddSpecies port, chunk 2: Form fields

---

### Task 3: Fill the LazyColumn body with 17 form fields

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt`

**Goal:** Replace the `item { Spacer(Modifier.height(0.dp)) }` placeholder with the 17 field items per spec §6.2. Add three bottom-of-file helpers: `monthShortSv`, `positionLabelSv`, `soilLabelSv`. Plus the "+ NY TAGG" affordance row.

- [ ] **Step 1: Read the current file (post-chunk-1)**

Confirm the placeholder item line in the `LazyColumn` body. The scaffold + state + dialogs from Task 2 remain intact.

- [ ] **Step 2: Replace the LazyColumn body**

Replace the single `item { Spacer(Modifier.height(0.dp)) }` with:

```kotlin
            // Common name
            item {
                Field(
                    label = "Artnamn",
                    value = commonName,
                    onValueChange = { commonName = it },
                    required = true,
                    error = if (showValidationErrors && commonName.isBlank()) "Artnamn krävs" else null,
                )
            }

            // Scientific name
            item {
                Field(
                    label = "Vetenskapligt namn",
                    value = scientificName,
                    onValueChange = { scientificName = it },
                    required = true,
                    error = if (showValidationErrors && scientificName.isBlank()) "Vetenskapligt namn krävs" else null,
                )
            }

            // Variant EN
            item {
                Field(
                    label = "Variant (engelska, valfri)",
                    value = variantName,
                    onValueChange = { variantName = it },
                )
            }

            // Variant SV
            item {
                Field(
                    label = "Variant (svenska, valfri)",
                    value = variantNameSv,
                    onValueChange = { variantNameSv = it },
                )
            }

            // Germination time min / max
            item {
                Field(
                    label = "Grobarhet dagar min",
                    value = germinationTimeDaysMin,
                    onValueChange = { germinationTimeDaysMin = it.filter { c -> c.isDigit() } },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && germinationTimeDaysMin.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }
            item {
                Field(
                    label = "Grobarhet dagar max (valfri)",
                    value = germinationTimeDaysMax,
                    onValueChange = { germinationTimeDaysMax = it.filter { c -> c.isDigit() } },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
            }

            // Days to harvest min / max
            item {
                Field(
                    label = "Dagar till skörd min",
                    value = daysToHarvestMin,
                    onValueChange = { daysToHarvestMin = it.filter { c -> c.isDigit() } },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && daysToHarvestMin.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }
            item {
                Field(
                    label = "Dagar till skörd max (valfri)",
                    value = daysToHarvestMax,
                    onValueChange = { daysToHarvestMax = it.filter { c -> c.isDigit() } },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
            }

            // Sowing depth
            item {
                Field(
                    label = "Sådjup mm",
                    value = sowingDepthMm,
                    onValueChange = { sowingDepthMm = it.filter { c -> c.isDigit() } },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && sowingDepthMm.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }

            // Height min / max
            item {
                Field(
                    label = "Höjd cm min",
                    value = heightCmMin,
                    onValueChange = { heightCmMin = it.filter { c -> c.isDigit() } },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && heightCmMin.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }
            item {
                Field(
                    label = "Höjd cm max (valfri)",
                    value = heightCmMax,
                    onValueChange = { heightCmMax = it.filter { c -> c.isDigit() } },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
            }

            // Sowing months
            item {
                FaltetChipMultiSelector(
                    label = "Såmånader (valfri)",
                    options = (1..12).toList(),
                    selected = selectedSowingMonths,
                    onSelectedChange = { selectedSowingMonths = it },
                    labelFor = { monthShortSv(it) },
                )
            }

            // Bloom months
            item {
                FaltetChipMultiSelector(
                    label = "Blomningsmånader (valfri)",
                    options = (1..12).toList(),
                    selected = selectedBloomMonths,
                    onSelectedChange = { selectedBloomMonths = it },
                    labelFor = { monthShortSv(it) },
                )
            }

            // Germination rate
            item {
                Field(
                    label = "Grobarhetsprocent",
                    value = germinationRate,
                    onValueChange = { germinationRate = it.filter { c -> c.isDigit() } },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    required = true,
                    error = if (showValidationErrors && germinationRate.toIntOrNull() == null) "Heltal krävs" else null,
                )
            }

            // Growing positions
            item {
                FaltetChipMultiSelector(
                    label = "Växtplats",
                    options = listOf("SUNNY", "PARTIALLY_SUNNY", "SHADOWY"),
                    selected = selectedPositions,
                    onSelectedChange = { selectedPositions = it },
                    labelFor = { positionLabelSv(it) },
                    required = true,
                )
            }

            // Soil types
            item {
                FaltetChipMultiSelector(
                    label = "Jordtyp",
                    options = listOf("CLAY", "SANDY", "LOAMY", "CHALKY", "PEATY", "SILTY"),
                    selected = selectedSoils,
                    onSelectedChange = { selectedSoils = it },
                    labelFor = { soilLabelSv(it) },
                    required = true,
                )
            }

            // Tags
            item {
                FaltetChipMultiSelector(
                    label = "Taggar (valfri)",
                    options = uiState.tags,
                    selected = selectedTags,
                    onSelectedChange = { selectedTags = it },
                    labelFor = { it.name },
                )
            }

            // + NY TAGG affordance
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

            item { Spacer(Modifier.height(80.dp)) }
```

- [ ] **Step 3: Add helpers at the bottom of the file**

Add these three private top-level functions at the very end of the file (after the `fun AddSpeciesScreen(...)` closing brace):

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

- [ ] **Step 4: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt
git commit -m "feat: Fältet AddSpecies chunk 2 — form fields"
```

---

## Phase 4 — AddSpecies port, chunk 3: AI photo section

---

### Task 4: AddSpeciesViewModel AI error setters

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt` (VM section only)

**Goal:** Add snackbar-friendly error setters in `identifyPlant` and `extractSpeciesInfo` catch blocks. Minimal change: 2 lines per method.

- [ ] **Step 1: Update `identifyPlant`**

Replace the method body (currently lines ~122–132 of the original file, now in the preserved VM at top of ported file) with:

```kotlin
fun identifyPlant(imageBase64: String) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(identifying = true, suggestions = emptyList(), error = null)
        try {
            val suggestions = repo.identifyPlant(IdentifyPlantRequest(imageBase64))
            _uiState.value = _uiState.value.copy(identifying = false, suggestions = suggestions)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(identifying = false, error = "Kunde inte identifiera bilden")
        }
    }
}
```

- [ ] **Step 2: Update `extractSpeciesInfo`**

Replace the method body (currently lines ~134–144) with:

```kotlin
fun extractSpeciesInfo(imageBase64: String) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(extracting = true, extractedInfo = null, error = null)
        try {
            val info = repo.extractSpeciesInfo(ExtractSpeciesInfoRequest(imageBase64))
            _uiState.value = _uiState.value.copy(extracting = false, extractedInfo = info)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(extracting = false, error = "Kunde inte extrahera information")
        }
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt
git commit -m "feat: AddSpeciesViewModel — surface AI errors for snackbar"
```

---

### Task 5: Add the AI photo section + suggestion cards + LaunchedEffects

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt`

**Goal:** Prepend the photo row + AI spinner + suggestion rows + auto-populate `LaunchedEffect`s to the composable. Add `SuggestionRow` private composable + `fillFromSuggestion` lambda.

- [ ] **Step 1: Add auto-populate + crop LaunchedEffects**

After the `LaunchedEffect(uiState.existingSpecies)` pre-fill block but before the `hasData` computation, add:

```kotlin
    // AI auto-populate from suggestions (front photo identification)
    LaunchedEffect(uiState.suggestions) {
        val top = uiState.suggestions.firstOrNull() ?: return@LaunchedEffect
        if (commonName.isBlank()) commonName = top.commonName
        if (scientificName.isBlank()) scientificName = top.species
        // Crop front bitmap to detected bounds
        val box = top.cropBox
        val src = frontBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            frontBitmap = cropped
            imageFrontBase64 = cropped.toCompressedBase64()
        }
    }

    // AI auto-populate from extraction (back photo info extraction)
    LaunchedEffect(uiState.extractedInfo) {
        val info = uiState.extractedInfo ?: return@LaunchedEffect
        if (commonName.isBlank()) info.commonName?.let { commonName = it }
        if (variantName.isBlank()) info.variantName?.let { variantName = it }
        if (variantNameSv.isBlank()) info.variantNameSv?.let { variantNameSv = it }
        if (scientificName.isBlank()) info.scientificName?.let { scientificName = it }
        if (germinationTimeDaysMin.isBlank()) info.germinationTimeDaysMin?.let { germinationTimeDaysMin = it.toString() }
        if (germinationTimeDaysMax.isBlank()) info.germinationTimeDaysMax?.let { germinationTimeDaysMax = it.toString() }
        if (daysToHarvestMin.isBlank()) info.daysToHarvestMin?.let { daysToHarvestMin = it.toString() }
        if (daysToHarvestMax.isBlank()) info.daysToHarvestMax?.let { daysToHarvestMax = it.toString() }
        if (sowingDepthMm.isBlank()) info.sowingDepthMm?.let { sowingDepthMm = it.toString() }
        if (heightCmMin.isBlank()) info.heightCmMin?.let { heightCmMin = it.toString() }
        if (heightCmMax.isBlank()) info.heightCmMax?.let { heightCmMax = it.toString() }
        if (germinationRate.isBlank()) info.germinationRate?.let { germinationRate = it.toString() }
        if (selectedBloomMonths.isEmpty()) info.bloomMonths?.let { selectedBloomMonths = it.toSet() }
        if (selectedSowingMonths.isEmpty()) info.sowingMonths?.let { selectedSowingMonths = it.toSet() }
        if (selectedPositions.isEmpty()) info.growingPositions?.let { selectedPositions = it.toSet() }
        if (selectedSoils.isEmpty()) info.soils?.let { selectedSoils = it.toSet() }
        // Crop back bitmap to detected bounds
        val box = info.cropBox
        val src = backBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            backBitmap = cropped
            imageBackBase64 = cropped.toCompressedBase64()
        }
    }
```

**IMPORTANT:** `PlantSuggestion` and `ExtractedSpeciesInfo` field names may differ from this template. Read the current file (which already has the original LaunchedEffects) to confirm:
- `top.cropBox` (or `top.cropBoxFront`?)
- `info.cropBox` (or named differently?)
- `info.growingPositions` (may be named `positions` on the extraction type)

Adapt to actual names.

- [ ] **Step 2: Add `fillFromSuggestion` lambda inside the composable**

Above the `FaltetScreenScaffold(...)` call:

```kotlin
    val fillFromSuggestion: (PlantSuggestion) -> Unit = { suggestion ->
        commonName = suggestion.commonName
        scientificName = suggestion.species
        val box = suggestion.cropBox
        val src = frontBitmap
        if (box != null && src != null) {
            val cropped = src.cropToBox(box)
            frontBitmap = cropped
            imageFrontBase64 = cropped.toCompressedBase64()
        }
    }
```

- [ ] **Step 3: Insert the photo + AI section at the top of the LazyColumn**

Before the existing "Common name" item (from chunk 2), prepend:

```kotlin
            // Photo row: front + back side-by-side
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        FaltetImagePicker(
                            label = "Framsida *",
                            value = frontBitmap,
                            onValueChange = { bitmap ->
                                frontBitmap = bitmap
                                if (bitmap != null) {
                                    val b64 = bitmap.toCompressedBase64()
                                    imageFrontBase64 = b64
                                    viewModel.identifyPlant(b64)
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
                                    val b64 = bitmap.toCompressedBase64()
                                    imageBackBase64 = b64
                                    viewModel.extractSpeciesInfo(b64)
                                } else {
                                    imageBackBase64 = null
                                }
                            },
                        )
                    }
                }
            }

            // AI spinner row
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

            // Suggestion cards
            if (uiState.suggestions.isNotEmpty()) {
                item { FaltetSectionHeader(label = "Förslag") }
                items(uiState.suggestions, key = { it.species }) { suggestion ->
                    SuggestionRow(suggestion = suggestion, onTap = { fillFromSuggestion(suggestion) })
                }
            }
```

- [ ] **Step 4: Add the `SuggestionRow` private composable**

Above the `monthShortSv` helper at the bottom of the file (after the `AddSpeciesScreen` composable closes):

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

- [ ] **Step 5: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt
git commit -m "feat: Fältet AddSpecies chunk 3 — AI photo section"
```

---

## Phase 5 — Verify + milestone

---

### Task 6: Verify + milestone

- [ ] **Step 1: Full Android build**

```bash
cd android && ./gradlew assembleDebug --no-daemon -q
```

Expected: BUILD SUCCESSFUL. APK at `android/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install + manual smoke (6 scenarios)**

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Launch the app. Navigate to AddSpecies (typically via admin or a "new species" entry point in the species list).

- [ ] **Scenario 1: Create mode**
  Open AddSpecies fresh (no speciesId). Fill: artnamn, vetenskapligt namn, grobarhet/skörd/sådjup/höjd min, grobarhetsprocent, växtplats (pick at least one), jordtyp (pick at least one), upload a front photo. Tap "Skapa". Expect: screen closes, back to caller.

- [ ] **Scenario 2: Edit mode**
  Open AddSpecies with an existing `speciesId` (navigate from a species list edit entry). Verify: all fields prefill including the photo URLs rendering. Change the common name. Tap "Spara". Expect: screen closes.

- [ ] **Scenario 3: AI identify**
  Create mode. Take or pick a front photo. Watch the spinner "Identifierar…" show. After a few seconds, suggestion rows appear under a "Förslag" header. Tap a suggestion. Expect: artnamn + vetenskapligt namn fill.

- [ ] **Scenario 4: AI extract**
  Same session. Take or pick a back photo. Watch "Extraherar information…" spinner. After extraction: numeric fields, months, positions, soils auto-populate for fields that were still blank.

- [ ] **Scenario 5: Dirty detection**
  Start typing in any field. Press system back. Expect: "Avbryt ändringar?" AlertDialog. Tap "Fortsätt redigera" — returns to form. Tap back again, tap "Avbryt ändringar" — exits screen.

- [ ] **Scenario 6: Create tag**
  Scroll to the Taggar section. Tap "+ NY TAGG". Dialog opens. Enter "Trädgård". Tap "Skapa". Dialog closes. New tag appears in the chip list on next state refresh (may require momentary wait for `loadData()` to repopulate).

- [ ] **Step 3: Milestone commit**

```bash
git commit --allow-empty -m "milestone: Android Fältet AddSpecies complete (Spec C4)"
```

---

## Verification summary

After Task 6:

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipMultiSelector.kt` shipped as a new primitive (five consumers on AddSpeciesScreen alone).
- `AddSpeciesScreen.kt` rendered Fältet: editorial masthead, cream body, hairline form fields, side-by-side photo pickers, fixed bottom submit bar, snackbar for AI errors.
- Dirty detection + discard confirmation + create-tag dialog preserved.
- AI identify + extract + auto-populate + auto-crop preserved verbatim.
- Create + edit modes both working with pre-fill.
- Swedish labels throughout.
- `./gradlew assembleDebug` green.

**Follow-ups:**

- **AddPlantEvent AI restoration** — reuse the `identifyPlant` pattern ported here.
- **Inline edit dialogs in GardenDetail + BedDetail** — carryover polish pass.
- **ApplySupply "show all categories" toggle** — minor regression.
- **Missing group dropdown in AddSpecies** — feature gap in original; separate decision.
- **Sub-spec D** — analytics, account, auth, MyWorld dashboard, SeasonSelector.

---

## Self-review notes

- **Spec §1 (goal):** Tasks 1–6 port the 862-line screen in 1 primitive + 3 port chunks + 1 verify.
- **Spec §2 (scope):** Only AddSpeciesScreen.kt modified + PhotoPicker.kt helpers reused unchanged.
- **Spec §3 (decisions):** All 6 brainstorm decisions implemented: `FaltetChipMultiSelector` only (Task 1); chunked port (Tasks 2–5); AI preserved with snackbar error setters (Tasks 4–5); dirty detection preserved (Task 2); photos side-by-side (Task 5); Swedish labels throughout (Tasks 3, 5).
- **Spec §4 (primitive):** Full source + 3 previews in Task 1.
- **Spec §5 (port pattern):** Scaffold (Task 2), dirty detection (Task 2), create-tag dialog (Task 2), AI photos + LaunchedEffects (Task 5), preservations called out.
- **Spec §6 (field structure):** All 17 fields enumerated in Task 3 with Swedish labels + validation. Photo row + AI spinner + suggestion rows in Task 5. Helpers in Task 3.
- **Spec §7 (phasing):** 1 + 1 + 1 + 2 + 1 = 6 tasks (Task 4 + Task 5 together form "Phase 4 chunk 3"). Matches spec intent: shell → fields → AI with AI split into VM update + composable addition.
- **Spec §8 (testing):** `@Preview` in primitive; compile gate per task; 6-scenario manual smoke in Task 6.
- **Spec §9 (follow-ups):** Called out.
