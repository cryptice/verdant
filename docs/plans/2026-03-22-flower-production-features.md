# Flower Production Features Implementation Plan

> Status note: Phases 1-7 are substantially complete and shipped. Phase 8 (Android) is partially done. This document has been rewritten to reflect the actual state of the repo; most detailed task breakdowns were removed once the work landed.

**Goal:** Turn Verdant from a general garden planner into a commercial flower production tool covering planning, growing, sales tracking, and season-over-season optimization.

**Architecture:** Quarkus + Kotlin backend with new entities, services, and REST endpoints. React web admin gets new pages and a crop calendar. Android app gets updated models and key screens. No production data exists, so breaking changes are acceptable.

**Deltas from the original architecture decision:**
- The schema did NOT stay consolidated as a single `V1__schema.sql`. After the initial rewrite, further changes landed as incremental migrations. Current state is `V1__schema.sql` plus `V2__onboarding.sql`, `V3__rename_cost_columns.sql`, `V4__advanced_mode.sql`, `V5__seed_stock_provider.sql`, `V6__seed_stock_decimal_cost.sql`, `V7__organizations.sql`, `V8__species_default_unit_type.sql`, `V9__task_species_list.sql`, `V10__species_group_membership.sql`, `V11__supply_inventory.sql`, `V12__range_fields.sql`, `V13__growing_workflows.sql`, `V14__remove_marketplace.sql`. Future schema changes should be new `V*.sql` files, not edits to V1.
- Two major feature areas were added post-plan: **growing workflows** (templates, step completion, progress tracking, auto-advance on plant events) and **supply inventory** (typed supply catalog, per-activity usage recording).
- The in-tree marketplace (Listings, MarketOrders, OrderItems) was built and then removed in V14. Marketplace features now live in a separate Blomsterportalen repository and integrate with Verdant as an out-of-tree consumer. The `Channel.FARMERS_MARKET` enum on `Customer` remains — that's grower-side channel labelling, not the removed marketplace.

**Tech stack:** Quarkus + Kotlin (backend), React + TypeScript + Tailwind (web), Kotlin + Jetpack Compose (Android), PostgreSQL, Flyway migrations.

---

## Phase status

| Phase | Status | Notes |
|-------|--------|-------|
| 1. Schema rewrite | Done | V1 rewrite landed. Further changes continued as V2..V14. |
| 2. Backend core entities | Done | Season, Customer, PestDiseaseLog, VarietyTrial, BouquetRecipe, SuccessionSchedule, ProductionTarget — repositories, services, resources all present. |
| 3. Backend planning engine | Done | Succession task generation + production forecast endpoints live. |
| 4. Backend analytics | Done | AnalyticsService + AnalyticsResource present. Bed history via garden ownership chain. Harvest stats include totalStems. |
| 5. Web UI core | Done | SeasonList, enhanced PlantDetail harvest dialog, cost fields on SeedInventory/SpeciesList. |
| 6. Web UI business | Done | CustomerList, PestDiseaseLog, VarietyTrials, BouquetRecipes pages all live. |
| 7. Web UI planning & analytics | Done | CropCalendar, SuccessionSchedules, ProductionTargets, Analytics dashboards live. |
| 8. Android | In progress | Models + API for all new entities, Season selector, harvest dialog flower fields, i18n for seasons/harvest event types — all done. Screens for customers, pest/disease, trials, bouquets, succession, targets, analytics — NOT started. |

---

## Phase 1: Schema rewrite — Done

- V1 consolidation landed as planned: all prior migrations collapsed into `V1__schema.sql` with the new tables and columns integrated directly into CREATE TABLE statements.
- Post-V1 schema changes landed as incremental migrations (V2..V14) rather than edits to V1. Notably: onboarding state (V2), cost column renames (V3), advanced mode (V4), seed stock provider linkage + decimal cost (V5, V6), organizations (V7), species default unit type (V8), task species-list support (V9), species group membership (V10), supply inventory (V11), range fields for species min/max (V12), growing workflows (V13), marketplace removal (V14).
- Entities created: `Season`, `Customer`, `PestDiseaseLog`, `VarietyTrial`, `BouquetRecipe` (+ item), `SuccessionSchedule`, `ProductionTarget`. Supporting enums (`PlantType`, `UnitType`, `Channel`, `PestCategory`, `Severity`, `Outcome`, `Verdict`, `Reception`, `ItemRole`) are in place.
- Existing entities extended: `Bed` has `lengthMeters`/`widthMeters`, `Species` has cost/expected-stems/vase-life/plantType, `Plant` has `seasonId`, `PlantEvent` has stem/grade/vaseLife/destination + new event types (BUDDING, FIRST_BLOOM, PEAK_BLOOM, LAST_BLOOM, LIFTED, DIVIDED, STORED, PINCHED, DISBUDDED), `SeedInventory` has cost/unitType/seasonId, `ScheduledTask` has `seasonId`/`successionScheduleId`.
- DTO layer covers all new entities with `Response`, `CreateXxxRequest`, `UpdateXxxRequest` triples.

---

## Phase 2: Backend core entities — Done

- Repositories present for every new entity: `SeasonRepository`, `CustomerRepository`, `PestDiseaseLogRepository`, `VarietyTrialRepository`, `BouquetRecipeRepository`, `SuccessionScheduleRepository`, `ProductionTargetRepository`. All follow the existing JDBC pattern (see `GardenRepository`).
- Services layered on top with userId authorization: `SeasonService`, `CustomerService`, `PestDiseaseService`, `VarietyTrialService`, `BouquetRecipeService`, `SuccessionScheduleService`, `ProductionTargetService`.
- REST resources live at the expected paths: `/api/seasons`, `/api/customers`, `/api/pest-disease-logs`, `/api/variety-trials`, `/api/bouquet-recipes`, `/api/succession-schedules`, `/api/production-targets`.
- Existing resources were extended with `seasonId` query filters where relevant.
- Image upload for pest/disease and bouquet recipe photos flows through `StorageService`.

---

## Phase 3: Backend planning engine — Done

- `POST /api/succession-schedules/{id}/generate-tasks` implemented: creates a ScheduledTask per succession round, linked back via `successionScheduleId`.
- `GET /api/production-targets/{id}/forecast` implemented: calculates totalStemsNeeded, plantsNeeded, seedsNeeded, suggestedSowDate from species metadata; returns warnings for missing defaults (expectedStemsPerPlant, germinationRate, daysToHarvest). Forecast response DTO lives in Android `Models.kt` already as `ProductionForecastResponse`.

---

## Phase 4: Backend analytics — Done

- `AnalyticsService` + `AnalyticsResource` present. Endpoints:
  - `GET /api/analytics/seasons` — per-season summaries with top species.
  - `GET /api/analytics/species/{id}/compare` — same species across seasons.
  - `GET /api/analytics/yield-per-bed` — stems/m² using bed `lengthMeters` × `widthMeters`.
- Bed history endpoint (`GET /api/beds/{id}/history`) authorizes through `bed → garden → garden.ownerId` as planned.
- `StatsResource` `harvests` response includes `totalStems` column; Android `HarvestStatRow` already reads it.

---

## Phase 5: Web UI — Core — Done

- `web/src/api/client.ts` carries typed entries for Season, Customer, PestDiseaseLog, VarietyTrial, BouquetRecipe, SuccessionSchedule, ProductionTarget, analytics response types; existing types updated for new fields.
- `SeasonList.tsx` page, routing, sidebar nav, and i18n keys all present.
- `PlantDetail.tsx` event dialog exposes flower-specific fields (stem count, stem length, quality grade, destination) when event type is HARVESTED and supports bloom event types.
- Cost tracking visible on `SpeciesList.tsx` and `SeedInventory.tsx`.

---

## Phase 6: Web UI — Business — Done

- `CustomerList.tsx`, `PestDiseaseLog.tsx`, `VarietyTrials.tsx`, `BouquetRecipes.tsx` all present with routes, nav items, and i18n keys.

---

## Phase 7: Web UI — Planning & Analytics — Done

- `CropCalendar.tsx` horizontal timeline view live.
- `SuccessionSchedules.tsx` + `ProductionTargets.tsx` (with forecast panel) + `Analytics.tsx` dashboard all live.

---

## Phase 8: Android — In progress

Verified against `android/app/src/main/kotlin/app/verdant/android/ui/`, `data/model/Models.kt`, `data/api/VerdantApi.kt`, and `res/values[-sv]/strings.xml`.

### Models + API coverage

- [x] **Season** — `SeasonResponse`, `CreateSeasonRequest` in `Models.kt`; full CRUD in `VerdantApi.kt` (`getSeasons`, `createSeason`, `updateSeason`, `deleteSeason`). Update request uses `Map<String, Any?>` rather than a typed `UpdateSeasonRequest` — functional but inconsistent with other entities.
- [~] **Customer** — `CustomerResponse` present; `getCustomers()` endpoint present. No `CreateCustomerRequest`/`UpdateCustomerRequest`, no POST/PUT/DELETE in `VerdantApi.kt`. Sufficient for harvest-dialog destination dropdown; missing for a full customer management screen.
- [ ] **PestDiseaseLog** — no model, no API client method. File to add to: `android/app/src/main/kotlin/app/verdant/android/data/model/Models.kt` and `android/app/src/main/kotlin/app/verdant/android/data/api/VerdantApi.kt`. Needs `PestDiseaseLogResponse`, `CreatePestDiseaseLogRequest`, `UpdatePestDiseaseLogRequest`, enums for category/severity/outcome, plus CRUD endpoints at `/api/pest-disease-logs`.
- [ ] **VarietyTrial** — no model, no API client method. Needs `VarietyTrialResponse`, Create/Update requests, enums for `Verdict` and `Reception`, and CRUD on `/api/variety-trials`.
- [ ] **BouquetRecipe** — no model, no API client method. Needs `BouquetRecipeResponse` with nested `BouquetRecipeItemResponse`, Create/Update requests including items, and CRUD on `/api/bouquet-recipes`.
- [x] **SuccessionSchedule** — `SuccessionScheduleResponse` in `Models.kt`; `getSuccessionSchedules`, `createSuccessionSchedule` (using `Map<String, Any?>`), `generateSuccessionTasks`, `deleteSuccessionSchedule` in `VerdantApi.kt`. No `UpdateSuccessionScheduleRequest`/PUT — acceptable if edit-in-place isn't planned for Android.
- [x] **ProductionTarget** — `ProductionTargetResponse`, `ProductionForecastResponse` in `Models.kt`; `getProductionTargets`, `createProductionTarget` (using `Map<String, Any?>`), `getProductionForecast`, `deleteProductionTarget` in `VerdantApi.kt`. Same pattern as succession schedule.
- [ ] **Analytics** — no response types, no API client methods. Needs `SeasonSummaryResponse`, `SpeciesYieldSummary`, `SpeciesComparisonResponse`, `SpeciesSeasonData`, `YieldPerBedResponse`, `BedSeasonYield` in `Models.kt`, plus endpoints `/api/analytics/seasons`, `/api/analytics/species/{id}/compare`, `/api/analytics/yield-per-bed` in `VerdantApi.kt`.

### Screens

- [x] **Season selector** — `android/app/src/main/kotlin/app/verdant/android/ui/season/SeasonSelectorScreen.kt` exists, is wired into `NavGraph.kt` (`Screen.Seasons`, drawer entry), and supports create/edit/refresh.
- [x] **Harvest dialog flower fields** — `AddPlantEventScreen.kt` exposes stem count, stem length, quality grade (A/B/C chips), and customer destination dropdown when event type is HARVESTED. Loads `customers` via `GardenRepository.getCustomers()` in the VM's init block. All new event types (BUDDING, FIRST_BLOOM, PEAK_BLOOM, LAST_BLOOM, LIFTED, DIVIDED, STORED, PINCHED, DISBUDDED) are selectable as secondary chips. **Latent issue:** `vaseLifeDays` is captured nowhere in this screen even though the request DTO supports it — add a field, or accept that vase-life is admin-UI-only. Worth a call.
- [ ] **Customer list / edit screen** — not present. Add under `android/app/src/main/kotlin/app/verdant/android/ui/customer/`. Required if Android should manage customers (not just pick from existing). Decide whether this is in scope for field use or whether customers stay web-managed.
- [ ] **Pest/disease log screen** — not present. Add under `android/app/src/main/kotlin/app/verdant/android/ui/pest/`. This is the most field-relevant missing screen — observations happen while walking beds.
- [ ] **Variety trials screen** — not present. Add under `android/app/src/main/kotlin/app/verdant/android/ui/trials/`. Lower priority (mostly data entry at season end).
- [ ] **Bouquet recipes screen** — not present. Add under `android/app/src/main/kotlin/app/verdant/android/ui/bouquet/`. Probably low-priority on mobile.
- [ ] **Succession schedules screen** — not present. Add under `android/app/src/main/kotlin/app/verdant/android/ui/succession/`. Android API client already supports it; just no UI.
- [ ] **Production targets screen** — not present. Add under `android/app/src/main/kotlin/app/verdant/android/ui/targets/`. Android API client supports list + forecast + create.
- [ ] **Analytics screen** — not present. Add under `android/app/src/main/kotlin/app/verdant/android/ui/analytics/`. Needs the model/API additions listed above first.

### i18n

Checked `values/strings.xml` (361 lines) and `values-sv/strings.xml` (359 lines).

- [x] **Season** strings — `seasons`, `new_season`, `edit_season`, `season_name`, `season_year`, `active_season`, `no_seasons` present in both EN and SV.
- [x] **Harvest flower fields** strings — `stem_count`, `stem_length`, `quality_grade`, `destination` present in both locales.
- [x] **New event types** — `budding`, `first_bloom`, `peak_bloom`, `last_bloom`, `lifted`, `divided`, `stored`, `pinched`, `disbudded` present in both.
- [ ] **Customer** strings — none present. Needs `customers`, `customer_name`, `channel`, channel enum labels (FLORIST, FARMERS_MARKET, CSA, WEDDING, WHOLESALE, DIRECT, OTHER), `contact_info`.
- [ ] **Pest/disease** strings — none present. Needs `pest_disease`, category labels (PEST, DISEASE, DEFICIENCY, OTHER), severity labels (LOW, MODERATE, HIGH, CRITICAL), outcome labels (RESOLVED, ONGOING, CROP_LOSS, MONITORING), plus `observed_date`, `treatment`, `outcome`.
- [ ] **Variety trials** strings — none present. Needs `variety_trials`, verdict labels (KEEP, EXPAND, REDUCE, DROP, UNDECIDED), reception labels (LOVED, LIKED, NEUTRAL, DISLIKED), `quality_score`, `customer_reception`.
- [ ] **Bouquets** strings — none present. Needs `bouquets`, `recipe_name`, `price`, item role labels (FLOWER, FOLIAGE, FILLER, ACCENT).
- [ ] **Succession schedules** strings — none present. Needs `successions`, `first_sow_date`, `interval_days`, `total_successions`, `seeds_per_succession`, `generate_tasks`.
- [ ] **Production targets** strings — none present. Needs `targets`, `stems_per_week`, `delivery_window`, `forecast`, `plants_needed`, `seeds_needed`, `suggested_sow_date`, `warnings`.
- [ ] **Analytics** strings — none present. Needs `analytics`, `season_summary`, `species_comparison`, `yield_per_bed`, `stems_per_m2`, etc.

### Navigation

- [x] `Screen.Seasons`, `Screen.Supplies`, `Screen.WorkflowProgress` wired into `NavGraph.kt`.
- [ ] Nav entries for any new screens added per the checklist above.

---

## Out-of-plan features that also shipped

Not tracked in the original plan but present in the codebase today. Listed for completeness; no action required.

- **Growing workflows** — template catalog, per-species workflow assignment, per-plant progress tracking, side-branch support, auto-advance when plant events are recorded. Backend: `WorkflowResource`, `WorkflowService`, `WorkflowRepository`, `Workflow` entity, V13 migration. Web: `WorkflowTemplates`, `WorkflowTemplateEdit`, `WorkflowProgress` pages. Android: `WorkflowProgressScreen` + step completion API.
- **Supply inventory** — typed supply catalog with properties per type, per-activity usage recording, season linkage. Backend: `SupplyResource`, `SupplyService`, `Supply` entity, V11 migration. Web: `Supplies` page. Android: `SupplyInventoryScreen` + `SupplyUsageDialog`.
- **Voice commands on field-work screens** — `ui/voice/` module integrated into sow/pot-up/plant-out/harvest/recover/discard flows.
- **Swedish default + language switcher** — admin sidebar language toggle, SV as default.
- **Bed length/width** — enables yield-per-m² analytics (used in `/api/analytics/yield-per-bed`).
- **Species group membership** — many-to-many groups (V10), used for task species-list acceptance criteria.
- **Range fields** — species min/max for germination time, days to harvest, height (V12).
