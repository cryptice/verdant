# Android Fältet — Sub-Spec D (Finale) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the Android Fältet port — ship `FaltetAvatar` primitive and Fältet-style the final 5 un-ported screens (`Splash`, `Auth`, `Analytics`, `Account`, `SeasonSelector`).

**Architecture:** One primitive commit + 5 independent screen ports + milestone. Each screen uses a different primitive-composition pattern (wordmark, full-bleed editorial, stat-dense list, hero+actions, list+dialog) so no reference port pays off. Swedish labels throughout. Preserve ViewModels, callbacks, and business logic verbatim.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 (`AlertDialog`, `CircularProgressIndicator`), Coil (`AsyncImage` for avatar), existing Fältet primitive set.

**Spec:** `docs/plans/2026-04-23-android-faltet-finale-design.md` — read before starting.

**Reality-check notes:**

- All 5 target screens already exist in the codebase — this is a rendering port, preserve VM + state + callbacks verbatim.
- Swedish inline copy replaces `R.string.*` references throughout.
- The spec calls for `FaltetChipSelector` with an empty label in Account — if that renders with a dangling empty row, drop the primitive and inline a `FlowRow` of two `FilterChip`s (documented fallback in spec §5.4).
- Auth has no scaffold — pure `Box` + `Column`. Splash has no scaffold — pure `Box`.
- `FaltetAvatar` uses Coil `AsyncImage`. Coil is already a dependency (used by `AsyncImage` in BedDetail/PlantDetail).

---

## File Structure

### New files

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetAvatar.kt`

### Modified files

- `android/app/src/main/kotlin/app/verdant/android/ui/splash/SplashScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/auth/AuthScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/analytics/AnalyticsScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/account/AccountScreen.kt`
- `android/app/src/main/kotlin/app/verdant/android/ui/season/SeasonSelectorScreen.kt`

---

## Phase 1 — Primitive

---

### Task 1: `FaltetAvatar`

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetAvatar.kt`

- [ ] **Step 1: Write the primitive**

```kotlin
// android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetAvatar.kt
package app.verdant.android.ui.faltet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetInk
import app.verdant.android.ui.theme.FaltetInkLine40
import coil.compose.AsyncImage

@Composable
fun FaltetAvatar(
    url: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(FaltetCream)
            .border(1.dp, FaltetInkLine40, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val initials = displayName?.trim()?.takeIf { it.isNotBlank() }
                ?.substring(0, 1)
                ?.uppercase()
                ?: "?"
            Text(
                text = initials,
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = (size.value * 0.4).sp,
                color = FaltetInk,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetAvatarPreview_Initials() {
    FaltetAvatar(url = null, displayName = "Erik Lindblad")
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetAvatarPreview_Unknown() {
    FaltetAvatar(url = null, displayName = null)
}

@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun FaltetAvatarPreview_Large() {
    FaltetAvatar(url = null, displayName = "Astrid", size = 88.dp)
}
```

- [ ] **Step 2: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

Expected: exit 0.

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetAvatar.kt
git commit -m "feat: FaltetAvatar primitive"
```

---

## Phase 2 — Screen ports

---

### Task 2: Port Splash

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/splash/SplashScreen.kt`

**Context:** 85 lines. Bootstrap-only screen (token check + silent Google sign-in). Body is a blank `Box`. Replace with cream-background Box + centered Fraunces "VERDANT" wordmark. Preserve ALL bootstrap logic and navigation.

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/splash/SplashScreen.kt` in full. Identify:
- Screen composable signature (callbacks: typically `onNavigateToAuth`, `onNavigateToMyWorld` or similar)
- The `LaunchedEffect` that does token check + silent sign-in + navigation
- The current body (should be just `Box { }` — possibly with no background color)

- [ ] **Step 2: Add imports**

Ensure these imports are present (add if missing, remove any now-unused imports):

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetInk
```

- [ ] **Step 3: Replace the body**

The bootstrap `LaunchedEffect` stays. Replace the visible body (the `Box` at the end of the composable) with:

```kotlin
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "VERDANT",
            fontFamily = FaltetDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 48.sp,
            color = FaltetInk,
            letterSpacing = 2.sp,
        )
    }
```

- [ ] **Step 4: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/splash/SplashScreen.kt
git commit -m "feat: Fältet port — Splash wordmark"
```

---

### Task 3: Port Auth

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/auth/AuthScreen.kt`

**Context:** 168 lines. Single-screen Google sign-in with Credential Manager. Replace the visible body with a full-bleed editorial layout — no `Scaffold`, no `TopAppBar`, no `FaltetScreenScaffold`.

**Preserve verbatim:**
- Package, `AuthViewModel`, all Credential Manager flow, Google `idToken` submission, all navigation callbacks.
- Error state handling (inline text display).

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/auth/AuthScreen.kt` in full. Identify:
- Composable signature (likely `onAuthenticated: () -> Unit` or similar)
- VM method for Google sign-in (takes `Context` or `Activity` for Credential Manager)
- State field names (`isLoading`, `error`, `authenticated`/`signedIn`)
- How error is currently shown

- [ ] **Step 2: Replace imports**

```kotlin
package app.verdant.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetCream
import app.verdant.android.ui.theme.FaltetDisplay
import app.verdant.android.ui.theme.FaltetForest
import app.verdant.android.ui.theme.FaltetInk
```

Keep all existing VM imports (AuthViewModel, Hilt, Credential Manager). Drop `Scaffold`, `TopAppBar`, `verdantTopAppBarColors`, `OutlinedButton`, `stringResource`, `R`, `MaterialTheme`, `RoundedCornerShape`.

- [ ] **Step 3: Replace the composable body**

Replace the body from the `@Composable fun AuthScreen(...)` body through the end. Preserve the signature and VM calls:

```kotlin
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.authenticated) {
        if (uiState.authenticated) onAuthenticated()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = "VERDANT",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 48.sp,
                color = FaltetInk,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Din trädgård, planerad.",
                fontSize = 14.sp,
                color = FaltetForest,
            )
            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { viewModel.signInWithGoogle(context) },
                enabled = !uiState.isLoading,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FaltetInk,
                    contentColor = FaltetCream,
                    disabledContainerColor = FaltetInk.copy(alpha = 0.4f),
                    disabledContentColor = FaltetCream,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = FaltetCream,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = "LOGGA IN MED GOOGLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                    )
                }
            }

            uiState.error?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = FaltetClay,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
```

**Adapt:**
- `onAuthenticated` callback name: if the actual signature uses `onSignedIn` or differs, keep whatever the caller passes.
- `viewModel.signInWithGoogle(context)`: check the actual VM method signature. If it takes an `Activity` rather than `Context`, use `(context as? androidx.activity.ComponentActivity) ?: error("...")` or adapt. Don't rename VM methods.
- `uiState.authenticated`: if the VM field is named `signedIn`, `authSuccess`, or similar, use that.
- `uiState.isLoading`: same — adapt to actual name.
- `uiState.error`: adapt to actual name.

- [ ] **Step 4: Add a `@Preview`**

At the end of the file:

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun AuthScreenPreview() {
    Box(
        modifier = Modifier.fillMaxSize().background(FaltetCream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = "VERDANT",
                fontFamily = FaltetDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 48.sp,
                color = FaltetInk,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Din trädgård, planerad.",
                fontSize = 14.sp,
                color = FaltetForest,
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = {},
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FaltetInk,
                    contentColor = FaltetCream,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    text = "LOGGA IN MED GOOGLE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}
```

- [ ] **Step 5: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/auth/AuthScreen.kt
git commit -m "feat: Fältet port — Auth full-bleed editorial"
```

---

### Task 4: Port Analytics

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/analytics/AnalyticsScreen.kt`

**Context:** 325 lines. Read-only analytics dashboard: season summaries, species comparison (dropdown-driven), yield-per-bed. No charts; stats render as cards. Replace with `FaltetScreenScaffold` + stat-dense sections using `FaltetMetadataRow`, `FaltetSectionHeader`, `FaltetListRow`, `FaltetDropdown`.

**Preserve verbatim:**
- Package, `AnalyticsState`, `AnalyticsViewModel` + all methods (`refresh`, `selectSpecies`, etc.), navigation callbacks.
- Data field types (`SeasonSummaryResponse`, `YieldPerBedResponse`, `SpeciesResponse`, `SpeciesComparisonResponse`).

**Masthead:** `mastheadLeft = "§ Analys"`, `mastheadCenter = "Analys"`.

- [ ] **Step 1: Read the current file**

Read `/Users/erik/development/verdant/android/app/src/main/kotlin/app/verdant/android/ui/analytics/AnalyticsScreen.kt` in full. Identify:
- Screen signature + callbacks
- `AnalyticsState` field names
- Actual field names on `SeasonSummaryResponse` / `YieldPerBedResponse` / `SpeciesComparisonResponse` (the spec template guesses — check reality)
- VM method for species selection
- Any existing helper functions (species display name, weight formatter)

- [ ] **Step 2: Replace imports**

```kotlin
package app.verdant.android.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.verdant.android.data.model.SpeciesResponse
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetDropdown
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetMetadataRow
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
```

Drop `TopAppBar`, `OutlinedTextField`, `Card`, `CardDefaults`, `RoundedCornerShape`, `verdantTopAppBarColors`, `ExposedDropdownMenuBox`, `stringResource`, `R`, `FontWeight`.

Preserve `AnalyticsViewModel` imports (Hilt, ViewModel, repo, data models).

- [ ] **Step 3: Preserve state + ViewModel verbatim**

Do not modify `AnalyticsState` or `AnalyticsViewModel`.

- [ ] **Step 4: Replace the composable body**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSpecies by remember { mutableStateOf<SpeciesResponse?>(null) }

    FaltetScreenScaffold(
        mastheadLeft = "§ Analys",
        mastheadCenter = "Analys",
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            ) {
                // Säsonger
                if (uiState.seasons.isNotEmpty()) {
                    item { FaltetSectionHeader(label = "Säsonger") }
                    uiState.seasons.forEach { season ->
                        item {
                            FaltetListRow(
                                title = season.name,
                                meta = season.year.toString(),
                            )
                        }
                        item {
                            FaltetMetadataRow(
                                label = "Stjälkar skördade",
                                value = season.stemCount.toString(),
                            )
                        }
                        item {
                            FaltetMetadataRow(
                                label = "Plantor",
                                value = season.plantCount.toString(),
                            )
                        }
                        item {
                            FaltetMetadataRow(
                                label = "Arter",
                                value = season.speciesCount.toString(),
                            )
                        }
                    }
                }

                // Jämförelse av arter
                item { FaltetSectionHeader(label = "Jämförelse av arter") }
                item {
                    FaltetDropdown(
                        label = "Art",
                        options = uiState.species,
                        selected = selectedSpecies,
                        onSelectedChange = {
                            selectedSpecies = it
                            viewModel.selectSpecies(it.id)
                        },
                        labelFor = { speciesDisplayName(it) },
                        searchable = true,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
                if (uiState.comparingSpecies) {
                    item { FaltetLoadingState(Modifier.height(80.dp)) }
                }
                uiState.speciesComparison?.seasons?.forEach { seasonComp ->
                    item { FaltetSectionHeader(label = seasonComp.name) }
                    item { FaltetMetadataRow(label = "Stjälkar", value = seasonComp.stemCount.toString()) }
                    item { FaltetMetadataRow(label = "Antal", value = seasonComp.plantCount.toString()) }
                    item { FaltetMetadataRow(label = "Skördar", value = seasonComp.harvestCount.toString()) }
                    item { FaltetMetadataRow(label = "Vikt", value = formatWeight(seasonComp.totalWeightGrams)) }
                }

                // Skörd per bädd
                if (uiState.yieldPerBed.isNotEmpty()) {
                    item { FaltetSectionHeader(label = "Skörd per bädd") }
                    uiState.yieldPerBed.forEach { bed ->
                        item {
                            FaltetListRow(
                                title = bed.name,
                                meta = bed.gardenName,
                            )
                        }
                        bed.seasons.forEach { season ->
                            item {
                                FaltetMetadataRow(
                                    label = season.name,
                                    value = "${season.stemCount} stjälkar · ${formatStemsPerSqm(season.stemsPerSqm)} / m²",
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

private fun speciesDisplayName(species: SpeciesResponse): String {
    val base = species.commonNameSv ?: species.commonName
    return species.variantNameSv?.let { "$base $it" }
        ?: species.variantName?.let { "$base $it" }
        ?: base
}

private fun formatWeight(grams: Double): String {
    return if (grams >= 1000) "${"%.1f".format(grams / 1000)} kg"
    else "${"%.0f".format(grams)} g"
}

private fun formatStemsPerSqm(value: Double): String {
    return "%.1f".format(value)
}
```

**Adapt:**
- Actual field names on `SeasonSummaryResponse` / `YieldPerBedResponse` / etc. — the template guesses `stemCount`, `plantCount`, `speciesCount`, `harvestCount`, `totalWeightGrams`, `stemsPerSqm`, `seasons`. Read the actual model classes in `app.verdant.android.data.model` and adapt. If any field doesn't exist, drop that `FaltetMetadataRow`.
- `speciesDisplayName` may already exist in the codebase — reuse if so.
- `formatWeight` already exists (at `ui/world/MyVerdantWorldScreen.kt`) — could import if visible, otherwise redefine as private at bottom of file.

- [ ] **Step 5: Add a `@Preview`**

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun AnalyticsSectionPreview() {
    androidx.compose.foundation.layout.Column {
        FaltetSectionHeader(label = "Säsonger 2026")
        FaltetListRow(title = "Sommar", meta = "2026")
        FaltetMetadataRow(label = "Stjälkar skördade", value = "482")
        FaltetMetadataRow(label = "Plantor", value = "96")
        FaltetMetadataRow(label = "Arter", value = "14")
    }
}
```

- [ ] **Step 6: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/analytics/AnalyticsScreen.kt
git commit -m "feat: Fältet port — Analytics stat tables"
```

---

### Task 5: Port Account

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/account/AccountScreen.kt`

**Context:** 196 lines. User profile + language toggle (sv/en) + sign out + delete account (with confirm dialog). Replace with `FaltetScreenScaffold` + `FaltetHero` (avatar + name + email) + language `FaltetChipSelector` + tappable action `FaltetListRow`s.

**Preserve:** `AccountViewModel`, VM methods (`setLanguage`, `signOut`, `deleteAccount`), user data flow.

**Masthead:** `§ Konto` / `"Konto"`.

- [ ] **Step 1: Read the current file**

Identify:
- Screen signature + callbacks (`onSignedOut`, `onBack`, etc.)
- `AccountState` fields: user profile shape (`displayName`, `email`, `photoUrl`?), current language, loading/error
- VM method names for language change, sign-out, delete

- [ ] **Step 2: Replace imports**

```kotlin
package app.verdant.android.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.verdant.android.ui.faltet.FaltetAvatar
import app.verdant.android.ui.faltet.FaltetChipSelector
import app.verdant.android.ui.faltet.FaltetHero
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.FaltetSectionHeader
import app.verdant.android.ui.theme.FaltetClay
```

Drop `TopAppBar`, `Card`, `OutlinedButton`, `Button`, `FilterChip`, `RoundedCornerShape`, `verdantTopAppBarColors`, `AsyncImage` (now inside FaltetAvatar), `CircleShape`, `Icons`, etc.

- [ ] **Step 3: Preserve state + VM verbatim**

- [ ] **Step 4: Replace the composable body**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onSignedOut: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.signedOut) {
        if (uiState.signedOut) onSignedOut()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Ta bort konto") },
            text = { Text("Vill du verkligen ta bort ditt konto? Detta kan inte ångras.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount()
                    showDeleteDialog = false
                }) { Text("Ta bort", color = FaltetClay) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Avbryt") }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Konto",
        mastheadCenter = "Konto",
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                uiState.user?.let { user ->
                    item {
                        FaltetHero(
                            title = user.displayName ?: "—",
                            subtitle = user.email,
                            leading = {
                                FaltetAvatar(
                                    url = user.photoUrl,
                                    displayName = user.displayName,
                                    size = 88.dp,
                                )
                            },
                        )
                    }
                }

                item { FaltetSectionHeader(label = "Språk") }
                item {
                    FaltetChipSelector(
                        label = "",
                        options = listOf("sv", "en"),
                        selected = uiState.currentLanguage,
                        onSelectedChange = { newLang ->
                            newLang?.let { viewModel.setLanguage(it) }
                        },
                        labelFor = { if (it == "sv") "Svenska" else "English" },
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }

                item { FaltetSectionHeader(label = "Konto") }
                item {
                    FaltetListRow(
                        title = "Logga ut",
                        onClick = { viewModel.signOut() },
                    )
                }
                item {
                    FaltetListRow(
                        title = "Ta bort konto",
                        onClick = { showDeleteDialog = true },
                    )
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}
```

**Adapt:**
- VM state field names (`user`, `currentLanguage`, `isLoading`, `signedOut`) — use actual names.
- VM method names (`setLanguage`, `signOut`, `deleteAccount`) — use actual names.
- User model fields (`displayName`, `email`, `photoUrl`) — confirm; adapt if different.
- If `FaltetChipSelector` with empty label renders poorly (a dangling empty row above the chips), drop it and inline a `FlowRow` of two `FilterChip`s. Accept minor visual deviation from primitive use.

- [ ] **Step 5: Add a `@Preview`**

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun AccountHeroPreview() {
    FaltetHero(
        title = "Erik Lindblad",
        subtitle = "erik@example.se",
        leading = {
            FaltetAvatar(url = null, displayName = "Erik Lindblad", size = 88.dp)
        },
    )
}
```

- [ ] **Step 6: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/account/AccountScreen.kt
git commit -m "feat: Fältet port — Account hero + actions"
```

---

### Task 6: Port SeasonSelector

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/season/SeasonSelectorScreen.kt`

**Context:** 326 lines. CRUD list of seasons with create/edit/delete via `AlertDialog`. Replace list with `FaltetListRow`s; replace dialog form with `Field` + `FaltetDatePicker`.

**Preserve:** `SeasonSelectorViewModel`, all CRUD methods (`create`, `update`, `delete`), state.

**Masthead:** `§ Plan` / `"Säsonger"`.

**FAB:** `FaltetFab(onClick = { /* reset form state + open dialog */ }, contentDescription = "Skapa säsong")`.

- [ ] **Step 1: Read the current file**

Identify:
- Screen signature + callbacks
- `SeasonSelectorState` field names (`seasons`, `isLoading`, `saving`, `error`)
- Season model fields (`id`, `name`, `year`, `firstFrostDate`, `lastFrostDate`, `isActive`)
- VM methods: `create(name: String, year: Int, lastFrost: String?, firstFrost: String?)`, `update(id, ...)`, `delete(id)`

- [ ] **Step 2: Replace imports**

```kotlin
package app.verdant.android.ui.season

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.verdant.android.ui.common.ConnectionErrorState
import app.verdant.android.ui.faltet.FaltetDatePicker
import app.verdant.android.ui.faltet.FaltetEmptyState
import app.verdant.android.ui.faltet.FaltetFab
import app.verdant.android.ui.faltet.FaltetListRow
import app.verdant.android.ui.faltet.FaltetLoadingState
import app.verdant.android.ui.faltet.FaltetScreenScaffold
import app.verdant.android.ui.faltet.Field
import app.verdant.android.ui.theme.FaltetClay
import app.verdant.android.ui.theme.FaltetForest
import java.time.LocalDate
```

Drop `TopAppBar`, `OutlinedTextField`, `Card`, `CardDefaults`, `RoundedCornerShape`, `Button`, `verdantTopAppBarColors`, `FloatingActionButton`, `Icons.Default.Add`, `stringResource`, `R`.

- [ ] **Step 3: Preserve state + VM + `SeasonResponse` usage**

- [ ] **Step 4: Replace the composable body**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonSelectorScreen(
    onBack: () -> Unit,
    viewModel: SeasonSelectorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingSeason by remember { mutableStateOf<SeasonResponse?>(null) }
    var formName by remember { mutableStateOf("") }
    var formYear by remember { mutableStateOf("") }
    var formLastFrost by remember { mutableStateOf<LocalDate?>(null) }
    var formFirstFrost by remember { mutableStateOf<LocalDate?>(null) }

    val openCreate: () -> Unit = {
        editingSeason = null
        formName = ""
        formYear = java.time.Year.now().value.toString()
        formLastFrost = null
        formFirstFrost = null
        showFormDialog = true
    }

    val openEdit: (SeasonResponse) -> Unit = { season ->
        editingSeason = season
        formName = season.name
        formYear = season.year.toString()
        formLastFrost = season.lastFrostDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        formFirstFrost = season.firstFrostDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        showFormDialog = true
    }

    val closeDialog: () -> Unit = {
        showFormDialog = false
        editingSeason = null
    }

    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = closeDialog,
            title = { Text(if (editingSeason != null) "Redigera säsong" else "Ny säsong") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        label = "Namn",
                        value = formName,
                        onValueChange = { formName = it },
                        required = true,
                    )
                    Field(
                        label = "År",
                        value = formYear,
                        onValueChange = { formYear = it.filter { c -> c.isDigit() } },
                        keyboardType = KeyboardType.Number,
                        required = true,
                    )
                    FaltetDatePicker(
                        label = "Sista frost (valfri)",
                        value = formLastFrost,
                        onValueChange = { formLastFrost = it },
                    )
                    FaltetDatePicker(
                        label = "Första frost (valfri)",
                        value = formFirstFrost,
                        onValueChange = { formFirstFrost = it },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val year = formYear.toIntOrNull() ?: return@TextButton
                        val lastFrost = formLastFrost?.toString()
                        val firstFrost = formFirstFrost?.toString()
                        val editing = editingSeason
                        if (editing != null) {
                            viewModel.update(editing.id, formName, year, lastFrost, firstFrost)
                        } else {
                            viewModel.create(formName, year, lastFrost, firstFrost)
                        }
                        closeDialog()
                    },
                    enabled = formName.isNotBlank() && formYear.toIntOrNull() != null && !uiState.saving,
                ) { Text(if (editingSeason != null) "Spara" else "Skapa", color = FaltetClay) }
            },
            dismissButton = {
                Row {
                    val editing = editingSeason
                    if (editing != null) {
                        TextButton(onClick = {
                            viewModel.delete(editing.id)
                            closeDialog()
                        }) { Text("Ta bort", color = FaltetClay) }
                    }
                    TextButton(onClick = closeDialog) { Text("Avbryt") }
                }
            },
        )
    }

    FaltetScreenScaffold(
        mastheadLeft = "§ Plan",
        mastheadCenter = "Säsonger",
        fab = { FaltetFab(onClick = openCreate, contentDescription = "Skapa säsong") },
    ) { padding ->
        when {
            uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                ConnectionErrorState(onRetry = { viewModel.refresh() })
            }
            uiState.seasons.isEmpty() -> FaltetEmptyState(
                headline = "Inga säsonger",
                subtitle = "Skapa din första säsong.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(uiState.seasons, key = { it.id }) { season ->
                    FaltetListRow(
                        title = season.name,
                        meta = buildString {
                            append(season.year.toString())
                            season.lastFrostDate?.let { append(" · Sista frost $it") }
                            season.firstFrostDate?.let { append(" · Första frost $it") }
                        },
                        leading = if (season.isActive) {
                            {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .drawBehind { drawCircle(FaltetClay) },
                                )
                            }
                        } else null,
                        stat = if (season.isActive) {
                            {
                                Text(
                                    text = "AKTIV",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.2.sp,
                                    color = FaltetClay,
                                )
                            }
                        } else null,
                        onClick = { openEdit(season) },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
```

**Adapt:**
- `SeasonResponse` import + field names (confirm `id`, `name`, `year`, `isActive`, `firstFrostDate: String?`, `lastFrostDate: String?`).
- `SeasonSelectorViewModel` method signatures — if they take different arg names, adapt.
- `uiState.saving` / `uiState.isLoading` / `uiState.error` / `uiState.seasons` — adapt to actual state field names.

- [ ] **Step 5: Add a `@Preview`**

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFFF5EFE2L)
@Composable
private fun SeasonRowPreview() {
    FaltetListRow(
        title = "Sommar",
        meta = "2026 · Sista frost 2026-05-15",
        leading = {
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind { drawCircle(FaltetClay) },
            )
        },
        stat = {
            Text(
                text = "AKTIV",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                color = FaltetClay,
            )
        },
        onClick = {},
    )
}
```

- [ ] **Step 6: Verify compile**

```bash
cd android && ./gradlew compileDebugKotlin --no-daemon -q
```

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/season/SeasonSelectorScreen.kt
git commit -m "feat: Fältet port — SeasonSelector"
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

- [ ] **Step 2: Install + manual smoke (5 scenarios)**

```bash
cd android && ./gradlew installDebug --no-daemon -q
```

- [ ] **Scenario 1: Splash** — clear app data, launch. Expect: "VERDANT" wordmark on cream briefly, then navigation to Auth (no session) or MyWorld (token saved).

- [ ] **Scenario 2: Auth** — launch with no saved session. Expect: wordmark + "Din trädgård, planerad." subtitle + centered clay "LOGGA IN MED GOOGLE" button on cream background. Tap button → Google Credential Manager flow. On success → navigate to MyWorld.

- [ ] **Scenario 3: Analytics** — open drawer, tap "Analys". Expect: masthead `§ Analys / Analys`. Scroll through Säsonger section (rows + metadata rows per season). Tap species dropdown, search, select → Jämförelse section populates. Scroll to Skörd per bädd section.

- [ ] **Scenario 4: Account** — open drawer, tap "Konto". Expect: masthead `§ Konto / Konto`. Hero shows avatar (image or initials) + name + email. Språk chip selector toggles. Tap "Logga ut" → signs out + returns to Auth. Tap "Ta bort konto" → confirmation dialog in Swedish; "Avbryt" closes, "Ta bort" (clay) confirms delete.

- [ ] **Scenario 5: SeasonSelector** — open drawer, tap "Säsonger". Expect: masthead `§ Plan / Säsonger`. Season rows with AKTIV badge on current. Tap FAB → Ny säsong dialog with 4 fields; Skapa saves, Avbryt closes. Tap an existing row → Redigera säsong dialog prefills; Spara saves, Ta bort (clay) deletes, Avbryt closes.

- [ ] **Step 3: Milestone commit**

```bash
git commit --allow-empty -m "milestone: Android Fältet complete (Spec D)"
```

---

## Verification summary

After Task 7:

- `android/app/src/main/kotlin/app/verdant/android/ui/faltet/FaltetAvatar.kt` shipped.
- All 5 target screens Fältet-styled.
- `./gradlew assembleDebug` green.
- Every top-level NavGraph screen is now Fältet-styled.

**Android Fältet port status:**

- ✅ Spec A — Foundation
- ✅ Spec B — 12 list screens
- ✅ Spec C1 — 4 detail screens
- ✅ Spec C2 — 5 form screens
- ✅ Spec C3 — 6 activity screens
- ✅ Spec C4 — AddSpecies
- ✅ Spec D — Splash + Auth + Analytics + Account + SeasonSelector

---

## Follow-up (outside this spec)

Regression polish pass (future spec):

- AddPlantEvent AI plant identification restoration (C2 regression — pattern now exists in C4).
- ApplySupply "show all categories" toggle (C3 regression).
- Inline edit dialogs in GardenDetail + BedDetail still use OutlinedTextField (C2 follow-up).

Feature gap (not a regression):

- AddSpecies missing group dropdown.

---

## Self-review notes

- **Spec §1 (goal):** Tasks 1–7 port 5 screens + 1 primitive.
- **Spec §2 (scope):** Splash/Auth/Analytics/Account/SeasonSelector → Tasks 2–6.
- **Spec §3 (decisions):** All 8 decisions implemented. One primitive (Task 1), 5 independent ports, no reference port, Swedish inline copy.
- **Spec §4 (primitive):** `FaltetAvatar` with 3 preview permutations (Task 1).
- **Spec §5 (per-screen):** Splash (Task 2), Auth (Task 3), Analytics (Task 4), Account (Task 5), SeasonSelector (Task 6) — each with complete code.
- **Spec §6 (phasing):** 1 + 5 + 1 = 7 tasks. Matches.
- **Spec §7 (testing):** `@Preview` per primitive + per screen; compile gate after each commit; 5-scenario smoke in Task 7.
- **Spec §8 (completion):** Milestone commit finalizes the Android Fältet port.
- **Spec §9 (follow-ups):** Regressions + feature gap called out.
