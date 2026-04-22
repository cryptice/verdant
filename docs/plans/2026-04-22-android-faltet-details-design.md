# Android Fältet — Sub-Spec C1: Detail Screens (Design)

**Status:** design, awaiting implementation plan
**Scope:** 4 Android detail screens
**Predecessor:** Spec B (Lists) shipped at commit `a2920bd` plus regression patches `8c0c42c` (TaskList delete) and `9732757` (MyVerdantWorld sections).
**Successors:** Sub-spec C2 (forms), C3 (activities), D (analytics/account/auth).

---

## 1. Goal

Port 4 Android detail screens (`GardenDetailScreen`, `BedDetailScreen`, `PlantDetailScreen`, `PlantedSpeciesDetailScreen`) from `TopAppBar` + Material3 `Card` rendering to the Fältet editorial aesthetic, introducing two new primitives (`FaltetMetadataRow`, `FaltetHero`) and composing the rest from Spec A + Spec B primitives.

**Non-goals:**
- No data-model, API, navigation, or feature changes.
- No new test infrastructure.
- Forms (create/edit) and activity flows are out of scope — sub-specs C2 and C3.

**Success criteria:**
- All 4 detail screens render with Fältet masthead + hero row + hairline-separated sections on cream.
- Typography: Fraunces italic for titles/values; mono uppercase for labels, meta, and workflow progress.
- Edit/delete affordances accessible via `IconButton`s in the masthead `right` slot, tint `FaltetClay`.
- Empty list sections show an inline italic "Inga X ännu." line; empty metadata sections are hidden.
- Each screen has a `@Preview`; `./gradlew assembleDebug` green; manual emulator smoke shows no layout breaks.

---

## 2. Scope

### In scope — 4 screens

| Screen | File | Current LOC | Notes |
|---|---|---|---|
| GardenDetail | `ui/garden/GardenDetailScreen.kt` | 317 | Reference port. Emoji + description + beds list. |
| BedDetail | `ui/bed/BedDetailScreen.kt` | 551 | Largest. Expandable condition groups + plants + supply log. |
| PlantDetail | `ui/plant/PlantDetailScreen.kt` | 386 | Coil hero image + workflow progress + event history. |
| PlantedSpeciesDetail | `ui/plants/PlantedSpeciesDetailScreen.kt` | 582 | Status-grouped plant list; drill-down to individual plants. |

### Out of scope — later sub-specs

- All form / create / edit screens → **C2**.
- All activity screens (Sow, PotUp, PlantOut, ApplySupply, AddPlantEvent, AddSeeds, AddSpecies) → **C3**.
- MyWorld dashboard, SeasonSelector → later standalone brainstorms.
- Analytics, Account, Auth, Splash → **D**.

---

## 3. Design Decisions (summary of brainstorm)

| # | Decision | Chosen |
|---|---|---|
| 1 | Spec decomposition | Split sub-spec C into C1 (detail, 4 screens), C2 (forms, 5 screens), C3 (activities, 7 screens). This spec covers C1 only. |
| 2 | Hero treatment | Masthead + distinct hero row (140dp image/emoji/placeholder + title in Fraunces 32sp + optional subtitle). Not full-bleed photo. |
| 3 | Edit/delete affordance | `IconButton`s in masthead `right` slot, tint `FaltetClay`. No TopAppBar. |
| 4 | Metadata row pattern | New `FaltetMetadataRow` primitive — horizontal mono-label + italic Fraunces value + hairline bottom. |
| 5 | Workflow progress (PlantDetail) | Inline mono caption: `SÅDD · KRUKAD · UTPLANTERAD · SKÖRDAD`, current stage in clay, past in ink, future dimmed. No new primitive. |
| 6 | Event history (PlantDetail) | Grouped by month via existing `FaltetSectionHeader` + `FaltetListRow`. Newest first. |
| 7 | Empty sections | List sections always render with inline italic "Inga X ännu." line. Metadata sections hide entirely when all fields are null. |
| 8 | Implementation strategy | Vertical slice. Primitives first, GardenDetail as reference port, batch the remaining 3. |

---

## 4. New primitives

Both under `android/app/src/main/kotlin/app/verdant/android/ui/faltet/`.

### 4.1 `FaltetMetadataRow.kt`

Horizontal label / value row for metadata sections (conditions, specs, stats).

**Signature:**

```kotlin
@Composable
fun FaltetMetadataRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    valueAccent: FaltetTone? = null,
)
```

**Visual:**
- `Row`, `fillMaxWidth()`, `heightIn(min = 44.dp)`, `padding(horizontal = 18.dp, vertical = 12.dp)`.
- Hairline `FaltetInkLine20` bottom separator (`drawBehind`, same pattern as `FaltetListRow`).
- Label: mono 10sp, letter-spacing 1.2sp, uppercase, `FaltetForest`, `weight = 2f`, right-padded 12dp.
- Value: Fraunces italic 16sp `FaltetInk` (or `valueAccent?.color()` if supplied), `TextAlign.End`, `weight = 3f`.
- Null value: render `"—"` in `FaltetForest.copy(alpha = 0.4f)`. Callers decide whether to show the row at all when the value is null (see §7 empty-section policy).

**Why not reuse existing `Field`:** `Field` is a labeled form input built for editing (20sp italic value stacked below label, 60+dp tall). For read-only key/value display we want horizontal, dense rows — different grammar.

**Preview permutations:**
- Populated: `FaltetMetadataRow("Jordtyp", "Lerig mylla")`
- Null value: `FaltetMetadataRow("pH", null)`
- Accent: `FaltetMetadataRow("Status", "Skördad", valueAccent = FaltetTone.Clay)`

### 4.2 `FaltetHero.kt`

Hero row — sits between masthead and scrollable body.

**Signature:**

```kotlin
@Composable
fun FaltetHero(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable BoxScope.() -> Unit)? = null,
)
```

**Visual:**
- `Row`, `fillMaxWidth().height(180.dp)`, cream background.
- Hairline `FaltetInkLine20` bottom separator.
- `padding(horizontal = 22.dp, vertical = 20.dp)`.
- `leading` slot (optional): `Box(Modifier.size(140.dp))` on the left. `BoxScope` receiver lets callers position content via `Modifier.align(...)`. When null, the title/subtitle takes the full width.
- Right column (`weight(1f)`, `padding(start = 20.dp)`):
  - Title: Fraunces italic 32sp `FaltetInk`, `maxLines = 2`, ellipsis.
  - Subtitle (optional): body 14sp `FaltetForest`, `maxLines = 3`, ellipsis, `padding(top = 8.dp)`.

**Preview permutations:**
- With `PhotoPlaceholder` leading: `FaltetHero(title = "Vildblommor", subtitle = "Lerig mylla · Sydvänd", leading = { PhotoPlaceholder(label = "Vildblommor", tone = PhotoTone.Sage, modifier = Modifier.fillMaxSize()) })`.
- With emoji leading: `FaltetHero(title = "Villan", subtitle = "Hemträdgården", leading = { Text("🌱", fontSize = 64.sp, modifier = Modifier.align(Alignment.Center)) })`.
- No leading: `FaltetHero(title = "Cosmos bipinnatus", subtitle = "84 plantor")`.

**Notes:**
- Callers decide leading content; no new primitive needed for variations.
- `PhotoPlaceholder` already accepts `modifier: Modifier` so passing `Modifier.fillMaxSize()` inside the leading Box works.

---

## 5. Screen port pattern

All detail screens share the same outer structure:

```kotlin
FaltetScreenScaffold(
    mastheadLeft = "§ <entity category>",
    mastheadCenter = entity.name,
    mastheadRight = {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Redigera", tint = FaltetClay, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DeleteOutline, "Ta bort", tint = FaltetClay, modifier = Modifier.size(18.dp))
            }
        }
    },
    fab = { /* only if the screen has a primary add-action (e.g., GardenDetail → create bed) */ },
) { padding ->
    when (val s = state) {
        Loading -> FaltetLoadingState(Modifier.padding(padding))
        is Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            ConnectionErrorState(onRetry = retry)
        }
        is Loaded -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { FaltetHero(title = s.entity.name, subtitle = s.entity.description, leading = { ... }) }
            // ... workflow-progress strip (PlantDetail only) ...
            // ... sections ...
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    // ... confirm-delete AlertDialog when showDeleteDialog ...
}
```

### 5.1 Section kinds

**List section:**
```kotlin
item { FaltetSectionHeader(label = "Plantor") }
if (plants.isEmpty()) {
    item { InlineEmpty("Inga plantor ännu.") }
} else {
    items(plants, key = { it.id }) { plant ->
        FaltetListRow(title = plant.name, meta = ..., stat = ..., onClick = ...)
    }
}
```

**Metadata section:**
```kotlin
val hasAnyField = listOf(bed.soilType, bed.soilPh?.toString(), bed.drainage).any { it != null }
if (hasAnyField) {
    item { FaltetSectionHeader(label = "Villkor") }
    item { FaltetMetadataRow("Jordtyp", bed.soilType) }
    item { FaltetMetadataRow("pH", bed.soilPh?.toString()) }
    item { FaltetMetadataRow("Dränering", bed.drainage) }
}
```
If the `hasAnyField` check returns false, the whole section (header + rows) is suppressed.

### 5.2 Inline-empty helper

Each detail screen includes a private composable:

```kotlin
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

### 5.3 Delete confirm dialog

Mirrors the TaskList patch pattern from Spec B:

```kotlin
if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("Ta bort <entity>") },
        text = { Text("Vill du ta bort \"${entity.name}\"?") },
        confirmButton = {
            TextButton(onClick = {
                viewModel.delete(entity.id)
                showDeleteDialog = false
                onBack()
            }) { Text("Ta bort", color = FaltetClay) }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteDialog = false }) { Text("Avbryt") }
        },
    )
}
```

### 5.4 Workflow progress strip (PlantDetail only)

Inserted as a single `item` between the hero and the first section:

```kotlin
item {
    val stages = listOf("SÅDD", "KRUKAD", "UTPLANTERAD", "SKÖRDAD")
    val currentIdx = stages.indexOf(currentStageLabel)  // -1 if unknown
    Text(
        text = buildAnnotatedString {
            stages.forEachIndexed { i, stage ->
                val color = when {
                    i < currentIdx -> FaltetInk
                    i == currentIdx -> FaltetClay
                    else -> FaltetForest.copy(alpha = 0.4f)
                }
                withStyle(SpanStyle(color = color)) { append(stage) }
                if (i < stages.size - 1) {
                    withStyle(SpanStyle(color = FaltetForest.copy(alpha = 0.4f))) { append("  ·  ") }
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

Map the ViewModel's status enum to the labels: `SEEDED` → `"SÅDD"`, `POTTED_UP` → `"KRUKAD"`, `PLANTED_OUT` / `GROWING` → `"UTPLANTERAD"`, `HARVESTED` → `"SKÖRDAD"`. Other states (e.g., `REMOVED`) do not light any stage — `currentIdx = -1`, all stages dimmed.

---

## 6. Per-screen structure

### 6.1 `GardenDetailScreen`

**Masthead:** `§ Trädgård` / `garden.name`.

**Hero:**
- `leading = { Text(garden.emoji ?: "🌱", fontSize = 64.sp, modifier = Modifier.align(Alignment.Center)) }`
- `title = garden.name`, `subtitle = garden.description`

**FAB:** `FaltetFab(onClick = onCreateBed, contentDescription = "Skapa bädd")`.

**Sections:**
1. **Bäddar** (list) — one `FaltetListRow` per bed. `title = bed.name`. `meta = "${bed.plantCount} plantor · ${bed.bedSize ?: "–"}"`. `stat = null` (Chip trailing optional if bed has a sun-exposure; inline composition). `onClick = { onBedClick(bed.id) }`. Empty: `InlineEmpty("Inga bäddar ännu. Tryck + för att skapa.")`.

### 6.2 `BedDetailScreen`

**Masthead:** `§ Bädd` / `bed.name`.

**Hero:**
- `leading = { PhotoPlaceholder(label = bed.name, tone = PhotoTone.Sage, modifier = Modifier.fillMaxSize()) }`
- `title = bed.name`, `subtitle = bed.description`

**Sections:**
1. **Villkor** — four expandable condition groups. Each group's top row is a `FaltetListRow` with `title` = group label, `meta` = a compact summary (e.g., `"Lerig mylla · pH 6.5"` for Jord), `actions` = chevron (Up/Down by expanded state), `onClick = { expanded = !expanded }`. When expanded, the group renders `FaltetMetadataRow` per field below the top row (indented via `padding(start = 18.dp)` on the row). Groups where all fields are null are suppressed.

    - **Jord** group: fields `Jordtyp`, `pH`, `Dränering`, `Upphöjd bädd` (bool → "Ja"/"Nej"/null).
    - **Exponering** group: fields `Sol`, `Väderstreck`, `Vindskydd`.
    - **Bevattning** group: fields `Bevattning`, `Frekvens`.
    - **Skydd** group: fields `Skydd`, `Sjukdomstryck`.

    Field-set boundaries follow the existing `BedResponse` model — adapt the field list to whatever columns the model actually exposes. If a named field doesn't exist on the model, skip it.

2. **Plantor** (list) — `FaltetListRow` per plant. `title` = `"${plant.commonName}${plant.variantName?.let { " $it" } ?: ""}"`. `meta` = `"${statusLabelSv(plant.status)} · ${formattedDate(plant.plantedDate)}"`. `stat` = count when `plant.count > 1`, otherwise null. `onClick = { onPlantClick(plant.id) }`. Empty: `InlineEmpty("Inga plantor ännu.")`.

3. **Gödsling & vatten** (list) — supply applications. `FaltetListRow` per application. `title = application.supplyName`. `meta = "${formattedDate(application.date)} · ${application.method ?: "—"}"`. `stat` = `formatQuantity(application.quantity, application.unit)` in mono. Empty: `InlineEmpty("Inga gödslingar ännu.")`.

### 6.3 `PlantDetailScreen`

**Masthead:** `§ Planta` / `"${plant.commonName}${plant.variantName?.let { " $it" } ?: ""}"`.

**Hero:**
- `leading = { if (plant.photoUrl != null) AsyncImage(model = plant.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else PhotoPlaceholder(label = plant.commonName, tone = PhotoTone.Blush, modifier = Modifier.fillMaxSize()) }`
- `title` = `"${plant.commonName}${plant.variantName?.let { " $it" } ?: ""}"`
- `subtitle` = `"${plant.speciesName ?: ""} · Bädd: ${plant.bedName ?: "—"}"` (drop segments when null)

**Sections:**
1. **Workflow progress strip** (no header; inline caption per §5.4).
2. **Händelser** — event history, grouped by month, newest-first.
    - Pre-compute in `remember(events)`:
      ```kotlin
      val byMonth = events
          .sortedByDescending { it.date }
          .groupBy { yearMonthOf(it.date) }
      ```
    - For each `(yearMonth, monthEvents)` pair: `item { FaltetSectionHeader(label = monthLabelSv(yearMonth)) }` then `items(monthEvents) { event -> FaltetListRow(...) }`.
    - Event row:
      - `leading = { Box(Modifier.size(10.dp).drawBehind { drawCircle(eventToneColor(event.type)) }) }`. Tone mapping: `SOWED`→Mustard, `POTTED_UP`→Sky, `PLANTED_OUT`→Sage, `HARVESTED`→Clay, `FERTILIZED`→Berry, `WATERED`→Sky, `NOTE`→Forest, else→Forest.
      - `title = eventTypeLabelSv(event.type)` (Swedish).
      - `meta = "${formattedDate(event.date)}${event.notes?.let { " · $it" } ?: ""}"` with `metaMaxLines = 2`.
      - `stat` = count or weight when present; mono 14sp ink.
    - Empty (no events at all): `InlineEmpty("Inga händelser ännu.")`.

**Swedish month label helper:**

```kotlin
private fun monthLabelSv(yearMonth: YearMonth): String {
    val names = arrayOf(
        "Januari", "Februari", "Mars", "April", "Maj", "Juni",
        "Juli", "Augusti", "September", "Oktober", "November", "December",
    )
    return "${names[yearMonth.monthValue - 1]} ${yearMonth.year}"
}
```

### 6.4 `PlantedSpeciesDetailScreen`

**Masthead:** `§ Art` / `species.commonName`.

**Hero:**
- `leading = { PhotoPlaceholder(label = species.commonName, tone = speciesTone(species), modifier = Modifier.fillMaxSize()) }`. `speciesTone(...)` reuses the category-to-tone mapping from Spec B's SpeciesList port (Sage/Blush/Butter/Berry/Mustard/Clay fallback).
- `title = species.commonName`
- `subtitle = "${species.scientificName ?: ""} · ${aggregatePlantCount} plantor"` (drop empty scientific name)

**Sections:**
1. **Plantor** — grouped by status (`SEEDED` → "Sådd", `POTTED_UP` → "Krukad", `PLANTED_OUT`/`GROWING` → "Utplanterad", `HARVESTED` → "Skördad", `RECOVERED` → "Återhämtad", `REMOVED` → "Borttagen"). Status order follows the lifecycle above.
    - Pre-compute `byStatus` as a `Map<String, List<Plant>>` ordered by the lifecycle.
    - For each non-empty `(status, plants)` pair: `item { FaltetSectionHeader(label = status) }` then `items(plants)`.
    - Row: `leading` = 10dp status dot (colors: Sådd=Mustard, Krukad=Sky, Utplanterad=Sage, Skördad=Clay, Återhämtad=Berry, Borttagen=InkLine40). `title` = bed name (fallback `"—"`). `meta` = `"${plant.variantName ?: ""} · ${formattedDate(plant.plantedDate)}"`. `stat` = count when multiple plants share the row. `onClick = { onPlantClick(plant.id) }`.
    - All statuses empty: `InlineEmpty("Inga plantor av denna art ännu.")`.

---

## 7. Empty-section policy (summary)

- **List section**: always render header. If list is empty, render one `InlineEmpty` line below. Keeps the page structure stable as the entity evolves over the season.
- **Metadata section**: compute `hasAnyField = fields.any { it != null }` before emitting the header. If false, suppress header + rows entirely. No point showing a header over a group of em-dashes.

Both policies are applied per-section; they're not a new primitive.

---

## 8. Phasing

### Phase 1 — Primitives (2 commits)

1. `FaltetMetadataRow.kt` + `@Preview`
2. `FaltetHero.kt` + `@Preview`s

Compile-green gate after each.

### Phase 2 — Reference port (1 commit + checkpoint)

3. Port `GardenDetailScreen.kt`. Validates:
   - Masthead-right edit/delete `IconButton` pattern
   - `FaltetHero` emoji-leading case
   - `InlineEmpty` helper + list-section structure
   - `FaltetFab` integration for create-bed

Manual emulator smoke after Phase 2. If primitive API is wrong, amend in new commits before Phase 3.

### Phase 3 — Batch ports (3 commits)

4. `BedDetailScreen.kt` — expandable condition groups + two list sections + suppress-empty-metadata policy.
5. `PlantDetailScreen.kt` — Coil `AsyncImage` hero, workflow progress strip, event history grouped by month.
6. `PlantedSpeciesDetailScreen.kt` — status-grouped list; `speciesTone` reuse.

### Phase 4 — Verify + milestone (1 empty commit)

7. `./gradlew assembleDebug` green. Manual smoke on all 4 screens. Empty milestone commit:

```
milestone: Android Fältet details complete (Spec C1)
```

**Total: 7 tasks, 6 code commits + 1 milestone.**

---

## 9. Testing

### Per primitive (Phase 1)

- `@Preview` composable(s) in the same file. Permutations per §4.1 and §4.2.
- `./gradlew compileDebugKotlin` green after each primitive commit.

### Per screen port (Phases 2–3)

- `@Preview` at the end of each screen file exercising a populated state and at least one empty-section case.
- `./gradlew compileDebugKotlin` green after each screen commit.
- Manual emulator smoke after Phase 2 (GardenDetail reference) and after Phase 3 (full batch).

### Not added this spec

- No snapshot testing.
- No new instrumented UI tests.
- No changes to existing view-model tests.

### Known non-issue

First cold install flashes system-serif before Fraunces arrives via Downloadable Fonts — do not gate smoke on this.

---

## 10. Follow-up (outside this spec)

- **Sub-spec C2** — form / create / edit screens (`CreateGarden`, `CreateBed`, `CreatePlant`, `TaskForm`, `AddPlantEvent`, possibly `AddSeeds`). Needs a new primitive set for labeled inputs / dropdowns / date pickers.
- **Sub-spec C3** — activity / workflow screens (Sow, PotUp, PlantOut, ApplySupply, BatchPlantOut, AddSpecies). Needs wizard-step / submit-bar primitives.
- **Sub-spec D** — analytics / account / auth, plus deferred MyWorld dashboard and SeasonSelector.

Each sub-spec brainstormed independently when ready.
