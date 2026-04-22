# Android Fältet — Sub-Spec C2 (Forms) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port 5 Android form screens (`CreateBed`, `CreateGarden`, `CreatePlant`, `TaskForm`, `AddPlantEvent`) to the Fältet editorial aesthetic using 6 new primitives + an extension to the existing `Field.kt`.

**Architecture:** Vertical slice. Phase 1 ships the 6 new primitives plus the `Field` extension and a `snackbarHost` slot addition to `FaltetScreenScaffold`. Phase 2 ports `CreateBedScreen` as reference. Phase 3 batches the remaining four ports (`CreatePlant`, `TaskForm`, `CreateGarden`, `AddPlantEvent`). Phase 4 runs `assembleDebug` and creates a milestone commit. Every form uses `FaltetScreenScaffold` + `Field` / `FaltetChipSelector` / `FaltetDropdown` / `FaltetDatePicker` / `FaltetImagePicker` composed in a `LazyColumn`, with a fixed-bottom `FaltetFormSubmitBar`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (incl. `ModalBottomSheet` and `DatePickerDialog`), Hilt, Coil, Android Activity Result API (`TakePicturePreview`, `GetContent`), existing `ui/faltet/` primitives.

**Spec:** `docs/plans/2026-04-22-android-faltet-forms-design.md` — read this before starting.

**Reality-check notes:**
- `FaltetScreenScaffold` currently lacks a `snackbarHost` parameter — Task 7 adds it.
- Android forms have no pagination. No lazy-loading of dropdown options beyond client-side search filter.
- This is a rendering port — preserve every ViewModel, state shape, navigation callback, existing validation logic, and business behavior verbatim. Only the visible body + submit-button placement change.
- Swedish copy is authored per-screen in this plan. Use what's specified.
- Where the plan references model field names (e.g., `BedSoilType`, `eventType`), confirm the actual names by reading the current screen file. If a referenced field doesn't exist, adapt without inventing new ones.

---

## File Structure

### New files (under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`)

- `FaltetChipSelector.kt` — single-select nullable filter-chip row
- `FaltetDropdown.kt` — field chrome + `ModalBottomSheet` picker with optional search
- `FaltetDatePicker.kt` — field chrome + Material `DatePickerDialog`
- `FaltetImagePicker.kt` — camera + gallery buttons + preview
- `FaltetSubmitButton.kt` — full-width ink button with loading state
- `FaltetFormSubmitBar.kt` — `bottomBar`-slot wrapper: cream bg + hairline top border + `FaltetSubmitButton`

### Modified files

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/Field.kt` — add `error: String? = null` + `required: Boolean = false` parameters
- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt` — add `snackbarHost: @Composable () -> Unit = {}` parameter
- `android/app/src/main/kotlin/app/verdant/android/ui/bed/CreateBedScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/plant/CreatePlantScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/task/TaskFormScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/garden/CreateGardenScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/plant/AddPlantEventScreen.kt`

---

## Phase 1 — Primitives

Each primitive is compile-gated. No screen consumes any of these until Phase 2.

---

### Task 1: Extend `Field.kt`

Add `error` and `required` parameters. Non-breaking change — existing callers unaffected.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/Field.kt`

- [ ] **Step 1: Read the current file**

Read `android/app/src/main/kotlin/app/verdant/android/ui/faltet/Field.kt` to confirm the existing signature.

- [ ] **Step 2: Replace the file contents**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/Field.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    accent: FaltetTone? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    required: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val valueColor = accent?.color() ?: FaltetInk
    val underlineColor = if (error != null) FaltetClay else FaltetInk
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
        Spacer(Modifier.height(4.dp))
        if (onValueChange != null) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontFamily = FaltetDisplay,
                    fontWeight = FontWeight.W300,
                    fontSize = 20.sp,
                    color = valueColor,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = underlineColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(vertical = 4.dp),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            fontFamily = FaltetDisplay,
                            fontSize = 20.sp,
                            color = FaltetForest.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
            )
        } else {
            Text(
                text = value,
                fontFamily = FaltetDisplay,
                fontWeight = FontWeight.W300,
                fontSize = 20.sp,
                color = valueColor,
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(underlineColor),
            )
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = error,
                fontSize = 12.sp,
                color = FaltetClay,
            )
        }
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/Field.kt
git commit -m "feat: extend Field with error + required params"
```

---

### Task 2: `FaltetChipSelector`

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipSelector.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipSelector.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
fun <T : Any> FaltetChipSelector(
    label: String,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T?) -> Unit,
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
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectedChange(if (isSelected) null else option) },
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
private fun FaltetChipSelectorPreview_Unselected() {
    FaltetChipSelector(
        label = "Jordtyp",
        options = listOf("Lera", "Sand", "Mylla"),
        selected = null,
        onSelectedChange = {},
        labelFor = { it },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetChipSelectorPreview_Selected() {
    FaltetChipSelector(
        label = "Jordtyp",
        options = listOf("Lera", "Sand", "Mylla"),
        selected = "Mylla",
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

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetChipSelector.kt
git commit -m "feat: FaltetChipSelector primitive"
```

---

### Task 3: `FaltetDropdown`

Field chrome that opens a `ModalBottomSheet` containing `FaltetSearchField` (optional) + filtered `FaltetListRow` options.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetDropdown.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetDropdown.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> FaltetDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    searchable: Boolean = true,
    placeholder: String = "Välj…",
    required: Boolean = false,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sheetOpen = true; query = "" }
                .drawBehind {
                    drawLine(
                        color = FaltetInk,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = selected?.let { labelFor(it) } ?: placeholder,
                fontFamily = FaltetDisplay,
                fontStyle = if (selected == null) FontStyle.Normal else FontStyle.Italic,
                fontWeight = FontWeight.W300,
                fontSize = 20.sp,
                color = if (selected == null) FaltetForest.copy(alpha = 0.4f) else FaltetInk,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = FaltetClay,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = FaltetCream,
        ) {
            val filtered = if (searchable && query.isNotBlank()) {
                options.filter { labelFor(it).contains(query, ignoreCase = true) }
            } else {
                options
            }
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                if (searchable) {
                    FaltetSearchField(value = query, onValueChange = { query = it }, placeholder = "SÖK")
                }
                LazyColumn {
                    items(filtered, key = { labelFor(it) }) { option ->
                        FaltetListRow(
                            title = labelFor(option),
                            onClick = {
                                onSelectedChange(option)
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) sheetOpen = false
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetDropdownPreview_Placeholder() {
    FaltetDropdown(
        label = "Art",
        options = listOf("Cosmos", "Zinnia", "Dahlia"),
        selected = null,
        onSelectedChange = {},
        labelFor = { it },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetDropdownPreview_Selected() {
    FaltetDropdown(
        label = "Art",
        options = listOf("Cosmos", "Zinnia", "Dahlia"),
        selected = "Cosmos",
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

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetDropdown.kt
git commit -m "feat: FaltetDropdown primitive"
```

---

### Task 4: `FaltetDatePicker`

Field chrome that opens Material3 `DatePickerDialog`, displays Swedish-formatted date.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetDatePicker.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetDatePicker.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.Year

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltetDatePicker(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Välj datum",
    required: Boolean = false,
) {
    var dialogOpen by remember { mutableStateOf(false) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )

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
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { dialogOpen = true }
                .drawBehind {
                    drawLine(
                        color = FaltetInk,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = value?.let { formatDateSv(it) } ?: placeholder,
                fontFamily = FaltetDisplay,
                fontStyle = if (value == null) FontStyle.Normal else FontStyle.Italic,
                fontWeight = FontWeight.W300,
                fontSize = 20.sp,
                color = if (value == null) FaltetForest.copy(alpha = 0.4f) else FaltetInk,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = FaltetClay,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (dialogOpen) {
        DatePickerDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(picked)
                    }
                    dialogOpen = false
                }) { Text("Välj", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Avbryt") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun formatDateSv(date: LocalDate): String {
    val months = arrayOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "aug", "sep", "okt", "nov", "dec")
    val m = months[date.monthValue - 1]
    val currentYear = Year.now().value
    return if (date.year == currentYear) "${date.dayOfMonth} $m" else "${date.dayOfMonth} $m ${date.year}"
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetDatePickerPreview_Empty() {
    FaltetDatePicker(label = "Deadline", value = null, onValueChange = {}, required = true)
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetDatePickerPreview_Populated() {
    FaltetDatePicker(
        label = "Deadline",
        value = LocalDate.of(2026, 5, 14),
        onValueChange = {},
    )
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetDatePicker.kt
git commit -m "feat: FaltetDatePicker primitive"
```

---

### Task 5: `FaltetImagePicker`

Camera + gallery buttons with preview thumbnail.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetImagePicker.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetImagePicker.kt
package app.verdant.android.ui.faltet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine40

@Composable
fun FaltetImagePicker(
    label: String,
    value: Bitmap?,
    onValueChange: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        if (bitmap != null) onValueChange(bitmap)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let(onValueChange)
        }
    }

    Column(modifier) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(8.dp))
        if (value == null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, FaltetInkLine40),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FaltetClay),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PhotoCamera, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("KAMERA", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.4.sp)
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, FaltetInkLine40),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FaltetClay),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Image, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("GALLERI", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.4.sp)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, FaltetInk),
            ) {
                Image(
                    bitmap = value.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = { onValueChange(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(FaltetCream.copy(alpha = 0.8f)),
                ) {
                    Icon(Icons.Default.Close, "Ta bort bild", tint = FaltetClay, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetImagePickerPreview_Empty() {
    FaltetImagePicker(label = "Foto", value = null, onValueChange = {})
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetImagePicker.kt
git commit -m "feat: FaltetImagePicker primitive"
```

---

### Task 6: `FaltetSubmitButton`

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSubmitButton.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSubmitButton.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun FaltetSubmitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    submitting: Boolean = false,
) {
    Button(
        onClick = { if (!submitting) onClick() },
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RectangleShape,
        enabled = enabled && !submitting,
        colors = ButtonDefaults.buttonColors(
            containerColor = FaltetInk,
            contentColor = FaltetCream,
            disabledContainerColor = FaltetInk.copy(alpha = 0.4f),
            disabledContentColor = FaltetCream,
        ),
    ) {
        if (submitting) {
            CircularProgressIndicator(
                color = FaltetCream,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = label.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetSubmitButtonPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FaltetSubmitButton(label = "Skapa", onClick = {})
        Spacer(Modifier.height(4.dp))
        FaltetSubmitButton(label = "Skapa", onClick = {}, enabled = false)
        Spacer(Modifier.height(4.dp))
        FaltetSubmitButton(label = "Skapa", onClick = {}, submitting = true)
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSubmitButton.kt
git commit -m "feat: FaltetSubmitButton primitive"
```

---

### Task 7: `FaltetFormSubmitBar` + `FaltetScreenScaffold` snackbar-host slot

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetFormSubmitBar.kt`
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt`

- [ ] **Step 1: Write `FaltetFormSubmitBar.kt`**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetFormSubmitBar.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetFormSubmitBar(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    submitting: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetCream)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        FaltetSubmitButton(
            label = label,
            onClick = onClick,
            enabled = enabled,
            submitting = submitting,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetFormSubmitBarPreview() {
    FaltetFormSubmitBar(label = "Skapa", onClick = {})
}
```

- [ ] **Step 2: Extend `FaltetScreenScaffold.kt` with `snackbarHost` slot**

Replace the full file contents with:

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import app.verdant.android.ui.theme.FaltetCream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaltetScreenScaffold(
    mastheadLeft: String,
    mastheadCenter: String,
    mastheadRight: @Composable (() -> Unit)? = null,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    fab: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = FaltetCream,
        topBar = {
            Column {
                topBar()
                Masthead(left = mastheadLeft, center = mastheadCenter, right = mastheadRight)
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = { fab?.invoke() },
        snackbarHost = snackbarHost,
        content = content,
    )
}
```

- [ ] **Step 3: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetFormSubmitBar.kt \
        android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetScreenScaffold.kt
git commit -m "feat: FaltetFormSubmitBar + FaltetScreenScaffold snackbarHost slot"
```

---

## Phase 2 — Reference port

---

### Task 8: Port `CreateBedScreen`

Validates `Field` required+error, `FaltetChipSelector` across 7 enum fields, decimal validation, `FaltetFormSubmitBar` wiring, form-level snackbar.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/bed/CreateBedScreen.kt`

**Context for the agent:**
- Current file (~431 lines) has: `CreateBedState`, `CreateBedViewModel.create(...)`, existing label helper functions at top (`bedSoilTypeLabel`, `bedSunExposureLabel`, `bedDrainageLabel`, `bedAspectLabel`, `bedIrrigationTypeLabel`, `bedProtectionLabel` — confirm exact names), `Scaffold` with `TopAppBar` + `OutlinedTextField`/`FilterChip` body, bottom submit button inside scrollable Column.
- Preserve: state, ViewModel (including all creation parameters), label helper functions (they are shared with `BedDetail` and `GardenDetail` ports from C1).
- Replace: `Scaffold + TopAppBar + verticalScroll + OutlinedTextField + FilterChip + submit Button` rendering with `FaltetScreenScaffold + LazyColumn + Field + FaltetChipSelector + FaltetFormSubmitBar`.

**Masthead:** `mastheadLeft = "§ Bädd"`, `mastheadCenter = "Ny bädd"`.

**Bottom bar:** `FaltetFormSubmitBar(label = "Skapa", onClick = submitAction, enabled = canSubmit, submitting = uiState.isLoading)`.

**Validation logic (adapt to current VM):**
- `name.isNotBlank()` → required.
- `soilPhText` (if non-blank): parse to `Double`, must be in `[3.0, 9.0]`, otherwise `phError = "pH måste vara mellan 3.0 och 9.0"`.
- `canSubmit = name.isNotBlank() && phError == null && !uiState.isLoading`.

**Field order:**
1. `Field(label = "Namn", value = name, onValueChange = { name = it }, required = true, error = if (nameError) "Namn krävs" else null)`
2. `Field(label = "Beskrivning (valfri)", value = description, onValueChange = { description = it })`
3. `FaltetChipSelector(label = "Jordtyp", options = BedSoilType.entries.toList(), selected = soilType, onSelectedChange = { soilType = it }, labelFor = { bedSoilTypeLabel(it) })`
4. `Field(label = "pH (valfri)", value = soilPhText, onValueChange = { soilPhText = it; /* revalidate */ }, keyboardType = KeyboardType.Decimal, error = phError)`
5. `FaltetChipSelector(label = "Dränering", options = BedDrainage.entries.toList(), selected = drainage, onSelectedChange = { drainage = it }, labelFor = { bedDrainageLabel(it) })`
6. `FaltetChipSelector(label = "Sol", options = BedSunExposure.entries.toList(), selected = sunExposure, onSelectedChange = { sunExposure = it }, labelFor = { bedSunExposureLabel(it) })`
7. `FaltetChipSelector(label = "Väderstreck", options = BedAspect.entries.toList(), selected = aspect, onSelectedChange = { aspect = it }, labelFor = { bedAspectLabel(it) })`
8. `FaltetChipSelector(label = "Bevattning", options = BedIrrigationType.entries.toList(), selected = irrigationType, onSelectedChange = { irrigationType = it }, labelFor = { bedIrrigationTypeLabel(it) })`
9. `FaltetChipSelector(label = "Skydd", options = BedProtection.entries.toList(), selected = protection, onSelectedChange = { protection = it }, labelFor = { bedProtectionLabel(it) })`
10. `FaltetChipSelector(label = "Upphöjd bädd (valfri)", options = listOf(true, false), selected = raisedBed, onSelectedChange = { raisedBed = it }, labelFor = { if (it) "Ja" else "Nej" })`

**Submit wiring:**

```kotlin
val submitAction = {
    val phValue = soilPhText.toDoubleOrNull()
    val phInvalid = soilPhText.isNotBlank() && (phValue == null || phValue < 3.0 || phValue > 9.0)
    phError = if (phInvalid) "pH måste vara mellan 3.0 och 9.0" else null
    nameError = name.isBlank()
    if (!nameError && phError == null) {
        viewModel.create(
            name = name,
            description = description,
            soilType = soilType?.name,
            soilPh = phValue,
            sunExposure = sunExposure?.name,
            aspect = aspect?.name,
            drainage = drainage?.name,
            irrigationType = irrigationType?.name,
            protection = protection?.name,
            raisedBed = raisedBed,
        )
    }
}
```

Adjust field list + enum-name extraction to match the actual `CreateBedRequest` and `CreateBedViewModel.create(...)` signature. If the VM takes enum objects directly (not strings), drop the `?.name` calls.

**Snackbar for errors:** wire `uiState.error` via `SnackbarHostState`:

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(uiState.error) {
    uiState.error?.let { snackbarHostState.showSnackbar(it) }
}
LaunchedEffect(uiState.createdId) {
    if (uiState.createdId != null) onCreated(uiState.createdId!!)
}
```

**Steps:**

1. Read the current `CreateBedScreen.kt` in full.
2. Replace imports (drop `TopAppBar`, `OutlinedTextField`, `FilterChip`, `RoundedCornerShape`, `verdantTopAppBarColors`, `stringResource`, `R`, `verticalScroll`, `rememberScrollState`, `Button`, `ExpandLess`, `ExpandMore`, etc. as they become unused). Add Fältet imports.
3. Preserve the state + ViewModel + label helpers verbatim.
4. Replace the `@Composable fun CreateBedScreen(...)` body with the new `FaltetScreenScaffold` structure. Keep the existing callback signature.
5. Add a `@Preview` showing a populated form state with a validation error on pH.
6. `cd android && ./gradlew compileDebugKotlin --no-daemon -q` — exit 0.
7. If green, commit:
   ```
   git add android/app/src/main/kotlin/app/verdant/android/ui/bed/CreateBedScreen.kt
   git commit -m "feat: Fältet port — CreateBed"
   ```
8. If red, BLOCKED with error output.

**Phase 2 checkpoint** — install + smoke:

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Open CreateBed on emulator. Verify:
- Masthead `§ Bädd / Ny bädd`; no TopAppBar.
- Scrollable form; fixed bottom bar with `SKAPA` ink button.
- Fraunces italic in text-field values; mono uppercase labels with clay `*` on required fields.
- ChipSelector rows visible; tap selects/deselects; clay fill when selected.
- Submit button disabled when name empty; enabling when name filled.
- pH entry "14" shows the error message in clay below field.
- Back nav returns to parent.

If a primitive API feels wrong, amend in new commits (no history rewrite) and fix CreateBed before Phase 3.

---

## Phase 3 — Batch ports

Each task reads its existing screen, preserves VM+state+callbacks+dialogs, replaces only the visible body.

---

### Task 9: Port `CreatePlantScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/plant/CreatePlantScreen.kt`

**Context:**
- Current file (~273 lines). Camera/gallery image picker (via `TakePicturePreview` + `GetContent` with Base64 encoding), `OutlinedTextField` for name/species/seed count, species suggestion cards.
- Preserve: state class, ViewModel (with all methods, including any Base64 compression), callback signature, suggestion logic + data source.
- Replace: `Scaffold + TopAppBar + Column + OutlinedTextField + camera buttons + submit Button` with `FaltetScreenScaffold + LazyColumn + FaltetImagePicker + Field + FaltetFormSubmitBar`.

**Masthead:** `mastheadLeft = "§ Planta"`, `mastheadCenter = "Ny planta"`.

**Bottom bar:** `FaltetFormSubmitBar(label = "Skapa", ...)`.

**Field order:**
1. `FaltetImagePicker(label = "Foto (valfri)", value = photoBitmap, onValueChange = { photoBitmap = it; /* retrigger Base64 encoding as current code does */ })`
2. `Field(label = "Namn", value = name, onValueChange = { name = it }, required = true, error = if (nameError) "Namn krävs" else null)`
3. `Field(label = "Art (valfri)", value = species, onValueChange = { species = it })`
4. `Field(label = "Antal frön (valfri)", value = seedCountText, onValueChange = { seedCountText = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number)`
5. **Species suggestions** (if the current screen has them): render as a private composable below the species field that shows a `FlowRow` of `Chip(label = suggestion, ...)` — tapping fills the `species` state. Use the existing data source.

**Canonical Base64 conversion:** preserve whatever utility the current file uses (`toCompressedBase64()`). If the current file converts in the composable, keep that call site. The `FaltetImagePicker` only emits `Bitmap?`; the screen's existing logic converts to Base64 for the VM.

**Steps:**
1. Read current file.
2. Replace imports.
3. Preserve state + VM.
4. Replace body.
5. Add `@Preview` for populated form.
6. Compile + commit: `git add <file> && git commit -m "feat: Fältet port — CreatePlant"`
7. If red, BLOCKED.

If field/VM names differ, adapt.

---

### Task 10: Port `TaskFormScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/task/TaskFormScreen.kt`

**Context:**
- Current file (~337 lines). Handles create + edit via `taskId` in route. Uses `ExposedDropdownMenuBox` for activity type + species; Material3 `DatePickerDialog` for deadline; numeric target count; notes textarea.
- Preserve: state, ViewModel (including fetch-for-edit logic), callback signature, existing Swedish labels for activity types.
- Replace: `Scaffold + TopAppBar + ExposedDropdownMenuBox + OutlinedTextField + DatePickerDialog + Button` with `FaltetScreenScaffold + LazyColumn + FaltetDropdown + FaltetDatePicker + Field + FaltetFormSubmitBar`.

**Masthead:**
- Create: `mastheadLeft = "§ Arbete"`, `mastheadCenter = "Ny uppgift"`.
- Edit: `mastheadLeft = "§ Arbete"`, `mastheadCenter = task.title` (from `uiState.task?.title ?: "Uppgift"`).

**Bottom bar:**
- Create mode: `FaltetFormSubmitBar(label = "Skapa", ...)`.
- Edit mode: `FaltetFormSubmitBar(label = "Spara", ...)`.

**Loading state:** show `FaltetLoadingState` when `uiState.isLoading && isEditMode`.

**Field order:**
1. `FaltetDropdown(label = "Aktivitet", options = listOf(<activity types>), selected = activityType, onSelectedChange = { activityType = it }, labelFor = { activityTypeLabelSv(it) }, searchable = false, required = true)`
2. `FaltetDropdown(label = "Art", options = uiState.species, selected = selectedSpecies, onSelectedChange = { selectedSpecies = it }, labelFor = { it.commonName + (it.variantName?.let { v -> " $v" } ?: "") }, searchable = true, required = true)` — adapt field names to the actual `SpeciesResponse` shape (may be `commonName`, `commonNameSv`, `name`, etc.). If `variantName` doesn't exist, drop it.
3. `FaltetDatePicker(label = "Deadline", value = deadline, onValueChange = { deadline = it }, required = true)` — `deadline: LocalDate?`. Replace any `String` representation in the current code with `LocalDate`; parse on load, format on save back to whatever string format the VM expects.
4. `Field(label = "Målantal (valfri)", value = targetCountText, onValueChange = { targetCountText = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number)`
5. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = { notes = it })`

**Validation:** activityType + species + deadline required; submit enabled when all three non-null.

**Steps:**
1. Read current file.
2. Replace imports. Drop `ExposedDropdownMenuBox`, `ExposedDropdownMenu`, `DropdownMenuItem`, `DatePickerDialog` (only imported from the primitive), `Icons.Default.CalendarMonth` (still used — keep), `TopAppBar`, `OutlinedTextField`, `Button`.
3. Preserve state + VM. Replace string-date handling with `LocalDate` conversions at the field boundary.
4. Replace body.
5. Add `@Preview` for populated create + populated edit state.
6. Compile + commit.

---

### Task 11: Port `CreateGardenScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/garden/CreateGardenScreen.kt`

**Context:**
- Current file (~774 lines). Has name/emoji/description form fields, location search, **map boundary drawer**, and possibly inline bed creation. Largest file in this sub-spec.
- Preserve verbatim: state, ViewModel, map-drawing component + its state machinery, location search logic, any existing emoji picker, inline bed creation logic if present, callback signature.
- Replace: `Scaffold + TopAppBar + Column + OutlinedTextField` around the map with `FaltetScreenScaffold + LazyColumn + Field + wrapped map component + FaltetFormSubmitBar`.

**Masthead:** `mastheadLeft = "§ Trädgård"`, `mastheadCenter = "Ny trädgård"`.

**Bottom bar:** `FaltetFormSubmitBar(label = "Skapa", ...)`.

**Field order:**
1. `Field(label = "Namn", value = name, onValueChange = { name = it }, required = true, error = if (nameError) "Namn krävs" else null)`
2. `Field(label = "Emoji", value = emoji, onValueChange = { emoji = it }, placeholder = "🌱")` — single-grapheme entry. Keep any existing emoji-picker affordance (e.g., a trailing icon button that opens a picker) without rebuilding it — just style the surrounding field.
3. `Field(label = "Beskrivning (valfri)", value = description, onValueChange = { description = it })`
4. **Location search**: if the current file has a location-search field, port it as a `Field` with trailing `Icons.Default.Search` icon. Display the resolved-location result as a small italic line below (`Text(..., fontFamily = FaltetDisplay, fontStyle = FontStyle.Italic, ...)`). Preserve the search callback wiring.
5. **Map boundary drawer**: render the existing map component as a single `item {}` in the `LazyColumn`. Wrap it:

   ```kotlin
   item {
       Column(
           modifier = Modifier
               .fillMaxWidth()
               .padding(horizontal = 18.dp)
               .drawBehind {
                   drawLine(
                       color = FaltetInkLine20,
                       start = Offset(0f, size.height),
                       end = Offset(size.width, size.height),
                       strokeWidth = 1.dp.toPx(),
                   )
               },
       ) {
           Text(
               text = "KARTA",
               fontFamily = FontFamily.Monospace,
               fontSize = 9.sp,
               letterSpacing = 1.4.sp,
               color = FaltetForest.copy(alpha = 0.7f),
           )
           Spacer(Modifier.height(8.dp))
           // the existing map component composable call, unchanged
           MapBoundaryDrawer(/* existing args */)
       }
   }
   ```

   Keep the map composable call exactly as currently invoked. Do not restyle map internals.

6. **Inline beds (if present)**: port each bed card to a `Column` item with `Field`s for name/description. Preserve the existing "add bed" FAB pattern within the garden form (rendered inline in the scroll, not as a screen FAB).

**Validation:** name required; other fields optional.

**Steps:**
1. Read current file.
2. Replace imports; preserve map-related imports.
3. Preserve state + VM + map + location search logic.
4. Replace body.
5. Add `@Preview` for populated form (mock the map component with a placeholder Box if it requires live state).
6. Compile + commit.

---

### Task 12: Port `AddPlantEventScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/plant/AddPlantEventScreen.kt`

**Context:**
- Current file (~454 lines). Multi-row `FilterChip`s for event type, conditional numeric fields (plant count, weight, stem count, stem length, vase life), quality grade chips, customer dropdown, notes, camera/gallery image picker.
- Preserve: state, ViewModel, callback signature, existing `eventTypeOptions`/`statusLabel`/`eventTypeLabelSv` helpers, Base64 encoding utility call.

**Masthead:** `mastheadLeft = "§ Händelse"`, `mastheadCenter = plant.name` (from `uiState.plant?.name ?: ""`).

**Bottom bar:** `FaltetFormSubmitBar(label = "Spara", ...)`.

**Field order:**
1. `FaltetImagePicker(label = "Foto (valfri)", value = photoBitmap, onValueChange = { photoBitmap = it })`
2. `FaltetChipSelector(label = "Händelsetyp", options = eventTypeOptions, selected = eventType, onSelectedChange = { eventType = it }, labelFor = { eventTypeLabelSv(it) }, required = true)`
3. **Conditional numeric fields** — render inside `if` blocks based on `eventType`:
    ```kotlin
    if (eventType in listOf(EventType.SEEDED, EventType.POTTED_UP, EventType.PLANTED_OUT, EventType.WATERED, EventType.FERTILIZED, EventType.HARVESTED)) {
        item { Field(label = "Antal plantor (valfri)", value = plantCountText, onValueChange = { plantCountText = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number) }
    }
    if (eventType == EventType.HARVESTED) {
        item { Field(label = "Vikt g (valfri)", value = weightText, ..., keyboardType = KeyboardType.Decimal) }
        item { Field(label = "Antal stjälkar (valfri)", value = stemCountText, ..., keyboardType = KeyboardType.Number) }
        item { Field(label = "Stjälklängd cm (valfri)", value = stemLengthText, ..., keyboardType = KeyboardType.Decimal) }
        item { Field(label = "Vaslivslängd dagar (valfri)", value = vaseLifeText, ..., keyboardType = KeyboardType.Number) }
        item { FaltetChipSelector(label = "Kvalitet (valfri)", options = QualityGrade.entries.toList(), selected = quality, onSelectedChange = { quality = it }, labelFor = { qualityLabelSv(it) }) }
        item { FaltetDropdown(label = "Kund (valfri)", options = uiState.customers, selected = selectedCustomer, onSelectedChange = { selectedCustomer = it }, labelFor = { it.name }, searchable = true) }
    }
    ```
    Adapt enum names (the actual event type enum may be `PlantEventType` or similar) and field names to what the VM state actually holds. If `QualityGrade` or `customers` don't exist in state, drop those items.
4. `Field(label = "Anteckningar (valfri)", value = notes, onValueChange = { notes = it })`

**Validation:** event type required. Submit enabled when eventType != null and !isSubmitting.

**Steps:**
1. Read current file.
2. Replace imports.
3. Preserve state + VM + helpers + Base64 utility call.
4. Replace body.
5. Add `@Preview` for populated HARVESTED event state.
6. Compile + commit.

---

## Phase 4 — Verify + milestone

---

### Task 13: Verify + milestone

- [ ] **Step 1: Full Android build**

```bash
cd android && ./gradlew assembleDebug --no-daemon -q
```

Expected: BUILD SUCCESSFUL. APK at `android/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install + manual smoke**

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Open each form in turn. Verify:
- Masthead `§ <entity>` left, Fraunces italic title center, no TopAppBar.
- Scrollable body with 16dp between fields; hairlines under text fields; clay `*` on required fields.
- ChipSelector taps select/deselect with clay fill.
- Dropdown taps open bottom sheet; search field functional; tapping a row selects and dismisses.
- Date picker opens Material dialog; chosen date renders as `"23 apr"` or `"23 apr 2025"`.
- Image picker shows KAMERA + GALLERI buttons; tapping launches system intents; selected image shows with × overlay.
- Submit button pinned to bottom; disabled when required fields missing; spinner on submit; closes screen on success; snackbar on error.

Screen checklist:

- [ ] CreateBed
- [ ] CreatePlant
- [ ] TaskForm (create + edit)
- [ ] CreateGarden (form works; map untouched)
- [ ] AddPlantEvent (conditional fields by event type)

- [ ] **Step 3: Milestone commit**

```bash
git commit --allow-empty -m "milestone: Android Fältet forms complete (Spec C2)"
```

---

## Verification summary

After Task 13:

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/` contains 6 new primitives (`FaltetChipSelector`, `FaltetDropdown`, `FaltetDatePicker`, `FaltetImagePicker`, `FaltetSubmitButton`, `FaltetFormSubmitBar`).
- `Field.kt` supports `error` + `required`.
- `FaltetScreenScaffold` accepts a `snackbarHost` slot.
- All 5 form screens render Fältet-editorial with fixed bottom submit bars.
- `./gradlew assembleDebug` green.
- ViewModels, nav callbacks, dialogs, data behavior unchanged.

**Follow-ups:**
- Inline edit dialogs in GardenDetail + BedDetail still use `OutlinedTextField`. Migrate in a post-C2 polish pass (1 commit per detail screen).
- **Sub-spec C3** — activity / workflow screens.
- **Sub-spec D** — analytics, account, auth + deferred MyWorld dashboard and SeasonSelector.

---

## Self-review notes

- **Spec §1 (goal):** Tasks 1–13 achieve the port.
- **Spec §2 (scope):** 5 screens → Tasks 8, 9, 10, 11, 12.
- **Spec §3 (decisions):** All 6 brainstorm decisions implemented. Map drawer preserved (Task 11). Chip selector + dropdown split (Tasks 2, 3). Fixed bottom bar (Task 7 + every port). ModalBottomSheet dropdown (Task 3). CreateBed reference port (Task 8).
- **Spec §4 (primitives):** Each primitive has its own task with full inline source (Tasks 1–7).
- **Spec §5 (port pattern):** Pattern laid out in Task 8 + reused in Tasks 9–12.
- **Spec §6 (per-screen):** Row orderings + validation per screen enumerated in Tasks 8–12.
- **Spec §7 (phasing):** 7 + 1 + 4 + 1 = 13 tasks. Matches.
- **Spec §8 (testing):** `@Preview` per primitive + per screen; compile gate after each; manual smoke at Phase 2 checkpoint (Task 8) and Phase 4 (Task 13).
- **Spec §9 (follow-ups):** Inline edit dialog polish noted; C3/D dependencies called out.
