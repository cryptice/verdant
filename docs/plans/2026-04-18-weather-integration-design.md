# Weather Integration — Design

**Status:** approved for planning (2026-04-18)
**Target:** Verdant backend (Quarkus+Kotlin), Verdant web (React), Verdant Android (Compose)
**Driver:** enable four weather-dependent use cases in one coherent subsystem — GDD-based harvest forecasting, frost/weather alerts, post-season analytics, and irrigation task signal.

---

## 1. Goals and scope

Four use cases, designed for together, implemented in phases:

- **A. GDD-based harvest forecasting.** Accumulate growing-degree-days from sowing to predict first bloom and harvest dates per plant, more accurately than the species "days to harvest" heuristic.
- **B. Frost and weather warnings.** Push notifications when a frost, heat wave, high-wind event, or heavy rain is forecast for a garden with active plants.
- **C. Post-season analytics overlay.** Weather joined with plant events and harvest stems to support "why did X do badly in year Y."
- **D. Irrigation signal.** Recent precipitation feeds task generation — skip watering tasks when the garden is already wet.

Decision recorded earlier: **design for all four upfront, implement in phases.** The data model and refresh cadence accommodate all consumers from day one; phasing is delivery only.

Data source is **SMHI** (Swedish Meteorological and Hydrological Institute). No auth required. Sweden-only today — provider abstraction is explicitly out of scope (YAGNI). If Verdant ever supports gardens outside Sweden, a `WeatherProvider` interface gets introduced at that point, not speculatively.

Notifications are **Android FCM**. Email and iOS are out of scope.

Backfill depth: **3 years** fixed, triggered on garden creation and on coordinate change.

---

## 2. Architecture and data model

### New tables (migration `V15__weather.sql` through `V18__gdd.sql`, one per milestone)

**`daily_weather`** — primary weather store.

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT IDENTITY PK` | |
| `garden_id` | `BIGINT NOT NULL` | FK to `garden` |
| `date` | `DATE NOT NULL` | |
| `observation_type` | `VARCHAR(16) NOT NULL` | `ACTUAL` or `FORECAST` |
| `temp_min_c` | `DOUBLE PRECISION` | |
| `temp_max_c` | `DOUBLE PRECISION` | |
| `temp_mean_c` | `DOUBLE PRECISION` | |
| `precipitation_mm` | `DOUBLE PRECISION` | |
| `wind_max_ms` | `DOUBLE PRECISION` | |
| `humidity_pct` | `DOUBLE PRECISION` | |
| `fetched_at` | `TIMESTAMPTZ NOT NULL` | |

Unique on `(garden_id, date, observation_type)`. A new forecast for the same date overwrites the old one; an arriving `ACTUAL` sits beside the prior `FORECAST` so forecast-error studies are possible.

**`weather_alert`** — alert audit and dedup.

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT IDENTITY PK` | |
| `garden_id` | `BIGINT NOT NULL` | FK |
| `target_date` | `DATE NOT NULL` | Date the alert is about |
| `threshold_type` | `VARCHAR(32) NOT NULL` | `FROST`, `HEAT`, `HIGH_WIND`, `HEAVY_RAIN` |
| `threshold_value` | `DOUBLE PRECISION NOT NULL` | The tripping value (e.g. `-2.0`) |
| `sent_at` | `TIMESTAMPTZ NOT NULL` | |
| `fcm_message_id` | `VARCHAR(128)` | As returned by FCM |
| `user_id` | `BIGINT NOT NULL` | Recipient |

Unique on `(garden_id, target_date, threshold_type, user_id)` — prevents duplicate sends for the same prediction to the same user. One-alert-per-(garden, date, threshold, user) rule; no resends if the forecast value shifts.

**`gdd_accumulation`** — cached derived metric.

| Column | Type | Notes |
|---|---|---|
| `garden_id` | `BIGINT NOT NULL` | |
| `species_id` | `BIGINT NOT NULL` | |
| `season_id` | `BIGINT NOT NULL` | |
| `base_temp_c` | `DOUBLE PRECISION NOT NULL` | From species, cached for invalidation detection |
| `accumulated_gdd` | `DOUBLE PRECISION NOT NULL` | Sum of daily GDD over actuals |
| `as_of_date` | `DATE NOT NULL` | Last actual included |
| `updated_at` | `TIMESTAMPTZ NOT NULL` | |

Primary key `(garden_id, species_id, season_id)`. Deleted and regenerated when `species.gdd_base_temp_c` changes or when plant events shift the scope boundary.

**`user_device`** — FCM token registration.

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT IDENTITY PK` | |
| `user_id` | `BIGINT NOT NULL` | |
| `fcm_token` | `VARCHAR(512) NOT NULL UNIQUE` | |
| `platform` | `VARCHAR(16) NOT NULL` | `ANDROID` for now |
| `last_seen_at` | `TIMESTAMPTZ NOT NULL` | |

Multi-device supported per user.

**`user_alert_preference`** — per-user opt-out and tuning.

| Column | Type | Notes |
|---|---|---|
| `user_id` | `BIGINT NOT NULL` | |
| `threshold_type` | `VARCHAR(32) NOT NULL` | Same enum as `weather_alert.threshold_type` plus `IRRIGATION_SKIP_THRESHOLD` |
| `enabled` | `BOOLEAN NOT NULL DEFAULT TRUE` | |
| `custom_value` | `DOUBLE PRECISION` | Nullable — overrides default trigger value |

Primary key `(user_id, threshold_type)`. Absent row means "use default + enabled." `IRRIGATION_SKIP_THRESHOLD` is stored only in this table — it never produces a `weather_alert` row and is never evaluated by `AlertEvaluator`. It is consumed only by `TaskGenerationService` (see section 5).

### Column additions to existing tables

- `garden.weather_backfill_status` — `VARCHAR(16) NULL` (`RUNNING`, `DONE`, `FAILED`). Surfaces whether the 3-year backfill has completed.
- `species.gdd_base_temp_c` — `DOUBLE PRECISION NULL` (falls back to 10°C when null).
- `species.gdd_to_first_bloom` — `DOUBLE PRECISION NULL`.
- `species.gdd_to_harvest` — `DOUBLE PRECISION NULL`.

> **Delta from the flower-production plan:** that plan stored `gdd_base_temp_c` on `Season`. We move it to `Species` because the base temperature is a crop trait, not a per-year constant.

### Backend module layout

New code lives alongside the existing structure:

```
backend/src/main/kotlin/app/verdant/
  service/weather/
    SmhiClient.kt                    # HTTP client, fixtures-driven tests
    WeatherIngestionService.kt       # Scheduled jobs, backfill orchestration
    AlertEvaluator.kt                # Threshold rules, dedup, FCM dispatch
    GddService.kt                    # Accumulation, cache management
    FcmService.kt                    # Firebase Admin SDK wrapper
  resource/
    WeatherResource.kt
    UserDeviceResource.kt
    AlertPreferenceResource.kt
  repository/
    DailyWeatherRepository.kt
    WeatherAlertRepository.kt
    GddAccumulationRepository.kt
    UserDeviceRepository.kt
    UserAlertPreferenceRepository.kt
  dto/
    WeatherDtos.kt
```

### External dependencies

- **Backend:** `com.google.firebase:firebase-admin` (Maven). Credentials via Application Default Credentials on GCP, via `FIREBASE_CREDENTIALS_PATH` env var locally.
- **Android:** Firebase Messaging SDK, `google-services.json` (per-environment).
- **SMHI:** no auth. Forecast endpoint `https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point/lon/{lon}/lat/{lat}/data.json`. Historical archive at a separate host.

---

## 3. Ingestion pipeline

### Scheduled jobs (Quarkus `@Scheduled`)

1. **Forecast refresh — every 6 hours** (`cron = "0 0 */6 * * ?"`). For each `Garden` with `latitude` and `longitude` set, hit SMHI's forecast endpoint, parse the next 10 days, upsert as `observation_type = FORECAST`. After each garden completes, call `AlertEvaluator.evaluate(gardenId)`.
2. **Actuals refresh — daily at 2am** (`cron = "0 0 2 * * ?"`). For each garden, fetch yesterday's observation from SMHI's archive, upsert as `observation_type = ACTUAL`. After each garden, invalidate affected `gdd_accumulation` rows (lazy regeneration on next read).

### Execution policy

- Per-garden serial, 250ms delay between calls, to avoid hammering SMHI.
- On HTTP 429 or 5xx: log, skip this garden, the next run recovers. No retry queue.
- Gardens without coordinates: silently skipped. Web surface shows a banner prompting the user to set coordinates.

### Backfill

Triggered when a garden is created with coordinates, or when coordinates change. Pulls daily `ACTUAL` observations for the 3 years prior to the trigger date. Runs in a Quarkus managed executor (not the request thread). Progress visible via `garden.weather_backfill_status`.

A coordinate change of any magnitude triggers a full re-backfill — the data set is small (~1000 rows/garden for 3 years) and a partial/conditional backfill is not worth the code.

---

## 4. Alert subsystem

### Evaluation

`AlertEvaluator.evaluate(gardenId)` runs after each forecast refresh. For each future `FORECAST` row within the next 72 hours, it evaluates each threshold type.

### Built-in thresholds

| Threshold | Default trigger | Applicability |
|---|---|---|
| `FROST` | `temp_min_c ≤ 0` | Garden has at least one `plant` with status `PLANTED_OUT` |
| `HEAT` | `temp_max_c ≥ 30` | Garden has an active season |
| `HIGH_WIND` | `wind_max_ms ≥ 15` | Garden has an active season |
| `HEAVY_RAIN` | `precipitation_mm ≥ 30` | Garden has an active season |

Boundary behaviour is `≤` / `≥` — `temp_min_c = 0.0` triggers `FROST`.

Per-user overrides via `user_alert_preference`: users can disable a type or supply a `custom_value` (e.g. "alert me at +2°C, not 0°C").

### Dedup

Before sending: check `weather_alert` for a row with matching `(garden_id, target_date, threshold_type, user_id)`. If present, skip. One-alert-per-combination rule — no resends if the forecast value shifts.

### Recipient resolution

For each garden, find the owning `org`, then every user in that org with an active `user_device` entry. Send to every token. On FCM errors `UNREGISTERED` or `INVALID_ARGUMENT`, delete that row from `user_device`.

### Push payload

```json
{
  "notification": {
    "title": "Frost alert — Norra trädgården",
    "body": "Temperatures down to -2°C expected Thursday night"
  },
  "data": {
    "type": "weather_alert",
    "gardenId": "42",
    "alertType": "FROST",
    "targetDate": "2026-04-23"
  }
}
```

Android tap handler routes to `WeatherScreen` for the referenced garden.

### FcmService

Wraps the Firebase Admin SDK. Credentials via Application Default Credentials on GCP (matches existing Cloud Build deploy). Local dev uses a service account JSON pointed at by `FIREBASE_CREDENTIALS_PATH`. Send is async (`CompletableFuture`). Failures are logged but never fail the evaluator.

---

## 5. GDD and analytics

### GDD formula

Daily GDD = `max(0, temp_mean_c - base_temp_c)`.

- `temp_mean_c` uses `ACTUAL` when available, `FORECAST` otherwise — so accumulation extends through the forecast horizon.
- `base_temp_c` comes from `species.gdd_base_temp_c`; falls back to **10°C** when null (standard horticulture default).

### Accumulation scope

Per `(garden_id, species_id, season_id)`:
- **Start date**: earliest `SOWN` event for that triple.
- **End date**: earliest `LAST_BLOOM` / `LIFTED` / complete-harvest event if present, otherwise today.

If no plant events exist, the accumulation row does not exist — the GDD endpoint returns empty.

### Caching

`gdd_accumulation.accumulated_gdd` is a running total with `as_of_date`. After the daily actuals refresh, `GddService.refreshGarden(gardenId)` walks affected species and recomputes forward from `as_of_date`. Row is deleted and regenerated on next read when `species.gdd_base_temp_c` changes or plant events shift the scope boundary. Forecast-based extension (beyond `as_of_date`) is computed on read, not stored.

### Harvest date forecasting (use case A)

Endpoint: `GET /api/gdd/plant/{plantId}`. Returns, per live `plant`:

- `accumulated_gdd_today`
- `predicted_first_bloom_date`
- `predicted_harvest_date`
- `confidence`: `high` when species has GDD metadata AND accumulation crosses the threshold within cached actuals + forecast horizon; `forecast-limited` when metadata exists but the threshold isn't reached within the available window; `low` when falling back to `species.days_to_harvest` date arithmetic because no GDD metadata exists.

Walks forward through cached actuals + forecast, stops when accumulated GDD crosses `species.gdd_to_first_bloom` / `gdd_to_harvest`. If the threshold isn't reached within the available window, `predicted_*_date` falls back to `days_to_harvest` arithmetic from the sowing event, and `confidence` is set to `forecast-limited`. No climatology-based projection beyond the forecast horizon — if the user wants a distant projection, they read the `days_to_harvest` fallback.

### Analytics overlay (use case C)

Three endpoints on `AnalyticsResource`:

- `GET /api/analytics/weather/season/{seasonId}` — per-garden daily min/max/mean/precip/wind for the season, one series per garden.
- `GET /api/analytics/weather/species-overlay/{speciesId}?seasonId=X` — plant events + weekly harvest stems joined with weekly mean temp and precip totals.
- `GET /api/analytics/gdd/{plantId}` — GDD curve from sowing to today + projected forward.

### Irrigation signal (use case D)

`TaskGenerationService` gains an input: `recent_precipitation_mm` (sum of last 7 days of `ACTUAL` for the garden). When generating watering tasks: skip generation for the garden if the sum ≥ **15mm**. Threshold is configurable via `user_alert_preference` with `threshold_type = IRRIGATION_SKIP_THRESHOLD` and `custom_value` in millimetres.

---

## 6. User-facing surfaces

### Web

- `pages/Weather.tsx` — new top-level page at `/weather`. Day-by-day cards for the next 7 days, active alerts badges, 2-week GDD sparkline for the active season. Garden picker when the org has multiple.
- **Dashboard widget** — compact "weather at a glance" block on `Dashboard.tsx` showing today + next 3 days for the primary garden, linking to the full weather page. Always shown; hidden only when no garden has coordinates.
- `PlantDetail.tsx` — GDD progress strip under the event timeline: `accumulated / gdd_to_harvest`, projected harvest date, confidence. Rendered only when the species has GDD metadata.
- `Analytics.tsx` — new "Weather overlay" tab consuming `/api/analytics/weather/species-overlay/`. Chart: weekly stems overlaid on weekly mean temp + precip bars.
- `Account.tsx` — "Weather alerts" section with per-threshold toggle and custom-value inputs, backed by `user_alert_preference`.
- i18n keys under new `weather.*` namespace in both `sv.json` and `en.json`.

### Android

- `ui/weather/WeatherScreen.kt` — mobile-adapted weather page: vertical day list, garden picker, alert badges at top.
- `ui/weather/AlertPreferencesScreen.kt` — under the Account drawer; mirrors web.
- `PlantDetailScreen.kt` — same GDD progress strip as web.
- `push/VerdantMessagingService.kt` — `FirebaseMessagingService` subclass. One notification channel per threshold type (`Frost warnings`, `Heat warnings`, etc.) so users can mute individually via Android settings. Tap intents route to `WeatherScreen` for the referenced garden.
- FCM token registration: on app start after login, `POST /api/user-devices` with the current token. Re-register on token refresh.
- `POST_NOTIFICATIONS` permission on Android 13+: requested the first time the user opens `WeatherScreen` or enables a threshold, **not** on app launch.
- Drawer: new "Weather" entry with `Icons.Default.Cloud`.
- i18n: matching `weather_*` keys in `values/strings.xml` and `values-sv/strings.xml`.

### Endpoints summary

- `GET /api/weather/garden/{id}?days=N` — forecast + actuals.
- `GET /api/weather/garden/{id}/alerts` — active and recent alerts.
- `GET /api/gdd/plant/{plantId}` — accumulation curve + projected dates.
- `GET /api/analytics/weather/season/{seasonId}`
- `GET /api/analytics/weather/species-overlay/{speciesId}`
- `GET /api/analytics/gdd/{plantId}`
- `POST /api/user-devices` — register FCM token.
- `DELETE /api/user-devices/{token}` — deregister.
- `GET /api/alert-preferences` — list user's preferences.
- `PUT /api/alert-preferences/{type}` — update one.

---

## 7. Phasing

Six milestones, each shippable independently.

### M1 — Ingestion foundation (no user-visible change)

- Migration `V15__weather.sql`: `daily_weather`, `garden.weather_backfill_status`.
- `SmhiClient` with fixtures + tests.
- `WeatherIngestionService` with the two scheduled jobs.
- Backfill on garden create and on coordinate change.
- Repository layer + read-only `GET /api/weather/garden/{id}` so data can be eyeballed during development.

### M2 — FCM infrastructure (plumbing only, no alerts yet)

- Migration `V16__user_device.sql`: `user_device`.
- Firebase Admin SDK dependency, service account wiring.
- `FcmService` + `UserDeviceResource` + `POST /api/user-devices`.
- Android: Firebase SDK, `google-services.json`, `VerdantMessagingService`, token registration on login.
- Dev-only endpoint `POST /api/dev/test-push` for end-to-end verification before wiring alerts.

### M3 — Frost alerts (first consumer of use case B)

- Migration `V17__weather_alerts.sql`: `weather_alert`, `user_alert_preference`.
- `AlertEvaluator` with dedup.
- Frost threshold wired; heat/wind/rain scaffolded but disabled by default.
- Android: notification channel "Weather alerts", tap handler routing to `WeatherScreen`.
- Web + Android `WeatherScreen` and `AlertPreferencesScreen`.
- End-to-end test: seeded frost forecast → alert row → FCM send → Android notification.

### M4 — Remaining alerts + GDD (use case A)

- Enable heat/wind/rain thresholds behind per-user preferences.
- Migration `V18__gdd.sql`: `species.gdd_base_temp_c`, `species.gdd_to_first_bloom`, `species.gdd_to_harvest`, `gdd_accumulation`.
- `GddService` + `GET /api/gdd/plant/{id}`.
- Web `PlantDetail.tsx` + Android `PlantDetailScreen.kt` get the GDD strip.
- Admin: species form exposes GDD metadata fields.

### M5 — Weather-overlay analytics (use case C)

- Three new analytics endpoints.
- Web `Analytics.tsx` weather-overlay tab.
- Android analytics screen gains a weather overlay section.

### M6 — Irrigation signal (use case D)

- `TaskGenerationService` gains the recent-precipitation input.
- Skip rule wired, default 15mm threshold.
- `IRRIGATION_SKIP_THRESHOLD` exposed in the `AlertPreferences` UI.

**Rough sizing.** M1 largest (SMHI client + ingestion + tests). M2–M3 comparable together (FCM plumbing is fiddly but one-time). M4–M6 progressively smaller. Estimate ~2–3 solo-dev weeks for the full arc. M1+M3 together already deliver a real product experience (frost alerts for plants in the ground).

---

## 8. Testing strategy

### Backend

- **SMHI client.** Fixtures in `backend/src/test/resources/smhi/`. `SmhiClient` takes a base URL; tests point at `MockWebServer`. Covers happy path, HTTP 429, malformed JSON, point-outside-Sweden.
- **Ingestion.** `@QuarkusTest` against the existing Postgres test container. Asserts upsert semantics (forecast overwrite, actual beside forecast), backfill covers exactly 3 years, cron skips gardens without coordinates.
- **AlertEvaluator.** Pure unit test with a fake `FcmService` that records sends. Covers boundary (`temp_min_c = 0.0` triggers `FROST`), dedup, custom overrides, token cleanup on `UNREGISTERED`.
- **GddService.** Unit test with synthetic `daily_weather` fixtures. Boundary (`mean = base` → 0), gaps in actuals, forecast-extension, recompute when actual lands.
- **FcmService.** Interface-level only; real Firebase calls are mocked.

### Web

- Component tests for `Weather.tsx`, Dashboard widget, `PlantDetail` GDD strip, `AlertPreferences`. `msw` mocks for new endpoints.
- E2E smoke in `web/e2e/secondary-flows.spec.ts`: weather page renders, alert preferences persist.

### Android

- ViewModel unit tests for `WeatherViewModel` and `AlertPreferencesViewModel` with a fake `GardenRepository`.
- `VerdantMessagingService` — intent-building tested in isolation; real FCM path exercised manually via M2's `/api/dev/test-push`.

### Manual verification milestones

- **M1.** Hit `GET /api/weather/garden/{id}` after a cron run; eyeball rows in Postgres.
- **M2.** `POST /api/dev/test-push` → confirm device receives a notification.
- **M3.** Seed `daily_weather` row with `temp_min_c = -2` for tomorrow → trigger eval → confirm push.

### Explicitly out of scope

- SMHI rate-limit behaviour under sustained load.
- Multi-org cross-contamination (covered by existing org tests).
- iOS — no client exists.

---

## 9. Open questions

None as of 2026-04-18 — all scoping decisions recorded above.
