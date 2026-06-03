# Weather Integration Completion + Two Smaller Gaps — Implementation Plan

**Status:** draft (2026-06-02)
**Targets:** backend (Quarkus/Kotlin), web (React), Android (Compose)
**Source of truth for weather design:** [`2026-04-18-weather-integration-design.md`](./2026-04-18-weather-integration-design.md). This plan is the *execution* plan; the design doc still governs data-model columns, thresholds, GDD formula, and payload shapes. Where this plan diverges from the design, the divergence is called out under **Deltas from the design** below.

This document covers three independent work streams. They can be delivered in any order:

1. **Weather integration** — finish the feature across all three clients (the bulk of the work).
2. **Gap A** — wire the placeholder harvest analytics on the web Dashboard / BedDetail / GardenDetail.
3. **Gap B** — add the Android workflow-template management UI to reach web parity.

---

## 0. Current state (verified 2026-06-02)

**Weather — M1 is partially shipped.**

- ✅ `V15__weather.sql` (`daily_weather`, `garden.weather_backfill_status`) applied.
- ✅ `entity/DailyWeather.kt`, `repository/DailyWeatherRepository.kt` (upsert + `findByGarden`).
- ✅ `service/weather/SmhiClient.kt` — `SmhiForecastParser.parse()` is **fully implemented** (forecast pipeline works end to end).
- ✅ `service/weather/WeatherIngestionService.kt` — both `@Scheduled` jobs + `backfillForGarden` + async executor.
- ✅ `resource/WeatherResource.kt` — `GET /api/weather/garden/{id}?days=N` returns forecast + actuals + backfill status.
- ✅ Tests: `WeatherIngestionServiceTest`, `WeatherResourceTest`, `DailyWeatherRepositoryTest`.
- ❌ **`SmhiArchiveParser.parseForPoint()` returns `null`** (`SmhiClient.kt:109`). Consequence: the actuals job and the 3-year backfill currently insert **zero** `ACTUAL` rows. Forecast-only data is in the store.
- ❌ `WeatherIngestionService.evaluateAlerts()` is an empty stub (`:98`, `TODO(M3)`).
- ❌ No FCM, no alert tables, no GDD, no analytics overlay, no irrigation signal.
- ❌ No web or Android weather UI of any kind.

**Migration numbering correction.** The design doc named migrations `V15`–`V18`. The DB is now at **`V41`**, and `V16`–`V41` are taken by unrelated features. All *new* migrations in this plan are therefore **`V42`+** and must be sequential (Flyway runs out-of-order disabled). Mapping below.

| Design name | Real name | Contents |
|---|---|---|
| `V15__weather.sql` | (already applied) | `daily_weather`, `garden.weather_backfill_status` |
| `V16__user_device.sql` | **`V42__user_device.sql`** | `user_device` |
| `V17__weather_alerts.sql` | **`V43__weather_alerts.sql`** | `weather_alert`, `user_alert_preference` |
| `V18__gdd.sql` | **`V44__gdd.sql`** | `species.gdd_*` columns, `gdd_accumulation` |

**Deltas from the design (2026-04-18):**

- Migration renumbering as above.
- **M1 has a hidden remainder**: the archive parser. Promoted to its own slice **M1.5** below because GDD (M4), analytics (M5), and irrigation (M6) all depend on `ACTUAL` rows. Frost alerts (M3) evaluate `FORECAST` rows and do **not** depend on it, so M2→M3 can ship first.
- Everything else in the design stands.

---

## 1. Weather — execution plan

Dependency graph:

```
M1 (done) ──> M1.5 (actuals) ──> M4 (GDD) ──> M5 (analytics)
   │                          └─> M6 (irrigation)
   └──> M2 (FCM) ──> M3 (frost alerts + Weather UI)
```

M2+M3 deliver a shippable product on their own (frost alerts for planted-out beds). M1.5 unlocks the analytics arc. Recommended order: **M2 → M3 → M1.5 → M4 → M5 → M6.**

### M1.5 — Actuals ingestion (archive parser)

No schema change. Backend only. Unblocks M4–M6.

- [ ] Implement `SmhiArchiveParser.parseForPoint(...)` in `service/weather/SmhiClient.kt`. SMHI's met-obs archive is station-based (`/api/version/1.0/parameter/{p}/station-set/all/period/latest-day/data.json`, and per-station period endpoints for historical days). Implementation:
  1. Parse the station list with lat/lon from the response.
  2. Pick the nearest active station to the garden `(lat, lon)` (haversine).
  3. Aggregate that station's hourly readings into one `DailyWeather(observationType = ACTUAL)` for the requested date (min/max/mean temp = parameter `2`/`1`; precip = parameter `7`/`5`; wind = parameter `4`). Re-use the per-day aggregation shape already in `SmhiForecastParser`.
- [ ] `fetchActual()` already calls the parser — verify the URL is right for *historical* dates (backfill requests dates up to 3 years back; `latest-day` only covers yesterday). Add a date-parameterised archive URL (`period/corrected-archive` or per-day) for the backfill path.
- [ ] Tests: fixtures under `backend/src/test/resources/smhi/archive/`. Cover nearest-station selection, day aggregation, missing-parameter rows, point-outside-Sweden (returns null gracefully — backfill skips). Extend `WeatherIngestionServiceTest` to assert backfill now produces `ACTUAL` rows.
- [ ] **Verify:** trigger `submitBackfill(gardenId)` against a real coordinate; confirm `daily_weather` fills with `ACTUAL` rows and `garden.weather_backfill_status = DONE`.

### M2 — FCM infrastructure (plumbing only, no alerts)

**Backend**
- [ ] Migration **`V42__user_device.sql`**: `user_device(id, user_id, fcm_token UNIQUE, platform, last_seen_at)`.
- [ ] `entity/UserDevice.kt`, `repository/UserDeviceRepository.kt`.
- [ ] Add dependency `com.google.firebase:firebase-admin` (pom). Credentials: Application Default Credentials on GCP (matches existing Cloud Build deploy); `FIREBASE_CREDENTIALS_PATH` env var locally. Wire into `application.properties` + document in `backend/README.md` and `deploy/`.
- [ ] `service/weather/FcmService.kt` — Firebase Admin SDK wrapper; async send (`CompletableFuture`); failures logged, never thrown. On `UNREGISTERED`/`INVALID_ARGUMENT`, delete the offending `user_device` row.
- [ ] `resource/UserDeviceResource.kt` — `POST /api/user-devices` (register/refresh token), `DELETE /api/user-devices/{token}`.
- [ ] Dev-only `POST /api/dev/test-push` (guarded to non-prod profile) for end-to-end verification before alerts exist.
- [ ] Tests: `FcmService` mocked; `UserDeviceResource` register/dedup/deregister.

**Android**
- [ ] Add Firebase Messaging SDK + `google-services` Gradle plugin; add `google-services.json` per environment (do **not** commit secrets — wire via the existing secrets mechanism, see `secrets/`).
- [ ] `push/VerdantMessagingService.kt` (`FirebaseMessagingService`): `onNewToken` → re-register; `onMessageReceived` → build notification (channels added in M3).
- [ ] On app start after login, `POST /api/user-devices` with the current token (add `registerDevice` to `VerdantApi` + a small `DeviceRepository`). Re-register on token refresh.
- [ ] **Verify:** `POST /api/dev/test-push` → device receives a notification.

### M3 — Frost alerts + Weather UI (first real user value)

**Backend**
- [ ] Migration **`V43__weather_alerts.sql`**: `weather_alert` (unique `(garden_id, target_date, threshold_type, user_id)`) and `user_alert_preference` (PK `(user_id, threshold_type)`), exactly per design §2.
- [ ] Entities + repositories: `WeatherAlert`, `UserAlertPreference`.
- [ ] `service/weather/AlertEvaluator.kt` — `evaluate(gardenId)`: for each `FORECAST` row in next 72h, evaluate thresholds (design §4 table). Frost wired (`temp_min_c ≤ 0`, applicable when garden has a `PLANTED_OUT` plant); heat/wind/rain **scaffolded but disabled by default**. Dedup against `weather_alert`. Recipient resolution: garden → owning org → org users with a `user_device`. Per-user overrides via `user_alert_preference`.
- [ ] Wire it: replace the `evaluateAlerts()` stub in `WeatherIngestionService.kt:98` with an injected `AlertEvaluator` call (remove the `TODO(M3)`).
- [ ] `resource/AlertPreferenceResource.kt` — `GET /api/alert-preferences`, `PUT /api/alert-preferences/{type}`.
- [ ] Extend `WeatherResource` — `GET /api/weather/garden/{id}/alerts` (active + recent).
- [ ] Tests: `AlertEvaluator` unit (fake `FcmService` recording sends) — boundary `temp_min_c = 0.0` triggers, dedup, custom override, token cleanup on `UNREGISTERED`. End-to-end `@QuarkusTest`: seed a `-2°C` forecast row → eval → alert row + FcmService send.

**Web** (`/workspaces/verdant/web`)
- [ ] `pages/Weather.tsx` at route `/weather`: 7-day day-cards, active-alert badges, garden picker (when org has >1 garden). Banner prompting coordinates when no garden has lat/lon.
- [ ] Dashboard "weather at a glance" block (today + next 3 days, links to `/weather`) — see Gap A note: this lands in `Dashboard.tsx`.
- [ ] `pages/Account.tsx` — "Weather alerts" section: per-threshold toggle + custom-value inputs, backed by `user_alert_preference`.
- [ ] API client: add `api.weather.*` (forGarden, alerts) and `api.alertPreferences.*` (list, update) to `src/api/client.ts`.
- [ ] i18n: new `weather.*` namespace in `sv.json` + `en.json`.
- [ ] Tests: component tests (`Weather`, dashboard block, alert prefs) with `msw`; E2E smoke in `web/e2e/`.

**Android** (`/workspaces/verdant/android`)
- [ ] `ui/weather/WeatherScreen.kt` + `WeatherViewModel` — vertical day list, garden picker, alert badges. Uses existing `GET /api/weather/garden/{id}` (+ new `/alerts`).
- [ ] `ui/weather/AlertPreferencesScreen.kt` + ViewModel — mirrors web; under the Account drawer.
- [ ] Notification channels per threshold type (Frost/Heat/Wind/Rain) so users mute individually in Android settings. Tap intent routes to `WeatherScreen` for the referenced garden (from push `data.gardenId`).
- [ ] Request `POST_NOTIFICATIONS` (API 33+) the first time the user opens `WeatherScreen` or enables a threshold — **not** at launch.
- [ ] Drawer: new "Weather" entry (`Icons.Default.Cloud`); nav graph entry.
- [ ] `VerdantApi`: add weather + alert-preference + (already in M2) device endpoints; models in a new `WeatherModels.kt`.
- [ ] i18n: `weather_*` keys in `values/strings.xml` + `values-sv/strings.xml`.
- [ ] Tests: `WeatherViewModel` / `AlertPreferencesViewModel` unit tests with a fake repository; `VerdantMessagingService` intent-building tested in isolation.
- [ ] **Verify (M3 end-to-end):** seed a frost forecast → cron/`evaluate` → push received → tap opens WeatherScreen.

### M4 — Remaining alerts + GDD (depends on M1.5)

**Backend**
- [ ] Enable heat/wind/rain thresholds behind per-user preference (flip the M3 scaffolding on).
- [ ] Migration **`V44__gdd.sql`**: `species.gdd_base_temp_c`, `species.gdd_to_first_bloom`, `species.gdd_to_harvest` (all nullable), and `gdd_accumulation` (PK `(garden_id, species_id, season_id)`), per design §2.
- [ ] `service/weather/GddService.kt` — accumulation per `(garden, species, season)`; daily GDD `= max(0, temp_mean_c - base_temp_c)` (`ACTUAL` preferred, `FORECAST` to extend; base temp falls back to 10°C). Cache with `as_of_date`; recompute forward after the actuals job; delete+regenerate on `gdd_base_temp_c` change or scope-boundary event shift.
- [ ] `GET /api/gdd/plant/{plantId}` — accumulated GDD today, predicted first-bloom/harvest dates, `confidence` (`high` / `forecast-limited` / `low`), per design §5.
- [ ] Hook `GddService.refreshGarden()` into the daily actuals job.
- [ ] Update species DTOs to carry the three GDD fields.
- [ ] Tests: `GddService` unit with synthetic `daily_weather` (boundary `mean = base` → 0, gaps in actuals, forecast extension, recompute on new actual).

**Web**
- [ ] `pages/PlantDetail.tsx` — GDD progress strip under the event timeline (`accumulated / gdd_to_harvest`, projected harvest date, confidence). Rendered only when species has GDD metadata.

**Admin** (`/workspaces/verdant/admin`)
- [ ] Species form exposes the three GDD metadata fields.

**Android**
- [ ] `PlantDetailScreen.kt` — same GDD strip; add `getPlantGdd` to `VerdantApi`.

### M5 — Weather-overlay analytics (depends on M1.5)

**Backend** — three read-only endpoints on `AnalyticsResource`:
- [ ] `GET /api/analytics/weather/season/{seasonId}` — per-garden daily min/max/mean/precip/wind series.
- [ ] `GET /api/analytics/weather/species-overlay/{speciesId}?seasonId=X` — plant events + weekly harvest stems joined with weekly mean temp + precip.
- [ ] `GET /api/analytics/gdd/{plantId}` — GDD curve sowing→today + projected forward.

**Web**
- [ ] `pages/Analytics.tsx` — new "Weather overlay" tab: weekly stems overlaid on weekly mean temp + precip bars.

**Android**
- [ ] Analytics screen gains a weather-overlay section.

### M6 — Irrigation signal (depends on M1.5)

**Backend**
- [ ] `TaskGenerationService` gains `recent_precipitation_mm` (sum of last 7 days `ACTUAL` for the garden). Skip watering-task generation when sum ≥ **15 mm** (configurable via `user_alert_preference` `threshold_type = IRRIGATION_SKIP_THRESHOLD`, `custom_value` in mm — note this row type lives only in `user_alert_preference`, never produces a `weather_alert`).
- [ ] Tests: task generation skips when wet, generates when dry, respects custom threshold.

**Web / Android**
- [ ] Surface `IRRIGATION_SKIP_THRESHOLD` in the existing Alert Preferences UI (web `Account.tsx` + Android `AlertPreferencesScreen.kt`).

### Weather — rough sizing

M1.5 small-medium (nearest-station logic + fixtures). M2+M3 the bulk (FCM plumbing is fiddly but one-time; two new client screens each). M4 medium (GDD math + strips). M5–M6 small. ~2–2.5 solo-dev weeks for the full arc; M2+M3 alone (~4–5 days) already ship frost alerts.

---

## 2. Gap A — wire placeholder harvest analytics (web + backend)

Three web pages render hardcoded harvest numbers. Backend has org-wide species harvest totals (`GET /api/stats/harvests`), per-season totals (`GET /api/analytics/seasons` → `totalStemsHarvested`), and per-bed-per-season stems (`GET /api/analytics/yield-per-bed`), but **no** weekly breakdown, year-over-year delta, per-bed total, or per-garden total.

**Placeholders to remove:**
- `web/src/pages/Dashboard.tsx:115` — `totalStems` falls back to `?? 142`; `:550` — `bestWeek` hardcoded to week `32`, delta hardcoded `+24 % vs 2024 ▲`.
- `web/src/pages/BedDetail.tsx:313` — harvest card stems hardcoded `0`.
- `web/src/pages/GardenDetail.tsx:122` — `const harvestStemsThisYear = 0`.

**Backend (add to `AnalyticsResource` / `PlantEventRepository`):**
- [ ] `GET /api/analytics/harvest-summary?seasonId=X` → `{ totalStems, bestWeek: { isoWeek, stems }, prevYearTotalStems }`. One season-scoped query grouped by ISO week supplies both `totalStems` (sum) and `bestWeek` (max bucket); `prevYearTotalStems` powers the delta. Drives the Dashboard card.
- [ ] `GET /api/beds/{id}/harvest-stats?seasonId=X` → `{ totalStems }` (extend `yield-per-bed` query, or add `PlantEventRepository.harvestStatsByBed`). Drives BedDetail.
- [ ] `GET /api/gardens/{id}/harvest-stats?seasonId=X` → `{ totalStems }` (sum over the garden's beds). Drives GardenDetail.
- [ ] Tests: repository aggregation correctness; resource org-scoping.

**Web:**
- [ ] Add `api.analytics.harvestSummary(seasonId)`, `api.beds.harvestStats(id, seasonId)`, `api.gardens.harvestStats(id, seasonId)` to `src/api/client.ts`.
- [ ] Dashboard: replace `?? 142`, the `week: 32` literal, and the `+24 %` literal with query data; compute delta = `(totalStems - prevYearTotalStems) / prevYearTotalStems`. Hide the delta line when `prevYearTotalStems == 0`.
- [ ] BedDetail / GardenDetail: replace the hardcoded `0`s with query results; show a quiet empty state when none.
- [ ] Remove the three `TODO`/`placeholder` comments.
- [ ] Tests: component tests with `msw` for the wired cards.

> Note: the M3 Dashboard "weather at a glance" block and this Gap A harvest card both live in `Dashboard.tsx`. If both streams run, coordinate the one file.

---

## 3. Gap B — Android workflow-template management UI (parity with web)

Android has the per-plant workflow editor (`PlantWorkflowSection`) and the species progress view (`WorkflowProgressScreen`), but **no** template list/editor and **no** assign-template control on the species detail screen. The web reference is `WorkflowTemplates.tsx`, `WorkflowTemplateEdit.tsx`, and the `WorkflowAccessPanel` in `SpeciesDetail.tsx`. The backend endpoints already exist; the Android API client is missing the template-CRUD half.

**API client — add to `data/api/VerdantApi.kt`:**
- [ ] `getTemplate(id)`, `createTemplate(req)`, `updateTemplate(id, req)`, `deleteTemplate(id)`
- [ ] `addTemplateStep(templateId, req)`, `updateStep(stepId, req)`, `deleteStep(stepId)`
  (these map to `GET/POST/PUT/DELETE /api/workflows/templates[/{id}]`, `POST /api/workflows/templates/{id}/steps`, `PUT/DELETE /api/workflows/steps/{stepId}`)

**Models — `data/model/WorkflowModels.kt`:**
- [ ] Add `steps: List<WorkflowStepResponse>` to `WorkflowTemplateResponse`.
- [ ] Add `WorkflowStepResponse` (name, eventType, daysAfterPrevious, optional, isSideBranch, sideBranchName, suggestedSupplyTypeId, suggestedQuantity, sortOrder).
- [ ] Add `CreateWorkflowTemplateRequest`, `UpdateWorkflowTemplateRequest`.

**Repository — `data/repository/WorkflowRepository.kt`:**
- [ ] Add template-CRUD + step-CRUD wrapper methods.

**Screens (new, under `ui/workflow/`):**
- [ ] `WorkflowTemplateListScreen.kt` + `WorkflowTemplateListViewModel.kt` — list (name, description, step count), "New Template", delete, tap-to-edit.
- [ ] `WorkflowTemplateEditScreen.kt` + `WorkflowTemplateEditViewModel.kt` — editable name/description; ordered step list with up/down reorder (`sortOrder`) and inline event-type selector; tap-step modal for full edit (optional, side-branch, suggested supply); add/delete step.

**Navigation — `ui/navigation/NavGraph.kt` (+ new `graphs/WorkflowGraph.kt`):**
- [ ] `Screen.WorkflowTemplates ("workflows/templates")` and `Screen.WorkflowTemplateEdit ("workflows/templates/{templateId}")`; register in `VerdantNavHost`. Pattern mirrors `SpeciesList → EditSpecies`. Add a drawer/menu entry to reach the template list.

**Assign control — `ui/plants/PlantedSpeciesDetailScreen.kt` (+ ViewModel):**
- [ ] Load species workflow (`getSpeciesWorkflow`) + available templates (`getWorkflowTemplates`). Add a section like the web `WorkflowAccessPanel`: if assigned, show template name + step count + "Sync from template" (`syncSpeciesWorkflow`) + link to progress/edit; if not, show a template dropdown + "Assign" (`assignSpeciesWorkflow`).

**i18n — `res/values/strings.xml` + `res/values-sv/strings.xml`:**
- [ ] `workflows_templates`, `workflows_new_template`, `workflows_template_name`, `workflows_template_description`, `workflows_steps`, `workflows_add_step`, `workflows_assign_template`, `workflows_switch_template`, `workflows_sync_from_template`, `workflows_no_workflow`, `workflows_select_template`, `workflows_edit_template`, `workflows_delete_template`, `workflows_step_name`, `workflows_days_after_previous`, `workflows_event_type`, `workflows_optional`, …

**Tests:**
- [ ] `WorkflowTemplateListViewModel` / `WorkflowTemplateEditViewModel` unit tests with a fake `WorkflowRepository`.

---

## 4. Cross-cutting verification

- **Backend:** `./mvnw -q test` (or the module's wrapper) after each migration + service. New Flyway migrations must apply cleanly on a fresh DB *and* on top of `V41`.
- **Web:** `npm run lint && npm run typecheck && npm test` in `web/`; Playwright smoke for new pages.
- **Android:** unit tests + `:app:compileDebugKotlin` (and `lint`) — the devcontainer has the Android SDK installed.
- **Secrets:** Firebase credentials (backend) and `google-services.json` (Android) must be wired through the existing `secrets/` mechanism and `deploy/` config — never committed.

## 5. Open questions / decisions to confirm before starting

1. **SMHI historical archive URL** — `latest-day` only covers yesterday; backfill needs per-day or `corrected-archive` access. Confirm the exact archive endpoint shape during M1.5 (spike against the live API).
2. **Firebase project** — does a Firebase project / service account already exist, or does one need provisioning? Gates M2.
3. **GDD metadata seeding** — `gdd_base_temp_c` etc. are null for all existing species; the GDD strip stays hidden until populated. Acceptable for first ship (confidence falls back to `days_to_harvest`).
4. **Gap A delta baseline** — "vs previous year" assumes a prior season exists; confirm the empty-state copy when it doesn't.
