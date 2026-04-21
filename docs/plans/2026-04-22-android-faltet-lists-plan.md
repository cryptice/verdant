# Android Fältet — Sub-Spec B (Lists) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port 12 Android list screens to the Fältet editorial aesthetic using seven new shared primitives under `ui/faltet/`, keeping data contracts and navigation unchanged.

**Architecture:** Vertical slice. Phase 1 ships the seven primitives with `@Preview`s. Phase 2 ports the simplest screen (`PlantedSpeciesListScreen`) and the most complex (`SupplyInventoryScreen`) to validate the primitive API. Phase 3 batches the remaining ten ports. Phase 4 runs `assembleDebug`, a manual smoke pass, and a milestone commit.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, existing `app.verdant.android.ui.theme` tokens (`FaltetCream`, `FaltetInk`, `FaltetForest`, `FaltetClay`, `FaltetMustard`, `FaltetBerry`, `FaltetSky`, `FaltetSage`, `FaltetBlush`, `FaltetButter`, `FaltetInkLine20`, `FaltetInkLine40`, `FaltetInkFill04`, `FaltetDisplay`), existing `ui/faltet/` primitives (`Chip`, `Rule`, `Stat`, `Field`, `PhotoPlaceholder`, `Masthead`, `FaltetScreenScaffold`, `FaltetFab`, `FaltetTone`).

**Spec:** `docs/plans/2026-04-22-android-faltet-lists-design.md` — read this before starting.

**Reality-check notes (spec §5.2 corrections):**
- **Pagination is web-only.** Android list screens do not have pagination; do not add it. Drop any `FaltetLedgerPagination(...)` references from spec §5.
- **SupplyInventory** has a two-level expandable structure (category → supply-type group → batches) and a "use-quantity" `AlertDialog` — no inline stepper. Port preserves that structure; slot mapping becomes: leading = category `Icon`, title = supply-type name, meta = category label, stat = total quantity + unit, actions = `ExpandMore/Less` chevron. The per-batch rows inside the expanded group use a secondary lightweight row rendering, not `FaltetListRow`.
- **TaskList** row: preserve the existing "tone dot" leading behavior. The current code has delete affordance, not a checkbox — port without adding a checkbox if the existing UX is delete-driven. Check the file at port time and align.
- **Port preserves ViewModel wiring, state shapes, navigation callbacks, and existing dialogs.** This is a rendering port, not a UX refactor.

---

## File Structure

### New files (all under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`)

- `FaltetListRow.kt` — hairline-bordered clickable row with named semantic slots
- `FaltetEmptyState.kt` — editorial empty-list state
- `FaltetSectionHeader.kt` — mono uppercase label with clay underline rule
- `FaltetSearchField.kt` — compact search input with leading icon + clear button
- `FaltetCheckbox.kt` — Fältet-styled wrapper for Material Checkbox
- `FaltetStepper.kt` — horizontal [−] value [+] integer stepper
- `FaltetLoadingState.kt` — centered clay spinner on cream background

### Modified files (12 screens)

- `android/app/src/main/kotlin/app/verdant/android/ui/plants/PlantedSpeciesListScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/supplies/SupplyInventoryScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/task/TaskListScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/inventory/SeedInventoryScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/activity/SpeciesListScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/succession/SuccessionSchedulesScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/targets/ProductionTargetsScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/trials/VarietyTrialsScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/bouquet/BouquetRecipesScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/customer/CustomerListScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/pest/PestDiseaseLogScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/world/MyVerdantWorldScreen.kt`

---

## Phase 1 — Primitives

Each primitive is a single small file with inline `@Preview`. Compile-green gate after each commit. No screen consumes any of these yet — at the end of Phase 1 the app still compiles and behaves identically to Spec A.

---

### Task 1: `FaltetListRow`

The workhorse. Hairline-bottom row with five named slots.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetListRow.kt`

- [ ] **Step 1: Write the primitive**

Create `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetListRow.kt` with:

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetListRow.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetListRow(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    meta: String? = null,
    metaMaxLines: Int = 1,
    stat: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(clickable)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) { leading() }
            Spacer(Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 18.sp,
                color = FaltetInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Text(
                    text = meta.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = FaltetForest,
                    maxLines = metaMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (stat != null || actions != null) {
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                stat?.invoke()
                actions?.invoke()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetListRowPreview_Minimal() {
    FaltetListRow(title = "Cosmos bipinnatus", meta = "Cosmos · Batch 12")
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetListRowPreview_Full() {
    FaltetListRow(
        title = "Cosmos bipinnatus",
        meta = "Cosmos · Batch 12 · Sådd vecka 14",
        leading = {
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(FaltetClay)
                    },
            )
        },
        stat = {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("84", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = FaltetInk)
                Text(" STK", fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.2.sp, color = FaltetForest)
            }
        },
        onClick = {},
    )
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0, no output.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetListRow.kt
git commit -m "feat: FaltetListRow primitive"
```

---

### Task 2: `FaltetEmptyState`

Centered editorial empty-state with optional action.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetEmptyState.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetEmptyState.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk

@Composable
fun FaltetEmptyState(
    headline: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FaltetCream)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headline,
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            color = FaltetInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = FaltetForest,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetEmptyStatePreview_NoAction() {
    FaltetEmptyState(
        headline = "Inga frön ännu",
        subtitle = "Börja med att lägga till ditt första frö.",
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetEmptyStatePreview_WithAction() {
    FaltetEmptyState(
        headline = "Inga uppgifter",
        subtitle = "Skapa din första uppgift för säsongen.",
        action = { Text("Lägg till") },
    )
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetEmptyState.kt
git commit -m "feat: FaltetEmptyState primitive"
```

---

### Task 3: `FaltetSectionHeader`

Mono-uppercase label with clay underline rule.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSectionHeader.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSectionHeader.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetForest

@Composable
fun FaltetSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetCream)
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
            color = FaltetForest,
        )
        Spacer(Modifier.height(4.dp))
        Spacer(
            Modifier
                .width(24.dp)
                .height(1.5.dp)
                .background(FaltetClay),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetSectionHeaderPreview() {
    Column {
        FaltetSectionHeader(label = "Idag")
        FaltetSectionHeader(label = "Denna vecka")
        FaltetSectionHeader(label = "Senare")
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSectionHeader.kt
git commit -m "feat: FaltetSectionHeader primitive"
```

---

### Task 4: `FaltetSearchField`

Compact search input with leading icon + clear button. Uses `BasicTextField` directly (not a wrapper over `Field`, which is a labeled-form input with different visual shape).

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSearchField.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSearchField.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20

@Composable
fun FaltetSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(FaltetCream)
            .drawBehind {
                drawLine(
                    color = FaltetInkLine20,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .heightIn(min = 44.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = FaltetForest,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = FaltetInk,
                ),
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = FaltetForest.copy(alpha = 0.6f),
                )
            }
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Rensa",
                    tint = FaltetForest,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetSearchFieldPreview_Empty() {
    FaltetSearchField(value = "", onValueChange = {}, placeholder = "SÖK")
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetSearchFieldPreview_Populated() {
    FaltetSearchField(value = "cosmos", onValueChange = {}, placeholder = "SÖK")
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetSearchField.kt
git commit -m "feat: FaltetSearchField primitive"
```

---

### Task 5: `FaltetCheckbox`

Fältet-styled wrapper over Material `Checkbox`.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetCheckbox.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetCheckbox.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetInkLine40

@Composable
fun FaltetCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = FaltetClay,
            uncheckedColor = FaltetInkLine40,
            checkmarkColor = FaltetCream,
        ),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetCheckboxPreview() {
    Row {
        FaltetCheckbox(checked = false, onCheckedChange = {})
        Spacer(Modifier.width(8.dp))
        FaltetCheckbox(checked = true, onCheckedChange = {})
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetCheckbox.kt
git commit -m "feat: FaltetCheckbox primitive"
```

---

### Task 6: `FaltetStepper`

Horizontal `[−] value [+]` integer stepper.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetStepper.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetStepper.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import app.verdant.android.ui.theme.FaltetInkLine40

@Composable
fun FaltetStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = Int.MAX_VALUE,
) {
    val decEnabled = value > min
    val incEnabled = value < max
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        StepperButton(
            icon = Icons.Default.Remove,
            enabled = decEnabled,
            onClick = onDecrement,
            contentDescription = "Minska",
        )
        Text(
            text = value.toString(),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = FaltetInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 24.dp),
        )
        StepperButton(
            icon = Icons.Default.Add,
            enabled = incEnabled,
            onClick = onIncrement,
            contentDescription = "Öka",
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val borderColor = if (enabled) FaltetInkLine40 else FaltetInkLine20
    val iconTint = if (enabled) FaltetInk else FaltetForest.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, borderColor)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetStepperPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FaltetStepper(value = 5, onDecrement = {}, onIncrement = {}, min = 0, max = 10)
        FaltetStepper(value = 0, onDecrement = {}, onIncrement = {}, min = 0, max = 10)
        FaltetStepper(value = 10, onDecrement = {}, onIncrement = {}, min = 0, max = 10)
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetStepper.kt
git commit -m "feat: FaltetStepper primitive"
```

---

### Task 7: `FaltetLoadingState`

Shared loading spinner — consolidates the current per-screen inline `CircularProgressIndicator`.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetLoadingState.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetLoadingState.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream

@Composable
fun FaltetLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = FaltetClay,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetLoadingStatePreview() {
    FaltetLoadingState()
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetLoadingState.kt
git commit -m "feat: FaltetLoadingState primitive"
```

---

## Phase 2 — Reference ports

Validate the primitive API against two screens. One checkpoint commit at the end.

---

### Task 8: Port `PlantedSpeciesListScreen`

Shortest, simplest list screen — validates the basic slot pattern.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/plants/PlantedSpeciesListScreen.kt`

**Context for the agent:**
- The current file has a `TopAppBar` + manual `OutlinedTextField` search + `Card`-based rows.
- The ViewModel (`PlantedSpeciesListViewModel`), state shape (`PlantedSpeciesListState`), and `VoiceCommandOverlay` wrapper must be preserved verbatim. Only the visible `Scaffold` body changes.
- The screen has no pagination; do not add any.
- The current `Scaffold { Column { OutlinedTextField + when { ... } } }` becomes `FaltetScreenScaffold { ... when { ... } }` with the search field as a `stickyHeader` in the `LazyColumn`.

**Target row mapping (spec §5.2):**
- `leading = null` (no status mark on this screen)
- `title = species.speciesName`
- `meta = species.scientificName` (already lowercase/mixed in source — wrap with `.uppercase()` via the primitive)
- `stat = "${species.activePlantCount} STK"` composable
- `actions = null`
- `onClick = { onSpeciesClick(species.speciesId) }`

**Masthead:** `left = "§ Odling"`, `center = "Utplanterade"`, `right = null`.

**Empty state:** `headline = "Inga utplanterade arter"`, `subtitle = "Börja med att plantera en sådd utomhus."`, `action = null`.

- [ ] **Step 1: Read the current file**

Open `android/app/src/main/kotlin/app/verdant/android/ui/plants/PlantedSpeciesListScreen.kt` to see the current structure. Lines 1–119 (imports, state, ViewModel) remain unchanged. Lines 121–250 (the `PlantedSpeciesListScreen` composable) are rewritten.

- [ ] **Step 2: Replace the imports block (lines 1–37)**

Replace everything from `package app.verdant.android.ui.plants` through the last `import javax.inject.Inject` with:

```kotlin
package app.verdant.android.ui.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.BatchEventRequest
import app.verdant.android.data.model.BatchSowRequest
import app.verdant.android.data.model.SpeciesPlantSummary
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSearchField
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.voice.VoiceCommandOverlay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
```

- [ ] **Step 3: Replace the composable body (lines 121–250)**

Replace from `@OptIn(ExperimentalMaterial3Api::class)` to the final `}` of the composable (just before the EOF) with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantedSpeciesListScreen(
    onBack: () -> Unit,
    onSpeciesClick: (Long) -> Unit,
    viewModel: PlantedSpeciesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    VoiceCommandOverlay(
        speciesList = uiState.speciesList,
        supplyList = uiState.supplyList,
        onPlantActivity = { action, quantity, species ->
            viewModel.executePlantActivity(action, quantity, species)
        },
        onSupplyUsage = { supply, quantity ->
            viewModel.executeSupplyUsage(supply, quantity)
        },
    ) {
        val filtered = remember(uiState.species, searchQuery) {
            if (searchQuery.isBlank()) uiState.species
            else {
                val q = searchQuery.lowercase()
                uiState.species.filter {
                    it.speciesName.lowercase().contains(q) ||
                        (it.scientificName?.lowercase()?.contains(q) == true)
                }
            }
        }

        FaltetScreenScaffold(
            mastheadLeft = "§ Odling",
            mastheadCenter = "Utplanterade",
        ) { padding ->
            when {
                uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
                uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ConnectionErrorState(onRetry = { viewModel.load() })
                }
                filtered.isEmpty() && searchQuery.isBlank() -> FaltetEmptyState(
                    headline = "Inga utplanterade arter",
                    subtitle = "Börja med att plantera en sådd utomhus.",
                    modifier = Modifier.padding(padding),
                )
                else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    item {
                        FaltetSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "SÖK ART",
                        )
                    }
                    items(filtered, key = { it.speciesId }) { species ->
                        FaltetListRow(
                            title = species.speciesName,
                            meta = species.scientificName,
                            stat = {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = species.activePlantCount.toString(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp,
                                        color = FaltetInk,
                                    )
                                    Text(
                                        text = " STK",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.2.sp,
                                        color = FaltetForest,
                                    )
                                }
                            },
                            onClick = { onSpeciesClick(species.speciesId) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/plants/PlantedSpeciesListScreen.kt
git commit -m "feat: Fältet port — PlantedSpeciesList"
```

---

### Task 9: Port `SupplyInventoryScreen`

The complex reference port — preserves the two-level expandable structure (category → supply-type group → batches) and the "Record usage" `AlertDialog`.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/supplies/SupplyInventoryScreen.kt`

**Context for the agent:**
- The current file has: `SupplyInventoryState`, `SupplyInventoryViewModel`, helpers (`CATEGORY_ORDER`, `categoryLabel`, `categoryLabelSv`, `formatQuantity`, `SupplyTypeGroup`), a use-dialog, and `SupplyTypeGroupCard` (expandable).
- Preserve everything except the `Scaffold` body and `SupplyTypeGroupCard`. The `AlertDialog` for "Record usage" stays — it's platform-idiomatic and spec §1 allows it.
- The `SupplyTypeGroupCard` is replaced with a `FaltetListRow` top row that expands to a batch sub-list on tap.
- There is **no search, no filter, no stepper** in the current implementation. Do not add them. (Spec §5.2 predicted these — they do not exist.)

**Masthead:** `left = "§ Inventarie"`, `center = "Förnödenheter"`, `right = null`.

**Empty state:** `headline = "Inga förnödenheter"`, `subtitle = "Lägg till din första inventarierad förnödenhet."`, `action = null`.

**Row mapping (top row of each supply-type group):**
- `leading = Icon(categoryIcon(category), tint = FaltetForest)` at 18dp — helper to add inline
- `title = group.name`
- `meta = categoryLabelSv(category)`
- `stat = formatQuantity(group.totalQuantity, group.unit)` as mono text
- `actions = Icon(if (expanded) ExpandLess else ExpandMore, tint = FaltetForest)`
- `onClick = { expanded = !expanded }`

**Expanded sub-rows (per batch):** rendered inline below the row, `padding(start = 54.dp, end = 18.dp, top = 8.dp, bottom = 8.dp)`, with a thin `FaltetInkLine20` bottom rule between batches. Each batch row shows quantity (mono 14sp ink), optional notes (body 12sp forest), optional cost (mono 12sp forest), and a "Använd" text button on the right.

- [ ] **Step 1: Read the current file**

Open `android/app/src/main/kotlin/app/verdant/android/ui/supplies/SupplyInventoryScreen.kt` (385 lines). Identify the unchanged blocks: imports (1–44 minus replacements), `SupplyInventoryState` (41–46), `SupplyInventoryViewModel` (48–84), `CATEGORY_ORDER` / `categoryLabel` / `categoryLabelSv` / `formatQuantity` / `SupplyTypeGroup` (86–123). Identify the changing blocks: imports that become unused, `SupplyInventoryScreen` body (125–287), `SupplyTypeGroupCard` (289–385).

- [ ] **Step 2: Replace the import block**

Replace the entire top-of-file import block (lines 1–37, up to and including `import javax.inject.Inject`) with:

```kotlin
package app.verdant.android.ui.supplies

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import app.verdant.android.data.model.SupplyInventoryResponse
import app.verdant.android.data.repository.GardenRepository
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine20
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
```

- [ ] **Step 3: Keep lines 39–123 unchanged**

`TAG`, `SupplyInventoryState`, `SupplyInventoryViewModel`, `CATEGORY_ORDER`, `categoryLabel`, `categoryLabelSv`, `formatQuantity`, `SupplyTypeGroup` all remain as-is.

- [ ] **Step 4: Add a category icon helper**

After the `SupplyTypeGroup` declaration (around line 123), add:

```kotlin
private fun categoryIcon(category: String): ImageVector = when (category) {
    "SOIL" -> Icons.Default.Grass
    "POT" -> Icons.Default.Inventory2
    "FERTILIZER" -> Icons.Default.Science
    "TRAY" -> Icons.Default.Inventory2
    "LABEL" -> Icons.Default.Label
    "OTHER" -> Icons.Default.Category
    else -> Icons.Default.Category
}
```

- [ ] **Step 5: Replace the `SupplyInventoryScreen` composable (lines 125–287)**

Replace the entire `@OptIn(ExperimentalMaterial3Api::class) @Composable fun SupplyInventoryScreen(...)` block with:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyInventoryScreen(
    onBack: () -> Unit,
    viewModel: SupplyInventoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    var useDialogBatch by remember { mutableStateOf<SupplyInventoryResponse?>(null) }
    var useAmount by remember { mutableStateOf("") }

    if (useDialogBatch != null) {
        val batch = useDialogBatch!!
        AlertDialog(
            onDismissRequest = { useDialogBatch = null; useAmount = "" },
            title = { Text("Registrera användning") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(batch.supplyTypeName)
                    Text(
                        "Tillgängligt: ${formatQuantity(batch.quantity, batch.unit)}",
                        fontSize = 13.sp,
                        color = FaltetForest,
                    )
                    OutlinedTextField(
                        value = useAmount,
                        onValueChange = { useAmount = it },
                        label = { Text("Använd antal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                val qty = useAmount.toDoubleOrNull()
                TextButton(
                    onClick = {
                        if (qty != null && qty > 0) {
                            viewModel.decrement(batch.id, qty)
                            useDialogBatch = null
                            useAmount = ""
                        }
                    },
                    enabled = qty != null && qty > 0 && qty <= batch.quantity,
                ) { Text("Använd") }
            },
            dismissButton = {
                TextButton(onClick = { useDialogBatch = null; useAmount = "" }) { Text("Avbryt") }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Inventarie",
        mastheadCenter = "Förnödenheter",
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null && uiState.items.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.items.isEmpty() -> FaltetEmptyState(
                headline = "Inga förnödenheter",
                subtitle = "Lägg till din första inventarierad förnödenhet.",
                modifier = Modifier.padding(padding),
            )
            else -> {
                val grouped = remember(uiState.items) {
                    uiState.items
                        .groupBy { it.category }
                        .toSortedMap(compareBy { CATEGORY_ORDER.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE })
                        .mapValues { (_, items) ->
                            items.groupBy { it.supplyTypeId }
                                .map { (typeId, batches) ->
                                    SupplyTypeGroup(
                                        supplyTypeId = typeId,
                                        name = batches.first().supplyTypeName,
                                        unit = batches.first().unit,
                                        totalQuantity = batches.sumOf { it.quantity },
                                        batches = batches.sortedByDescending { it.quantity },
                                    )
                                }
                                .sortedBy { it.name }
                        }
                }
                LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                    grouped.forEach { (category, typeGroups) ->
                        item(key = "header_$category") {
                            FaltetSectionHeader(label = categoryLabelSv(category))
                        }
                        items(typeGroups, key = { "type_${it.supplyTypeId}" }) { typeGroup ->
                            SupplyTypeFaltetRow(
                                group = typeGroup,
                                category = category,
                                isDecrementing = uiState.decrementingId,
                                onUseBatch = { batch ->
                                    useDialogBatch = batch
                                    useAmount = formatQuantity(batch.quantity, "").trim()
                                },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
```

- [ ] **Step 6: Replace `SupplyTypeGroupCard` with `SupplyTypeFaltetRow`**

Replace the `SupplyTypeGroupCard` composable (previously at lines 289–385) with:

```kotlin
@Composable
private fun SupplyTypeFaltetRow(
    group: SupplyTypeGroup,
    category: String,
    isDecrementing: Long?,
    onUseBatch: (SupplyInventoryResponse) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        FaltetListRow(
            title = group.name,
            meta = categoryLabelSv(category),
            leading = {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = FaltetForest,
                    modifier = Modifier.size(18.dp),
                )
            },
            stat = {
                Text(
                    text = formatQuantity(group.totalQuantity, group.unit),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = FaltetInk,
                )
            },
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
            Column(Modifier.fillMaxWidth()) {
                group.batches.forEachIndexed { index, batch ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                if (index < group.batches.size - 1) {
                                    drawLine(
                                        color = FaltetInkLine20,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx(),
                                    )
                                }
                            }
                            .padding(start = 54.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = formatQuantity(batch.quantity, batch.unit),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = FaltetInk,
                            )
                            batch.notes?.let { notes ->
                                Text(
                                    text = notes,
                                    fontSize = 12.sp,
                                    color = FaltetForest,
                                )
                            }
                            batch.costSek?.let { cost ->
                                Text(
                                    text = "${cost / 100.0} kr",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = FaltetForest,
                                )
                            }
                        }
                        if (isDecrementing == batch.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = FaltetClay,
                            )
                        } else {
                            TextButton(onClick = { onUseBatch(batch) }) {
                                Text("Använd", color = FaltetClay, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 7: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 8: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/supplies/SupplyInventoryScreen.kt
git commit -m "feat: Fältet port — SupplyInventory"
```

- [ ] **Step 9: Phase 2 checkpoint**

Install + smoke on emulator or device:

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Manually verify both screens:
- **PlantedSpeciesList** — title Fraunces italic, meta mono uppercase, stat shows "N STK" with mono digits and smaller STK label, search field sticky at top, empty state readable.
- **SupplyInventory** — category section headers with clay rule underneath, supply-type rows with chevron, tapping expands a sub-list of batches with the "Använd" button.

If the `FaltetListRow` slot API turns out wrong (e.g., insufficient vertical alignment control, missing a slot), amend the primitive in a new commit (don't rewrite Phase 1 history) and fix these two reference screens before Phase 3.

---

## Phase 3 — Batch ports

Each task follows the same shape: replace the `Scaffold` + card rendering with `FaltetScreenScaffold` + `FaltetListRow`, preserve the ViewModel and state, update the empty state.

**Per-screen template** — each batch task replaces existing `Scaffold { ... when (state) { ... } }` with:

```kotlin
FaltetScreenScaffold(
    mastheadLeft = "…",
    mastheadCenter = "…",
    fab = { /* FaltetFab(onClick = ..., contentDescription = ...) if screen has an add button */ },
) { padding ->
    when {
        uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
        uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            ConnectionErrorState(onRetry = { viewModel.load() })
        }
        uiState.items.isEmpty() -> FaltetEmptyState(
            headline = "…",
            subtitle = "…",
            modifier = Modifier.padding(padding),
        )
        else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(uiState.items, key = { it.id }) { row ->
                FaltetListRow(
                    title = /* from row */,
                    meta = /* from row */,
                    leading = /* per mapping */,
                    stat = /* per mapping */,
                    actions = /* per mapping */,
                    onClick = { /* existing nav callback */ },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
```

**Common imports to add to every batch-ported screen:**

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
```

Add `FaltetFab` / `FaltetSectionHeader` / `FaltetSearchField` / `FaltetCheckbox` / `FaltetStepper` imports only when used.

**Every batch task runs compile before commit:**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

---

### Task 10: Port `TaskListScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/task/TaskListScreen.kt`

**Instructions for the agent:**
- Read the file in full first to understand: ViewModel (`TaskListViewModel`), state (`TaskListState` with `tasks: List<ScheduledTaskResponse>`), and the tone/color logic for priority/status dots.
- Preserve ViewModel methods (`loadTasks`, `deleteTask`, etc.) and all navigation callbacks.
- The existing UX may have a delete affordance rather than a checkbox. Check: if there is no completion action in the ViewModel, do not add one — keep `actions = null` for now (sub-spec C's activity screens will own task-completion).
- Group tasks by due-date section: compute sections client-side as `Idag` (today), `I morgon` (tomorrow), `Denna vecka` (this week, excluding today/tomorrow), `Senare` (later), `Förfallna` (overdue — if any task is before today). Emit `FaltetSectionHeader` before each group. Skip empty sections.

**Masthead:** `left = "§ Arbete"`, `center = "Uppgifter"`.

**Empty state:** `headline = "Inga uppgifter"`, `subtitle = "Skapa din första uppgift för säsongen."`, `action = null`.

**FAB:** present — `FaltetFab(onClick = { onAddTask() }, contentDescription = "Lägg till uppgift")` (hook up to whatever nav callback exists in the current signature; typical name is `onCreateTask` or `onAddTask`).

**Row mapping:**
- `leading` = 10dp colored dot — color by task priority or status (inherit current logic, but render as `Box(Modifier.size(10.dp).drawBehind { drawCircle(toneColor) })` instead of the current chip)
- `title` = `task.title`
- `meta` = `"$dueDate · $speciesName"` (fall back to `"$dueDate"` only if no species) — compute from task's existing date/species fields
- `stat` = null
- `actions` = null
- `onClick` = `{ onTaskClick(task.id) }` (whatever the existing callback is)

- [ ] **Step 1: Read the current file and identify imports/VM/state**

```bash
wc -l android/app/src/main/kotlin/app/verdant/android/ui/task/TaskListScreen.kt
```

Read the entire file. Note the exact navigation callbacks in the `@Composable fun TaskListScreen(...)` signature.

- [ ] **Step 2: Rewrite the body using the per-screen template**

Replace imports (keep the `data class`, ViewModel, helpers), replace the `@Composable fun TaskListScreen` body with the template. Add the date-grouping logic as a `remember` block over `uiState.tasks`.

- [ ] **Step 3: Compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 4: Add a `@Preview`**

Add a preview composable at the end of the file exercising a 3-row populated state and an empty state.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/task/TaskListScreen.kt
git commit -m "feat: Fältet port — TaskList"
```

---

### Task 11: Port `SeedInventoryScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/inventory/SeedInventoryScreen.kt`

**Instructions for the agent:**
- Read the file fully first. The current implementation has increment/decrement buttons inline. Replace those with `FaltetStepper` only if the ViewModel exposes `onIncrement(id)` / `onDecrement(id)` actions — otherwise preserve whatever action model exists and wire into `FaltetStepper(onDecrement = ..., onIncrement = ...)`.
- If the current screen uses a quantity dialog (like SupplyInventory), preserve it and use `FaltetListRow` with `actions = Icon(MoreVert)` or a plain `TextButton("Använd")` — do not invent a stepper where there isn't one.

**Masthead:** `left = "§ Inventarie"`, `center = "Fröförråd"`.

**Empty state:** `headline = "Inga frön ännu"`, `subtitle = "Börja med att lägga till ditt första frö."`, `action = null`.

**Row mapping (default, no stepper):**
- `leading = null`
- `title` = variety display name (e.g., `"${seed.commonName} ${seed.variantName ?: ""}".trim()`)
- `meta` = species scientific name if available
- `stat` = quantity + unit as mono text (`Text("${seed.quantity} ${seed.unit}", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = FaltetInk)`)
- `actions` = `FaltetStepper(...)` if VM exposes inc/dec actions, otherwise null
- `onClick` = existing nav callback (may be null)

- [ ] **Step 1: Read the current file and identify the inc/dec or dialog pattern**

- [ ] **Step 2: Rewrite body per template, wiring stepper only if VM supports it**

- [ ] **Step 3: Compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 4: Add a `@Preview`**

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/inventory/SeedInventoryScreen.kt
git commit -m "feat: Fältet port — SeedInventory"
```

---

### Task 12: Port `SpeciesListScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/activity/SpeciesListScreen.kt`

**Masthead:** `left = "§ Sort"`, `center = "Sorter"`.

**Empty state:** `headline = "Inga sorter"`, `subtitle = "Lägg till en sort för att börja."`, `action = null`.

**Row mapping:**
- `leading` = 10dp dot in a tone mapped from species category (use `FaltetTone` if there's a category-to-tone helper; otherwise pick a stable tone per-category inline — e.g. `Sage` for veggies, `Blush` for cut flowers, default `Mustard`)
- `title` = species common name
- `meta` = category label (Swedish)
- `stat` = batch count as "N BATCHAR" (mono 16sp ink + mono 9sp forest trailing label, like PlantedSpeciesList)
- `actions` = null
- `onClick` = existing species-click callback

- [ ] **Step 1: Read the current file**
- [ ] **Step 2: Rewrite body per template**
- [ ] **Step 3: Compile**
- [ ] **Step 4: Add a `@Preview`**
- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/activity/SpeciesListScreen.kt
git commit -m "feat: Fältet port — SpeciesList"
```

---

### Task 13: Port `SuccessionSchedulesScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/succession/SuccessionSchedulesScreen.kt`

**Masthead:** `left = "§ Plan"`, `center = "Successioner"`.

**Empty state:** `headline = "Inga successioner"`, `subtitle = "Planera din första succession."`, `action = null`.

**Grouping:** group rows by status (Planerad / Sådd / Utplanterad / Avslutad). Emit `FaltetSectionHeader(label = statusLabel(status))` before each group. Skip empty groups.

**Row mapping:**
- `leading` = 10dp status dot (colors: `Planerad`=`FaltetSky`, `Sådd`=`FaltetMustard`, `Utplanterad`=`FaltetSage`, `Avslutad`=`FaltetInkLine40`)
- `title` = species name
- `meta` = `"Vecka ${weekNumber} · ${statusLabel(status)}"`
- `stat` = plant count as `"N ST"` (mono 16sp + mono 9sp)
- `actions` = null

- [ ] **Step 1: Read the current file**
- [ ] **Step 2: Rewrite body per template with grouping**
- [ ] **Step 3: Compile**
- [ ] **Step 4: Add a `@Preview`**
- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/succession/SuccessionSchedulesScreen.kt
git commit -m "feat: Fältet port — SuccessionSchedules"
```

---

### Task 14: Port `ProductionTargetsScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/targets/ProductionTargetsScreen.kt`

**Masthead:** `left = "§ Plan"`, `center = "Produktionsmål"`.

**Empty state:** `headline = "Inga produktionsmål"`, `subtitle = "Sätt upp mål för säsongens produktion."`, `action = null`.

**Row mapping:**
- `leading = null`
- `title` = species name
- `meta` = period label (e.g., `"2026 · V. 14–26"`)
- `stat` = split composable: `"{actual} / {target}"` with `{actual}` in Fraunces italic 16sp ink and `"/ {target}"` in mono 14sp forest, laid out in a `Row(verticalAlignment = Alignment.Bottom)`
- `actions` = null
- `onClick` = existing callback

- [ ] **Step 1: Read the current file**
- [ ] **Step 2: Rewrite body per template; inline the split-stat composable in the `stat` slot**
- [ ] **Step 3: Compile**
- [ ] **Step 4: Add a `@Preview`**
- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/targets/ProductionTargetsScreen.kt
git commit -m "feat: Fältet port — ProductionTargets"
```

---

### Task 15: Port `VarietyTrialsScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/trials/VarietyTrialsScreen.kt`

**Masthead:** `left = "§ Forskning"`, `center = "Sortförsök"`.

**Empty state:** `headline = "Inga sortförsök"`, `subtitle = "Starta ditt första försök."`, `action = null`.

**Row mapping:**
- `leading = null`
- `title` = variety name
- `meta` = `"${year} · ${speciesCommonName}"`
- `stat` = existing `Chip` primitive for result (e.g., "OK" / "FAVORIT" / "AVBRUTEN"), using appropriate `FaltetTone`
- `actions` = null

- [ ] **Step 1: Read the current file**
- [ ] **Step 2: Rewrite body per template**
- [ ] **Step 3: Compile**
- [ ] **Step 4: Add a `@Preview`**
- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/trials/VarietyTrialsScreen.kt
git commit -m "feat: Fältet port — VarietyTrials"
```

---

### Task 16: Port `BouquetRecipesScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/bouquet/BouquetRecipesScreen.kt`

**Instructions:**
- Do NOT reuse `PhotoPlaceholder` in the leading slot — it's built for full-bleed hero images with `fillMaxWidth().aspectRatio(...)` and won't fit a 24dp cell. Render a mini thumb inline:

```kotlin
leading = {
    Box(
        Modifier
            .size(24.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        FaltetBlush.copy(alpha = 0.35f),
                        FaltetBlush.copy(alpha = 0.12f),
                        FaltetCream,
                    ),
                ),
            )
            .border(1.dp, FaltetInk),
    )
}
```

**Masthead:** `left = "§ Bukett"`, `center = "Recept"`.

**Empty state:** `headline = "Inga recept"`, `subtitle = "Designa din första bukett."`, `action = null`.

**Row mapping:**
- `leading` = mini radial thumb (above)
- `title` = recipe name
- `meta` = `"${stemCount} stjälkar · ${seasonLabel}"`
- `stat` = price in Fraunces 16sp ink (`"${price} KR"`)
- `actions` = null

- [ ] **Step 1: Read the current file**
- [ ] **Step 2: Rewrite body per template with mini thumb in leading slot**
- [ ] **Step 3: Compile**
- [ ] **Step 4: Add a `@Preview`**
- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/bouquet/BouquetRecipesScreen.kt
git commit -m "feat: Fältet port — BouquetRecipes"
```

---

### Task 17: Port `CustomerListScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/customer/CustomerListScreen.kt`

**Masthead:** `left = "§ Kund"`, `center = "Kunder"`.

**Empty state:** `headline = "Inga kunder"`, `subtitle = "Lägg till din första kund."`, `action = null`.

**Row mapping:**
- `leading = null`
- `title` = customer name
- `meta` = segment label (or "—" if none)
- `stat = null`
- `actions` = two `IconButton`s for phone + email, tint `FaltetClay`, 36dp size, only rendered when the corresponding value is non-null:

```kotlin
actions = {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        customer.phone?.let {
            IconButton(onClick = { onPhoneClick(it) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Phone, "Ring", tint = FaltetClay, modifier = Modifier.size(18.dp))
            }
        }
        customer.email?.let {
            IconButton(onClick = { onEmailClick(it) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Email, "Mejla", tint = FaltetClay, modifier = Modifier.size(18.dp))
            }
        }
    }
}
```

If the existing ViewModel / screen signature doesn't expose `onPhoneClick` / `onEmailClick`, preserve whatever it does (e.g., `Intent(ACTION_DIAL)` via `LocalContext.current`) and wrap in a local lambda.

- [ ] **Step 1: Read the current file**
- [ ] **Step 2: Rewrite body per template with inline phone/email IconButtons**
- [ ] **Step 3: Compile**
- [ ] **Step 4: Add a `@Preview`**
- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/customer/CustomerListScreen.kt
git commit -m "feat: Fältet port — CustomerList"
```

---

### Task 18: Port `PestDiseaseLogScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/pest/PestDiseaseLogScreen.kt`

**Instructions:**
- This screen has the most complexity (~524 lines). Preserve all existing filter / search state and the ViewModel.
- If the current screen has a search field, sticky-header it using `FaltetSearchField`. If it has filter chips (category / severity), keep them as an inline `Row` of `Chip` primitive below the search field (also inside the sticky header `item`).

**Masthead:** `left = "§ Hälsa"`, `center = "Skadegörare"`.

**Empty state:** `headline = "Inga observationer"`, `subtitle = "Logga din första skadegörar-observation."`, `action = null`.

**Row mapping:**
- `leading` = 10dp severity dot (`Låg`=`FaltetSage`, `Medel`=`FaltetMustard`, `Hög`=`FaltetClay`, `Kritisk`=`FaltetBerry`)
- `title` = observation title (species + short description or "${species}: ${pest}" pattern — use whatever the existing card already displays as the primary line)
- `meta` = `"${formattedDate} · ${speciesName}"`, render with `metaMaxLines = 2` so the description can wrap
- `stat = null`
- `actions = null`

- [ ] **Step 1: Read the current file — identify search/filter state if any**
- [ ] **Step 2: Rewrite body per template, sticky-header search + filter chips if present**
- [ ] **Step 3: Compile**
- [ ] **Step 4: Add a `@Preview`**
- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/pest/PestDiseaseLogScreen.kt
git commit -m "feat: Fältet port — PestDiseaseLog"
```

---

### Task 19: Port `MyVerdantWorldScreen`

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/world/MyVerdantWorldScreen.kt`

**Masthead:** `left = "§ Min värld"`, `center = "Trädgårdar"`.

**Empty state:** `headline = "Inga trädgårdar"`, `subtitle = "Skapa din första trädgård."`, `action = null` (FAB in scaffold handles the add-flow).

**Row mapping:**
- `leading = null`
- `title` = garden name
- `meta` = location (or "—" if none)
- `stat` = bed count as `"N BÄDDAR"` (mono 16sp + mono 9sp)
- `actions = null`
- `onClick` = existing garden-click callback

- [ ] **Step 1: Read the current file**
- [ ] **Step 2: Rewrite body per template**
- [ ] **Step 3: Compile**
- [ ] **Step 4: Add a `@Preview`**
- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/world/MyVerdantWorldScreen.kt
git commit -m "feat: Fältet port — MyVerdantWorld"
```

---

## Phase 4 — Verify + milestone

---

### Task 20: Verify + milestone

- [ ] **Step 1: Full Android build**

```bash
cd android && ./gradlew assembleDebug --no-daemon -q
```

Expected: `BUILD SUCCESSFUL`. APK at `android/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Install + manual smoke**

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

Open each ported screen in order. For each, verify:
- Cream background, ink primary text, forest secondary text.
- Fraunces italic in row titles; mono uppercase in meta and headers.
- Clay accents on active states (chevron, FAB, checkbox, stepper border when enabled).
- Hairline bottom borders between rows.
- Zero corner radius on rows/cards/buttons.
- Empty states readable and per-screen copy correct.
- No layout breaks or crashes on scroll.

Screen checklist:

- [ ] PlantedSpeciesList
- [ ] SupplyInventory (expand a group, tap "Använd", dismiss dialog)
- [ ] TaskList (grouping, dots colored correctly)
- [ ] SeedInventory
- [ ] SpeciesList
- [ ] SuccessionSchedules (status groups)
- [ ] ProductionTargets (split stat)
- [ ] VarietyTrials
- [ ] BouquetRecipes (mini thumb)
- [ ] CustomerList (phone/email buttons)
- [ ] PestDiseaseLog (search + filter if present)
- [ ] MyVerdantWorld

- [ ] **Step 3: Milestone commit**

```bash
git commit --allow-empty -m "milestone: Android Fältet lists complete (Spec B)"
```

---

## Verification summary

After Task 20:

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/` contains 7 new primitives (`FaltetListRow`, `FaltetEmptyState`, `FaltetSectionHeader`, `FaltetSearchField`, `FaltetCheckbox`, `FaltetStepper`, `FaltetLoadingState`) plus the Spec-A primitives (`Chip`, `Rule`, `Stat`, `Field`, `PhotoPlaceholder`, `Masthead`, `FaltetScreenScaffold`, `FaltetFab`, `FaltetTone`).
- 12 list screens render with Fältet editorial styling.
- `./gradlew assembleDebug` green.
- Existing ViewModels, navigation callbacks, and dialogs unchanged.

**Follow-ups:**
- Sub-spec C — detail / forms / activity screens.
- Sub-spec D — analytics / account / auth + deferred MyWorld dashboard and SeasonSelector.

---

## Self-review notes

- **Spec §1 (goal):** Tasks 1–20 collectively achieve the port.
- **Spec §2 (scope):** 12 screens → Tasks 8, 9, 10–19 cover all 12.
- **Spec §3 (decisions):** Named-slot `FaltetListRow` → Task 1. Editorial empty state → Task 2. Mono+clay section header → Task 3. Search field → Task 4. Checkbox + stepper → Tasks 5, 6. Loading state → Task 7.
- **Spec §4 (primitives):** Each primitive has its own task with full inline code.
- **Spec §5 (port pattern):** Template in Phase 3 intro; per-screen row mapping in the table carried into each task.
- **Spec §6 (phasing):** 7 + 2 + 10 + 1 = 20 tasks, matches design doc exactly.
- **Spec §7 (testing):** `@Preview` required per primitive (Tasks 1–7) and per screen (Steps labeled "Add a @Preview" in Tasks 10–19; PlantedSpeciesList + SupplyInventory previews folded into Phase 2 smoke).
- **Spec reality-check corrections:** SupplyInventory adjusted for expandable structure (Task 9); TaskList guarded against inventing a checkbox (Task 10); pagination noted as web-only and not added.
