# Garden Areas — Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring garden areas ("Platser") and recurring maintenance rules to the Android app, against the backend API as it actually shipped.

**Architecture:** Areas get a detail screen and a create screen modelled on the existing bed screens, listed under their garden. The maintenance-rule logic lives in ONE plain-Kotlin controller that both the area and bed detail ViewModels hold, with a single stateless Compose section rendering it — that shared pair is what makes "beds get rules too" nearly free, and it is unit-testable without Compose or Android.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Retrofit + Gson, Coroutines/Flow. Tests: JUnit 4 + kotlinx-coroutines-test + Turbine.

**Spec:** `docs/plans/2026-08-26-garden-areas-design.md`
**Backend plan (shipped):** `docs/plans/2026-08-26-garden-areas-backend-plan.md`
**Web plan (sibling client):** `docs/plans/2026-08-26-garden-areas-web-plan.md`

## Amendment — 2026-08-27, after the backend and web halves shipped

This plan was written before the web client was executed and before a cross-stack
review of the shipped backend + web. Three things changed. Read this before Task 8.

**1. The backend contract moved. `anchorDate` is now honoured.**
When this plan was written, `anchor_date` was persisted and read by nothing — a
rule's due date derived only from the event log, so a rule created with "last done
three weeks ago" fired immediately. That is fixed:
`MaintenanceDueCalculator.effectiveLastDone` now takes the LATER of the resolved
event date and `anchorDate`, and both due-computing sites use it. Consequently
`MaintenanceRuleResponse.lastDoneDate` now reports that combined value, not the raw
event date. No model change is needed — the field already exists — but the Android
rule editor's "last done" field genuinely delays a rule now, so its hint copy should
say so rather than hedging.

**2. The web client had a Critical bug that Android does NOT have. Do not "fix" it here.**
On web, the task drawer navigated to the place instead of completing the task, so
rule-backed tasks never left `PENDING`, and `findActiveWithoutOpenTask`'s
`NOT EXISTS (… status = 'PENDING')` locked that rule out forever — the engine fired
once per rule. Android is already correct, verified against the current code:

- `ui/task/TaskListScreen.kt:240` puts `onCompleteToggle` on **every** `TaskRow`,
  ungated by task type, so an area task can already be completed.
- `ui/task/TaskListScreen.kt:108-120` `completeTask` sends
  `speciesId = if (task.bedId != null) null else task.speciesId`. For an area task
  `speciesId` is null anyway, and the server's place-scoped branch
  (`ScheduledTaskService.completePartially`) skips species validation for
  `bedId != null || gardenAreaId != null`. So it already works.
- `ui/task/TaskListScreen.kt:149,164` already buckets **overdue** — web had no
  overdue bucket at all, which is what made its bug unrecoverable.
- It already floors `processedCount` with `coerceAtLeast(1)`.

**Do not add a second completion path.** Task 8's job on this point is to verify the
existing one behaves correctly for area-scoped tasks, not to build one.

**3. What Task 8 must still do**, unchanged from the original: render area tasks with
a readable title rather than a raw enum, guard the edits the server rejects on
rule-backed tasks, and keep the delete copy honest about the task reappearing.

## Global Constraints

- **Write against the API as shipped, not as the design doc describes it.** The shapes below were read out of the merged backend.
- **There is no mocking library in this module.** `app/build.gradle.kts` test deps are exactly `junit:junit:4.13.2`, `kotlinx-coroutines-test`, and `app.cash.turbine`. Tests use **hand-written fakes** — see `app/src/test/kotlin/app/verdant/android/ui/task/TaskListViewModelTest.kt` and its `FakeTaskRepository`. Do not add Mockito or MockK.
- **Consequence for design:** anything a unit test needs to fake must be an **interface**. `TaskRepository` (interface) + `DefaultTaskRepository` (impl) + a `@Binds` in `di/RepositoryModule.kt` is the established pattern, and this plan follows it for the two new repositories. Concrete `@Singleton class X @Inject constructor(...)` repositories like `BedRepository` need no module entry but also cannot be faked.
- Models are Gson data classes with `@SerializedName` on **every** field — see `data/model/GardenModels.kt`. A missing annotation silently deserializes to null when the field name survives minification.
- API calls are `suspend` functions on `data/api/VerdantApi.kt`. Repositories wrap them; ViewModels call repositories, never the API directly.
- ViewModels are `@HiltViewModel` with `@Inject constructor`, route args via `SavedStateHandle`, state as `MutableStateFlow` exposed through `asStateFlow()`. Follow `ui/bed/BedDetailViewModel.kt`, including its sealed `UiState` with `Loading` / `Error` / `Loaded` and the "once loaded, stay loaded on refresh failure" behaviour.
- **Strings live in BOTH `app/src/main/res/values/strings.xml` (default/English) and `app/src/main/res/values-sv/strings.xml` (Swedish), edited in the same commit.** Note they are already slightly out of step — 459 vs 457 entries — so do not treat equal counts as your check; verify each key you add exists in both files by name.
- The UI label for a `GardenArea` is **"Plats" / "Platser"**. Never `Location` — `ui/location/` already holds `TrayLocationsScreen`, a different concept (where portable trays sit). Put area screens in a new `ui/area/` package.
- **No map or polygon work.** `boundaryJson` round-trips through the API untouched. Do not extend `CreateGardenScreen`'s map wizard, and do not add area polygons to it.
- Photos use `Bitmap.toCompressedBase64(maxSize = 800)` from `ui/activity/PhotoPicker.kt`, exactly as `ui/bed/BedPhotosSection.kt` does.

### The shipped API

```
GET  POST        /api/gardens/{gardenId}/areas
GET  PUT  DELETE /api/areas/{id}
GET  POST        /api/areas/{id}/events        ?limit= on GET
GET  POST        /api/areas/{id}/photos
DELETE           /api/areas/{id}/photos/{photoId}

GET              /api/maintenance-rules?bedId=&areaId=    (at most one; both = 400)
POST             /api/maintenance-rules
PUT  DELETE      /api/maintenance-rules/{id}
```

Field lists, verbatim from the merged backend:

- `GardenAreaResponse`: `id, gardenId, gardenName, name, description, category, boundaryJson, sizeSqm, createdAt, updatedAt`
- `GardenAreaEventResponse`: `id, gardenAreaId, eventType, eventDate, notes, createdAt`
- `GardenAreaPhotoResponse`: `id, gardenAreaId, photoUrl, reason, description, capturedAt, createdAt`
- `MaintenanceRuleResponse`: `id, bedId, bedName, gardenAreaId, gardenAreaName, activityType, intervalDays, anchorDate, seasonStartMonth, seasonStartDay, seasonEndMonth, seasonEndDay, active, notes, lastDoneDate, nextDueDate, createdAt, updatedAt`
- `ScheduledTaskResponse` gains `gardenAreaId, gardenAreaName, maintenanceRuleId`
- `CreateGardenAreaRequest`: `name`, `category` required; `description`, `boundaryJson`, `sizeSqm` optional
- `UpdateGardenAreaRequest`: all optional — **omitted fields keep their value**
- `CreateGardenAreaEventRequest`: `activityType` required; `eventDate`, `notes` optional
- `CreateGardenAreaPhotoRequest`: `imageBase64`, `reason` required; `description`, `capturedAt` optional
- `CreateMaintenanceRuleRequest`: exactly one of `bedId`/`gardenAreaId`; `activityType`, `intervalDays` (≥1); optional `anchorDate`, four `season*`, `notes`
- `UpdateMaintenanceRuleRequest`: all optional, plus **`clearSeasonWindow: Boolean`**

### Three server behaviours the UI must respect

1. **`clearSeasonWindow` is the only way to remove a season window.** Sending nulls does nothing — the server cannot distinguish omitted from null. Sending it *alongside* any `season*` value is a **400**.
2. **Rule-backed tasks reject `activityType` and `targetCount` edits** (400 when `maintenanceRuleId != null`). The activity belongs to the rule; the count is always 1.
3. **Deleting a pending maintenance task does not dismiss it.** The scheduler recreates it next morning because the work is still undone. Never label it "dismiss", "skip", or "klar för nu" — pausing the rule is the way to stop it.

### Enums

`GardenAreaCategory`: `WALKWAY, LAWN, HEDGE, COMPOST, GREENHOUSE, WATER_FEATURE, STRUCTURE, OTHER`

| Activity | Beds | Areas |
|---|---|---|
| `WATER` | yes | yes |
| `WEED` | yes | yes |
| `FERTILIZE` | yes | no |
| `MOW`, `RAKE`, `PRUNE`, `EDGE`, `SWEEP`, `TOP_UP`, `CLEAN`, `INSPECT` | no | yes |

Area event types are those activity names plus `NOTE`.

### Running tests

```bash
cd android && ./gradlew :app:testDebugUnitTest
cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.ui.area.*"
```

The Android SDK must be present (the devcontainer ships platform-35). If the build fails on a missing SDK or `local.properties`, report BLOCKED rather than editing `local.properties` — it is host-specific and deliberately not clobbered.

---

### Task 1: Models, API endpoints, and repositories

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/data/model/GardenAreaModels.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/data/model/MaintenanceModels.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/data/repository/GardenAreaRepository.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/data/repository/MaintenanceRuleRepository.kt`
- Modify: `android/app/src/main/kotlin/app/verdant/android/data/api/VerdantApi.kt`
- Modify: `android/app/src/main/kotlin/app/verdant/android/data/model/TaskModels.kt` (three new `ScheduledTaskResponse` fields)
- Modify: `android/app/src/main/kotlin/app/verdant/android/di/RepositoryModule.kt`
- Test: `android/app/src/test/kotlin/app/verdant/android/data/model/GardenAreaModelsTest.kt`

**Interfaces:**
- Produces: `GardenAreaResponse`, `CreateGardenAreaRequest`, `UpdateGardenAreaRequest`, `GardenAreaEventResponse`, `CreateGardenAreaEventRequest`, `GardenAreaPhotoResponse`, `CreateGardenAreaPhotoRequest`, `MaintenanceRuleResponse`, `CreateMaintenanceRuleRequest`, `UpdateMaintenanceRuleRequest`; interfaces `GardenAreaRepository` and `MaintenanceRuleRepository` with `Default*` implementations bound in Hilt.

- [ ] **Step 1: Write the failing test**

Gson deserialization is where a missing `@SerializedName` bites, so pin it. Create `android/app/src/test/kotlin/app/verdant/android/data/model/GardenAreaModelsTest.kt`:

```kotlin
package app.verdant.android.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenAreaModelsTest {

    private val gson = Gson()

    @Test
    fun `GardenAreaResponse deserializes every field the backend sends`() {
        val json = """
            {"id":5,"gardenId":1,"gardenName":"Trädgården","name":"Grusgången",
             "description":"Vid växthuset","category":"WALKWAY",
             "boundaryJson":null,"sizeSqm":12.5,
             "createdAt":"2026-06-01T10:00:00Z","updatedAt":"2026-06-01T10:00:00Z"}
        """.trimIndent()

        val area = gson.fromJson(json, GardenAreaResponse::class.java)

        assertEquals(5L, area.id)
        assertEquals(1L, area.gardenId)
        assertEquals("Trädgården", area.gardenName)
        assertEquals("Grusgången", area.name)
        assertEquals("Vid växthuset", area.description)
        assertEquals("WALKWAY", area.category)
        assertNull(area.boundaryJson)
        assertEquals(12.5, area.sizeSqm!!, 0.001)
    }

    @Test
    fun `MaintenanceRuleResponse deserializes the derived dates and a null season window`() {
        val json = """
            {"id":7,"bedId":null,"bedName":null,"gardenAreaId":5,"gardenAreaName":"Grusgången",
             "activityType":"WEED","intervalDays":21,"anchorDate":null,
             "seasonStartMonth":null,"seasonStartDay":null,
             "seasonEndMonth":null,"seasonEndDay":null,
             "active":true,"notes":null,
             "lastDoneDate":null,"nextDueDate":"2026-06-25",
             "createdAt":"2026-06-01T10:00:00Z","updatedAt":"2026-06-01T10:00:00Z"}
        """.trimIndent()

        val rule = gson.fromJson(json, MaintenanceRuleResponse::class.java)

        assertNull(rule.bedId)
        assertEquals(5L, rule.gardenAreaId)
        assertEquals("WEED", rule.activityType)
        assertEquals(21, rule.intervalDays)
        assertNull(rule.lastDoneDate)
        assertEquals("2026-06-25", rule.nextDueDate)
        assertTrue(rule.active)
        assertNull(rule.seasonStartMonth)
    }

    @Test
    fun `MaintenanceRuleResponse deserializes a wrap-around season window`() {
        val json = """
            {"id":8,"bedId":3,"bedName":"Bädd 1","gardenAreaId":null,"gardenAreaName":null,
             "activityType":"WATER","intervalDays":7,"anchorDate":"2026-05-01",
             "seasonStartMonth":11,"seasonStartDay":1,
             "seasonEndMonth":3,"seasonEndDay":31,
             "active":false,"notes":"Vintertid",
             "lastDoneDate":"2026-05-20","nextDueDate":"2026-11-01",
             "createdAt":"2026-06-01T10:00:00Z","updatedAt":"2026-06-01T10:00:00Z"}
        """.trimIndent()

        val rule = gson.fromJson(json, MaintenanceRuleResponse::class.java)

        assertEquals(3L, rule.bedId)
        assertEquals(11, rule.seasonStartMonth)
        assertEquals(31, rule.seasonEndDay)
        assertEquals("2026-05-20", rule.lastDoneDate)
        assertEquals(false, rule.active)
    }

    @Test
    fun `ScheduledTaskResponse carries the new area and rule fields`() {
        val json = """
            {"id":42,"speciesId":null,"speciesName":null,
             "bedId":null,"bedName":null,"gardenName":"Trädgården",
             "gardenAreaId":5,"gardenAreaName":"Grusgången","maintenanceRuleId":7,
             "activityType":"WEED","earliestDate":"2026-06-25","deadline":"2026-06-25",
             "targetCount":1,"remainingCount":1,"status":"PENDING","notes":null,
             "seasonId":null,"successionScheduleId":null,
             "originGroupId":null,"originGroupName":null,"acceptableSpecies":[],
             "createdAt":"2026-06-25T03:30:00Z","updatedAt":"2026-06-25T03:30:00Z"}
        """.trimIndent()

        val task = gson.fromJson(json, ScheduledTaskResponse::class.java)

        assertEquals(5L, task.gardenAreaId)
        assertEquals("Grusgången", task.gardenAreaName)
        assertEquals(7L, task.maintenanceRuleId)
    }

    @Test
    fun `CreateGardenAreaPhotoRequest serializes imageBase64, never a url`() {
        val json = gson.toJson(CreateGardenAreaPhotoRequest(imageBase64 = "QUJD", reason = "PROGRESS"))
        assertTrue(json.contains("imageBase64"))
        // The API mints the storage path itself; a client-supplied URL was a
        // cross-tenant delete hole and no longer exists in the contract.
        assertTrue(!json.contains("photoUrl"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.data.model.GardenAreaModelsTest"`
Expected: FAIL — unresolved reference `GardenAreaResponse`.

- [ ] **Step 3: Write the models**

Create `data/model/GardenAreaModels.kt`:

```kotlin
package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class GardenAreaResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("gardenId") val gardenId: Long,
    @SerializedName("gardenName") val gardenName: String?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("category") val category: String,
    @SerializedName("boundaryJson") val boundaryJson: String?,
    @SerializedName("sizeSqm") val sizeSqm: Double?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class CreateGardenAreaRequest(
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null,
    @SerializedName("sizeSqm") val sizeSqm: Double? = null,
)

/** Omitted fields keep their current server-side value. */
data class UpdateGardenAreaRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("boundaryJson") val boundaryJson: String? = null,
    @SerializedName("sizeSqm") val sizeSqm: Double? = null,
)

data class GardenAreaEventResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("gardenAreaId") val gardenAreaId: Long,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("eventDate") val eventDate: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class CreateGardenAreaEventRequest(
    @SerializedName("activityType") val activityType: String,
    @SerializedName("eventDate") val eventDate: String? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class GardenAreaPhotoResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("gardenAreaId") val gardenAreaId: Long,
    @SerializedName("photoUrl") val photoUrl: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("description") val description: String?,
    @SerializedName("capturedAt") val capturedAt: String,
    @SerializedName("createdAt") val createdAt: String,
)

/**
 * Raw image bytes. The server mints the storage path and public URL — the
 * contract deliberately has no client-supplied URL field, because one would
 * let a caller aim a later delete at another org's blob.
 */
data class CreateGardenAreaPhotoRequest(
    @SerializedName("imageBase64") val imageBase64: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("capturedAt") val capturedAt: String? = null,
)
```

Create `data/model/MaintenanceModels.kt`:

```kotlin
package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class MaintenanceRuleResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("bedId") val bedId: Long?,
    @SerializedName("bedName") val bedName: String?,
    @SerializedName("gardenAreaId") val gardenAreaId: Long?,
    @SerializedName("gardenAreaName") val gardenAreaName: String?,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("intervalDays") val intervalDays: Int,
    @SerializedName("anchorDate") val anchorDate: String?,
    @SerializedName("seasonStartMonth") val seasonStartMonth: Int?,
    @SerializedName("seasonStartDay") val seasonStartDay: Int?,
    @SerializedName("seasonEndMonth") val seasonEndMonth: Int?,
    @SerializedName("seasonEndDay") val seasonEndDay: Int?,
    @SerializedName("active") val active: Boolean,
    @SerializedName("notes") val notes: String?,
    /** Derived server-side from the event log. Null when never done. */
    @SerializedName("lastDoneDate") val lastDoneDate: String?,
    /** Derived server-side: when the next task will be created. */
    @SerializedName("nextDueDate") val nextDueDate: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

/** Exactly one of [bedId] / [gardenAreaId] — supplying both or neither is a 400. */
data class CreateMaintenanceRuleRequest(
    @SerializedName("bedId") val bedId: Long? = null,
    @SerializedName("gardenAreaId") val gardenAreaId: Long? = null,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("intervalDays") val intervalDays: Int,
    @SerializedName("anchorDate") val anchorDate: String? = null,
    @SerializedName("seasonStartMonth") val seasonStartMonth: Int? = null,
    @SerializedName("seasonStartDay") val seasonStartDay: Int? = null,
    @SerializedName("seasonEndMonth") val seasonEndMonth: Int? = null,
    @SerializedName("seasonEndDay") val seasonEndDay: Int? = null,
    @SerializedName("notes") val notes: String? = null,
)

data class UpdateMaintenanceRuleRequest(
    @SerializedName("activityType") val activityType: String? = null,
    @SerializedName("intervalDays") val intervalDays: Int? = null,
    @SerializedName("anchorDate") val anchorDate: String? = null,
    @SerializedName("seasonStartMonth") val seasonStartMonth: Int? = null,
    @SerializedName("seasonStartDay") val seasonStartDay: Int? = null,
    @SerializedName("seasonEndMonth") val seasonEndMonth: Int? = null,
    @SerializedName("seasonEndDay") val seasonEndDay: Int? = null,
    /**
     * The ONLY way to remove a season window — nulls are indistinguishable
     * from omitted fields server-side. Cannot be combined with any season*
     * value; doing so is a 400.
     */
    @SerializedName("clearSeasonWindow") val clearSeasonWindow: Boolean = false,
    @SerializedName("active") val active: Boolean? = null,
    @SerializedName("notes") val notes: String? = null,
)
```

- [ ] **Step 4: Extend `ScheduledTaskResponse`**

In `data/model/TaskModels.kt`, add to `ScheduledTaskResponse`, positioned after `gardenName` to match the server's field order:

```kotlin
    @SerializedName("gardenAreaId") val gardenAreaId: Long? = null,
    @SerializedName("gardenAreaName") val gardenAreaName: String? = null,
    @SerializedName("maintenanceRuleId") val maintenanceRuleId: Long? = null,
```

Defaults keep every existing construction site compiling.

- [ ] **Step 5: Add the API endpoints**

In `data/api/VerdantApi.kt`, after the bed photo endpoints:

```kotlin
    @GET("api/gardens/{gardenId}/areas")
    suspend fun getGardenAreas(@Path("gardenId") gardenId: Long): List<GardenAreaResponse>

    @POST("api/gardens/{gardenId}/areas")
    suspend fun createGardenArea(
        @Path("gardenId") gardenId: Long,
        @Body request: CreateGardenAreaRequest,
    ): GardenAreaResponse

    @GET("api/areas/{id}")
    suspend fun getGardenArea(@Path("id") id: Long): GardenAreaResponse

    @PUT("api/areas/{id}")
    suspend fun updateGardenArea(
        @Path("id") id: Long,
        @Body request: UpdateGardenAreaRequest,
    ): GardenAreaResponse

    @DELETE("api/areas/{id}")
    suspend fun deleteGardenArea(@Path("id") id: Long)

    @GET("api/areas/{id}/events")
    suspend fun getGardenAreaEvents(
        @Path("id") id: Long,
        @Query("limit") limit: Int = 50,
    ): List<GardenAreaEventResponse>

    @POST("api/areas/{id}/events")
    suspend fun logGardenAreaEvent(
        @Path("id") id: Long,
        @Body request: CreateGardenAreaEventRequest,
    ): GardenAreaEventResponse

    @GET("api/areas/{id}/photos")
    suspend fun getGardenAreaPhotos(@Path("id") id: Long): List<GardenAreaPhotoResponse>

    @POST("api/areas/{id}/photos")
    suspend fun addGardenAreaPhoto(
        @Path("id") id: Long,
        @Body request: CreateGardenAreaPhotoRequest,
    ): GardenAreaPhotoResponse

    @DELETE("api/areas/{id}/photos/{photoId}")
    suspend fun deleteGardenAreaPhoto(
        @Path("id") id: Long,
        @Path("photoId") photoId: Long,
    )

    // At most one filter — supplying both is a 400.
    @GET("api/maintenance-rules")
    suspend fun getMaintenanceRules(
        @Query("bedId") bedId: Long? = null,
        @Query("areaId") areaId: Long? = null,
    ): List<MaintenanceRuleResponse>

    @POST("api/maintenance-rules")
    suspend fun createMaintenanceRule(
        @Body request: CreateMaintenanceRuleRequest,
    ): MaintenanceRuleResponse

    @PUT("api/maintenance-rules/{id}")
    suspend fun updateMaintenanceRule(
        @Path("id") id: Long,
        @Body request: UpdateMaintenanceRuleRequest,
    ): MaintenanceRuleResponse

    @DELETE("api/maintenance-rules/{id}")
    suspend fun deleteMaintenanceRule(@Path("id") id: Long)
```

Add the matching imports.

- [ ] **Step 6: Write the repositories as interfaces**

Both are interfaces with `Default*` implementations, because Tasks 3 and 4 unit-test ViewModels that depend on them and this module has no mocking library.

`data/repository/GardenAreaRepository.kt`:

```kotlin
package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateGardenAreaEventRequest
import app.verdant.android.data.model.CreateGardenAreaPhotoRequest
import app.verdant.android.data.model.CreateGardenAreaRequest
import app.verdant.android.data.model.GardenAreaEventResponse
import app.verdant.android.data.model.GardenAreaPhotoResponse
import app.verdant.android.data.model.GardenAreaResponse
import app.verdant.android.data.model.UpdateGardenAreaRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Garden area CRUD, its event log, and its photos.
 *
 * An interface rather than a concrete class so ViewModel tests can supply a
 * hand-written fake — this module has no mocking library.
 */
interface GardenAreaRepository {
    suspend fun list(gardenId: Long): List<GardenAreaResponse>
    suspend fun get(id: Long): GardenAreaResponse
    suspend fun create(gardenId: Long, request: CreateGardenAreaRequest): GardenAreaResponse
    suspend fun update(id: Long, request: UpdateGardenAreaRequest): GardenAreaResponse
    suspend fun delete(id: Long)
    suspend fun events(id: Long, limit: Int = 50): List<GardenAreaEventResponse>
    suspend fun logEvent(id: Long, request: CreateGardenAreaEventRequest): GardenAreaEventResponse
    suspend fun photos(id: Long): List<GardenAreaPhotoResponse>
    suspend fun addPhoto(id: Long, request: CreateGardenAreaPhotoRequest): GardenAreaPhotoResponse
    suspend fun deletePhoto(id: Long, photoId: Long)
}

@Singleton
class DefaultGardenAreaRepository @Inject constructor(
    private val api: VerdantApi,
) : GardenAreaRepository {
    override suspend fun list(gardenId: Long) = api.getGardenAreas(gardenId)
    override suspend fun get(id: Long) = api.getGardenArea(id)
    override suspend fun create(gardenId: Long, request: CreateGardenAreaRequest) =
        api.createGardenArea(gardenId, request)
    override suspend fun update(id: Long, request: UpdateGardenAreaRequest) =
        api.updateGardenArea(id, request)
    override suspend fun delete(id: Long) = api.deleteGardenArea(id)
    override suspend fun events(id: Long, limit: Int) = api.getGardenAreaEvents(id, limit)
    override suspend fun logEvent(id: Long, request: CreateGardenAreaEventRequest) =
        api.logGardenAreaEvent(id, request)
    override suspend fun photos(id: Long) = api.getGardenAreaPhotos(id)
    override suspend fun addPhoto(id: Long, request: CreateGardenAreaPhotoRequest) =
        api.addGardenAreaPhoto(id, request)
    override suspend fun deletePhoto(id: Long, photoId: Long) =
        api.deleteGardenAreaPhoto(id, photoId)
}
```

`data/repository/MaintenanceRuleRepository.kt`, same shape:

```kotlin
package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateMaintenanceRuleRequest
import app.verdant.android.data.model.MaintenanceRuleResponse
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import javax.inject.Inject
import javax.inject.Singleton

interface MaintenanceRuleRepository {
    /** Pass exactly one of [bedId] / [areaId], or neither for the whole org. */
    suspend fun list(bedId: Long? = null, areaId: Long? = null): List<MaintenanceRuleResponse>
    suspend fun create(request: CreateMaintenanceRuleRequest): MaintenanceRuleResponse
    suspend fun update(id: Long, request: UpdateMaintenanceRuleRequest): MaintenanceRuleResponse
    suspend fun delete(id: Long)
}

@Singleton
class DefaultMaintenanceRuleRepository @Inject constructor(
    private val api: VerdantApi,
) : MaintenanceRuleRepository {
    override suspend fun list(bedId: Long?, areaId: Long?) = api.getMaintenanceRules(bedId, areaId)
    override suspend fun create(request: CreateMaintenanceRuleRequest) =
        api.createMaintenanceRule(request)
    override suspend fun update(id: Long, request: UpdateMaintenanceRuleRequest) =
        api.updateMaintenanceRule(id, request)
    override suspend fun delete(id: Long) = api.deleteMaintenanceRule(id)
}
```

- [ ] **Step 7: Bind them in Hilt**

In `di/RepositoryModule.kt`, alongside the existing `TaskRepository` / `OrgRepository` bindings, add:

```kotlin
    @Binds
    @Singleton
    abstract fun bindGardenAreaRepository(impl: DefaultGardenAreaRepository): GardenAreaRepository

    @Binds
    @Singleton
    abstract fun bindMaintenanceRuleRepository(
        impl: DefaultMaintenanceRuleRepository,
    ): MaintenanceRuleRepository
```

Match the module's existing style — if its bindings are `@Provides` rather than `@Binds`, follow whichever is actually there.

- [ ] **Step 8: Run test to verify it passes**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.data.model.GardenAreaModelsTest"`
Expected: PASS, 5 tests.

- [ ] **Step 9: Compile the whole module**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. This is what catches a Hilt wiring mistake — DI errors surface at annotation-processing time, not in unit tests.

- [ ] **Step 10: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/data \
        android/app/src/main/kotlin/app/verdant/android/di/RepositoryModule.kt \
        android/app/src/test/kotlin/app/verdant/android/data/model/GardenAreaModelsTest.kt
git commit -m "feat(android): garden area and maintenance rule models, API, repositories"
```

---

### Task 2: Presentation helpers

Pure Kotlin, no Android imports, no coroutines. Mirrors the existing `data/model/SpeciesSorting.kt` + `EnumsTest.kt` precedent.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/data/model/MaintenanceFormatting.kt`
- Test: `android/app/src/test/kotlin/app/verdant/android/data/model/MaintenanceFormattingTest.kt`

**Interfaces:**
- Produces: `MAINTENANCE_ACTIVITIES`, `MaintenanceTarget`, `activitiesForTarget(target)`, `AREA_CATEGORIES`, `DueState`, `dueState(rule, todayIso)`, `hasSeasonWindow(rule)`, `seasonWindowMonthDays(rule)`.

**Note on strings:** labels are NOT here. Android localises through `strings.xml`, so this file returns keys/enums and the Compose layer resolves them with `stringResource`. Do not hardcode Swedish here — that is the web plan's approach, not this one.

- [ ] **Step 1: Write the failing test**

```kotlin
package app.verdant.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceFormattingTest {

    private fun rule(
        activityType: String = "WEED",
        intervalDays: Int = 21,
        active: Boolean = true,
        nextDueDate: String = "2026-06-25",
        seasonStartMonth: Int? = null,
        seasonStartDay: Int? = null,
        seasonEndMonth: Int? = null,
        seasonEndDay: Int? = null,
    ) = MaintenanceRuleResponse(
        id = 1, bedId = null, bedName = null,
        gardenAreaId = 5, gardenAreaName = "Gången",
        activityType = activityType, intervalDays = intervalDays, anchorDate = null,
        seasonStartMonth = seasonStartMonth, seasonStartDay = seasonStartDay,
        seasonEndMonth = seasonEndMonth, seasonEndDay = seasonEndDay,
        active = active, notes = null,
        lastDoneDate = null, nextDueDate = nextDueDate,
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `beds accept exactly the three activities the backend allows`() {
        assertEquals(
            listOf("WATER", "WEED", "FERTILIZE"),
            activitiesForTarget(MaintenanceTarget.BED),
        )
    }

    @Test
    fun `areas exclude FERTILIZE and include the area-only work`() {
        val area = activitiesForTarget(MaintenanceTarget.AREA)
        assertFalse(area.contains("FERTILIZE"))
        assertTrue(area.contains("MOW"))
        assertTrue(area.contains("WEED"))
    }

    @Test
    fun `the two targets together cover every activity`() {
        val union = (activitiesForTarget(MaintenanceTarget.BED) +
            activitiesForTarget(MaintenanceTarget.AREA)).toSet()
        assertEquals(MAINTENANCE_ACTIVITIES.toSet(), union)
    }

    @Test
    fun `AREA_CATEGORIES matches the backend enum`() {
        assertEquals(
            listOf(
                "WALKWAY", "LAWN", "HEDGE", "COMPOST",
                "GREENHOUSE", "WATER_FEATURE", "STRUCTURE", "OTHER",
            ),
            AREA_CATEGORIES,
        )
    }

    @Test
    fun `an overdue rule reports how many days late`() {
        val state = dueState(rule(nextDueDate = "2026-06-20"), "2026-06-25")
        assertTrue(state is DueState.Overdue)
        assertEquals(5, (state as DueState.Overdue).days)
    }

    @Test
    fun `a rule due today is Due, not Overdue`() {
        assertTrue(dueState(rule(nextDueDate = "2026-06-25"), "2026-06-25") is DueState.Due)
    }

    @Test
    fun `an upcoming rule reports days remaining`() {
        val state = dueState(rule(nextDueDate = "2026-07-02"), "2026-06-25")
        assertEquals(7, (state as DueState.Upcoming).days)
    }

    @Test
    fun `a paused rule is Inactive however overdue its date looks`() {
        val state = dueState(rule(active = false, nextDueDate = "2020-01-01"), "2026-06-25")
        assertTrue(state is DueState.Inactive)
    }

    @Test
    fun `a season window needs all four bounds`() {
        assertFalse(hasSeasonWindow(rule()))
        assertTrue(hasSeasonWindow(rule(
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10, seasonEndDay = 15,
        )))
        // Three of four is not a window — the server rejects it, so neither do we.
        assertFalse(hasSeasonWindow(rule(
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10,
        )))
    }

    @Test
    fun `seasonWindowMonthDays returns the pair in stored order, wrap-around included`() {
        assertNull(seasonWindowMonthDays(rule()))

        val summer = seasonWindowMonthDays(rule(
            seasonStartMonth = 4, seasonStartDay = 1, seasonEndMonth = 10, seasonEndDay = 15,
        ))!!
        assertEquals(4 to 1, summer.first)
        assertEquals(10 to 15, summer.second)

        // Nov 1 – Mar 31 is one continuous winter season, not an error.
        val winter = seasonWindowMonthDays(rule(
            seasonStartMonth = 11, seasonStartDay = 1, seasonEndMonth = 3, seasonEndDay = 31,
        ))!!
        assertEquals(11 to 1, winter.first)
        assertEquals(3 to 31, winter.second)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.data.model.MaintenanceFormattingTest"`
Expected: FAIL — unresolved reference `activitiesForTarget`.

- [ ] **Step 3: Write the implementation**

```kotlin
package app.verdant.android.data.model

/** Mirrors the backend's MaintenanceActivity enum, in declaration order. */
val MAINTENANCE_ACTIVITIES = listOf(
    "WATER", "WEED", "FERTILIZE",
    "MOW", "RAKE", "PRUNE", "EDGE", "SWEEP", "TOP_UP", "CLEAN", "INSPECT",
)

/** Mirrors the backend's GardenAreaCategory enum, in declaration order. */
val AREA_CATEGORIES = listOf(
    "WALKWAY", "LAWN", "HEDGE", "COMPOST",
    "GREENHOUSE", "WATER_FEATURE", "STRUCTURE", "OTHER",
)

enum class MaintenanceTarget { BED, AREA }

private val BED_ACTIVITIES = listOf("WATER", "WEED", "FERTILIZE")

/**
 * Which activities the server will accept for a target. Offering one it
 * rejects — FERTILIZE on an area, MOW on a bed — is a guaranteed 400, so
 * pickers must filter by this rather than showing everything.
 */
fun activitiesForTarget(target: MaintenanceTarget): List<String> = when (target) {
    MaintenanceTarget.BED -> BED_ACTIVITIES
    MaintenanceTarget.AREA -> MAINTENANCE_ACTIVITIES.filter { it != "FERTILIZE" }
}

sealed interface DueState {
    data object Inactive : DueState
    data class Overdue(val days: Int) : DueState
    data object Due : DueState
    data class Upcoming(val days: Int) : DueState
}

/** Whole days between two ISO yyyy-MM-dd strings. */
private fun daysBetween(fromIso: String, toIso: String): Int {
    val from = java.time.LocalDate.parse(fromIso)
    val to = java.time.LocalDate.parse(toIso)
    return java.time.temporal.ChronoUnit.DAYS.between(from, to).toInt()
}

/**
 * [MaintenanceRuleResponse.nextDueDate] is computed server-side; this only
 * classifies it for display. A paused rule is never overdue — pausing must
 * stop a rule nagging, not freeze it in a red state.
 */
fun dueState(rule: MaintenanceRuleResponse, todayIso: String): DueState {
    if (!rule.active) return DueState.Inactive
    val delta = daysBetween(todayIso, rule.nextDueDate)
    return when {
        delta < 0 -> DueState.Overdue(-delta)
        delta == 0 -> DueState.Due
        else -> DueState.Upcoming(delta)
    }
}

fun hasSeasonWindow(rule: MaintenanceRuleResponse): Boolean =
    rule.seasonStartMonth != null && rule.seasonStartDay != null &&
        rule.seasonEndMonth != null && rule.seasonEndDay != null

/**
 * The window as ((startMonth, startDay), (endMonth, endDay)), or null when
 * the rule applies year-round. Returned in stored order: a start after the
 * end means the window wraps the new year, which is valid.
 */
fun seasonWindowMonthDays(
    rule: MaintenanceRuleResponse,
): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
    if (!hasSeasonWindow(rule)) return null
    return (rule.seasonStartMonth!! to rule.seasonStartDay!!) to
        (rule.seasonEndMonth!! to rule.seasonEndDay!!)
}
```

`java.time` is safe here: `minSdk = 28` (set in `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt:27`), comfortably above the API 26 threshold, so no desugaring dependency is needed.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.data.model.MaintenanceFormattingTest"`
Expected: PASS, 10 tests.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/data/model/MaintenanceFormatting.kt \
        android/app/src/test/kotlin/app/verdant/android/data/model/MaintenanceFormattingTest.kt
git commit -m "feat(android): maintenance activity, category, and due-state helpers"
```

---

### Task 3: The shared maintenance rules controller

Plain Kotlin holding all rule loading and mutation, so the area and bed ViewModels share one implementation and one test.

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/maintenance/MaintenanceRulesController.kt`
- Test: `android/app/src/test/kotlin/app/verdant/android/ui/maintenance/MaintenanceRulesControllerTest.kt`

**Interfaces:**
- Consumes: `MaintenanceRuleRepository` (Task 1), `MaintenanceTarget` (Task 2).
- Produces: `MaintenanceRulesState(rules, isLoading, error)`; `MaintenanceRulesController(repository, target, targetId, scope)` with `state: StateFlow<MaintenanceRulesState>`, `refresh()`, `create(request)`, `update(id, request)`, `delete(id)`.

- [ ] **Step 1: Write the failing test**

Follow `TaskListViewModelTest`'s shape: `StandardTestDispatcher`, `runTest`, Turbine, and a hand-written fake.

```kotlin
package app.verdant.android.ui.maintenance

import app.cash.turbine.test
import app.verdant.android.data.model.CreateMaintenanceRuleRequest
import app.verdant.android.data.model.MaintenanceRuleResponse
import app.verdant.android.data.model.MaintenanceTarget
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import app.verdant.android.data.repository.MaintenanceRuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceRulesControllerTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun rule(id: Long = 1, activityType: String = "WEED") = MaintenanceRuleResponse(
        id = id, bedId = null, bedName = null,
        gardenAreaId = 5, gardenAreaName = "Gången",
        activityType = activityType, intervalDays = 21, anchorDate = null,
        seasonStartMonth = null, seasonStartDay = null,
        seasonEndMonth = null, seasonEndDay = null,
        active = true, notes = null,
        lastDoneDate = null, nextDueDate = "2026-06-25",
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z",
    )

    private class FakeRuleRepository(
        var rules: MutableList<MaintenanceRuleResponse> = mutableListOf(),
        var failWith: Exception? = null,
    ) : MaintenanceRuleRepository {
        var lastListBedId: Long? = null
        var lastListAreaId: Long? = null
        var lastCreate: CreateMaintenanceRuleRequest? = null
        var lastUpdate: Pair<Long, UpdateMaintenanceRuleRequest>? = null
        var deletedIds = mutableListOf<Long>()

        override suspend fun list(bedId: Long?, areaId: Long?): List<MaintenanceRuleResponse> {
            failWith?.let { throw it }
            lastListBedId = bedId
            lastListAreaId = areaId
            return rules.toList()
        }
        override suspend fun create(request: CreateMaintenanceRuleRequest): MaintenanceRuleResponse {
            failWith?.let { throw it }
            lastCreate = request
            val created = rule(id = 99, activityType = request.activityType)
            rules.add(created)
            return created
        }
        override suspend fun update(id: Long, request: UpdateMaintenanceRuleRequest): MaintenanceRuleResponse {
            failWith?.let { throw it }
            lastUpdate = id to request
            return rules.first { it.id == id }
        }
        override suspend fun delete(id: Long) {
            failWith?.let { throw it }
            deletedIds.add(id)
            rules.removeAll { it.id == id }
        }
    }

    @Test
    fun `refresh loads rules scoped to an area, never with both filters`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.refresh()
        advanceUntilIdle()

        controller.state.test {
            val s = awaitItem()
            assertEquals(1, s.rules.size)
            assertNull(s.error)
        }
        assertEquals(5L, repo.lastListAreaId)
        assertNull(repo.lastListBedId)
    }

    @Test
    fun `refresh on a bed target passes bedId only`() = runTest {
        val repo = FakeRuleRepository()
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.BED, 3, this)

        controller.refresh()
        advanceUntilIdle()

        assertEquals(3L, repo.lastListBedId)
        assertNull(repo.lastListAreaId)
    }

    @Test
    fun `create sends exactly one target id and reloads`() = runTest {
        val repo = FakeRuleRepository()
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.create(activityType = "MOW", intervalDays = 14)
        advanceUntilIdle()

        val sent = repo.lastCreate!!
        assertEquals(5L, sent.gardenAreaId)
        assertNull(sent.bedId)
        assertEquals("MOW", sent.activityType)
        assertEquals(14, sent.intervalDays)

        controller.state.test { assertEquals(1, awaitItem().rules.size) }
    }

    @Test
    fun `clearing a season window never sends season values alongside the flag`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.clearSeasonWindow(ruleId = 1)
        advanceUntilIdle()

        val (id, request) = repo.lastUpdate!!
        assertEquals(1L, id)
        assertTrue(request.clearSeasonWindow)
        // Sending any season* value with the flag is a 400 server-side.
        assertNull(request.seasonStartMonth)
        assertNull(request.seasonStartDay)
        assertNull(request.seasonEndMonth)
        assertNull(request.seasonEndDay)
    }

    @Test
    fun `delete removes the rule and reloads`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)

        controller.delete(1)
        advanceUntilIdle()

        assertEquals(listOf(1L), repo.deletedIds)
        controller.state.test { assertTrue(awaitItem().rules.isEmpty()) }
    }

    @Test
    fun `a load failure surfaces an error without clearing rules already shown`() = runTest {
        val repo = FakeRuleRepository(mutableListOf(rule()))
        val controller = MaintenanceRulesController(repo, MaintenanceTarget.AREA, 5, this)
        controller.refresh()
        advanceUntilIdle()

        repo.failWith = RuntimeException("nätverksfel")
        controller.refresh()
        advanceUntilIdle()

        controller.state.test {
            val s = awaitItem()
            assertNotNull(s.error)
            // Keeping the last good list matches BedDetailViewModel's
            // "once loaded, stay loaded" behaviour.
            assertEquals(1, s.rules.size)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.ui.maintenance.MaintenanceRulesControllerTest"`
Expected: FAIL — unresolved reference `MaintenanceRulesController`.

- [ ] **Step 3: Write the controller**

It must:

- Be a plain class taking `(repository: MaintenanceRuleRepository, target: MaintenanceTarget, targetId: Long, scope: CoroutineScope)` — the scope is the host ViewModel's `viewModelScope`, and a `TestScope` in tests.
- Expose `state: StateFlow<MaintenanceRulesState>` where `MaintenanceRulesState(rules: List<MaintenanceRuleResponse> = emptyList(), isLoading: Boolean = false, error: String? = null)`.
- `refresh()` calls `repository.list(bedId = ..., areaId = ...)` with **exactly one** non-null, derived from `target`. Never both — that is a 400.
- On failure, set `error` but **keep the previously loaded rules**, matching `BedDetailViewModel`'s stay-loaded behaviour.
- `create(activityType, intervalDays, anchorDate?, season bounds?, notes?)` builds a `CreateMaintenanceRuleRequest` with `bedId`/`gardenAreaId` set from the target, then refreshes.
- `update(id, request)` passes through, then refreshes.
- `clearSeasonWindow(ruleId)` sends `UpdateMaintenanceRuleRequest(clearSeasonWindow = true)` and **nothing else season-related** — the combination is a 400.
- `delete(id)` deletes, then refreshes.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.ui.maintenance.MaintenanceRulesControllerTest"`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/maintenance \
        android/app/src/test/kotlin/app/verdant/android/ui/maintenance
git commit -m "feat(android): shared maintenance rules controller"
```

---

### Task 4: Area detail screen

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/area/GardenAreaDetailScreen.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/area/GardenAreaDetailViewModel.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/maintenance/MaintenanceRulesSection.kt`
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/navigation/NavGraph.kt`
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/navigation/graphs/GardenGraph.kt`
- Modify: both `strings.xml` files
- Test: `android/app/src/test/kotlin/app/verdant/android/ui/area/GardenAreaDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `GardenAreaRepository`, `MaintenanceRulesController`, `activitiesForTarget`, `dueState`.
- Produces: `Screen.GardenAreaDetail` route; `GardenAreaDetailUiState`; `MaintenanceRulesSection` composable.

- [ ] **Step 1: Write the failing ViewModel test**

Create `GardenAreaDetailViewModelTest` with a `FakeGardenAreaRepository` (hand-written, implementing the Task 1 interface) and a `FakeMaintenanceRuleRepository`. Cover:

- Loading an area emits `Loading` then `Loaded` with the area, its events, and its rules.
- A load failure emits `Error`.
- `logEvent` posts a `CreateGardenAreaEventRequest` with the chosen activity, then **reloads both the event list and the rules** — logging work moves the derived clock, so a stale `nextDueDate` would otherwise be shown. Assert the rule repository's `list` was called again after the log.
- `logEvent` accepts `NOTE` as well as activity types.
- Deleting the area sets a `deleted` flag the screen navigates on.

The reload-rules-after-logging assertion is the important one: it is the visible payoff of the derived-clock design and the easiest thing to get wrong.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.ui.area.GardenAreaDetailViewModelTest"`
Expected: FAIL — unresolved reference.

- [ ] **Step 3: Write the ViewModel**

Model on `ui/bed/BedDetailViewModel.kt`: `@HiltViewModel`, `SavedStateHandle.get<Long>("areaId")!!`, a sealed `GardenAreaDetailUiState` with `Loading` / `Error` / `Loaded(area, events, photos, isRefreshing, deleted, toastMessage)`, and a `MaintenanceRulesController` instance built with `viewModelScope`. It is simpler than the bed one — no plants, no harvest stats, no supply applications.

- [ ] **Step 4: Write `MaintenanceRulesSection`**

A **stateless** composable taking `state: MaintenanceRulesState`, the target, and callbacks (`onCreate`, `onUpdate`, `onClearSeasonWindow`, `onDelete`, `onToggleActive`). Both this screen and the bed screen render it, which is what keeps Task 7 to a handful of lines.

It must:

- Render each rule's activity via `stringResource` (never a hardcoded Swedish string — see Task 2's note), its interval, and its season window when present.
- Show a due badge coloured by `dueState(rule, today)`: overdue, due, upcoming, inactive.
- Offer add / edit / delete, and a pause toggle mapping to `active`.
- Offer activities from `activitiesForTarget(target)` only.
- In the edit sheet, treat the season window as all-or-none, and route "remove the window" through `onClearSeasonWindow` rather than sending nulls — nulls do nothing server-side.
- Show an empty state explaining that nothing recurs until a rule is added — this is where "nothing is seeded" becomes visible.

- [ ] **Step 5: Add the route**

In `NavGraph.kt`, beside the other garden-scoped screens:

```kotlin
    data object GardenAreaDetail : Screen("area/{areaId}") {
        fun create(areaId: Long) = "area/$areaId"
    }
```

In `GardenGraph.kt`, following the `BedDetail` composable's shape:

```kotlin
    composable(
        Screen.GardenAreaDetail.route,
        arguments = listOf(navArgument("areaId") { type = NavType.LongType }),
    ) {
        GardenAreaDetailScreen(
            onBack = { navController.popBackStack() },
            onGardenClick = { gardenId -> navController.navigate(Screen.GardenDetail.create(gardenId)) },
        )
    }
```

- [ ] **Step 6: Add the strings**

Add every new key to **both** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-sv/strings.xml`: area screen title, category names, activity names, maintenance section title, interval and season labels, due/overdue/paused badges, log-maintenance sheet, empty states, delete confirmation.

Those two files are already 2 entries out of step, so do not use equal totals as your check — verify each key you add exists in both by name.

- [ ] **Step 7: Verify**

Run:
```bash
cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.ui.area.*" && ./gradlew :app:assembleDebug
```
Expected: tests pass, build succeeds.

- [ ] **Step 8: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/area \
        android/app/src/main/kotlin/app/verdant/android/ui/maintenance \
        android/app/src/main/kotlin/app/verdant/android/ui/navigation \
        android/app/src/main/res/values/strings.xml \
        android/app/src/main/res/values-sv/strings.xml \
        android/app/src/test/kotlin/app/verdant/android/ui/area
git commit -m "feat(android): garden area detail screen with maintenance rules"
```

---

### Task 5: Area photos

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/area/GardenAreaPhotosSection.kt`
- Modify: `GardenAreaDetailScreen.kt`, `GardenAreaDetailViewModel.kt`

- [ ] **Step 1: Build the section**

Copy `ui/bed/BedPhotosSection.kt` and substitute:

| From | To |
|---|---|
| `bedId` | `areaId` |
| `BedPhotoResponse` | `GardenAreaPhotoResponse` |
| `CreateBedPhotoRequest` | `CreateGardenAreaPhotoRequest` |
| bed repository photo calls | `GardenAreaRepository.photos/addPhoto/deletePhoto` |

The `PhotoPicker` integration and `Bitmap.toCompressedBase64(maxSize = 800)` carry over unchanged.

**The base64 path matters.** The API takes `imageBase64` and mints the URL server-side. It does not accept a URL — an earlier backend draft did, and that was a cross-tenant delete hole. Do not add any affordance that supplies a URL.

- [ ] **Step 2: Wire it in**

Add photo state to `GardenAreaDetailUiState.Loaded` and render the section where `BedDetailScreen` places `BedPhotosSection`.

- [ ] **Step 3: Verify**

Run: `cd android && ./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug`

Then on a device or emulator: add a photo to an area, confirm it renders and deletes. Confirm the stored URL is a `storage.googleapis.com` path the server minted.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/area
git commit -m "feat(android): area photo gallery and capture"
```

---

### Task 6: Create areas, and list them under their garden

**Files:**
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/area/CreateGardenAreaScreen.kt`
- Create: `android/app/src/main/kotlin/app/verdant/android/ui/area/CreateGardenAreaViewModel.kt`
- Modify: `NavGraph.kt`, `GardenGraph.kt`, `ui/garden/GardenDetailScreen.kt`
- Modify: both `strings.xml` files
- Test: `android/app/src/test/kotlin/app/verdant/android/ui/area/CreateGardenAreaViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Cover: a valid submission posts `CreateGardenAreaRequest` with the chosen name and category; submission is blocked while the name is blank; a failure surfaces an error without navigating away.

- [ ] **Step 2: Build the screen**

Model on `ui/bed/CreateBedScreen.kt`. Fields: name (required), category (required, a picker over `AREA_CATEGORIES` labelled from `strings.xml`), description (optional), size in m² (optional).

`boundaryJson` is **not** a field. The API carries it but no client draws polygons and this plan adds no map work.

- [ ] **Step 3: Add the route**

```kotlin
    data object CreateGardenArea : Screen("garden/{gardenId}/area/create") {
        fun create(gardenId: Long) = "garden/$gardenId/area/create"
    }
```

plus the `composable(...)` entry in `GardenGraph.kt`, mirroring `CreateBed`.

- [ ] **Step 4: List areas on the garden screen**

In `GardenDetailScreen.kt`, add an areas section below beds: a header ("Platser") with an add action navigating to `Screen.CreateGardenArea.create(gardenId)`, rows showing name and category navigating to `Screen.GardenAreaDetail.create(areaId)`, and an empty state.

Its ViewModel must load areas alongside beds. Sort by name — reuse `NaturalNameComparator` (referenced by `ui/bed/BedSort.kt`) so "Gång #10" follows "Gång #9", matching how beds already sort.

Mirror the beds section's layout so the two read as siblings — that visual equivalence is the point of areas being a first-class place.

- [ ] **Step 5: Strings, in both files**

- [ ] **Step 6: Verify**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "app.verdant.android.ui.area.*" && ./gradlew :app:assembleDebug`

On a device: create an area from a garden, confirm it appears in the list and opens.

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/area \
        android/app/src/main/kotlin/app/verdant/android/ui/garden \
        android/app/src/main/kotlin/app/verdant/android/ui/navigation \
        android/app/src/main/res/values/strings.xml \
        android/app/src/main/res/values-sv/strings.xml \
        android/app/src/test/kotlin/app/verdant/android/ui/area
git commit -m "feat(android): create areas and list them under their garden"
```

---

### Task 7: Beds get maintenance rules

The payoff for Tasks 3 and 4: one controller instance and one composable call.

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailViewModel.kt`
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/bed/BedDetailScreen.kt`

- [ ] **Step 1: Add the controller to the bed ViewModel**

Inject `MaintenanceRuleRepository` and build a `MaintenanceRulesController(repo, MaintenanceTarget.BED, bedId, viewModelScope)`. Expose its `state` so the screen can render the section, and refresh it on load.

- [ ] **Step 2: Render the section**

Add `MaintenanceRulesSection(...)` to `BedDetailScreen`, below the bed's meta and above the plants list, with `MaintenanceTarget.BED`.

- [ ] **Step 3: Keep the weed/water actions honest**

`BedDetailViewModel` already has weed and water actions calling `BedRepository.weed`/`water`. Those write the exact `bed_event` rows the backend derives "last done" from, so pressing them moves any matching rule's clock.

Refresh the rules controller in both actions' success paths, so the section's next-due date updates immediately rather than showing a stale value. This is the visible payoff of the derived-clock design and it should be obvious in the UI.

- [ ] **Step 4: Extend the existing test**

Add a case to whichever test covers `BedDetailViewModel` (create one following `TaskListViewModelTest`'s shape if none exists) asserting that a successful weed refreshes the rules — i.e. the rule repository's `list` is called again afterwards.

- [ ] **Step 5: Verify**

Run: `cd android && ./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug`

On a device: add a WEED rule to a bed, note the next-due date, press "Rensa ogräs", confirm the date jumps forward by the interval without leaving the screen.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/bed \
        android/app/src/test/kotlin/app/verdant/android/ui/bed
git commit -m "feat(android): maintenance rules on bed detail"
```

---

### Task 8: Area-scoped tasks in the task list

**Files:**
- Modify: `android/app/src/main/kotlin/app/verdant/android/ui/task/` (list and form screens/ViewModels)
- Modify: both `strings.xml` files
- Test: extend `android/app/src/test/kotlin/app/verdant/android/ui/task/TaskListViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Add cases to `TaskListViewModelTest`:

- A task with `gardenAreaName` and no `speciesName` produces a title naming the place and the activity, not a bare enum.
- A task with `maintenanceRuleId != null` is reported as rule-backed, so the UI can disable the fields the server rejects.

- [ ] **Step 2: Fix the title**

Wherever the task list derives a display title (mirroring the web app's `taskTitle`), a maintenance task has no species and must not render as the raw enum `"MOW"`. Fall back through the place: activity label plus `gardenAreaName ?: bedName`, resolving the activity through `strings.xml`.

- [ ] **Step 2b: VERIFY the existing completion path — do not add one**

Android already completes tasks from the list; see the Amendment at the top of this
plan. Confirm, by reading `ui/task/TaskListScreen.kt`, that:

- the complete affordance is reachable for a task with `speciesId == null`,
  `bedId == null`, `gardenAreaId != null`
- `completeTask` sends `speciesId = null` for such a task (it does, because
  `task.speciesId` is already null — but confirm rather than assume)
- the overdue bucket includes it once its deadline passes

Then add a ViewModel test asserting `completeTask` on an area-scoped task posts
`speciesId = null` and `processedCount = remainingCount`, using a hand-written fake
repository. This pins the behaviour that, on web, was missing entirely and locked
every rule permanently. Do NOT build a second completion path.

- [ ] **Step 3: Guard rule-backed task edits**

The backend rejects `activityType` and `targetCount` changes on any task with `maintenanceRuleId != null` — the activity belongs to the rule and the count is always 1.

The Android task form seeds both fields from the existing task and sends both on save (`TaskFormScreen.kt` around the activity and count inputs). Unchanged echoes are accepted, so nothing breaks today — but a user who changes the activity on a maintenance task gets an opaque 400.

When the loaded task is rule-backed: disable the activity and count inputs and show a short line saying the task comes from a maintenance rule, with a way to open that rule's place (`Screen.GardenAreaDetail.create(gardenAreaId)` or `Screen.BedDetail.create(bedId)`) to change the rule itself.

- [ ] **Step 4: Do not call deletion "dismiss"**

Deleting a pending rule-backed task does not stop it — the scheduler recreates it the next morning, because the work is still undone. Wherever the task list offers delete on a task with `maintenanceRuleId != null`, the confirmation must say it will reappear while the rule is active, and point at pausing the rule as the way to actually stop it. Do not label it "dismiss", "skip", or "klar för nu".

- [ ] **Step 5: Strings, in both files**

- [ ] **Step 6: Verify**

Run: `cd android && ./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug`

On a device: with a maintenance task present, confirm it renders with a readable title, that the edit form disables activity and count with an explanation, and that the delete copy warns it will return.

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/kotlin/app/verdant/android/ui/task \
        android/app/src/main/res/values/strings.xml \
        android/app/src/main/res/values-sv/strings.xml \
        android/app/src/test/kotlin/app/verdant/android/ui/task
git commit -m "feat(android): render and guard area-scoped maintenance tasks"
```

---

## What this plan deliberately leaves out

- **Any map or polygon work**, including adding areas to `CreateGardenScreen`'s wizard. `boundaryJson` round-trips untouched.
- **Instrumented UI tests.** This module has none today; adding a first Espresso/Compose-test harness is its own piece of work.
- **Offline caching.** Areas and rules are fetched on demand like everything else in the app.
- **Dashboard/My World surfacing.** Maintenance tasks are ordinary `ScheduledTask` rows and already appear wherever tasks appear.
