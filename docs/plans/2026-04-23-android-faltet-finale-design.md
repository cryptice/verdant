# Android Fältet — Sub-Spec D: Finale (Design)

**Status:** design, awaiting implementation plan
**Scope:** 5 un-ported screens + 1 new primitive
**Predecessor:** Spec C4 (AddSpecies) shipped at milestone `73dd255`.
**Successors:** none — this is the final Fältet sub-spec. Remaining items are a separate regression polish pass.

---

## 1. Goal

Complete the Android Fältet port by porting the final 5 un-ported screens (`Splash`, `Auth`, `Analytics`, `Account`, `SeasonSelector`) and introducing one new primitive (`FaltetAvatar`).

**Non-goals:**

- No API or data-model changes.
- No navigation-graph changes (SeasonSelector edit stays in an AlertDialog; no new routes).
- No new `FaltetDialog` primitive — dialog chrome remains inline per-screen with the consistent pattern from C1–C4.
- No chart library for Analytics — stats render as `FaltetMetadataRow`s + existing `Stat` primitive.

**Success criteria:**

- **Splash** shows the Fraunces italic wordmark "VERDANT" on `FaltetCream` for the bootstrap duration.
- **Auth** renders as full-bleed editorial: wordmark + subtitle + centered clay "LOGGA IN MED GOOGLE" button + inline error text. No scaffold; no masthead.
- **Analytics** renders with `FaltetScreenScaffold` + stat-dense sections using `FaltetMetadataRow` + `FaltetSectionHeader` + `FaltetDropdown` for species comparison.
- **Account** renders with `FaltetHero` (avatar + name + email) + language `FaltetChipSelector` + tappable action `FaltetListRow`s for sign-out + account deletion.
- **SeasonSelector** renders list as `FaltetListRow`s + FAB for create; edit/create dialog uses `Field` + `FaltetDatePicker` inside Material `AlertDialog`.
- `FaltetAvatar` available for Account and future reuse.
- Swedish labels throughout.
- `@Preview` per primitive and per screen; `./gradlew assembleDebug` green; manual smoke on 5 scenarios.

---

## 2. Scope

### In scope — 5 screens + 1 primitive

| # | File | Current LOC | Treatment |
|---|---|---|---|
| Primitive | `ui/faltet/FaltetAvatar.kt` (new) | — | Circular user avatar with Coil image + initials placeholder fallback |
| Splash | `ui/splash/SplashScreen.kt` | 85 | Add Fraunces wordmark to blank Box; preserve bootstrap logic |
| Auth | `ui/auth/AuthScreen.kt` | 168 | Full-bleed editorial rewrite; no scaffold |
| Analytics | `ui/analytics/AnalyticsScreen.kt` | 325 | Scaffold + stat-dense sections (MetadataRow + SectionHeader + Dropdown) |
| Account | `ui/account/AccountScreen.kt` | 196 | Scaffold + Hero (avatar+name+email) + language chip + action rows |
| SeasonSelector | `ui/season/SeasonSelectorScreen.kt` | 326 | Scaffold + ListRow + FAB + inline create/edit dialog with Field + FaltetDatePicker |

### Out of scope

- `MyVerdantWorldScreen` — already ported in Spec B + restore patch `9732757`.
- `DashboardScreen.kt` — legacy/unused, not wired into NavGraph. Leave untouched.

### Carry-over regressions (not this spec)

Consolidated for a future **regression polish pass** — not blocking Spec D:

- `AddPlantEvent` AI plant identification dropped (C2 regression; restoration pattern now exists in C4's AddSpecies port).
- `ApplySupply` "show all categories" toggle dropped (C3 regression).
- Inline edit dialogs in `GardenDetail` + `BedDetail` still use `OutlinedTextField` (C2 follow-up).
- `AddSpecies` missing group dropdown (feature gap in original, not a regression).

---

## 3. Design decisions (summary of brainstorm)

| # | Decision | Chosen |
|---|---|---|
| 1 | Scope | All 5 screens including Splash. Total ~1,100 LOC. |
| 2 | New primitives | `FaltetAvatar` only. No `FaltetDialog` — inline dialog chrome. |
| 3 | Splash treatment | Wordmark + cream background. No subtitle. |
| 4 | Auth structure | Full-bleed editorial; no `FaltetScreenScaffold` (pre-auth has no `§` category or drawer). |
| 5 | Analytics layout | `FaltetMetadataRow` + `FaltetSectionHeader` + existing `Stat` primitive for stat-dense tables. |
| 6 | Account structure | Hero with avatar + metadata sections + tappable action ListRows. |
| 7 | SeasonSelector structure | Minimal port — list rows + FAB + inline AlertDialog with `Field` + `FaltetDatePicker`. No new route. |
| 8 | Implementation strategy | No reference port — each screen uses a different pattern. One primitive commit + 5 port commits + milestone. |

---

## 4. New primitive

### 4.1 `FaltetAvatar.kt`

Circular user avatar with Coil-backed image + initials placeholder fallback.

**Signature:**

```kotlin
@Composable
fun FaltetAvatar(
    url: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
)
```

**Visual:**

- `Box(modifier.size(size).clip(CircleShape).border(1.dp, FaltetInkLine40, CircleShape))`.
- If `url != null`: Coil `AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())`.
- If `url == null`: `FaltetCream` fill + centered initials (first char of `displayName.trim()` uppercased; `"?"` if both null or blank). Initials rendered in `FaltetDisplay` italic, size = `(size.value * 0.4).sp`, `FaltetInk`.

**Preview permutations:**

- `displayName = "Erik"` + `url = null` → "E" initial.
- `displayName = null` + `url = null` → "?".
- With a mock URL (a stable data URI or placeholder image; Preview won't hit the network, so the URL branch may render blank — fine).

**Reuse:**

- Account screen at `size = 88.dp` inside `FaltetHero.leading`.
- Future: CustomerList rows could adopt at `size = 40.dp` (deferred).

---

## 5. Per-screen structure

### 5.1 Splash

**File:** `ui/splash/SplashScreen.kt`

**Preserve:** all bootstrap logic (token check, silent Google sign-in, navigation branching, `LaunchedEffect`).

**Replace** the empty `Box` with:

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

No other changes. No spinner, no subtitle, no progress indicator.

### 5.2 Auth

**File:** `ui/auth/AuthScreen.kt`

**Preserve:** `AuthViewModel`, Credential Manager flow, all callbacks, error state flow.

**Replace** the entire visible body — no `Scaffold`, no `TopAppBar`, no `FaltetScreenScaffold`:

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
                onClick = { viewModel.signInWithGoogle(context) },  // adapt to actual VM signature
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

**Adaptation:** confirm the actual `AuthViewModel` method name for Google sign-in and success flag. The current VM likely takes `Activity` or `Context` for `CredentialManager`; preserve that wiring.

### 5.3 Analytics

**File:** `ui/analytics/AnalyticsScreen.kt`

**Masthead:** `§ Analys` / `"Analys"`.

**Bottom bar:** omit (no submit).

**Body structure in `LazyColumn`:**

1. **Säsonger** section:
   ```kotlin
   item { FaltetSectionHeader(label = "Säsonger") }
   items(uiState.seasons, key = { it.year }) { season ->
       FaltetListRow(title = season.name, meta = season.year.toString())
       // stats for this season as FaltetMetadataRows
       FaltetMetadataRow(label = "Stjälkar skördade", value = season.stemCount.toString())
       FaltetMetadataRow(label = "Plantor", value = season.plantCount.toString())
       FaltetMetadataRow(label = "Arter", value = season.speciesCount.toString())
   }
   ```
   Note: `FaltetMetadataRow` outside `item { }` — wrap each in `item { }` blocks. Emit multiple items per season.

2. **Jämförelse av arter** section:
   ```kotlin
   item { FaltetSectionHeader(label = "Jämförelse av arter") }
   item {
       FaltetDropdown(
           label = "Art",
           options = uiState.species,
           selected = selectedSpecies,
           onSelectedChange = { selectedSpecies = it; viewModel.selectSpecies(it.id) },
           labelFor = { speciesDisplayName(it) },
           searchable = true,
       )
   }
   if (uiState.comparingSpecies) {
       item { FaltetLoadingState(Modifier.height(80.dp)) }
   }
   uiState.speciesComparison?.let { comparison ->
       comparison.seasons.forEach { seasonComp ->
           item { FaltetSectionHeader(label = seasonComp.name) }
           item { FaltetMetadataRow(label = "Stjälkar", value = seasonComp.stemCount.toString()) }
           item { FaltetMetadataRow(label = "Antal", value = seasonComp.plantCount.toString()) }
           item { FaltetMetadataRow(label = "Skördar", value = seasonComp.harvestCount.toString()) }
           item { FaltetMetadataRow(label = "Vikt", value = formatWeight(seasonComp.totalWeightGrams)) }
       }
   }
   ```
   Adapt field names to the actual `SeasonSummaryResponse` / `SpeciesComparisonResponse` shapes.

3. **Skörd per bädd** section:
   ```kotlin
   item { FaltetSectionHeader(label = "Skörd per bädd") }
   items(uiState.yieldPerBed, key = { it.bedId }) { bed ->
       FaltetListRow(title = bed.name, meta = bed.gardenName)
       bed.seasons.forEach { season ->
           FaltetMetadataRow(label = season.name, value = "${season.stemCount} stjälkar · ${season.stemsPerSqm} / m²")
       }
   }
   ```
   Same wrapping caveat — each primitive call inside its own `item { }`.

**Loading + error:** standard `FaltetLoadingState` when initial load; `ConnectionErrorState` on error; wrap in the `when` guard from C1/C2 pattern.

**No charts.** Pure text + stat rows. `formatWeight` helper (g vs kg) already exists at `ui/world/MyVerdantWorldScreen.kt` — copy or import.

### 5.4 Account

**File:** `ui/account/AccountScreen.kt`

**Masthead:** `§ Konto` / `"Konto"`.

**Body:**

```kotlin
LazyColumn(Modifier.fillMaxSize().padding(padding)) {
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
            label = "",  // header above already supplies context
            options = listOf("sv", "en"),
            selected = currentLanguage,
            onSelectedChange = { newLang -> newLang?.let { viewModel.setLanguage(it) } },
            labelFor = { if (it == "sv") "Svenska" else "English" },
        )
    }

    item { FaltetSectionHeader(label = "Konto") }
    item {
        FaltetListRow(
            title = "Logga ut",
            onClick = { viewModel.signOut(); onSignedOut() },
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
```

**Notes:**
- If `FaltetChipSelector` with an empty `label` renders a dangling empty row above the chips, accept the minor visual and pass a single-space `" "` or adapt the primitive if it doesn't allow empty label. Best alternative: use an inline `FlowRow` of two `FilterChip`s directly, at the cost of losing primitive consistency. Recommend trying empty-label first; adapt if ugly.
- Navigation post-sign-out: preserve the existing `onSignedOut()` callback or VM-level redirect flag.

### 5.5 SeasonSelector

**File:** `ui/season/SeasonSelectorScreen.kt`

**Masthead:** `§ Plan` / `"Säsonger"`.

**FAB:** `FaltetFab(onClick = { editingSeason = null; formName = ""; formYear = ""; formLastFrost = null; formFirstFrost = null; showFormDialog = true }, contentDescription = "Skapa säsong")`.

**Body:**

```kotlin
when {
    uiState.isLoading -> FaltetLoadingState(Modifier.padding(padding))
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
                    season.firstFrostDate?.let { append(" · Första frost $it") }
                    season.lastFrostDate?.let { append(" · Sista frost $it") }
                },
                leading = if (season.isActive) {
                    { Box(Modifier.size(10.dp).drawBehind { drawCircle(FaltetClay) }) }
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
                onClick = {
                    editingSeason = season
                    formName = season.name
                    formYear = season.year.toString()
                    formLastFrost = season.lastFrostDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    formFirstFrost = season.firstFrostDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    showFormDialog = true
                },
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

if (showFormDialog) {
    AlertDialog(
        onDismissRequest = { showFormDialog = false; editingSeason = null },
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
                    if (editingSeason != null) {
                        viewModel.update(
                            editingSeason!!.id,
                            formName,
                            year,
                            formLastFrost?.toString(),
                            formFirstFrost?.toString(),
                        )
                    } else {
                        viewModel.create(
                            formName,
                            year,
                            formLastFrost?.toString(),
                            formFirstFrost?.toString(),
                        )
                    }
                    showFormDialog = false
                    editingSeason = null
                },
                enabled = formName.isNotBlank() && formYear.toIntOrNull() != null && !uiState.saving,
            ) { Text(if (editingSeason != null) "Spara" else "Skapa", color = FaltetClay) }
        },
        dismissButton = {
            Row {
                if (editingSeason != null) {
                    TextButton(onClick = {
                        viewModel.delete(editingSeason!!.id)
                        showFormDialog = false
                        editingSeason = null
                    }) { Text("Ta bort", color = FaltetClay) }
                }
                TextButton(onClick = {
                    showFormDialog = false
                    editingSeason = null
                }) { Text("Avbryt") }
            }
        },
    )
}
```

**Adaptations:**
- `viewModel.create` / `viewModel.update` / `viewModel.delete` signatures — confirm actual names from `SeasonSelectorViewModel`.
- `Season.firstFrostDate` / `lastFrostDate` — confirm field names; may be `String?` ISO dates.
- `Season.isActive` — confirm boolean field exists.

---

## 6. Phasing

### Phase 1 — Primitive (1 commit)

1. `FaltetAvatar.kt` + `@Preview`s.

### Phase 2 — Screen ports (5 commits)

2. Splash — add wordmark.
3. Auth — full-bleed editorial rewrite.
4. Analytics — scaffold + stat-dense sections.
5. Account — hero + avatar + language + action rows + delete dialog.
6. SeasonSelector — list rows + FAB + inline edit/create dialog.

### Phase 3 — Verify + milestone (1 empty commit)

7. `./gradlew assembleDebug` green. Manual smoke on 5 scenarios. Empty milestone commit:

```
milestone: Android Fältet complete (Spec D)
```

**Total: 7 tasks, 6 code commits + 1 milestone.**

---

## 7. Testing

### Per primitive (Phase 1)

- `@Preview`s for `FaltetAvatar`: with URL, with name-only initials, with null everything.
- `./gradlew compileDebugKotlin` green.

### Per screen port (Phase 2)

- `@Preview` at end of each file exercising populated state.
- `./gradlew compileDebugKotlin` green after each commit.

### Manual smoke at Phase 3 — 5 scenarios

1. **Splash** — clear app data, launch; observe "VERDANT" wordmark briefly before navigation.
2. **Auth** — launch without saved session; see wordmark + subtitle + clay button; tap Google sign-in → authenticate.
3. **Analytics** — navigate from drawer; verify season section headers, season stat rows, species dropdown + loading + comparison stats, yield-per-bed rows.
4. **Account** — navigate from drawer; see avatar (or initials) + name + email in Hero; language chip toggles; sign out works; delete dialog "Ta bort" in clay, "Avbryt" works.
5. **SeasonSelector** — navigate from drawer; see season list with AKTIV badge; tap FAB → create dialog works; tap row → edit dialog with Delete button; save/delete/cancel all work.

### Not added this spec

- No snapshot testing.
- No new instrumented UI tests.
- No changes to existing view-model tests — VM signatures unchanged.

### Known non-issue

First cold install flashes system-serif before Fraunces arrives via Downloadable Fonts.

---

## 8. Completion state after Spec D

Android Fältet port status:

- ✅ Spec A — Foundation
- ✅ Spec B — 12 list screens
- ✅ Spec C1 — 4 detail screens
- ✅ Spec C2 — 5 form screens
- ✅ Spec C3 — 6 activity screens
- ✅ Spec C4 — AddSpecies
- ✅ Spec D — Splash + Auth + Analytics + Account + SeasonSelector

Every top-level screen in the NavGraph is Fältet-styled.

---

## 9. Follow-up (outside this spec)

**Regression polish pass** — consolidate these into a single future spec:

- `AddPlantEvent` AI plant identification restoration (analog to AddSpecies' AI flow from C4).
- `ApplySupply` "show all categories" toggle.
- Inline edit dialogs in `GardenDetail` + `BedDetail` — migrate to `Field` primitive.

**Feature gap (not a regression):**

- `AddSpecies` missing group dropdown — a separate decision, not strictly a port.
