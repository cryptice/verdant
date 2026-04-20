# Bed Conditions Implementation Plan

**Goal:** Add optional horticultural-profile fields to `Bed` (soil, sun, drainage, aspect, irrigation, protection, raised, pH), surface the three most load-bearing values as chips + filter on the garden detail view, and mirror on Android.

**Architecture:** Flat nullable columns on `bed` (no separate profile table, no history). `VARCHAR + CHECK` enums matching the project convention. pH validated [3.0, 9.0] at the backend; web form does soft hints. One migration `V16__bed_conditions.sql`. No bespoke `conditionsNotes` field — the existing `description` covers free-text.

**Tech stack:** Quarkus + Kotlin (backend), Flyway, React + TypeScript + Tailwind (web), Kotlin + Jetpack Compose (Android), PostgreSQL.

**Design summary:** see the grilled design captured in the project transcript on 2026-04-19 and summarised below.

---

## Fields (all optional on create + update)

| Column | Type | Enum values / range |
|---|---|---|
| `soil_type` | VARCHAR(16) | SANDY, LOAMY, CLAY, SILTY, PEATY, CHALKY |
| `soil_ph` | DOUBLE PRECISION | [3.0, 9.0] |
| `sun_exposure` | VARCHAR(16) | FULL_SUN, PARTIAL_SUN, PARTIAL_SHADE, FULL_SHADE |
| `drainage` | VARCHAR(16) | POOR, MODERATE, GOOD, SHARP |
| `aspect` | VARCHAR(4) | FLAT, N, NE, E, SE, S, SW, W, NW |
| `irrigation_type` | VARCHAR(16) | DRIP, SPRINKLER, SOAKER_HOSE, MANUAL, NONE |
| `protection` | VARCHAR(16) | OPEN_FIELD, ROW_COVER, LOW_TUNNEL, HIGH_TUNNEL, GREENHOUSE, COLDFRAME |
| `raised_bed` | BOOLEAN | nullable |

---

## Tasks

### Task 1 — Migration V16

Create `backend/src/main/resources/db/migration/V16__bed_conditions.sql`. Single `ALTER TABLE bed ADD COLUMN` per field. CHECK constraints for each enum + pH range. Commit alone.

### Task 2 — Entity + DTO + repository

Extend `entity/Bed.kt`, `dto/BedDtos.kt` (BedResponse + CreateBedRequest + UpdateBedRequest), `repository/BedRepository.kt` (persist, update, toBed mapper), `service/BedService.kt` (wire new fields through, keep `updateBed` null-means-keep semantics). Commit.

### Task 3 — Web: form + detail

`web/src/pages/BedForm.tsx` — add a "Conditions (optional)" collapsible section below the existing fields. Collapsed by default on create, auto-expanded when editing a bed with any condition set. Two-column grid on desktop.

`web/src/pages/BedDetail.tsx` — surface the same section (same component, hosted inside the detail edit flow if there is one; otherwise in BedDetail's edit dialog/page).

pH input: soft validation hint when outside [3.0, 9.0]; form allows submission and relies on backend to reject.

i18n: add `bed.conditions.*` keys to `en.json` and `sv.json`. Swedish labels use horticultural register (SANDY = Sandjord, CLAY = Lerjord, SHARP = Snabbdränerad, COLDFRAME = Kallbänk, FULL_SUN = Full sol, etc.).

Commit.

### Task 4 — Web: garden detail chips + filter

`web/src/pages/GardenDetail.tsx` — for each bed card, render up to three chips matching the fields that have values: ☀️ sun / 💧 drainage / 🏠 protection. Hide the chip strip entirely if all three are null. Skip null fields (no placeholders).

Filter toolbar above the bed list: three dropdown pills (Sun / Drainage / Protection), each with "Any" + the enum values. Strict match — selecting a value excludes beds with null on that field. "Any" passes all beds. OR within a field, AND across fields.

Commit.

### Task 5 — Android: form + detail

`android/app/src/main/kotlin/app/verdant/android/data/model/Models.kt` — add new fields to `BedResponse`, `CreateBedRequest`, `UpdateBedRequest`. Add `SoilType`, `SunExposure`, `Drainage`, `Aspect`, `IrrigationType`, `Protection` companion-object value lists matching existing enum-as-string pattern.

`android/app/src/main/kotlin/app/verdant/android/ui/bed/CreateBedScreen.kt` and `BedDetailScreen.kt` — collapsible "Conditions" section following the same pattern as the web form. Single-column on mobile. pH input with a soft hint on out-of-range.

`android/app/src/main/res/values/strings.xml` + `values-sv/strings.xml` — matching keys.

Commit.

### Task 6 — Android: garden detail chips

`android/app/src/main/kotlin/app/verdant/android/ui/garden/GardenDetailScreen.kt` — render up to three chips per bed card matching the web's sun / drainage / protection set. Hide strip when all null.

**No** filter toolbar on Android per the design — field-use pattern is "walk to bed, tap."

Commit.

### Task 7 — Data export patch

`backend/src/main/kotlin/app/verdant/service/DataExportService.kt` + `dto/UserDtos.kt` (whichever DTO serializes beds for export) — include the 8 new fields on the exported bed shape. Mechanical.

Commit.

### Task 8 — Verification + milestone

- Backend: `cd backend && ./gradlew test -q` green.
- Web: `cd web && npx tsc --noEmit` clean.
- Android: `cd android && ./gradlew compileDebugKotlin --no-daemon -q` green.

Milestone commit: `milestone: bed conditions complete`.

---

## Timing

Executed on 2026-04-19, before weather M2. Weather migrations V17/V18/V19 renumbered in `docs/plans/2026-04-18-weather-integration-plan.md` accordingly.
