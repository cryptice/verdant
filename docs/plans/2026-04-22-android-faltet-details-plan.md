# Android Fältet — Sub-Spec C1 (Details) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port 4 Android detail screens (`GardenDetail`, `BedDetail`, `PlantDetail`, `PlantedSpeciesDetail`) to the Fältet editorial aesthetic using 2 new primitives (`FaltetMetadataRow`, `FaltetHero`) composed with existing Spec A + Spec B primitives.

**Architecture:** Vertical slice. Phase 1 ships `FaltetMetadataRow` and `FaltetHero` with `@Preview`s. Phase 2 ports `GardenDetail` (smallest) to validate the primitive API. Phase 3 batches the remaining three ports (`BedDetail`, `PlantDetail`, `PlantedSpeciesDetail`). Phase 4 runs `assembleDebug` and creates an empty milestone commit. Every screen uses `FaltetScreenScaffold` + `FaltetHero` + `LazyColumn` with `FaltetSectionHeader` + `FaltetListRow` / `FaltetMetadataRow`. Edit/delete affordances live in the masthead `right` slot as `IconButton`s tinted `FaltetClay`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Coil (for `AsyncImage` on `PlantDetail`), existing `app.verdant.android.ui.theme` tokens, existing `ui/faltet/` primitives from Spec A + Spec B.

**Spec:** `docs/plans/2026-04-22-android-faltet-details-design.md` — read this before starting.

**Reality-check notes:**
- Android detail screens have **no pagination**; do not add any.
- This is a rendering port — preserve every ViewModel, state shape, navigation callback, existing dialog, and business behavior verbatim. Only the visible `Scaffold` body and the helper composables within the file change.
- Swedish copy is authored per-screen in this plan. Do not invent copy — use what's specified below.
- Where the plan references model field names (e.g., `bed.soilType`), the engineer must confirm the actual field name by reading the current file. If the model differs, adapt the field list without inventing values. If a referenced field does not exist, omit that row.

---

## File Structure

### New files (under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`)

- `FaltetMetadataRow.kt` — horizontal mono-label / italic-value row with hairline bottom
- `FaltetHero.kt` — 180dp hero row with optional 140dp leading content slot

### Modified files (4 screens)

- `android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/plant/PlantDetailScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/plants/PlantedSpeciesDetailScreen.kt`

### Shared patterns (copied inline into each ported screen)

Each ported screen's file contains:
- An `InlineEmpty` private composable (same code across all 4 files).
- A confirm-delete `AlertDialog` when the ViewModel exposes a delete operation.

These are tiny helpers — don't extract them to a shared file. Keeping them local keeps the port self-contained.

---

## Phase 1 — Primitives

Two small files. Compile-green gate after each. No screen consumes them yet.

---

### Task 1: `FaltetMetadataRow`

Horizontal label/value row for metadata sections.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetMetadataRow.kt`

- [ ] **Step 1: Write the primitive**

Create `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetMetadataRow.kt` with:

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetMetadataRow.kt
package app.verdant.android.ui.faltet

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetMetadataRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    valueAccent: FaltetTone? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = FaltetForest,
            modifier = Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(12.dp))
        val valueColor = valueAccent?.color() ?: FaltetInk
        Text(
            text = value ?: "—",
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            color = if (value == null) FaltetForest.copy(alpha = 0.4f) else valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetMetadataRowPreview_Populated() {
    FaltetMetadataRow("Jordtyp", "Lerig mylla")
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetMetadataRowPreview_Null() {
    FaltetMetadataRow("pH", null)
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetMetadataRowPreview_Accent() {
    FaltetMetadataRow("Status", "Skördad", valueAccent = FaltetTone.Clay)
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetMetadataRow.kt
git commit -m "feat: FaltetMetadataRow primitive"
```

---

### Task 2: `FaltetHero`

180dp hero row with a 140dp leading Box for image/emoji/placeholder.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetHero.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetHero.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetHero(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable BoxScope.() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(FaltetCream)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        if (leading != null) {
            Box(modifier = Modifier.size(140.dp)) { leading() }
            Spacer(Modifier.width(20.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 32.sp,
                color = FaltetInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = FaltetForest,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetHeroPreview_WithPhotoPlaceholder() {
    FaltetHero(
        title = "Vildblommor",
        subtitle = "Lerig mylla · Sydvänd",
        leading = {
            PhotoPlaceholder(
                label = "Vildblommor",
                tone = PhotoTone.Sage,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetHeroPreview_WithEmoji() {
    FaltetHero(
        title = "Villan",
        subtitle = "Hemträdgården",
        leading = {
            Text(
                text = "🌱",
                fontSize = 64.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetHeroPreview_NoLeading() {
    FaltetHero(title = "Cosmos bipinnatus", subtitle = "84 plantor")
}
```

**Note:** `PhotoPlaceholder` uses `.fillMaxWidth().aspectRatio(...)` internally, so passing `Modifier.fillMaxSize()` gives it the full 140dp box. This works because the outer `Box` has fixed size.

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetHero.kt
git commit -m "feat: FaltetHero primitive"
```

---

## Phase 2 — Reference port

Validate the primitive API against the smallest detail screen before batching the other three.

---

### Task 3: Port `GardenDetailScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt`

**Context for the agent:**
- Current file (~317 lines) has: `BedConditionChips` private composable, `GardenDetailState`, `GardenDetailViewModel`, and `GardenDetailScreen` composable with TopAppBar + `OutlinedTextField` edit dialog + LazyColumn of beds.
- Preserve verbatim: `GardenDetailState`, `GardenDetailViewModel` (entire `@HiltViewModel class`), the edit-dialog state machinery and `UpdateGardenRequest` wiring, and any navigation callbacks in the screen signature.
- Replace: the `Scaffold { TopAppBar + LazyColumn + Card }` body. Also delete the `BedConditionChips` helper — in the port, per-bed conditions are expressed as `FaltetChip`s in the `stat` slot (or dropped if too noisy; see row mapping below).
- No pagination.

**Masthead:** `mastheadLeft = "§ Trädgård"`, `mastheadCenter = garden.name`.

**Masthead `right`:** two `IconButton`s for edit + delete, tint `FaltetClay`, 36dp each. Edit opens the existing edit dialog; delete opens a confirm `AlertDialog`.

**FAB:** `FaltetFab(onClick = onCreateBed, contentDescription = "Skapa bädd")`.

**Hero:**
- `title = garden.name`
- `subtitle = garden.description`
- `leading`: centered emoji, 64sp — `Text(garden.emoji ?: "🌱", fontSize = 64.sp, modifier = Modifier.align(Alignment.Center))`

**Section — Bäddar (list):**
- Header: `FaltetSectionHeader("Bäddar")`.
- Populated: one `FaltetListRow` per bed.
  - `title = bed.name`
  - `meta = "${bed.plantCount} plantor${bed.bedSize?.let { " · $it" } ?: ""}"`
  - `stat = null`
  - `onClick = { onBedClick(bed.id) }`
- Empty: `InlineEmpty("Inga bäddar ännu. Tryck + för att skapa.")`

**Delete confirm dialog** — shown when `showDeleteDialog` is true. Text: `"Vill du ta bort trädgården \"${garden.name}\"?"`. Confirm label `"Ta bort"` in `FaltetClay`; dismiss `"Avbryt"`. On confirm, call the existing delete VM method and then `onBack()`.

**Edit dialog:** keep the existing edit `AlertDialog` body verbatim. Swap any `R.string.*` references for the Swedish literals used by the current dialog. If the existing dialog uses `OutlinedTextField`, that's fine — leave it; Fältet form styling belongs to C2.

- [ ] **Step 1: Read the current file**

```bash
wc -l /Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt
```

Read the whole file to understand: the exact ViewModel signature (particularly methods like `updateGarden`, `deleteGarden`), the current `GardenDetailScreen(...)` composable signature and its callbacks (likely `onBack`, `onBedClick`, `onCreateBed`, possibly an `onEditBed`), and the existing edit-dialog state.

- [ ] **Step 2: Replace the imports block**

Replace from `package app.verdant.android.ui.garden` through the last import with:

```kotlin
package app.verdant.android.ui.garden

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BedResponse
import app.verdant.android.data.model.GardenResponse
import app.verdant.android.data.model.UpdateGardenRequest
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetHero
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
```

Drop any remaining imports that are unused after the port (likely: `Icons.AutoMirrored.Filled.ArrowBack`, `RoundedCornerShape`, `Card`, `CardDefaults`, `TopAppBar`, `stringResource`, `verdantTopAppBarColors`, `app.verdant.android.R`, `bedDrainageLabel`, `bedProtectionLabel`, `bedSunExposureLabel`, `FontWeight`). Also delete the `BedConditionChips` private composable.

- [ ] **Step 3: Preserve `GardenDetailState` and `GardenDetailViewModel`**

Keep both exactly as they exist in the current file. Do not modify any method signatures or behavior.

- [ ] **Step 4: Replace the `GardenDetailScreen` composable**

Replace from `@OptIn(ExperimentalMaterial3Api::class) @Composable fun GardenDetailScreen(...)` through the end of the composable (and through the end of `BedConditionChips` if it was below it) with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenDetailScreen(
    onBack: () -> Unit,
    onBedClick: (Long) -> Unit,
    onCreateBed: () -> Unit,
    viewModel: GardenDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onBack()
    }

    if (showEditDialog && uiState.garden != null) {
        EditGardenDialog(
            garden = uiState.garden!!,
            onDismiss = { showEditDialog = false },
            onSave = { name, description, emoji ->
                viewModel.updateGarden(UpdateGardenRequest(name = name, description = description, emoji = emoji))
                showEditDialog = false
            },
        )
    }

    if (showDeleteDialog && uiState.garden != null) {
        val gardenName = uiState.garden!!.name
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Ta bort trädgård") },
            text = { Text("Vill du ta bort trädgården \"${gardenName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGarden()
                    showDeleteDialog = false
                }) { Text("Ta bort", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Avbryt") }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Trädgård",
        mastheadCenter = uiState.garden?.name ?: "",
        mastheadRight = {
            if (uiState.garden != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Redigera", tint = FaltetClay, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, "Ta bort", tint = FaltetClay, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        fab = { FaltetFab(onClick = onCreateBed, contentDescription = "Skapa bädd") },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.garden == null -> FaltetEmptyState(
                headline = "Trädgården hittades inte",
                subtitle = "Trädgården kan ha tagits bort.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                val garden = uiState.garden!!
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        FaltetHero(
                            title = garden.name,
                            subtitle = garden.description,
                            leading = {
                                Text(
                                    text = garden.emoji ?: "🌱",
                                    fontSize = 64.sp,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            },
                        )
                    }
                    item { FaltetSectionHeader(label = "Bäddar") }
                    if (uiState.beds.isEmpty()) {
                        item { InlineEmpty("Inga bäddar ännu. Tryck + för att skapa.") }
                    } else {
                        items(uiState.beds, key = { it.id }) { bed ->
                            FaltetListRow(
                                title = bed.name,
                                meta = buildString {
                                    append("${bed.plantCount} plantor")
                                    bed.bedSize?.let { append(" · $it") }
                                },
                                onClick = { onBedClick(bed.id) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun InlineEmpty(text: String) {
    Text(
        text = text,
        fontFamily = FaltetDisplay,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        color = FaltetForest,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
    )
}
```

**Note on `EditGardenDialog`:** keep whatever private composable the existing file uses to render the edit dialog (likely named `EditGardenDialog` or an inline `AlertDialog`). If the existing file has an inline dialog without a helper, extract it to a `private @Composable fun EditGardenDialog(...)` with the signature shown above. Preserve all existing dialog logic — only rename the outer wrapper and make sure the copy is Swedish.

**If ViewModel method names differ** (e.g., `updateGarden` vs `update` vs `save`): use whatever the current ViewModel exposes. Don't rename VM methods. If fields like `garden.bedSize` don't exist on `GardenResponse`'s `BedResponse`, drop the `· $it` segment.

- [ ] **Step 5: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 6: Add a `@Preview`**

Append to the end of the file:

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun InlineEmptyPreview() {
    InlineEmpty("Inga bäddar ännu. Tryck + för att skapa.")
}
```

A full-screen preview requires mocking a Hilt VM and is high-friction; the inline-empty preview is enough to exercise the new Fältet styling in the Preview pane.

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt
git commit -m "feat: Fältet port — GardenDetail"
```

- [ ] **Step 8: Phase 2 checkpoint**

Install + smoke:

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Open GardenDetail on emulator. Verify:
- Masthead shows `§ Trädgård` left, garden name center (Fraunces italic), edit + delete icons right (clay).
- Hero shows large emoji on left, Fraunces 32sp title, optional description below.
- Bäddar section header with clay underline.
- Bed rows hairline-separated; tap navigates to BedDetail.
- Empty bed state shows italic "Inga bäddar ännu. Tryck + för att skapa."
- FAB present bottom-right, clay on ink.
- Edit + delete dialogs open and work; Swedish copy.

If the `FaltetHero` / `FaltetMetadataRow` APIs feel wrong, amend the primitive files in new commits (no history rewrite) and fix GardenDetail before Phase 3.

---

## Phase 3 — Batch ports

Each task reads its existing screen in full, rewrites the rendering while preserving VM/state/callbacks, commits. Compile-green gate before each commit.

---

### Task 4: Port `BedDetailScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailScreen.kt`

**Context for the agent:**
- Largest screen (~551 lines). Has expandable condition groups, plants list, supply-applications list, edit & delete affordances, plus helper label functions (`bedSoilTypeLabel`, `bedSunExposureLabel`, `bedDrainageLabel`, `bedProtectionLabel`, `bedAspectLabel`, `bedWateringLabel` — names vary; these are re-exported and used by GardenDetail too). Preserve these label functions — they are reused.
- ViewModel (`BedDetailViewModel`) and state (`BedDetailState`) preserved verbatim.
- No pagination.

**Reference port (for structural conventions):** `android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt` after Task 3.

**Masthead:** `mastheadLeft = "§ Bädd"`, `mastheadCenter = bed.name`.

**Masthead `right`:** edit + delete `IconButton`s, same pattern as GardenDetail.

**FAB:** omit unless the current screen has an "add plant to bed" primary action already. If it does, wire `FaltetFab(onClick = onAddPlantToBed, contentDescription = "Lägg till planta")`.

**Hero:**
- `title = bed.name`
- `subtitle = bed.description`
- `leading`:
```kotlin
leading = {
    PhotoPlaceholder(
        label = bed.name,
        tone = PhotoTone.Sage,
        modifier = Modifier.fillMaxSize(),
    )
}
```

**Delete confirm dialog:** `"Ta bort bädd"` title, `"Vill du ta bort bädden \"${bed.name}\"?"` body, `"Ta bort"` confirm in `FaltetClay`, `"Avbryt"` dismiss. On confirm call the VM's delete method and `onBack()`.

**Section — Villkor (metadata with expandable groups):**

Build four sub-groups. Each sub-group uses an expand-collapse pattern: the top row is a `FaltetListRow` showing a compact summary; tapping expands to reveal `FaltetMetadataRow`s.

```kotlin
@Composable
private fun ConditionGroup(
    title: String,
    summary: String?,
    fields: List<Pair<String, String?>>,
) {
    if (fields.all { it.second == null }) return  // suppress whole group when fully empty
    var expanded by remember { mutableStateOf(false) }
    Column {
        FaltetListRow(
            title = title,
            meta = summary,
            actions = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Dölj" else "Visa",
                    tint = FaltetForest,
                    modifier = Modifier.size(18.dp),
                )
            },
            onClick = { expanded = !expanded },
        )
        AnimatedVisibility(visible = expanded) {
            Column {
                fields.forEach { (label, value) ->
                    if (value != null) {
                        FaltetMetadataRow(label = label, value = value)
                    }
                }
            }
        }
    }
}
```

**Sub-group definitions** (adapt field names to the actual `BedResponse` model; read the file to confirm):

```kotlin
// Inside the scaffold content, after the hero:
val hasAnyCondition = listOf(
    bed.soilType, bed.soilPh?.toString(), bed.drainage, bed.raisedBed,
    bed.sunExposure, bed.aspect, bed.windProtection,
    bed.irrigation, bed.irrigationFrequency,
    bed.protection, bed.diseasePresure,
).any { it != null }
if (hasAnyCondition) {
    item { FaltetSectionHeader(label = "Villkor") }
    item {
        ConditionGroup(
            title = "Jord",
            summary = listOfNotNull(
                bed.soilType?.let { bedSoilTypeLabel(it) },
                bed.soilPh?.let { "pH $it" },
            ).joinToString(" · ").takeIf { it.isNotBlank() },
            fields = listOf(
                "Jordtyp" to bed.soilType?.let { bedSoilTypeLabel(it) },
                "pH" to bed.soilPh?.toString(),
                "Dränering" to bed.drainage?.let { bedDrainageLabel(it) },
                "Upphöjd bädd" to bed.raisedBed?.let { if (it) "Ja" else "Nej" },
            ),
        )
    }
    item {
        ConditionGroup(
            title = "Exponering",
            summary = listOfNotNull(
                bed.sunExposure?.let { bedSunExposureLabel(it) },
                bed.aspect?.let { bedAspectLabel(it) },
            ).joinToString(" · ").takeIf { it.isNotBlank() },
            fields = listOf(
                "Sol" to bed.sunExposure?.let { bedSunExposureLabel(it) },
                "Väderstreck" to bed.aspect?.let { bedAspectLabel(it) },
                "Vindskydd" to bed.windProtection?.let { if (it) "Ja" else "Nej" },
            ),
        )
    }
    item {
        ConditionGroup(
            title = "Bevattning",
            summary = bed.irrigation?.let { bedWateringLabel(it) },
            fields = listOf(
                "Bevattning" to bed.irrigation?.let { bedWateringLabel(it) },
                "Frekvens" to bed.irrigationFrequency,
            ),
        )
    }
    item {
        ConditionGroup(
            title = "Skydd",
            summary = bed.protection?.let { bedProtectionLabel(it) },
            fields = listOf(
                "Skydd" to bed.protection?.let { bedProtectionLabel(it) },
                "Sjukdomstryck" to bed.diseasePresure,
            ),
        )
    }
}
```

If any field referenced above (e.g., `bed.raisedBed`, `bed.windProtection`) does not exist on the actual `BedResponse`, drop that `Pair` from the `fields` list. Do not invent model fields.

**Section — Plantor (list):**
- Header: `FaltetSectionHeader("Plantor")`
- Populated: `FaltetListRow` per plant with:
  - `title = "${plant.commonName}${plant.variantName?.let { " $it" } ?: ""}"`
  - `meta = "${statusLabelSv(plant.status)} · ${formattedDate(plant.plantedDate)}"`
  - `stat` = count when `plant.count > 1` (mono split "N STK"), otherwise null
  - `onClick = { onPlantClick(plant.id) }`
- Empty: `InlineEmpty("Inga plantor ännu.")`

Add helpers at the bottom of the file:

```kotlin
private fun statusLabelSv(status: String?): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Krukad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    null -> "—"
    else -> status
}

private fun formattedDate(date: String?): String {
    if (date == null) return "—"
    return try {
        val parsed = java.time.LocalDate.parse(date)
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

**Section — Gödsling & vatten (list):**
- Header: `FaltetSectionHeader("Gödsling & vatten")`
- Populated: `FaltetListRow` per supply application with:
  - `title = application.supplyName` (or whatever the existing model calls the supply-type name)
  - `meta = "${formattedDate(application.appliedDate)}${application.method?.let { " · $it" } ?: ""}"`
  - `stat` = quantity + unit as mono text (`Text("${application.quantity} ${application.unit}", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = FaltetInk)`)
  - `onClick = null`
- Empty: `InlineEmpty("Inga gödslingar ännu.")`

- [ ] **Step 1: Read the current `BedDetailScreen.kt` in full**

Note: `BedResponse` field names, ViewModel methods (especially delete), screen signature callbacks, existing label helper names (they are `@Composable`? Plain functions? Check), existing dialogs.

- [ ] **Step 2: Replace imports**

Replace the import block with Fältet imports (following Task 3's pattern), adding:
```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import app.verdant.android.ui.faltet.FaltetMetadataRow
import app.verdant.android.ui.faltet.PhotoPlaceholder
import app.verdant.android.ui.faltet.PhotoTone
import app.verdant.android.ui.theme.FaltetInk
import androidx.compose.ui.text.font.FontFamily
```

Keep existing label helper imports. Drop `TopAppBar`, `Card`, `CardDefaults`, `RoundedCornerShape`, `verdantTopAppBarColors`, `stringResource`, `R`, `FontWeight` if unused after port.

- [ ] **Step 3: Preserve state + ViewModel + label helpers**

Do not modify. Label helpers (`bedSoilTypeLabel`, etc.) stay as-is.

- [ ] **Step 4: Replace the `BedDetailScreen` composable**

Use the GardenDetail port as a structural reference: same scaffold outline, same `showEditDialog` / `showDeleteDialog` state pattern. The body's `else ->` branch contains the `LazyColumn` with hero + Villkor section + Plantor section + Gödsling section, per the pattern above.

- [ ] **Step 5: Add the private `ConditionGroup` + `InlineEmpty` composables + date/status helpers**

Insert at bottom of the file.

- [ ] **Step 6: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Must exit 0.

- [ ] **Step 7: Add a `@Preview`**

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun ConditionGroupPreview() {
    ConditionGroup(
        title = "Jord",
        summary = "Lerig mylla · pH 6.5",
        fields = listOf(
            "Jordtyp" to "Lerig mylla",
            "pH" to "6.5",
            "Dränering" to "Bra",
            "Upphöjd bädd" to "Ja",
        ),
    )
}
```

- [ ] **Step 8: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailScreen.kt
git commit -m "feat: Fältet port — BedDetail"
```

---

### Task 5: Port `PlantDetailScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/plant/PlantDetailScreen.kt`

**Context for the agent:**
- Current file (~386 lines) has Coil `AsyncImage` hero, workflow progress card, and event history list. Preserve ViewModel (`PlantDetailViewModel`) and state verbatim.
- The event model fields (date, type, notes, count, weight) need confirming from the current file.
- No pagination.

**Masthead:** `mastheadLeft = "§ Planta"`, `mastheadCenter = "${plant.commonName}${plant.variantName?.let { " $it" } ?: ""}"`.

**Masthead `right`:** edit + delete `IconButton`s. Edit likely navigates to an edit route (e.g., `onEditPlant(plant.id)`). Delete opens a confirm dialog.

**FAB:** if the current screen has an "add event" primary action (very likely — AddPlantEvent is a core workflow): `FaltetFab(onClick = { onAddEvent(plant.id) }, contentDescription = "Lägg till händelse")`.

**Hero:**
- `title = "${plant.commonName}${plant.variantName?.let { " $it" } ?: ""}"`
- `subtitle` = build from scientific name + bed: `listOfNotNull(plant.speciesName?.takeIf { it.isNotBlank() }, plant.bedName?.let { "Bädd: $it" }).joinToString(" · ").takeIf { it.isNotBlank() }`
- `leading`:
```kotlin
leading = {
    if (plant.photoUrl != null) {
        coil.compose.AsyncImage(
            model = plant.photoUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
    } else {
        PhotoPlaceholder(
            label = plant.commonName,
            tone = PhotoTone.Blush,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

Add imports as needed:
```kotlin
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
```

If `plant.photoUrl` isn't the correct field name, adapt.

**Workflow progress strip** (rendered between hero and event-history section, no section header):

```kotlin
item {
    val stages = listOf("SÅDD", "KRUKAD", "UTPLANTERAD", "SKÖRDAD")
    val currentIdx = when (plant.status) {
        "SEEDED" -> 0
        "POTTED_UP" -> 1
        "PLANTED_OUT", "GROWING" -> 2
        "HARVESTED" -> 3
        else -> -1
    }
    Text(
        text = androidx.compose.ui.text.buildAnnotatedString {
            stages.forEachIndexed { i, stage ->
                val color = when {
                    i < currentIdx -> FaltetInk
                    i == currentIdx -> FaltetClay
                    else -> FaltetForest.copy(alpha = 0.4f)
                }
                withStyle(androidx.compose.ui.text.SpanStyle(color = color)) { append(stage) }
                if (i < stages.size - 1) {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = FaltetForest.copy(alpha = 0.4f))) {
                        append("  ·  ")
                    }
                }
            }
        },
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
    )
}
```

Imports:
```kotlin
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
```

**Section — Händelser (event history, grouped by month):**

Pre-compute before the `LazyColumn`:

```kotlin
val eventsByMonth: List<Pair<java.time.YearMonth, List<PlantEventResponse>>> = remember(uiState.events) {
    uiState.events
        .sortedByDescending { it.eventDate }
        .groupBy {
            try {
                java.time.YearMonth.from(java.time.LocalDate.parse(it.eventDate))
            } catch (e: Exception) {
                java.time.YearMonth.now()
            }
        }
        .toList()
}
```

Replace `PlantEventResponse` and `eventDate` with the actual model/field names.

Render:

```kotlin
if (eventsByMonth.isEmpty()) {
    item { FaltetSectionHeader(label = "Händelser") }
    item { InlineEmpty("Inga händelser ännu.") }
} else {
    eventsByMonth.forEach { (yearMonth, events) ->
        item(key = "header_${yearMonth}") {
            FaltetSectionHeader(label = monthLabelSv(yearMonth))
        }
        items(events, key = { it.id }) { event ->
            FaltetListRow(
                leading = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .drawBehind { drawCircle(eventToneColor(event.type)) },
                    )
                },
                title = eventTypeLabelSv(event.type),
                meta = buildString {
                    append(formattedDate(event.eventDate))
                    event.notes?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                },
                metaMaxLines = 2,
                stat = {
                    val statText = listOfNotNull(
                        event.count?.takeIf { it > 0 }?.let { "$it" },
                        event.weightGrams?.takeIf { it > 0 }?.let { "${it.toInt()}g" },
                    ).joinToString(" · ")
                    if (statText.isNotBlank()) {
                        Text(
                            text = statText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = FaltetInk,
                        )
                    }
                },
            )
        }
    }
}
```

Helpers at file bottom:

```kotlin
private fun eventToneColor(type: String?): androidx.compose.ui.graphics.Color = when (type) {
    "SEEDED", "SOWED" -> FaltetMustard
    "POTTED_UP" -> FaltetSky
    "PLANTED_OUT" -> FaltetSage
    "HARVESTED" -> FaltetClay
    "FERTILIZED" -> FaltetBerry
    "WATERED" -> FaltetSky
    "NOTE" -> FaltetForest
    else -> FaltetForest
}

private fun eventTypeLabelSv(type: String?): String = when (type) {
    "SEEDED", "SOWED" -> "Sådd"
    "POTTED_UP" -> "Krukad"
    "PLANTED_OUT" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    "FERTILIZED" -> "Gödslad"
    "WATERED" -> "Vattnad"
    "NOTE" -> "Anteckning"
    null -> "—"
    else -> type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

private fun formattedDate(date: String?): String {
    if (date == null) return "—"
    return try {
        val parsed = java.time.LocalDate.parse(date)
        "${parsed.dayOfMonth} ${monthShortSv(parsed.monthValue)}"
    } catch (e: Exception) {
        date
    }
}

private fun monthShortSv(month: Int): String = arrayOf(
    "jan", "feb", "mar", "apr", "maj", "jun",
    "jul", "aug", "sep", "okt", "nov", "dec",
)[month - 1]

private fun monthLabelSv(yearMonth: java.time.YearMonth): String {
    val names = arrayOf(
        "Januari", "Februari", "Mars", "April", "Maj", "Juni",
        "Juli", "Augusti", "September", "Oktober", "November", "December",
    )
    return "${names[yearMonth.monthValue - 1]} ${yearMonth.year}"
}
```

Add palette imports as needed:
```kotlin
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky
```

- [ ] **Step 1: Read the current file in full**
- [ ] **Step 2: Replace imports (prune TopAppBar / Card / stringResource; add Fältet + Coil + time + tone imports)**
- [ ] **Step 3: Preserve state + ViewModel verbatim**
- [ ] **Step 4: Replace `PlantDetailScreen` composable body following the GardenDetail pattern + hero + workflow strip + grouped event history**
- [ ] **Step 5: Add helper functions at bottom (`eventToneColor`, `eventTypeLabelSv`, `formattedDate`, `monthShortSv`, `monthLabelSv`, `InlineEmpty`)**
- [ ] **Step 6: Compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 7: Add a `@Preview`**

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun WorkflowStripPreview() {
    Text(
        text = androidx.compose.ui.text.buildAnnotatedString {
            val stages = listOf("SÅDD", "KRUKAD", "UTPLANTERAD", "SKÖRDAD")
            val currentIdx = 2
            stages.forEachIndexed { i, stage ->
                val color = when {
                    i < currentIdx -> FaltetInk
                    i == currentIdx -> FaltetClay
                    else -> FaltetForest.copy(alpha = 0.4f)
                }
                withStyle(androidx.compose.ui.text.SpanStyle(color = color)) { append(stage) }
                if (i < stages.size - 1) {
                    withStyle(androidx.compose.ui.text.SpanStyle(color = FaltetForest.copy(alpha = 0.4f))) {
                        append("  ·  ")
                    }
                }
            }
        },
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
    )
}
```

- [ ] **Step 8: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/plant/PlantDetailScreen.kt
git commit -m "feat: Fältet port — PlantDetail"
```

---

### Task 6: Port `PlantedSpeciesDetailScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/plants/PlantedSpeciesDetailScreen.kt`

**Context for the agent:**
- Current file (~582 lines) is an aggregated species view with status-grouped plant list and drill-down to individual plants.
- Preserve ViewModel + state + callbacks verbatim.
- No pagination.
- Reuse the `speciesTone(category)` logic from the Spec B port of `SpeciesListScreen` — probably already exists as a local helper there. If so, copy the helper into this file (do not extract to shared yet). If it doesn't exist, use this definition:

```kotlin
private fun speciesTone(categoryName: String?): PhotoTone {
    val n = categoryName?.lowercase() ?: ""
    return when {
        n.contains("grönsak") -> PhotoTone.Sage
        n.contains("snittblom") || n.contains("blom") -> PhotoTone.Blush
        n.contains("ört") -> PhotoTone.Butter
        n.contains("frukt") -> PhotoTone.Sage
        else -> PhotoTone.Sage
    }
}
```

**Masthead:** `mastheadLeft = "§ Art"`, `mastheadCenter = species.commonName`.

**Masthead `right`:** drop edit/delete icons entirely — this is an aggregation view, not a single editable entity.

**Hero:**
- `title = species.commonName`
- `subtitle` = build: `listOfNotNull(species.scientificName?.takeIf { it.isNotBlank() }, "${aggregatePlantCount} plantor").joinToString(" · ")`
- `leading = { PhotoPlaceholder(label = species.commonName, tone = speciesTone(species.category?.name), modifier = Modifier.fillMaxSize()) }`

Replace `species.category?.name` with whatever the model exposes (typical shape: `SpeciesGroupRef` with a `name` field).

**Section — Plantor (grouped by status):**

Pre-compute:

```kotlin
val statusOrder = listOf("SEEDED", "POTTED_UP", "PLANTED_OUT", "GROWING", "HARVESTED", "RECOVERED", "REMOVED")
val byStatus: List<Pair<String, List<PlantRef>>> = remember(uiState.plants) {
    uiState.plants
        .groupBy { it.status ?: "UNKNOWN" }
        .toList()
        .sortedBy { (status, _) ->
            statusOrder.indexOf(status).takeIf { it >= 0 } ?: Int.MAX_VALUE
        }
}
```

Replace `PlantRef` with the actual model type.

Render:

```kotlin
if (byStatus.isEmpty() || byStatus.all { it.second.isEmpty() }) {
    item { FaltetSectionHeader(label = "Plantor") }
    item { InlineEmpty("Inga plantor av denna art ännu.") }
} else {
    byStatus.forEach { (status, plants) ->
        if (plants.isEmpty()) return@forEach
        item(key = "status_${status}") {
            FaltetSectionHeader(label = statusLabelSv(status))
        }
        items(plants, key = { "plant_${it.id}" }) { plant ->
            FaltetListRow(
                leading = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .drawBehind { drawCircle(statusColor(plant.status)) },
                    )
                },
                title = plant.bedName ?: "—",
                meta = buildString {
                    plant.variantName?.takeIf { it.isNotBlank() }?.let { append(it); append(" · ") }
                    append(formattedDate(plant.plantedDate))
                },
                stat = if ((plant.count ?: 1) > 1) {
                    {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = (plant.count ?: 1).toString(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                color = FaltetInk,
                            )
                            Text(
                                text = " ST",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp,
                                color = FaltetForest,
                            )
                        }
                    }
                } else null,
                onClick = { onPlantClick(plant.id) },
            )
        }
    }
}
```

Add helpers at bottom (partially shared with BedDetail — copy inline, don't extract yet):

```kotlin
private fun statusLabelSv(status: String): String = when (status) {
    "SEEDED" -> "Sådd"
    "POTTED_UP" -> "Krukad"
    "PLANTED_OUT", "GROWING" -> "Utplanterad"
    "HARVESTED" -> "Skördad"
    "RECOVERED" -> "Återhämtad"
    "REMOVED" -> "Borttagen"
    else -> status
}

private fun statusColor(status: String?): androidx.compose.ui.graphics.Color = when (status) {
    "SEEDED" -> FaltetMustard
    "POTTED_UP" -> FaltetSky
    "PLANTED_OUT", "GROWING" -> FaltetSage
    "HARVESTED" -> FaltetClay
    "RECOVERED" -> FaltetBerry
    "REMOVED" -> FaltetInkLine40
    else -> FaltetForest
}

private fun formattedDate(date: String?): String {
    if (date == null) return "—"
    return try {
        val parsed = java.time.LocalDate.parse(date)
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

Palette imports:
```kotlin
import app.verdant.android.ui.theme.FaltetBerry
import app.verdant.android.ui.theme.FaltetInkLine40
import app.verdant.android.ui.theme.FaltetMustard
import app.verdant.android.ui.theme.FaltetSage
import app.verdant.android.ui.theme.FaltetSky
```

- [ ] **Step 1: Read the current file in full**
- [ ] **Step 2: Replace imports following the pattern used in Tasks 3–5**
- [ ] **Step 3: Preserve state + ViewModel verbatim**
- [ ] **Step 4: Replace `PlantedSpeciesDetailScreen` composable body with scaffold + hero + grouped plants section**
- [ ] **Step 5: Add helper functions + `InlineEmpty` + `speciesTone`**
- [ ] **Step 6: Compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 7: Add a `@Preview`**

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun StatusDotPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
        listOf("SEEDED", "POTTED_UP", "PLANTED_OUT", "HARVESTED", "REMOVED").forEach { status ->
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind { drawCircle(statusColor(status)) },
            )
        }
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/plants/PlantedSpeciesDetailScreen.kt
git commit -m "feat: Fältet port — PlantedSpeciesDetail"
```

---

## Phase 4 — Verify + milestone

---

### Task 7: Verify + milestone

- [ ] **Step 1: Full Android build**

```bash
cd android && ./gradlew assembleDebug --no-daemon -q
```

Expected: BUILD SUCCESSFUL. APK at `android/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install + manual smoke**

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Open each ported screen in order. Verify:
- Cream background, ink primary text, forest secondary text.
- Masthead shows `§ Trädgård` / `§ Bädd` / `§ Planta` / `§ Art` on the four screens.
- Fraunces 32sp hero titles; emoji on GardenDetail, `PhotoPlaceholder` on BedDetail and PlantedSpeciesDetail, Coil image or `PhotoPlaceholder` on PlantDetail.
- Edit + delete icons in masthead-right on the three editable screens (not on PlantedSpeciesDetail).
- BedDetail condition groups collapse/expand with chevron; `FaltetMetadataRow` hairline bottoms visible.
- PlantDetail workflow strip shows `SÅDD · KRUKAD · UTPLANTERAD · SKÖRDAD` with the current stage in clay.
- PlantDetail event history grouped by month, newest month first.
- PlantedSpeciesDetail plants grouped under status section headers.
- Empty list sections show italic "Inga X ännu." in forest.
- Delete dialogs work and navigate back.

Screen checklist:

- [ ] GardenDetail
- [ ] BedDetail
- [ ] PlantDetail
- [ ] PlantedSpeciesDetail

- [ ] **Step 3: Milestone commit**

```bash
git commit --allow-empty -m "milestone: Android Fältet details complete (Spec C1)"
```

---

## Verification summary

After Task 7:

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/` contains two new primitives (`FaltetMetadataRow`, `FaltetHero`) added to the existing set.
- All four detail screens render Fältet-editorial.
- `./gradlew assembleDebug` green.
- ViewModels, navigation, dialogs, and data behavior unchanged.

**Follow-ups:**
- **Sub-spec C2** — forms (CreateGarden, CreateBed, CreatePlant, TaskForm, AddPlantEvent, AddSeeds). Needs new primitives for labeled inputs + dropdowns + date pickers.
- **Sub-spec C3** — activities (Sow, PotUp, PlantOut, ApplySupply, BatchPlantOut, AddSpecies). Needs wizard/step primitives.
- **Sub-spec D** — analytics, account, auth + deferred MyWorld dashboard and SeasonSelector.

---

## Self-review notes

- **Spec §1 (goal):** Tasks 1–7 collectively achieve the port.
- **Spec §2 (scope):** 4 detail screens → Tasks 3, 4, 5, 6.
- **Spec §3 (decisions):** Masthead + hero row → Task 2 + every port. Edit/delete in masthead-right → every port. `FaltetMetadataRow` + `FaltetHero` → Tasks 1, 2. Workflow progress mono strip → Task 5. Event history grouped by month → Task 5. Empty-section policy → handled per-section in Tasks 3–6.
- **Spec §4 (primitives):** `FaltetMetadataRow` and `FaltetHero` defined with full inline source in Tasks 1–2.
- **Spec §5 (port pattern):** Template is carried into each port with the masthead + hero + section list + `InlineEmpty` helper.
- **Spec §6 (per-screen):** Row mappings, section orders, and condition group definitions specified in Tasks 3–6.
- **Spec §7 (empty-section policy):** Applied per-section: list sections render header + `InlineEmpty` when empty; metadata sections (BedDetail Villkor + each `ConditionGroup`) compute `hasAnyField` / `fields.all { it.second == null }` and suppress the whole section when fully empty.
- **Spec §8 (phasing):** 2 primitives + 1 ref port + 3 batch + 1 milestone = 7 tasks. Matches.
- **Spec §9 (testing):** `@Preview` per primitive (Tasks 1–2), at least one `@Preview` per screen (Tasks 3–6), compile-green gate after every code commit, manual smoke at Phase 2 checkpoint (Task 3 Step 8) and Phase 4 (Task 7 Step 2).
