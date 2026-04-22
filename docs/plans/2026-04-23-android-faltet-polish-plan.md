# Android Fältet — Polish Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore 4 regressions accumulated during Specs C1–D and add the missing group dropdown on AddSpecies, without introducing any new primitives.

**Architecture:** 6 independent commits, one per change. Each targets a single file, each is compile-green before commit, each has a clear revert boundary. No new primitives — everything composes from the existing Fältet set. One empty milestone commit caps the pass.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (`AlertDialog`), existing Fältet primitive set (`Field`, `FaltetCheckbox`, `FaltetChipSelector`, `FaltetDropdown`, `FaltetSectionHeader`, `FaltetScreenScaffold` with snackbar slot).

**Spec:** `docs/plans/2026-04-23-android-faltet-polish-design.md` — read before starting.

**Reality-check notes:**
- Each task instructs the agent to first read the target file and adapt to actual VM/state/model field names. Templates show the shape; reality wins on names.
- No new `@Preview` is required per task — each change is a targeted edit to an existing file.
- Dispatch order matters: the 6 tasks are independent, but **Task 1 (AddPlantEvent)** and **Task 6 (AddSpecies)** both copy the AI suggestion pattern from the C4 AddSpecies port, so running Task 6 close to Task 1 keeps context warm.

---

## File Structure

### Modified files (6 tasks, 6 files)

| # | File |
|---|---|
| 1 | `android/app/src/main/kotlin/app/verdant/android/ui/plant/AddPlantEventScreen.kt` |
| 2 | `android/app/src/main/kotlin/app/verdant/android/ui/activity/ApplySupplyScreen.kt` |
| 3 | `android/app/src/main/kotlin/app/verdant/android/ui/account/AccountScreen.kt` |
| 4 | `android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt` |
| 5 | `android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailScreen.kt` |
| 6 | `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt` |

### No new files

---

## Phase 1 — Regression restores

---

### Task 1: AddPlantEvent — restore AI identification

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/plant/AddPlantEventScreen.kt`

**Goal:** After front-photo capture, call `viewModel.identifyPlant(base64)` and render a spinner + tappable suggestion cards. Pattern mirrors C4 AddSpeciesScreen.

**Preserve verbatim:** all existing VM wiring, Base64 utility call, state, navigation.

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/plant/AddPlantEventScreen.kt` in full. Confirm:
- The front `FaltetImagePicker` slot and its `onValueChange` lambda.
- VM state fields: `identifying: Boolean`, `suggestions: List<PlantSuggestion>` (may be named slightly differently — check `AddPlantEventState`).
- VM method `identifyPlant(base64: String)` or similar.
- Whether error handling is needed in the VM catch block (spec 4.1 step 5).

Also read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt` — copy the `SuggestionRow` private composable pattern from there (spec 4.1 step 4 has the source).

- [ ] **Step 2: Add imports**

Ensure these imports are present (add any missing ones):

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.data.model.PlantSuggestion
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
```

- [ ] **Step 3: Wire the front-photo callback to `identifyPlant`**

Locate the front `FaltetImagePicker(label = "Foto (valfri)", ...)` call (or whatever the current label is). Modify its `onValueChange` to call `identifyPlant` when a new bitmap arrives:

```kotlin
FaltetImagePicker(
    label = "Foto (valfri)",
    value = photoBitmap,
    onValueChange = { bitmap ->
        photoBitmap = bitmap
        if (bitmap != null) {
            val b64 = bitmap.toCompressedBase64()
            imageBase64 = b64
            viewModel.identifyPlant(b64)
        } else {
            imageBase64 = null
        }
    },
)
```

**Adapt:** if the current state var is named `imageBase64`/`photoBase64`/etc., use the actual name. If the VM method is named differently (`suggestSpecies`, `runIdentification`), use the actual name.

- [ ] **Step 4: Add AI spinner + suggestion card items to the LazyColumn**

Find the LazyColumn body. After the photo picker item, add:

```kotlin
if (uiState.identifying) {
    item {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            CircularProgressIndicator(
                color = FaltetClay,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Identifierar…",
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
        SuggestionRow(suggestion = suggestion)
    }
}
```

**Adapt:** confirm field names `uiState.identifying` and `uiState.suggestions`. If they differ, use actual names.

- [ ] **Step 5: Add the `SuggestionRow` private composable at the bottom of the file**

```kotlin
@Composable
private fun SuggestionRow(suggestion: PlantSuggestion) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
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

Non-tappable — the original AddPlantEvent flow displayed suggestions as informational cards; the species is already bound to the plant context.

- [ ] **Step 6: Add VM error surfacing (if missing)**

Check the VM's `identifyPlant` method. If the catch block silently swallows errors without setting `error` on state, update it:

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

If the VM already emits `error` on failure, skip this step.

- [ ] **Step 7: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 8: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/plant/AddPlantEventScreen.kt
git commit -m "fix: restore AddPlantEvent AI identification"
```

---

### Task 2: ApplySupply — restore "Visa alla" toggle

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/ApplySupplyScreen.kt`

**Goal:** Add a checkbox-row above the supply dropdown that toggles `uiState.showAllCategories` (or the equivalent VM state), restoring the user's ability to see supplies beyond fertilizer.

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/activity/ApplySupplyScreen.kt` in full. Confirm:
- The VM state field (e.g., `showAllCategories: Boolean`) and setter method name (e.g., `setShowAllCategories(value: Boolean)`).
- Where the supply `FaltetDropdown` is in the LazyColumn body.
- Whether the dropdown options are already filtered by the VM state (via a `remember` or VM-derived list) — if so, the toggle just drives existing filtering.

- [ ] **Step 2: Add imports**

Add if missing:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.faltet.FaltetCheckbox
import app.verdant.android.ui.theme.FaltetInk
```

- [ ] **Step 3: Add the checkbox-row item above the supply dropdown**

In the LazyColumn body, insert this `item { ... }` immediately before the supply `FaltetDropdown` item:

```kotlin
item {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.setShowAllCategories(!uiState.showAllCategories) }
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        FaltetCheckbox(
            checked = uiState.showAllCategories,
            onCheckedChange = null,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Visa alla kategorier",
            fontSize = 14.sp,
            color = FaltetInk,
        )
    }
}
```

**Adapt:** replace `uiState.showAllCategories` and `viewModel.setShowAllCategories(...)` with the actual VM state field name and setter method name. If the VM uses a different naming convention (`showAllSupplies`, `includeAllCategories`, `filterEnabled`), use the actual names.

If the dropdown's `options` list is not currently derived from the VM state, add a `remember` block that filters `uiState.supplies` by category when `!uiState.showAllCategories`:

```kotlin
val visibleSupplies = remember(uiState.supplies, uiState.showAllCategories) {
    if (uiState.showAllCategories) uiState.supplies
    else uiState.supplies.filter { it.category == "FERTILIZER" }
}
```

Then pass `options = visibleSupplies` to the `FaltetDropdown`. (Check first — the VM or composable may already do this filtering.)

- [ ] **Step 4: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/activity/ApplySupplyScreen.kt
git commit -m "fix: restore ApplySupply 'visa alla kategorier' toggle"
```

---

### Task 3: Account — surface VM errors via snackbar

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/account/AccountScreen.kt`

**Goal:** When `uiState.error != null`, show it as a snackbar via the scaffold's `snackbarHost` slot. Restores the error-handling dropped in Spec D.

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/account/AccountScreen.kt`. Confirm:
- `AccountState` has an `error: String?` field (or similar — may be `errorMessage` / `lastError`).
- Whether `FaltetScreenScaffold` is already called with `snackbarHost = { }` (empty lambda) or without the parameter.

- [ ] **Step 2: Add imports**

Add if missing:

```kotlin
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
```

- [ ] **Step 3: Add snackbar state + LaunchedEffect**

Inside `AccountScreen(...)`, after the `val uiState by viewModel.uiState.collectAsState()` line (and before the `FaltetScreenScaffold(...)` call), add:

```kotlin
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }
```

- [ ] **Step 4: Wire snackbar host into scaffold**

Modify the `FaltetScreenScaffold(...)` call to pass the snackbar host. Locate:

```kotlin
FaltetScreenScaffold(
    mastheadLeft = "§ Konto",
    mastheadCenter = "Konto",
) { padding ->
```

Change to:

```kotlin
FaltetScreenScaffold(
    mastheadLeft = "§ Konto",
    mastheadCenter = "Konto",
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { padding ->
```

**Adapt:** the VM error state field name may be `error`, `errorMessage`, or similar — use the actual name.

- [ ] **Step 5: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/account/AccountScreen.kt
git commit -m "fix: surface Account VM errors via snackbar"
```

---

## Phase 2 — Primitive migration + feature

---

### Task 4: GardenDetail — migrate edit dialog to `Field`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt`

**Goal:** Replace `OutlinedTextField` instances in the private `EditGardenDialog` composable with the Fältet `Field` primitive. Keep the `AlertDialog` chrome intact.

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt`. Locate the private `EditGardenDialog` composable (or the inline AlertDialog body if it's not extracted). Note:
- Text fields currently present (name, description, emoji — or whatever the current code has).
- Any `RoundedCornerShape` or `OutlinedTextField`-specific styling.

- [ ] **Step 2: Migrate each `OutlinedTextField` to `Field`**

For each text field, replace:

```kotlin
OutlinedTextField(
    value = name,
    onValueChange = { name = it },
    label = { Text(/* whatever */) },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
)
```

With:

```kotlin
Field(
    label = "Namn",
    value = name,
    onValueChange = { name = it },
    required = true,
)
```

Swedish labels:
- `Namn` — required on garden.
- `Beskrivning (valfri)` — optional (drop `required = true`).
- `Emoji` — not required; use placeholder `"🌱"`:
  ```kotlin
  Field(
      label = "Emoji",
      value = emoji,
      onValueChange = { emoji = it },
      placeholder = "🌱",
  )
  ```

**Adapt:** use actual state variable names (if they differ from `name` / `description` / `emoji`).

- [ ] **Step 3: Clean up imports**

Drop unused imports: `OutlinedTextField`, `RoundedCornerShape`, `TextFieldDefaults` if they're no longer used anywhere in the file. Add:

```kotlin
import app.verdant.android.ui.faltet.Field
```

if not already present.

- [ ] **Step 4: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt
git commit -m "refactor: migrate GardenDetail edit dialog to Field primitive"
```

---

### Task 5: BedDetail — migrate edit dialog to `Field` + `FaltetChipSelector`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailScreen.kt`

**Goal:** Replace `OutlinedTextField` + inline `FilterChip` rows in the bed edit dialog with Fältet `Field` + `FaltetChipSelector`. Reuse the non-composable label helpers added during Spec C2.

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailScreen.kt`. Locate the edit dialog (may be in a private composable or inline). Note:
- Text fields (name, description, soil pH — pH is a numeric text field).
- Condition chip rows and which label helpers they use (likely `bedSoilTypeLabel`, `bedSunExposureLabel`, `bedDrainageLabel`, `bedAspectLabel`, `bedIrrigationTypeLabel`, `bedProtectionLabel` — some may be `@Composable` for `stringResource`).
- Whether non-composable `*LabelStr` helpers exist in `CreateBedScreen.kt` from the C2 port — they should. If not, add them inline.

- [ ] **Step 2: Migrate text fields**

For each `OutlinedTextField`, migrate to `Field`:

```kotlin
// Before:
OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(/* ... */) }, ...)

// After:
Field(label = "Namn", value = name, onValueChange = { name = it }, required = true)
```

- Name: required.
- Description: `Field(label = "Beskrivning (valfri)", value = description, onValueChange = { description = it })`.
- Soil pH: `Field(label = "pH (valfri)", value = soilPhText, onValueChange = { soilPhText = it }, keyboardType = KeyboardType.Decimal)`.

- [ ] **Step 3: Migrate chip rows to `FaltetChipSelector`**

For each condition (Jordtyp, Dränering, Sol, Väderstreck, Bevattning, Skydd), replace the existing `FlowRow { FilterChip(...) }` with:

```kotlin
FaltetChipSelector(
    label = "Jordtyp",
    options = BedSoilType.values,
    selected = soilType,
    onSelectedChange = { soilType = it },
    labelFor = { bedSoilTypeLabelStr(it) },
)
```

Swedish labels per condition:
- `Jordtyp` → `bedSoilTypeLabelStr`
- `Dränering` → `bedDrainageLabelStr`
- `Sol` → `bedSunExposureLabelStr`
- `Väderstreck` → `bedAspectLabelStr`
- `Bevattning` → `bedIrrigationTypeLabelStr`
- `Skydd` → `bedProtectionLabelStr`

**Adapt:**
- Use actual state var names for each condition selection.
- If any `*LabelStr` helper doesn't exist in `CreateBedScreen.kt`, either define it inline at the bottom of `BedDetailScreen.kt` with Swedish literals, or add it to the original shared location.
- If a condition doesn't exist in the current edit dialog, skip it.

Example for raised bed (boolean):

```kotlin
FaltetChipSelector(
    label = "Upphöjd bädd (valfri)",
    options = listOf(true, false),
    selected = raisedBed,
    onSelectedChange = { raisedBed = it },
    labelFor = { if (it) "Ja" else "Nej" },
)
```

- [ ] **Step 4: Clean up imports**

Drop unused: `OutlinedTextField`, `FilterChip`, `FilterChipDefaults`, `RoundedCornerShape`, `FlowRow` (if no longer used), `RectangleShape` (if no longer used).

Add:

```kotlin
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.faltet.FaltetChipSelector
```

- [ ] **Step 5: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailScreen.kt
git commit -m "refactor: migrate BedDetail edit dialog to Fältet primitives"
```

**Note:** if `CreateBedScreen.kt` or another file was also touched (e.g., to add a missing `*LabelStr` helper), include it in the commit.

---

### Task 6: AddSpecies — add group dropdown + create-group dialog

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt`

**Goal:** Add a `FaltetDropdown` for species group + a "+ NY GRUPP" affordance that opens an AlertDialog for creating a new group. Wire the selection into submit + dirty detection + pre-fill.

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt` in full. Confirm:
- `AddSpeciesState` already has `groups: List<SpeciesGroupResponse>` populated.
- `AddSpeciesViewModel.createGroup(name: String)` exists.
- `SpeciesResponse` has a `groupId: Long?` field.
- `CreateSpeciesRequest` and `UpdateSpeciesRequest` accept a `groupId` field (or similar). If they don't, check the repo/API layer to see what's expected — likely add `groupId: Long? = null` to those request types. Confirm before proceeding.

- [ ] **Step 2: Add state declarations**

Inside the `AddSpeciesScreen(...)` composable, alongside the existing state (near the other `var ... by remember { mutableStateOf(...) }` lines near the top), add:

```kotlin
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
```

- [ ] **Step 3: Extend pre-fill in `LaunchedEffect(uiState.existingSpecies)`**

Inside the `LaunchedEffect(uiState.existingSpecies)` block, alongside other pre-fills, add:

```kotlin
    selectedGroupId = s.groupId
```

- [ ] **Step 4: Extend `hasData` + `hasChanges` for dirty detection**

Find the `hasData` expression — add `|| selectedGroupId != null` to it.

Find the `hasChanges` expression (the `if (!isEdit) hasData else { ... }` block) — in the edit branch, add `|| selectedGroupId != s.groupId` to the chain of comparisons.

- [ ] **Step 5: Add the group dropdown + "+ NY GRUPP" affordance**

In the LazyColumn body, immediately **before** the Tags section (which starts with `FaltetChipMultiSelector(label = "Taggar (valfri)", ...)`), insert:

```kotlin
            // Group (valfri)
            item {
                val selectedGroup = uiState.groups.find { it.id == selectedGroupId }
                FaltetDropdown(
                    label = "Grupp (valfri)",
                    options = uiState.groups,
                    selected = selectedGroup,
                    onSelectedChange = { group -> selectedGroupId = group.id },
                    labelFor = { it.name },
                    searchable = false,
                )
            }

            item {
                Text(
                    text = "+ NY GRUPP",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = FaltetClay,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNewGroupDialog = true }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
```

- [ ] **Step 6: Add the create-group dialog**

Alongside the existing dialog-rendering blocks (discard dialog, new-tag dialog), add:

```kotlin
    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false; newGroupName = "" },
            title = { Text("Ny grupp") },
            text = {
                Field(
                    label = "Namn",
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    required = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            viewModel.createGroup(newGroupName.trim())
                            showNewGroupDialog = false
                            newGroupName = ""
                        }
                    },
                    enabled = newGroupName.isNotBlank(),
                ) { Text("Skapa", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false; newGroupName = "" }) { Text("Avbryt") }
            },
        )
    }
```

- [ ] **Step 7: Wire `groupId` into submit**

Find the `submitAction` block. In both the `viewModel.updateSpecies(...)` and `viewModel.createSpecies(...)` calls, add `groupId = selectedGroupId` to the request builders.

Example:

```kotlin
viewModel.createSpecies(
    CreateSpeciesRequest(
        commonName = commonName,
        scientificName = scientificName.takeIf { it.isNotBlank() },
        // ... other fields
        tagIds = selectedTagIds.toList(),
        growingPositions = selectedPositions.toList(),
        soils = selectedSoils.toList(),
        groupId = selectedGroupId,  // NEW
    )
)
```

Same for `UpdateSpeciesRequest`.

**If `CreateSpeciesRequest` / `UpdateSpeciesRequest` don't have a `groupId` field:** add it to both data classes (look under `app.verdant.android.data.model`). The backend either accepts it already (since it persists the field on `SpeciesResponse.groupId`) or the addition is cosmetic. Consult existing API schema if uncertain — but a `Long? = null` default field on a request data class is a safe addition.

- [ ] **Step 8: Add imports**

Ensure present:

```kotlin
import app.verdant.android.ui.faltet.FaltetDropdown
```

- [ ] **Step 9: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 10: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/activity/AddSpeciesScreen.kt
# Also include any model changes (e.g., CreateSpeciesRequest.kt) if they were modified:
git add android/app/src/main/kotlin/app/verdant/android/data/model/CreateSpeciesRequest.kt
git add android/app/src/main/kotlin/app/verdant/android/data/model/UpdateSpeciesRequest.kt
git commit -m "feat: AddSpecies group dropdown + create-new flow"
```

---

## Phase 3 — Verify + milestone

---

### Task 7: Verify + milestone

- [ ] **Step 1: Full Android build**

```bash
cd android && ./gradlew assembleDebug --no-daemon -q
```

Expected: BUILD SUCCESSFUL. APK at `android/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install + manual smoke (6 scenarios)**

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

- [ ] **Scenario 1 — AddPlantEvent AI:** open AddPlantEvent for any plant. Take or pick a front photo. Verify the "Identifierar…" spinner appears, then suggestion cards appear under a "Förslag" section header with confidence percentages.

- [ ] **Scenario 2 — ApplySupply toggle:** open ApplySupply from a bed context. Verify "Visa alla kategorier" checkbox appears above the supply dropdown. Toggle it on — dropdown list expands to include non-fertilizer supplies. Toggle off — list restricts to fertilizer only.

- [ ] **Scenario 3 — Account errors:** from Account, force a VM error (enable airplane mode + tap sign-out, or any action that hits network). Verify a snackbar in Swedish appears at the bottom of the screen instead of silent failure.

- [ ] **Scenario 4 — GardenDetail edit:** open GardenDetail for any garden. Tap the edit icon in the masthead right. Verify the edit dialog renders with Fältet `Field` inputs (mono uppercase label + Fraunces italic value + 1dp underline) instead of Material outlined text fields.

- [ ] **Scenario 5 — BedDetail edit:** open BedDetail for any bed. Tap the edit icon. Verify `Field`s for name + description + pH, and `FaltetChipSelector` rows for each condition (Jordtyp, Dränering, Sol, Väderstreck, Bevattning, Skydd) with clay-colored selected state.

- [ ] **Scenario 6 — AddSpecies group:** open AddSpecies in create mode (from admin/species list). Scroll to find the "Grupp (valfri)" dropdown above Taggar. Tap "+ NY GRUPP", enter a name like "Nya blommor", tap Skapa. The dialog closes, and after a brief refresh tick the new group appears in the dropdown options. Select it, fill in other required fields, submit — verify the species is created with the group association.

- [ ] **Step 3: Milestone commit**

```bash
git commit --allow-empty -m "milestone: Android Fältet polish pass complete"
```

---

## Verification summary

After Task 7:

- AddPlantEvent displays AI identification spinner + suggestion cards.
- ApplySupply has "Visa alla kategorier" toggle above the supply dropdown.
- Account surfaces VM errors via snackbar.
- GardenDetail + BedDetail edit dialogs use Fältet primitives.
- AddSpecies has a group dropdown + create-new flow.
- `./gradlew assembleDebug` green.

**Android Fältet project state:**

- ✅ Spec A — Foundation
- ✅ Spec B — 12 list screens
- ✅ Spec C1 — 4 detail screens
- ✅ Spec C2 — 5 form screens
- ✅ Spec C3 — 6 activity screens
- ✅ Spec C4 — AddSpecies
- ✅ Spec D — Splash + Auth + Analytics + Account + SeasonSelector
- ✅ Polish pass — 4 regressions restored + group dropdown shipped

The Android Fältet project is complete.

---

## Self-review notes

- **Spec §1 (goal):** Tasks 1–6 restore 4 regressions + add 1 feature. Task 7 verifies.
- **Spec §2 (scope):** Each of the 6 changes has its own task (1 → AddPlantEvent, 2 → ApplySupply, 3 → Account, 4 → GardenDetail, 5 → BedDetail, 6 → AddSpecies).
- **Spec §3 (decisions):** All 3 decisions implemented: all 5 regressions + feature, 6 independent commits, no new primitives.
- **Spec §4.1 (AddPlantEvent AI):** Task 1 — photo callback (Step 3), spinner + suggestions (Step 4), SuggestionRow helper (Step 5), optional VM error-handling (Step 6).
- **Spec §4.2 (ApplySupply toggle):** Task 2 — checkbox row above dropdown.
- **Spec §4.3 (Account snackbar):** Task 3 — snackbar state + LaunchedEffect + scaffold wiring.
- **Spec §4.4 (GardenDetail dialog):** Task 4 — OutlinedTextField → Field migration.
- **Spec §4.5 (BedDetail dialog):** Task 5 — OutlinedTextField → Field + FilterChip → FaltetChipSelector.
- **Spec §4.6 (AddSpecies group):** Task 6 — state (Step 2), pre-fill (Step 3), dirty detection (Step 4), dropdown + affordance (Step 5), dialog (Step 6), submit wiring (Step 7).
- **Spec §5 (phasing):** 3 + 3 + 1 = 7 tasks. Matches.
- **Spec §6 (testing):** Compile gate per task; 6-scenario manual smoke in Task 7.
