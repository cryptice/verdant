# Android Fältet — Polish Pass (Design)

**Status:** design, awaiting implementation plan
**Scope:** 6 changes across 5 screen files; no new primitives
**Predecessors:** Specs A, B, C1, C2, C3, C4, D all complete. Last milestone `7524a66` (Spec D finale).
**Successors:** None planned. After this spec ships, the Android Fältet project is feature-complete relative to pre-port state + the one feature addition (AddSpecies group dropdown).

---

## 1. Goal

Close out the Android Fältet port by:

1. Restoring four regressions accumulated during Specs C1–D.
2. Adding the long-standing missing group dropdown on AddSpecies.

**Non-goals:**

- No new primitives — everything composes from existing Fältet primitives.
- No data-model or API changes.
- No unrelated refactors — each change tightly scoped.

**Success criteria:**

- **AddPlantEvent** displays AI identification spinner + suggestion cards after front-photo capture, matching the C4 AddSpecies pattern.
- **ApplySupply** has a "Visa alla kategorier" toggle that shows all supplies (not just fertilizer) when enabled.
- **GardenDetail + BedDetail** edit dialogs use `Field` primitives (and `FaltetChipSelector` for BedDetail enum conditions) instead of `OutlinedTextField` / `FilterChip`.
- **Account** VM errors surface as snackbars.
- **AddSpecies** has a group dropdown with inline "+ NY GRUPP" create flow.
- `./gradlew assembleDebug` green; manual smoke on 6 scenarios.

---

## 2. Scope

### Changes (6 files touched once each; GardenDetail + BedDetail are separate commits)

| # | Change | File | Type |
|---|---|---|---|
| 1 | Restore AI identification | `ui/plant/AddPlantEventScreen.kt` | Regression restore (C2) |
| 2 | Restore "Visa alla" toggle | `ui/activity/ApplySupplyScreen.kt` | Regression restore (C3) |
| 3 | Surface VM errors via snackbar | `ui/account/AccountScreen.kt` | Regression restore (D) |
| 4 | Migrate edit dialog to `Field` | `ui/garden/GardenDetailScreen.kt` | Primitive migration (C2 follow-up) |
| 5 | Migrate edit dialog to `Field` + `FaltetChipSelector` | `ui/bed/BedDetailScreen.kt` | Primitive migration (C2 follow-up) |
| 6 | Add group dropdown + create flow | `ui/activity/AddSpeciesScreen.kt` | Feature addition |

### Out of scope

- Any other pre-existing bugs not listed above.
- New primitives.
- Unrelated refactors.

---

## 3. Design decisions (summary of brainstorm)

| # | Decision | Chosen |
|---|---|---|
| 1 | Scope | All 5 regressions + the one feature addition (AddSpecies group dropdown). Total 6 changes. |
| 2 | Implementation approach | 6 independent commits (one per change), each with a clear revert boundary. Plus one empty milestone commit. |
| 3 | Primitives | No new primitives; reuse existing Fältet set. |

---

## 4. Per-item specifications

### 4.1 AddPlantEvent — restore AI identification

**File:** `ui/plant/AddPlantEventScreen.kt`

**What was dropped in C2:** After capturing a front photo, `viewModel.identifyPlant()` was called; results populated `uiState.suggestions: List<PlantSuggestion>`. The C2 port preserved the VM state fields but dropped the UI (no spinner, no suggestion cards).

**Restore pattern — follow C4 AddSpecies:**

1. **Photo callback** — inside the front `FaltetImagePicker`'s `onValueChange`, when a new bitmap arrives:
   ```kotlin
   onValueChange = { bitmap ->
       photoBitmap = bitmap
       if (bitmap != null) {
           val b64 = bitmap.toCompressedBase64()
           imageBase64 = b64
           viewModel.identifyPlant(b64)
       } else {
           imageBase64 = null
       }
   }
   ```

2. **AI spinner row** — after the photo/upload slot, when `uiState.identifying`:
   ```kotlin
   if (uiState.identifying) {
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
                   text = "Identifierar…",
                   fontFamily = FontFamily.Monospace,
                   fontSize = 10.sp,
                   letterSpacing = 1.2.sp,
                   color = FaltetForest,
               )
           }
       }
   }
   ```

3. **Suggestion cards** — when `uiState.suggestions.isNotEmpty()`:
   ```kotlin
   if (uiState.suggestions.isNotEmpty()) {
       item { FaltetSectionHeader(label = "Förslag") }
       items(uiState.suggestions, key = { it.species }) { suggestion ->
           SuggestionRow(suggestion = suggestion)
       }
   }
   ```

4. **`SuggestionRow` composable** (copy from AddSpeciesScreen or define locally):

   ```kotlin
   @Composable
   private fun SuggestionRow(
       suggestion: PlantSuggestion,
       onTap: (() -> Unit)? = null,
   ) {
       Row(
           verticalAlignment = Alignment.CenterVertically,
           modifier = Modifier
               .fillMaxWidth()
               .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
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

   **Tap behavior:** the implementer should check the original (pre-C2-port) behavior. Since AddPlantEvent is for a specific plant (species already known), tapping a suggestion likely didn't auto-populate anything — the cards were informational/confirmation. Pass `onTap = null` if that's the case; leave non-clickable.

5. **VM error surfacing:** if `identifyPlant()` doesn't already set `error` on failure, add `error = "Kunde inte identifiera bilden"` to its catch block (same as C4 Task 4 did for AddSpecies).

### 4.2 ApplySupply — restore "Visa alla" toggle

**File:** `ui/activity/ApplySupplyScreen.kt`

**What was dropped in C3:** The VM state holds `showAllCategories: Boolean` (filter toggle). The pre-port UI had a checkbox; the C3 port kept the state but removed the UI. Supply dropdown now always filters to fertilizer-only.

**Restore — checkbox-row above the supply dropdown:**

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
            onCheckedChange = null,  // row-level click handles it
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

**Adaptations:**
- Confirm the actual VM state field name (`showAllCategories` / `showAllSupplies` / etc.) and setter method name by reading the current file.
- If the existing dropdown `options` list is already derived from `uiState.showAllCategories` via a `remember` / filter expression, the toggle just drives that filter — no change to dropdown code.

### 4.3 Account — surface VM errors via snackbar

**File:** `ui/account/AccountScreen.kt`

**What was dropped in D:** The pre-port had a `ConnectionErrorState` branch rendering when `uiState.error != null`. The D port removed it, leaving VM errors silently swallowed.

**Restore via snackbar:**

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(uiState.error) {
    uiState.error?.let { snackbarHostState.showSnackbar(it) }
}

FaltetScreenScaffold(
    mastheadLeft = "§ Konto",
    mastheadCenter = "Konto",
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { padding ->
    // existing body unchanged
}
```

Errors surface transiently — consistent with C1–D patterns. No full-screen error replacement.

### 4.4 GardenDetail — migrate edit dialog

**File:** `ui/garden/GardenDetailScreen.kt`

**Current state:** Private `EditGardenDialog` composable uses `OutlinedTextField` for name / description / emoji.

**Migrate** each `OutlinedTextField` to `Field`:

```kotlin
// Before:
OutlinedTextField(
    value = name,
    onValueChange = { name = it },
    label = { Text("Namn") },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
)

// After:
Field(
    label = "Namn",
    value = name,
    onValueChange = { name = it },
    required = true,
)
```

Repeat for description and emoji fields. Drop `OutlinedTextField` + `RoundedCornerShape` imports. Keep the `AlertDialog` chrome intact.

### 4.5 BedDetail — migrate edit dialog

**File:** `ui/bed/BedDetailScreen.kt`

**Current state:** Edit dialog uses `OutlinedTextField` for name/description and inline `FilterChip` rows for bed conditions.

**Migrate:**

- Text fields: `OutlinedTextField` → `Field`.
- Enum chip rows: replace the inline `FlowRow { FilterChip(...) }` pattern with `FaltetChipSelector` (single-select nullable). Reuse non-composable label helpers (`bedSoilTypeLabelStr`, `bedDrainageLabelStr`, etc.) added during Spec C2.

Example chip migration:

```kotlin
// Before:
FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    BedSoilType.values.forEach { type ->
        FilterChip(
            selected = soilType == type,
            onClick = { soilType = if (soilType == type) null else type },
            label = { Text(bedSoilTypeLabel(type)) },
        )
    }
}

// After:
FaltetChipSelector(
    label = "Jordtyp",
    options = BedSoilType.values,
    selected = soilType,
    onSelectedChange = { soilType = it },
    labelFor = { bedSoilTypeLabelStr(it) },
)
```

Apply to each condition: Jordtyp, Dränering, Sol, Väderstreck, Bevattning, Skydd.

### 4.6 AddSpecies — group dropdown + create-group dialog

**File:** `ui/activity/AddSpeciesScreen.kt`

**Feature:** Add the UI entry point for selecting / creating a group (VM method `createGroup()` exists; `uiState.groups` is populated; `SpeciesResponse.groupId` is a real field).

**State additions** (alongside existing state near the top of the composable):

```kotlin
var selectedGroupId by remember { mutableStateOf<Long?>(null) }
var showNewGroupDialog by remember { mutableStateOf(false) }
var newGroupName by remember { mutableStateOf("") }
```

**Pre-fill** (in `LaunchedEffect(uiState.existingSpecies)`, inside the existing pre-fill block):

```kotlin
selectedGroupId = s.groupId
```

**`selectedGroup` derived at the field boundary** (no separate state var needed):

```kotlin
val selectedGroup = uiState.groups.find { it.id == selectedGroupId }
```

**Dirty detection** — add to both `hasData` and `hasChanges` expressions:

- `hasData`: add `|| selectedGroupId != null`
- `hasChanges` (edit mode): add `|| selectedGroupId != s.groupId`

**Group dropdown item** — insert **before** the Taggar (tags) section in the LazyColumn:

```kotlin
item {
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

**Create-group dialog** (model after existing create-tag dialog):

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

**Submit** — add `groupId = selectedGroupId` to both `CreateSpeciesRequest` and `UpdateSpeciesRequest` builders in the `submitAction` block.

**Adaptation:** if `CreateSpeciesRequest` / `UpdateSpeciesRequest` don't have a `groupId` field, add it to those model classes (minimal data-model change — one nullable Long per request) OR check what the existing (pre-port) code did. The VM already calls `repo.createSpecies(request)` and `repo.updateSpecies(id, request)` — the repo / backend API presumably accepts `groupId`; this is just wiring through.

---

## 5. Phasing

### Phase 1 — Regression restores (3 commits)

1. AddPlantEvent AI identification restoration.
2. ApplySupply "Visa alla" toggle.
3. Account snackbar error surfacing.

### Phase 2 — Primitive migration + feature (3 commits)

4. GardenDetail edit dialog → `Field`.
5. BedDetail edit dialog → `Field` + `FaltetChipSelector`.
6. AddSpecies group dropdown + create-group dialog.

### Phase 3 — Verify + milestone (1 empty commit)

7. `./gradlew assembleDebug` green. Manual smoke on 6 scenarios. Empty milestone commit:

```
milestone: Android Fältet polish pass complete
```

**Total: 7 tasks, 6 code commits + 1 milestone.**

---

## 6. Testing

### Per task

- `./gradlew compileDebugKotlin` green before commit.
- No new `@Preview`s required — each change is a targeted edit to an existing file.

### Manual smoke at Phase 3 — 6 scenarios

1. **AddPlantEvent AI** — open AddPlantEvent for a plant; take or pick a front photo; verify "Identifierar…" spinner, then suggestion cards under "Förslag" with confidence percentages.

2. **ApplySupply toggle** — open ApplySupply; verify "Visa alla kategorier" checkbox above supply dropdown; toggle on → dropdown shows all supply categories; toggle off → only fertilizer.

3. **Account errors** — trigger a VM error (offline + attempt a VM action that hits network); verify snackbar appears in Swedish.

4. **GardenDetail edit** — tap edit on a garden; verify dialog shows Fältet `Field`s (mono uppercase label, italic Fraunces value, 1dp underline) instead of Material outlined boxes.

5. **BedDetail edit** — tap edit on a bed; verify `Field`s for text + `FaltetChipSelector`s for each condition group (Jordtyp, Sol, etc.) with clay selected-state.

6. **AddSpecies group** — open AddSpecies (create mode); verify "Grupp (valfri)" dropdown appears before Taggar; verify "+ NY GRUPP" affordance; tap it, enter a name, tap Skapa; verify new group appears in dropdown on next list refresh.

### Not added

- No snapshot tests.
- No new instrumented UI tests.
- No VM test changes (no VM signature changes).

### Known non-issue

First cold install flashes system-serif before Fraunces arrives via Downloadable Fonts.

---

## 7. Completion state

After this spec ships, the Android Fältet project is:

- ✅ Spec A — Foundation
- ✅ Spec B — 12 list screens
- ✅ Spec C1 — 4 detail screens
- ✅ Spec C2 — 5 form screens
- ✅ Spec C3 — 6 activity screens
- ✅ Spec C4 — AddSpecies
- ✅ Spec D — Splash + Auth + Analytics + Account + SeasonSelector
- ✅ Polish Pass — 4 regressions restored + group dropdown shipped

Every top-level screen is Fältet-styled and feature-complete relative to pre-port state plus the group-dropdown addition.
