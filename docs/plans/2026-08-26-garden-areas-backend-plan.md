# Garden Areas — Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GardenArea` as a first-class place under a garden, plus a recurring-maintenance engine that turns due `maintenance_rule` rows into ordinary `ScheduledTask` rows via a daily job.

**Architecture:** Three Flyway migrations add `garden_area`, its event and photo tables, and a `maintenance_rule` table whose target is either a bed or an area (two nullable FKs, one-of check constraint). "Last done" is never stamped — it is derived from the existing event log so that logging work by hand resets the reminder. A `@Scheduled` job computes due dates and inserts one task per due rule; a partial unique index makes that idempotent by construction.

**Tech Stack:** Quarkus 3 + Kotlin, Gradle, PostgreSQL, Flyway, plain JDBC via `AgroalDataSource`, JUnit 5, mockito-kotlin, `@QuarkusTest`.

**Spec:** `docs/plans/2026-08-26-garden-areas-design.md`

## Global Constraints

- Code name is `GardenArea` / `garden_area`. Never `Location` — `TrayLocation` already exists and would be ambiguous. The Swedish UI label "Plats" is a client concern and appears nowhere in the backend.
- Migrations are `V42`, `V43`, `V44`. The highest existing migration is `V41__plant_workflow_step.sql`.
- Org scoping for areas goes through `garden.org_id`, exactly as `bed` does. There is **no** `org_id` column on `garden_area`.
- Bed rules are restricted to activities that have a `PlantEventType` equivalent (`WATER`, `WEED`, `FERTILIZE`). Area rules accept every activity that declares `GARDEN_AREA`.
- Repositories are plain JDBC over `AgroalDataSource` with hand-written `ResultSet` mappers. Do not introduce Panache, JPA, or an ORM.
- Resources use `@Path("/api")`, `@Produces`/`@Consumes(MediaType.APPLICATION_JSON)`, `@Authenticated`, and read the org from injected `OrgContext`. Follow `BedResource`.
- Service tests are plain JUnit + mockito-kotlin (see `ScheduledTaskServiceTest`). Repository tests are `@QuarkusTest` with `@Inject` on real repositories against Postgres (see `SupplyApplicationRepositoryTest`).
- Season windows are inclusive at both ends and repeat yearly. Wrap-around (`Nov 1 – Mar 31`) must work.
- Nothing is seeded. No default rules for any bed or area — behaviour changes only when a user creates a rule.

### Running tests

Full backend suite (Docker only, no local JDK needed):

```bash
./scripts/run-tests.sh backend
```

A single test class, which is what you want between steps:

```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.MaintenanceDueCalculatorTest" \
  --no-daemon --console=plain
```

With a local JDK 21 you can instead run `cd backend && ./gradlew test --tests "..."`.

---

### Task 1: Maintenance activity and area category enums

Pure Kotlin with no database, so it goes first and everything else can lean on it.

**Files:**
- Create: `backend/src/main/kotlin/app/verdant/entity/Maintenance.kt`
- Modify: none
- Test: `backend/src/test/kotlin/app/verdant/entity/MaintenanceActivityTest.kt`

**Interfaces:**
- Consumes: `PlantEventType` from `app/verdant/entity/PlantEvent.kt`.
- Produces: `enum class MaintenanceTarget { BED, GARDEN_AREA }`; `enum class MaintenanceActivity(val targets: Set<MaintenanceTarget>, val bedEventType: PlantEventType?)` with `fun appliesTo(target: MaintenanceTarget): Boolean`, `companion object { fun forTarget(target: MaintenanceTarget): List<MaintenanceActivity>; fun parse(value: String): MaintenanceActivity }`; `enum class GardenAreaCategory`; `const val AREA_EVENT_NOTE = "NOTE"`.

- [ ] **Step 1: Write the failing test**

```kotlin
package app.verdant.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MaintenanceActivityTest {

    @Test
    fun `every bed activity maps to a plant event type`() {
        val bedActivities = MaintenanceActivity.forTarget(MaintenanceTarget.BED)
        assertTrue(bedActivities.isNotEmpty())
        bedActivities.forEach { activity ->
            assertTrue(
                activity.bedEventType != null,
                "$activity applies to beds but has no bedEventType, so bed history could not record it",
            )
        }
    }

    @Test
    fun `bed activities are exactly water weed and fertilize`() {
        assertEquals(
            listOf(MaintenanceActivity.WATER, MaintenanceActivity.WEED, MaintenanceActivity.FERTILIZE),
            MaintenanceActivity.forTarget(MaintenanceTarget.BED),
        )
    }

    @Test
    fun `fertilize does not apply to areas`() {
        assertFalse(MaintenanceActivity.FERTILIZE.appliesTo(MaintenanceTarget.GARDEN_AREA))
        assertTrue(MaintenanceActivity.FERTILIZE.appliesTo(MaintenanceTarget.BED))
    }

    @Test
    fun `mow applies only to areas`() {
        assertTrue(MaintenanceActivity.MOW.appliesTo(MaintenanceTarget.GARDEN_AREA))
        assertFalse(MaintenanceActivity.MOW.appliesTo(MaintenanceTarget.BED))
    }

    @Test
    fun `parse accepts a known name and rejects an unknown one`() {
        assertEquals(MaintenanceActivity.WEED, MaintenanceActivity.parse("WEED"))
        assertThrows<IllegalArgumentException> { MaintenanceActivity.parse("MULCH") }
    }

    @Test
    fun `every activity applies to at least one target`() {
        MaintenanceActivity.entries.forEach { activity ->
            assertTrue(activity.targets.isNotEmpty(), "$activity applies to nothing")
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.entity.MaintenanceActivityTest" --no-daemon --console=plain
```
Expected: FAIL — compilation error, `MaintenanceActivity` is unresolved.

- [ ] **Step 3: Write minimal implementation**

Create `backend/src/main/kotlin/app/verdant/entity/Maintenance.kt`:

```kotlin
package app.verdant.entity

/** What a maintenance rule can be attached to. */
enum class MaintenanceTarget { BED, GARDEN_AREA }

/**
 * Work that recurs on a bed or a garden area.
 *
 * [bedEventType] is how the activity is recorded in `bed_event`. Activities
 * without one cannot be attached to a bed, because bed history would have no
 * way to express them — that invariant is pinned by MaintenanceActivityTest.
 */
enum class MaintenanceActivity(
    val targets: Set<MaintenanceTarget>,
    val bedEventType: PlantEventType?,
) {
    WATER(setOf(MaintenanceTarget.BED, MaintenanceTarget.GARDEN_AREA), PlantEventType.WATERED),
    WEED(setOf(MaintenanceTarget.BED, MaintenanceTarget.GARDEN_AREA), PlantEventType.WEEDED),
    FERTILIZE(setOf(MaintenanceTarget.BED), PlantEventType.APPLIED_SUPPLY),
    MOW(setOf(MaintenanceTarget.GARDEN_AREA), null),
    RAKE(setOf(MaintenanceTarget.GARDEN_AREA), null),
    PRUNE(setOf(MaintenanceTarget.GARDEN_AREA), null),
    EDGE(setOf(MaintenanceTarget.GARDEN_AREA), null),
    SWEEP(setOf(MaintenanceTarget.GARDEN_AREA), null),
    TOP_UP(setOf(MaintenanceTarget.GARDEN_AREA), null),
    CLEAN(setOf(MaintenanceTarget.GARDEN_AREA), null),
    INSPECT(setOf(MaintenanceTarget.GARDEN_AREA), null);

    fun appliesTo(target: MaintenanceTarget): Boolean = target in targets

    companion object {
        fun forTarget(target: MaintenanceTarget): List<MaintenanceActivity> =
            entries.filter { it.appliesTo(target) }

        fun parse(value: String): MaintenanceActivity =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown maintenance activity: $value")
    }
}

enum class GardenAreaCategory {
    WALKWAY, LAWN, HEDGE, COMPOST, GREENHOUSE, WATER_FEATURE, STRUCTURE, OTHER
}

/** Free-text area log entry, stored in the same column as activity events. */
const val AREA_EVENT_NOTE = "NOTE"
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.entity.MaintenanceActivityTest" --no-daemon --console=plain
```
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/entity/Maintenance.kt \
        backend/src/test/kotlin/app/verdant/entity/MaintenanceActivityTest.kt
git commit -m "feat(backend): maintenance activity and garden area category enums"
```

---

### Task 2: `garden_area` schema, entities, and repositories

**Files:**
- Create: `backend/src/main/resources/db/migration/V42__garden_area.sql`
- Create: `backend/src/main/resources/db/migration/V43__garden_area_event_photo.sql`
- Create: `backend/src/main/kotlin/app/verdant/entity/GardenArea.kt`
- Create: `backend/src/main/kotlin/app/verdant/repository/GardenAreaRepository.kt`
- Create: `backend/src/main/kotlin/app/verdant/repository/GardenAreaEventRepository.kt`
- Create: `backend/src/main/kotlin/app/verdant/repository/GardenAreaPhotoRepository.kt`
- Test: `backend/src/test/kotlin/app/verdant/repository/GardenAreaRepositoryTest.kt`

**Interfaces:**
- Consumes: `GardenAreaCategory` and `AREA_EVENT_NOTE` from Task 1.
- Produces: `data class GardenArea(id, gardenId, name, description, category, boundaryJson, sizeSqm, createdAt, updatedAt)`; `data class GardenAreaEvent(id, gardenAreaId, eventType, eventDate, notes, createdAt)`; `data class GardenAreaPhoto(id, gardenAreaId, photoUrl, reason, description, capturedAt, createdAt)`; `GardenAreaRepository` with `findById(Long): GardenArea?`, `findByGardenId(Long): List<GardenArea>`, `countByGardenIds(Set<Long>): Map<Long, Int>`, `persist(GardenArea): GardenArea`, `update(GardenArea)`, `delete(Long)`; `GardenAreaEventRepository` with `persist(GardenAreaEvent): GardenAreaEvent`, `findByAreaId(Long, Int): List<GardenAreaEvent>`, `findLatestDate(Long, String): LocalDate?`; `GardenAreaPhotoRepository` with `persist`, `findById`, `findByAreaId`, `delete`.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/kotlin/app/verdant/repository/GardenAreaRepositoryTest.kt`:

```kotlin
package app.verdant.repository

import app.verdant.entity.Garden
import app.verdant.entity.GardenArea
import app.verdant.entity.GardenAreaCategory
import app.verdant.entity.GardenAreaEvent
import app.verdant.entity.Organization
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class GardenAreaRepositoryTest {

    @Inject lateinit var areas: GardenAreaRepository
    @Inject lateinit var events: GardenAreaEventRepository
    @Inject lateinit var gardens: GardenRepository
    @Inject lateinit var orgs: OrganizationRepository

    private var gardenId: Long = 0

    @BeforeEach
    fun setUp() {
        val org = orgs.persist(Organization(name = "Area test org"))
        gardenId = gardens.persist(Garden(name = "Area test garden", orgId = org.id!!)).id!!
    }

    private fun newArea(name: String = "Grusgång") = GardenArea(
        gardenId = gardenId,
        name = name,
        description = "Vid växthuset",
        category = GardenAreaCategory.WALKWAY,
        boundaryJson = """[{"lat":59.1,"lng":18.1}]""",
        sizeSqm = 12.5,
    )

    @Test
    fun `persist assigns an id and round-trips every field`() {
        val saved = areas.persist(newArea())
        val found = areas.findById(saved.id!!)!!

        assertEquals("Grusgång", found.name)
        assertEquals("Vid växthuset", found.description)
        assertEquals(GardenAreaCategory.WALKWAY, found.category)
        assertEquals("""[{"lat":59.1,"lng":18.1}]""", found.boundaryJson)
        assertEquals(12.5, found.sizeSqm)
        assertEquals(gardenId, found.gardenId)
    }

    @Test
    fun `findByGardenId returns only that garden's areas`() {
        areas.persist(newArea("A"))
        areas.persist(newArea("B"))
        val otherGarden = gardens.persist(
            Garden(name = "Other", orgId = orgs.persist(Organization(name = "Other org")).id!!)
        )
        areas.persist(newArea("C").copy(gardenId = otherGarden.id!!))

        assertEquals(listOf("A", "B"), areas.findByGardenId(gardenId).map { it.name })
    }

    @Test
    fun `update changes name category and size`() {
        val saved = areas.persist(newArea())
        areas.update(saved.copy(name = "Gräsmatta", category = GardenAreaCategory.LAWN, sizeSqm = 40.0))

        val found = areas.findById(saved.id!!)!!
        assertEquals("Gräsmatta", found.name)
        assertEquals(GardenAreaCategory.LAWN, found.category)
        assertEquals(40.0, found.sizeSqm)
    }

    @Test
    fun `delete removes the area`() {
        val saved = areas.persist(newArea())
        areas.delete(saved.id!!)
        assertNull(areas.findById(saved.id!!))
    }

    @Test
    fun `countByGardenIds counts per garden`() {
        areas.persist(newArea("A"))
        areas.persist(newArea("B"))
        assertEquals(mapOf(gardenId to 2), areas.countByGardenIds(setOf(gardenId)))
    }

    @Test
    fun `findLatestDate returns the newest matching event date`() {
        val area = areas.persist(newArea())
        events.persist(GardenAreaEvent(gardenAreaId = area.id!!, eventType = "WEED", eventDate = LocalDate.of(2026, 5, 1)))
        events.persist(GardenAreaEvent(gardenAreaId = area.id!!, eventType = "WEED", eventDate = LocalDate.of(2026, 6, 1)))
        events.persist(GardenAreaEvent(gardenAreaId = area.id!!, eventType = "MOW", eventDate = LocalDate.of(2026, 7, 1)))

        assertEquals(LocalDate.of(2026, 6, 1), events.findLatestDate(area.id!!, "WEED"))
        assertEquals(LocalDate.of(2026, 7, 1), events.findLatestDate(area.id!!, "MOW"))
        assertNull(events.findLatestDate(area.id!!, "RAKE"))
    }

    @Test
    fun `deleting an area cascades to its events`() {
        val area = areas.persist(newArea())
        events.persist(GardenAreaEvent(gardenAreaId = area.id!!, eventType = "WEED", eventDate = LocalDate.of(2026, 5, 1)))
        areas.delete(area.id!!)
        assertEquals(emptyList<GardenAreaEvent>(), events.findByAreaId(area.id!!, 50))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.repository.GardenAreaRepositoryTest" --no-daemon --console=plain
```
Expected: FAIL — compilation error, `GardenArea` and `GardenAreaRepository` are unresolved.

- [ ] **Step 3: Write the migrations**

Create `backend/src/main/resources/db/migration/V42__garden_area.sql`:

```sql
-- A place in the garden that is not a growing bed but still needs work:
-- gravel walkways, lawns, hedges, compost corners. Sibling of `bed` under
-- `garden`; org scoping goes through garden.org_id exactly as bed does, so
-- there is deliberately no org_id column here.

CREATE TABLE garden_area (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    garden_id     BIGINT       NOT NULL REFERENCES garden(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    category      VARCHAR(32)  NOT NULL,
    boundary_json TEXT,
    size_sqm      DOUBLE PRECISION,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_garden_area_garden ON garden_area(garden_id);
```

Create `backend/src/main/resources/db/migration/V43__garden_area_event_photo.sql`:

```sql
-- Maintenance log for an area. event_type holds a MaintenanceActivity value
-- (WEED, MOW, …) or 'NOTE'. Areas grow nothing, so there is no plants_affected
-- column and no past-tense enum — the activity name is stored as-is.

CREATE TABLE garden_area_event (
    id             BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    garden_area_id BIGINT      NOT NULL REFERENCES garden_area(id) ON DELETE CASCADE,
    event_type     VARCHAR(32) NOT NULL,
    event_date     DATE        NOT NULL,
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_garden_area_event_area
    ON garden_area_event(garden_area_id, event_date DESC);

-- Mirrors bed_photo field for field.
CREATE TABLE garden_area_photo (
    id             BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    garden_area_id BIGINT      NOT NULL REFERENCES garden_area(id) ON DELETE CASCADE,
    photo_url      TEXT        NOT NULL,
    reason         VARCHAR(32) NOT NULL,
    description    TEXT,
    captured_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_garden_area_photo_area
    ON garden_area_photo(garden_area_id, captured_at DESC);
```

- [ ] **Step 4: Write the entities**

Create `backend/src/main/kotlin/app/verdant/entity/GardenArea.kt`:

```kotlin
package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class GardenArea(
    val id: Long? = null,
    val gardenId: Long,
    val name: String,
    val description: String? = null,
    val category: GardenAreaCategory,
    val boundaryJson: String? = null,
    val sizeSqm: Double? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

/** [eventType] is a [MaintenanceActivity] name or [AREA_EVENT_NOTE]. */
data class GardenAreaEvent(
    val id: Long? = null,
    val gardenAreaId: Long,
    val eventType: String,
    val eventDate: LocalDate = LocalDate.now(),
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
)

data class GardenAreaPhoto(
    val id: Long? = null,
    val gardenAreaId: Long,
    val photoUrl: String,
    val reason: BedPhotoReason,
    val description: String? = null,
    val capturedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
)
```

`BedPhotoReason` is reused from `app/verdant/entity/BedPhoto.kt` rather than duplicated — a photo of an area is captured for the same reasons as a photo of a bed.

- [ ] **Step 5: Write the repositories**

Create `backend/src/main/kotlin/app/verdant/repository/GardenAreaRepository.kt`:

```kotlin
package app.verdant.repository

import app.verdant.entity.GardenArea
import app.verdant.entity.GardenAreaCategory
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class GardenAreaRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): GardenArea? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM garden_area WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toGardenArea() else null }
        }
    }

    fun findByGardenId(gardenId: Long): List<GardenArea> = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM garden_area WHERE garden_id = ? ORDER BY id").use { ps ->
            ps.setLong(1, gardenId)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toGardenArea()) } }
        }
    }

    fun countByGardenIds(gardenIds: Set<Long>): Map<Long, Int> {
        if (gardenIds.isEmpty()) return emptyMap()
        val placeholders = gardenIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT garden_id, COUNT(*) FROM garden_area WHERE garden_id IN ($placeholders) GROUP BY garden_id"
            ).use { ps ->
                gardenIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong("garden_id"), rs.getInt("count")) }
                }
            }
        }
    }

    fun persist(area: GardenArea): GardenArea = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO garden_area (garden_id, name, description, category, boundary_json, size_sqm,
                                        created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, now(), now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, area.gardenId)
            ps.setString(2, area.name)
            ps.setString(3, area.description)
            ps.setString(4, area.category.name)
            ps.setString(5, area.boundaryJson)
            area.sizeSqm?.let { ps.setDouble(6, it) } ?: ps.setNull(6, java.sql.Types.DOUBLE)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); area.copy(id = rs.getLong(1)) }
        }
    }

    fun update(area: GardenArea) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE garden_area
                   SET name = ?, description = ?, category = ?, boundary_json = ?, size_sqm = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, area.name)
                ps.setString(2, area.description)
                ps.setString(3, area.category.name)
                ps.setString(4, area.boundaryJson)
                area.sizeSqm?.let { ps.setDouble(5, it) } ?: ps.setNull(5, java.sql.Types.DOUBLE)
                ps.setLong(6, area.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM garden_area WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toGardenArea() = GardenArea(
        id = getLong("id"),
        gardenId = getLong("garden_id"),
        name = getString("name"),
        description = getString("description"),
        category = GardenAreaCategory.valueOf(getString("category")),
        boundaryJson = getString("boundary_json"),
        sizeSqm = getObject("size_sqm") as? Double,
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
```

Create `backend/src/main/kotlin/app/verdant/repository/GardenAreaEventRepository.kt`:

```kotlin
package app.verdant.repository

import app.verdant.entity.GardenAreaEvent
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement
import java.time.LocalDate

@ApplicationScoped
class GardenAreaEventRepository(private val ds: AgroalDataSource) {

    fun persist(event: GardenAreaEvent): GardenAreaEvent = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO garden_area_event (garden_area_id, event_type, event_date, notes, created_at)
               VALUES (?, ?, ?, ?, now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, event.gardenAreaId)
            ps.setString(2, event.eventType)
            ps.setDate(3, Date.valueOf(event.eventDate))
            ps.setString(4, event.notes)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); event.copy(id = rs.getLong(1)) }
        }
    }

    fun findByAreaId(areaId: Long, limit: Int = 50): List<GardenAreaEvent> = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT * FROM garden_area_event WHERE garden_area_id = ?
               ORDER BY event_date DESC, id DESC LIMIT ?"""
        ).use { ps ->
            ps.setLong(1, areaId)
            ps.setInt(2, limit)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toEvent()) } }
        }
    }

    /** Newest [eventType] date for the area, or null if it has never been logged. */
    fun findLatestDate(areaId: Long, eventType: String): LocalDate? = ds.connection.use { conn ->
        conn.prepareStatement(
            "SELECT MAX(event_date) AS latest FROM garden_area_event WHERE garden_area_id = ? AND event_type = ?"
        ).use { ps ->
            ps.setLong(1, areaId)
            ps.setString(2, eventType)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getDate("latest")?.toLocalDate() else null }
        }
    }

    private fun ResultSet.toEvent() = GardenAreaEvent(
        id = getLong("id"),
        gardenAreaId = getLong("garden_area_id"),
        eventType = getString("event_type"),
        eventDate = getDate("event_date").toLocalDate(),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
```

Create `backend/src/main/kotlin/app/verdant/repository/GardenAreaPhotoRepository.kt`:

```kotlin
package app.verdant.repository

import app.verdant.entity.BedPhotoReason
import app.verdant.entity.GardenAreaPhoto
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp

@ApplicationScoped
class GardenAreaPhotoRepository(private val ds: AgroalDataSource) {

    fun persist(photo: GardenAreaPhoto): GardenAreaPhoto = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO garden_area_photo (garden_area_id, photo_url, reason, description, captured_at, created_at)
               VALUES (?, ?, ?, ?, ?, now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, photo.gardenAreaId)
            ps.setString(2, photo.photoUrl)
            ps.setString(3, photo.reason.name)
            ps.setString(4, photo.description)
            ps.setTimestamp(5, Timestamp.from(photo.capturedAt))
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); photo.copy(id = rs.getLong(1)) }
        }
    }

    fun findById(id: Long): GardenAreaPhoto? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM garden_area_photo WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toPhoto() else null }
        }
    }

    fun findByAreaId(areaId: Long): List<GardenAreaPhoto> = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT * FROM garden_area_photo WHERE garden_area_id = ?
               ORDER BY captured_at DESC, id DESC"""
        ).use { ps ->
            ps.setLong(1, areaId)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toPhoto()) } }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM garden_area_photo WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toPhoto() = GardenAreaPhoto(
        id = getLong("id"),
        gardenAreaId = getLong("garden_area_id"),
        photoUrl = getString("photo_url"),
        reason = BedPhotoReason.valueOf(getString("reason")),
        description = getString("description"),
        capturedAt = getTimestamp("captured_at").toInstant(),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
```

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.repository.GardenAreaRepositoryTest" --no-daemon --console=plain
```
Expected: PASS, 7 tests. Flyway applies `V42` and `V43` at startup against the throwaway database.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/migration/V42__garden_area.sql \
        backend/src/main/resources/db/migration/V43__garden_area_event_photo.sql \
        backend/src/main/kotlin/app/verdant/entity/GardenArea.kt \
        backend/src/main/kotlin/app/verdant/repository/GardenArea*.kt \
        backend/src/test/kotlin/app/verdant/repository/GardenAreaRepositoryTest.kt
git commit -m "feat(backend): garden_area schema, entities, and repositories"
```

---

### Task 3: Garden area DTOs, service, and resource

**Files:**
- Create: `backend/src/main/kotlin/app/verdant/dto/GardenAreaDtos.kt`
- Create: `backend/src/main/kotlin/app/verdant/service/GardenAreaService.kt`
- Create: `backend/src/main/kotlin/app/verdant/resource/GardenAreaResource.kt`
- Modify: `backend/src/main/kotlin/app/verdant/resource/GardenResource.kt` (add nested list/create under `/{gardenId}/areas`)
- Test: `backend/src/test/kotlin/app/verdant/service/GardenAreaServiceTest.kt`

**Interfaces:**
- Consumes: `GardenAreaRepository`, `GardenAreaEventRepository`, `GardenAreaPhotoRepository` (Task 2); `GardenRepository`; `MaintenanceActivity`, `MaintenanceTarget`, `AREA_EVENT_NOTE` (Task 1).
- Produces: `GardenAreaResponse`, `CreateGardenAreaRequest`, `UpdateGardenAreaRequest`, `GardenAreaEventResponse`, `CreateGardenAreaEventRequest`, `GardenAreaPhotoResponse`, `CreateGardenAreaPhotoRequest`; `GardenAreaService` with `getAreasForGarden(gardenId, orgId)`, `getArea(areaId, orgId)`, `createArea(gardenId, request, orgId)`, `updateArea(areaId, request, orgId)`, `deleteArea(areaId, orgId)`, `listEvents(areaId, orgId, limit)`, `logEvent(areaId, request, orgId)`, `listPhotos`, `addPhoto`, `deletePhoto`, and `requireArea(areaId, orgId): GardenArea` used by Task 8.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/kotlin/app/verdant/service/GardenAreaServiceTest.kt`:

```kotlin
package app.verdant.service

import app.verdant.dto.CreateGardenAreaEventRequest
import app.verdant.dto.CreateGardenAreaRequest
import app.verdant.dto.UpdateGardenAreaRequest
import app.verdant.entity.Garden
import app.verdant.entity.GardenArea
import app.verdant.entity.GardenAreaCategory
import app.verdant.entity.GardenAreaEvent
import app.verdant.repository.GardenAreaEventRepository
import app.verdant.repository.GardenAreaPhotoRepository
import app.verdant.repository.GardenAreaRepository
import app.verdant.repository.GardenRepository
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class GardenAreaServiceTest {

    private val areas: GardenAreaRepository = mock()
    private val events: GardenAreaEventRepository = mock()
    private val photos: GardenAreaPhotoRepository = mock()
    private val gardens: GardenRepository = mock()
    private val storage: StorageService = mock()
    private val service = GardenAreaService(areas, events, photos, gardens, storage)

    private val orgId = 10L
    private val otherOrgId = 99L
    private val gardenId = 1L
    private val areaId = 5L

    private val garden = Garden(id = gardenId, name = "Trädgården", orgId = orgId)
    private val area = GardenArea(
        id = areaId, gardenId = gardenId, name = "Grusgång",
        category = GardenAreaCategory.WALKWAY,
    )

    @Test
    fun `createArea persists under the garden`() {
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        whenever(areas.persist(any())).thenReturn(area)

        val result = service.createArea(
            gardenId,
            CreateGardenAreaRequest(name = "Grusgång", category = "WALKWAY"),
            orgId,
        )

        assertEquals("Grusgång", result.name)
        assertEquals("WALKWAY", result.category)
        assertEquals(gardenId, result.gardenId)
    }

    @Test
    fun `createArea rejects an unknown category`() {
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        assertThrows<BadRequestException> {
            service.createArea(gardenId, CreateGardenAreaRequest(name = "X", category = "PATIO"), orgId)
        }
        verify(areas, never()).persist(any())
    }

    @Test
    fun `createArea hides a garden belonging to another org`() {
        whenever(gardens.findById(gardenId)).thenReturn(garden.copy(orgId = otherOrgId))

        assertThrows<NotFoundException> {
            service.createArea(gardenId, CreateGardenAreaRequest(name = "X", category = "LAWN"), orgId)
        }
    }

    @Test
    fun `getArea hides an area in another org's garden`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden.copy(orgId = otherOrgId))

        assertThrows<NotFoundException> { service.getArea(areaId, orgId) }
    }

    @Test
    fun `updateArea leaves omitted fields untouched`() {
        whenever(areas.findById(areaId)).thenReturn(area.copy(description = "Ursprunglig"))
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        val result = service.updateArea(areaId, UpdateGardenAreaRequest(name = "Nytt namn"), orgId)

        assertEquals("Nytt namn", result.name)
        assertEquals("Ursprunglig", result.description)
        assertEquals("WALKWAY", result.category)
    }

    @Test
    fun `logEvent accepts an activity that applies to areas`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        whenever(events.persist(any())).thenAnswer { it.arguments[0] as GardenAreaEvent }

        val result = service.logEvent(
            areaId,
            CreateGardenAreaEventRequest(activityType = "WEED", eventDate = LocalDate.of(2026, 6, 1)),
            orgId,
        )

        assertEquals("WEED", result.eventType)
        assertEquals(LocalDate.of(2026, 6, 1), result.eventDate)
    }

    @Test
    fun `logEvent accepts a plain note`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)
        whenever(events.persist(any())).thenAnswer { it.arguments[0] as GardenAreaEvent }

        val result = service.logEvent(
            areaId,
            CreateGardenAreaEventRequest(activityType = "NOTE", notes = "Grus behöver fyllas på"),
            orgId,
        )

        assertEquals("NOTE", result.eventType)
    }

    @Test
    fun `logEvent rejects a bed-only activity`() {
        whenever(areas.findById(areaId)).thenReturn(area)
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        assertThrows<BadRequestException> {
            service.logEvent(areaId, CreateGardenAreaEventRequest(activityType = "FERTILIZE"), orgId)
        }
        verify(events, never()).persist(any())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.GardenAreaServiceTest" --no-daemon --console=plain
```
Expected: FAIL — compilation error, `GardenAreaService` is unresolved.

- [ ] **Step 3: Write the DTOs**

Create `backend/src/main/kotlin/app/verdant/dto/GardenAreaDtos.kt`:

```kotlin
package app.verdant.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class GardenAreaResponse(
    val id: Long,
    val gardenId: Long,
    val gardenName: String?,
    val name: String,
    val description: String?,
    val category: String,
    val boundaryJson: String?,
    val sizeSqm: Double?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateGardenAreaRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:NotBlank
    val category: String,
    val boundaryJson: String? = null,
    @field:Positive
    val sizeSqm: Double? = null,
)

data class UpdateGardenAreaRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    val category: String? = null,
    val boundaryJson: String? = null,
    @field:Positive
    val sizeSqm: Double? = null,
)

data class GardenAreaEventResponse(
    val id: Long,
    val gardenAreaId: Long,
    val eventType: String,
    val eventDate: LocalDate,
    val notes: String?,
    val createdAt: Instant,
)

data class CreateGardenAreaEventRequest(
    @field:NotBlank
    val activityType: String,
    val eventDate: LocalDate? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class GardenAreaPhotoResponse(
    val id: Long,
    val gardenAreaId: Long,
    val photoUrl: String,
    val reason: String,
    val description: String?,
    val capturedAt: Instant,
    val createdAt: Instant,
)

data class CreateGardenAreaPhotoRequest(
    @field:NotBlank
    val photoUrl: String,
    @field:NotBlank
    val reason: String,
    @field:Size(max = 2000)
    val description: String? = null,
    val capturedAt: Instant? = null,
)
```

- [ ] **Step 4: Write the service**

Create `backend/src/main/kotlin/app/verdant/service/GardenAreaService.kt`:

```kotlin
package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.*
import app.verdant.repository.GardenAreaEventRepository
import app.verdant.repository.GardenAreaPhotoRepository
import app.verdant.repository.GardenAreaRepository
import app.verdant.repository.GardenRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.time.Instant
import java.time.LocalDate

@ApplicationScoped
class GardenAreaService(
    private val areaRepository: GardenAreaRepository,
    private val eventRepository: GardenAreaEventRepository,
    private val photoRepository: GardenAreaPhotoRepository,
    private val gardenRepository: GardenRepository,
    private val storageService: StorageService,
) {
    /** Loads an area, or 404s if it does not exist or belongs to another org. */
    fun requireArea(areaId: Long, orgId: Long): GardenArea {
        val area = areaRepository.findById(areaId) ?: throw NotFoundException("Area not found")
        val garden = gardenRepository.findById(area.gardenId) ?: throw NotFoundException("Area not found")
        if (garden.orgId != orgId) throw NotFoundException("Area not found")
        return area
    }

    private fun requireGarden(gardenId: Long, orgId: Long): Garden {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Garden not found")
        return garden
    }

    private fun parseCategory(value: String): GardenAreaCategory =
        runCatching { GardenAreaCategory.valueOf(value) }
            .getOrElse { throw BadRequestException("Unknown area category: $value") }

    fun getAreasForGarden(gardenId: Long, orgId: Long): List<GardenAreaResponse> {
        val garden = requireGarden(gardenId, orgId)
        return areaRepository.findByGardenId(gardenId).map { it.toResponse(garden.name) }
    }

    fun getArea(areaId: Long, orgId: Long): GardenAreaResponse {
        val area = requireArea(areaId, orgId)
        return area.toResponse(gardenRepository.findById(area.gardenId)?.name)
    }

    fun createArea(gardenId: Long, request: CreateGardenAreaRequest, orgId: Long): GardenAreaResponse {
        val garden = requireGarden(gardenId, orgId)
        val category = parseCategory(request.category)
        val saved = areaRepository.persist(
            GardenArea(
                gardenId = gardenId,
                name = request.name,
                description = request.description,
                category = category,
                boundaryJson = request.boundaryJson,
                sizeSqm = request.sizeSqm,
            )
        )
        return saved.toResponse(garden.name)
    }

    fun updateArea(areaId: Long, request: UpdateGardenAreaRequest, orgId: Long): GardenAreaResponse {
        val area = requireArea(areaId, orgId)
        val updated = area.copy(
            name = request.name ?: area.name,
            description = request.description ?: area.description,
            category = request.category?.let { parseCategory(it) } ?: area.category,
            boundaryJson = request.boundaryJson ?: area.boundaryJson,
            sizeSqm = request.sizeSqm ?: area.sizeSqm,
        )
        areaRepository.update(updated)
        return updated.toResponse(gardenRepository.findById(area.gardenId)?.name)
    }

    fun deleteArea(areaId: Long, orgId: Long) {
        requireArea(areaId, orgId)
        areaRepository.delete(areaId)
    }

    fun listEvents(areaId: Long, orgId: Long, limit: Int = 50): List<GardenAreaEventResponse> {
        requireArea(areaId, orgId)
        return eventRepository.findByAreaId(areaId, limit).map { it.toResponse() }
    }

    fun logEvent(areaId: Long, request: CreateGardenAreaEventRequest, orgId: Long): GardenAreaEventResponse {
        requireArea(areaId, orgId)
        val eventType = validateAreaEventType(request.activityType)
        val saved = eventRepository.persist(
            GardenAreaEvent(
                gardenAreaId = areaId,
                eventType = eventType,
                eventDate = request.eventDate ?: LocalDate.now(),
                notes = request.notes,
            )
        )
        return saved.toResponse()
    }

    fun listPhotos(areaId: Long, orgId: Long): List<GardenAreaPhotoResponse> {
        requireArea(areaId, orgId)
        return photoRepository.findByAreaId(areaId).map { it.toResponse() }
    }

    fun addPhoto(areaId: Long, request: CreateGardenAreaPhotoRequest, orgId: Long): GardenAreaPhotoResponse {
        requireArea(areaId, orgId)
        val reason = runCatching { BedPhotoReason.valueOf(request.reason) }
            .getOrElse { throw BadRequestException("Unknown photo reason: ${request.reason}") }
        val saved = photoRepository.persist(
            GardenAreaPhoto(
                gardenAreaId = areaId,
                photoUrl = request.photoUrl,
                reason = reason,
                description = request.description,
                capturedAt = request.capturedAt ?: Instant.now(),
            )
        )
        return saved.toResponse()
    }

    fun deletePhoto(areaId: Long, photoId: Long, orgId: Long) {
        requireArea(areaId, orgId)
        val photo = photoRepository.findById(photoId) ?: throw NotFoundException("Photo not found")
        if (photo.gardenAreaId != areaId) throw NotFoundException("Photo not found")
        storageService.deleteByPath(photo.photoUrl)
        photoRepository.delete(photoId)
    }

    companion object {
        /**
         * Area events hold a MaintenanceActivity that applies to areas, or a
         * plain NOTE. A bed-only activity such as FERTILIZE is rejected so the
         * log cannot record work an area can't receive.
         */
        fun validateAreaEventType(value: String): String {
            if (value == AREA_EVENT_NOTE) return AREA_EVENT_NOTE
            val activity = runCatching { MaintenanceActivity.parse(value) }
                .getOrElse { throw BadRequestException("Unknown activity: $value") }
            if (!activity.appliesTo(MaintenanceTarget.GARDEN_AREA)) {
                throw BadRequestException("Activity $value does not apply to garden areas")
            }
            return activity.name
        }
    }
}

private fun GardenArea.toResponse(gardenName: String?) = GardenAreaResponse(
    id = id!!,
    gardenId = gardenId,
    gardenName = gardenName,
    name = name,
    description = description,
    category = category.name,
    boundaryJson = boundaryJson,
    sizeSqm = sizeSqm,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun GardenAreaEvent.toResponse() = GardenAreaEventResponse(
    id = id ?: 0,
    gardenAreaId = gardenAreaId,
    eventType = eventType,
    eventDate = eventDate,
    notes = notes,
    createdAt = createdAt,
)

private fun GardenAreaPhoto.toResponse() = GardenAreaPhotoResponse(
    id = id!!,
    gardenAreaId = gardenAreaId,
    photoUrl = photoUrl,
    reason = reason.name,
    description = description,
    capturedAt = capturedAt,
    createdAt = createdAt,
)
```

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.GardenAreaServiceTest" --no-daemon --console=plain
```
Expected: PASS, 8 tests.

- [ ] **Step 6: Write the resource**

Create `backend/src/main/kotlin/app/verdant/resource/GardenAreaResource.kt`:

```kotlin
package app.verdant.resource

import app.verdant.dto.CreateGardenAreaEventRequest
import app.verdant.dto.CreateGardenAreaPhotoRequest
import app.verdant.dto.UpdateGardenAreaRequest
import app.verdant.filter.OrgContext
import app.verdant.service.GardenAreaService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class GardenAreaResource(
    private val areaService: GardenAreaService,
    private val orgContext: OrgContext,
) {
    @GET
    @Path("/areas/{id}")
    fun get(@PathParam("id") id: Long) = areaService.getArea(id, orgContext.orgId)

    @PUT
    @Path("/areas/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateGardenAreaRequest) =
        areaService.updateArea(id, request, orgContext.orgId)

    @DELETE
    @Path("/areas/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        areaService.deleteArea(id, orgContext.orgId)
        return Response.noContent().build()
    }

    @GET
    @Path("/areas/{id}/events")
    fun listEvents(@PathParam("id") id: Long, @QueryParam("limit") @DefaultValue("50") limit: Int) =
        areaService.listEvents(id, orgContext.orgId, limit)

    @POST
    @Path("/areas/{id}/events")
    fun logEvent(@PathParam("id") id: Long, @Valid request: CreateGardenAreaEventRequest): Response {
        val event = areaService.logEvent(id, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(event).build()
    }

    @GET
    @Path("/areas/{id}/photos")
    fun listPhotos(@PathParam("id") id: Long) = areaService.listPhotos(id, orgContext.orgId)

    @POST
    @Path("/areas/{id}/photos")
    fun addPhoto(@PathParam("id") id: Long, @Valid request: CreateGardenAreaPhotoRequest) =
        areaService.addPhoto(id, request, orgContext.orgId)

    @DELETE
    @Path("/areas/{id}/photos/{photoId}")
    fun deletePhoto(@PathParam("id") id: Long, @PathParam("photoId") photoId: Long): Response {
        areaService.deletePhoto(id, photoId, orgContext.orgId)
        return Response.noContent().build()
    }
}
```

- [ ] **Step 7: Add the nested endpoints to `GardenResource`**

In `backend/src/main/kotlin/app/verdant/resource/GardenResource.kt`, inject `GardenAreaService` alongside the existing `BedService` and add, directly after the existing `@Path("/{gardenId}/beds")` pair:

```kotlin
    @GET
    @Path("/{gardenId}/areas")
    fun listAreas(@PathParam("gardenId") gardenId: Long) =
        gardenAreaService.getAreasForGarden(gardenId, orgContext.orgId)

    @POST
    @Path("/{gardenId}/areas")
    fun createArea(@PathParam("gardenId") gardenId: Long, @Valid request: CreateGardenAreaRequest): Response {
        val area = gardenAreaService.createArea(gardenId, request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(area).build()
    }
```

Add `import app.verdant.dto.CreateGardenAreaRequest` and `import app.verdant.service.GardenAreaService` at the top.

- [ ] **Step 8: Run the full backend suite**

Run: `./scripts/run-tests.sh backend`
Expected: PASS. The new resource is picked up by Quarkus at startup; a failure here usually means a CDI wiring problem in `GardenResource`.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/dto/GardenAreaDtos.kt \
        backend/src/main/kotlin/app/verdant/service/GardenAreaService.kt \
        backend/src/main/kotlin/app/verdant/resource/GardenAreaResource.kt \
        backend/src/main/kotlin/app/verdant/resource/GardenResource.kt \
        backend/src/test/kotlin/app/verdant/service/GardenAreaServiceTest.kt
git commit -m "feat(backend): garden area CRUD, events, and photos API"
```

---

### Task 4: `maintenance_rule` schema, entity, and repository

**Files:**
- Create: `backend/src/main/resources/db/migration/V44__maintenance_rule.sql`
- Create: `backend/src/main/kotlin/app/verdant/entity/MaintenanceRule.kt`
- Create: `backend/src/main/kotlin/app/verdant/repository/MaintenanceRuleRepository.kt`
- Test: `backend/src/test/kotlin/app/verdant/repository/MaintenanceRuleRepositoryTest.kt`

**Interfaces:**
- Consumes: `MaintenanceActivity` (Task 1), `garden_area` table (Task 2).
- Produces: `data class MaintenanceRule(id, orgId, bedId, gardenAreaId, activity, intervalDays, anchorDate, seasonStartMonth, seasonStartDay, seasonEndMonth, seasonEndDay, active, notes, createdAt, updatedAt)` with `val target: MaintenanceTarget`; `MaintenanceRuleRepository` with `findById(Long): MaintenanceRule?`, `findByBedId(Long): List<MaintenanceRule>`, `findByAreaId(Long): List<MaintenanceRule>`, `findByOrgId(Long): List<MaintenanceRule>`, `findActiveWithoutOpenTask(): List<MaintenanceRule>`, `persist(MaintenanceRule): MaintenanceRule`, `update(MaintenanceRule)`, `delete(Long)`.

Note the `scheduled_task` columns are added by this same migration because the partial unique index that guards the scheduler lives on that table and references `maintenance_rule`.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/kotlin/app/verdant/repository/MaintenanceRuleRepositoryTest.kt`:

```kotlin
package app.verdant.repository

import app.verdant.entity.Garden
import app.verdant.entity.GardenArea
import app.verdant.entity.GardenAreaCategory
import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import app.verdant.entity.MaintenanceTarget
import app.verdant.entity.Organization
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class MaintenanceRuleRepositoryTest {

    @Inject lateinit var rules: MaintenanceRuleRepository
    @Inject lateinit var areas: GardenAreaRepository
    @Inject lateinit var gardens: GardenRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var tasks: ScheduledTaskRepository

    private var orgId: Long = 0
    private var areaId: Long = 0

    @BeforeEach
    fun setUp() {
        orgId = orgs.persist(Organization(name = "Rule test org")).id!!
        val gardenId = gardens.persist(Garden(name = "Rule garden", orgId = orgId)).id!!
        areaId = areas.persist(
            GardenArea(gardenId = gardenId, name = "Gång", category = GardenAreaCategory.WALKWAY)
        ).id!!
    }

    private fun areaRule(
        activity: MaintenanceActivity = MaintenanceActivity.WEED,
        intervalDays: Int = 21,
        active: Boolean = true,
    ) = MaintenanceRule(
        orgId = orgId,
        gardenAreaId = areaId,
        activity = activity,
        intervalDays = intervalDays,
        anchorDate = LocalDate.of(2026, 5, 1),
        seasonStartMonth = 4, seasonStartDay = 1,
        seasonEndMonth = 10, seasonEndDay = 15,
        active = active,
    )

    @Test
    fun `persist round-trips every field`() {
        val saved = rules.persist(areaRule())
        val found = rules.findById(saved.id!!)!!

        assertEquals(areaId, found.gardenAreaId)
        assertNull(found.bedId)
        assertEquals(MaintenanceActivity.WEED, found.activity)
        assertEquals(21, found.intervalDays)
        assertEquals(LocalDate.of(2026, 5, 1), found.anchorDate)
        assertEquals(4, found.seasonStartMonth)
        assertEquals(15, found.seasonEndDay)
        assertTrue(found.active)
        assertEquals(MaintenanceTarget.GARDEN_AREA, found.target)
    }

    @Test
    fun `a rule with no season window persists nulls`() {
        val saved = rules.persist(
            areaRule().copy(
                seasonStartMonth = null, seasonStartDay = null,
                seasonEndMonth = null, seasonEndDay = null,
            )
        )
        val found = rules.findById(saved.id!!)!!
        assertNull(found.seasonStartMonth)
        assertNull(found.seasonEndDay)
    }

    @Test
    fun `a rule with neither target is rejected by the check constraint`() {
        assertThrows(Exception::class.java) {
            rules.persist(areaRule().copy(gardenAreaId = null))
        }
    }

    @Test
    fun `findByAreaId returns that area's rules`() {
        rules.persist(areaRule(MaintenanceActivity.WEED))
        rules.persist(areaRule(MaintenanceActivity.MOW))
        assertEquals(2, rules.findByAreaId(areaId).size)
    }

    @Test
    fun `findActiveWithoutOpenTask skips inactive rules`() {
        val inactive = rules.persist(areaRule(active = false))
        val active = rules.persist(areaRule(activity = MaintenanceActivity.MOW))

        val found = rules.findActiveWithoutOpenTask().map { it.id }
        assertTrue(active.id in found)
        assertTrue(inactive.id !in found)
    }

    @Test
    fun `findActiveWithoutOpenTask skips a rule that already has a pending task`() {
        val rule = rules.persist(areaRule())
        assertTrue(rules.findActiveWithoutOpenTask().any { it.id == rule.id })

        tasks.persist(
            ScheduledTask(
                orgId = orgId,
                gardenAreaId = areaId,
                maintenanceRuleId = rule.id,
                activityType = "WEED",
                earliestDate = LocalDate.of(2026, 6, 1),
                deadline = LocalDate.of(2026, 6, 1),
                targetCount = 1,
                remainingCount = 1,
                status = ScheduledTaskStatus.PENDING,
            )
        )

        assertTrue(rules.findActiveWithoutOpenTask().none { it.id == rule.id })
    }

    @Test
    fun `a second pending task for the same rule violates the unique index`() {
        val rule = rules.persist(areaRule())
        val task = ScheduledTask(
            orgId = orgId,
            gardenAreaId = areaId,
            maintenanceRuleId = rule.id,
            activityType = "WEED",
            earliestDate = LocalDate.of(2026, 6, 1),
            deadline = LocalDate.of(2026, 6, 1),
            targetCount = 1,
            remainingCount = 1,
        )
        tasks.persist(task)
        assertThrows(Exception::class.java) { tasks.persist(task) }
    }

    @Test
    fun `deleting an area cascades to its rules`() {
        val rule = rules.persist(areaRule())
        areas.delete(areaId)
        assertNull(rules.findById(rule.id!!))
    }
}
```

This test depends on `ScheduledTask` carrying `gardenAreaId` and `maintenanceRuleId`, which Task 5 adds. Implement Task 5's entity and repository changes as part of getting this test green if you are running tasks strictly in order — or run Task 5 first. The plan orders the migration here because the index belongs with the table it references.

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.repository.MaintenanceRuleRepositoryTest" --no-daemon --console=plain
```
Expected: FAIL — compilation error, `MaintenanceRule` is unresolved.

- [ ] **Step 3: Write the migration**

Create `backend/src/main/resources/db/migration/V44__maintenance_rule.sql`:

```sql
-- Recurring maintenance. A rule targets exactly one of a bed or a garden area
-- (two nullable FKs plus a check, rather than a generic target_type/target_id
-- pair, so cascades and referential integrity still work).
--
-- Season bounds are month/day pairs, not dates, so the window repeats every
-- year. start > end means the window wraps the new year (Nov 1 – Mar 31).
--
-- There is deliberately no last_completed_at column: "last done" is derived
-- from the event log, so logging work by hand also resets the reminder.

CREATE TABLE maintenance_rule (
    id                 BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    org_id             BIGINT      NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    bed_id             BIGINT      REFERENCES bed(id) ON DELETE CASCADE,
    garden_area_id     BIGINT      REFERENCES garden_area(id) ON DELETE CASCADE,
    activity_type      VARCHAR(32) NOT NULL,
    interval_days      INT         NOT NULL CHECK (interval_days >= 1),
    anchor_date        DATE,
    season_start_month SMALLINT CHECK (season_start_month BETWEEN 1 AND 12),
    season_start_day   SMALLINT CHECK (season_start_day   BETWEEN 1 AND 31),
    season_end_month   SMALLINT CHECK (season_end_month   BETWEEN 1 AND 12),
    season_end_day     SMALLINT CHECK (season_end_day     BETWEEN 1 AND 31),
    active             BOOLEAN     NOT NULL DEFAULT true,
    notes              TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT maintenance_rule_one_target CHECK (
        (bed_id IS NOT NULL)::int + (garden_area_id IS NOT NULL)::int = 1
    ),
    CONSTRAINT maintenance_rule_season_all_or_none CHECK (
        num_nonnulls(season_start_month, season_start_day,
                     season_end_month, season_end_day) IN (0, 4)
    )
);

CREATE INDEX idx_maintenance_rule_org  ON maintenance_rule(org_id);
CREATE INDEX idx_maintenance_rule_bed  ON maintenance_rule(bed_id)         WHERE bed_id IS NOT NULL;
CREATE INDEX idx_maintenance_rule_area ON maintenance_rule(garden_area_id) WHERE garden_area_id IS NOT NULL;

-- Tasks can now target an area, and can record which rule produced them.
ALTER TABLE scheduled_task
    ADD COLUMN garden_area_id      BIGINT REFERENCES garden_area(id) ON DELETE CASCADE,
    ADD COLUMN maintenance_rule_id BIGINT REFERENCES maintenance_rule(id) ON DELETE SET NULL;

CREATE INDEX idx_scheduled_task_garden_area
    ON scheduled_task(garden_area_id) WHERE garden_area_id IS NOT NULL;

-- At most one open task per rule. This is what makes the daily scheduler
-- idempotent by construction: a double run cannot produce duplicates.
CREATE UNIQUE INDEX idx_scheduled_task_open_rule
    ON scheduled_task(maintenance_rule_id)
    WHERE maintenance_rule_id IS NOT NULL AND status = 'PENDING';
```

- [ ] **Step 4: Write the entity**

Create `backend/src/main/kotlin/app/verdant/entity/MaintenanceRule.kt`:

```kotlin
package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

/**
 * A recurring piece of maintenance on exactly one bed or garden area.
 *
 * There is no last-done field: see LastDoneResolver, which derives it from
 * the event log so that hand-logged work also resets the clock.
 */
data class MaintenanceRule(
    val id: Long? = null,
    val orgId: Long,
    val bedId: Long? = null,
    val gardenAreaId: Long? = null,
    val activity: MaintenanceActivity,
    val intervalDays: Int,
    val anchorDate: LocalDate? = null,
    val seasonStartMonth: Int? = null,
    val seasonStartDay: Int? = null,
    val seasonEndMonth: Int? = null,
    val seasonEndDay: Int? = null,
    val active: Boolean = true,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    val target: MaintenanceTarget
        get() = if (bedId != null) MaintenanceTarget.BED else MaintenanceTarget.GARDEN_AREA
}
```

- [ ] **Step 5: Write the repository**

Create `backend/src/main/kotlin/app/verdant/repository/MaintenanceRuleRepository.kt`:

```kotlin
package app.verdant.repository

import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class MaintenanceRuleRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): MaintenanceRule? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM maintenance_rule WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toRule() else null }
        }
    }

    fun findByBedId(bedId: Long): List<MaintenanceRule> = query(
        "SELECT * FROM maintenance_rule WHERE bed_id = ? ORDER BY id", bedId
    )

    fun findByAreaId(areaId: Long): List<MaintenanceRule> = query(
        "SELECT * FROM maintenance_rule WHERE garden_area_id = ? ORDER BY id", areaId
    )

    fun findByOrgId(orgId: Long): List<MaintenanceRule> = query(
        "SELECT * FROM maintenance_rule WHERE org_id = ? ORDER BY id", orgId
    )

    /**
     * Active rules with no PENDING task outstanding — the scheduler's work list.
     * The NOT EXISTS mirrors the partial unique index on scheduled_task.
     */
    fun findActiveWithoutOpenTask(): List<MaintenanceRule> = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT r.* FROM maintenance_rule r
               WHERE r.active = true
                 AND NOT EXISTS (
                     SELECT 1 FROM scheduled_task t
                     WHERE t.maintenance_rule_id = r.id AND t.status = 'PENDING'
                 )
               ORDER BY r.id"""
        ).use { ps ->
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toRule()) } }
        }
    }

    fun persist(rule: MaintenanceRule): MaintenanceRule = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO maintenance_rule (org_id, bed_id, garden_area_id, activity_type, interval_days,
                                             anchor_date, season_start_month, season_start_day,
                                             season_end_month, season_end_day, active, notes,
                                             created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, rule.orgId)
            ps.setObject(2, rule.bedId)
            ps.setObject(3, rule.gardenAreaId)
            ps.setString(4, rule.activity.name)
            ps.setInt(5, rule.intervalDays)
            rule.anchorDate?.let { ps.setDate(6, Date.valueOf(it)) } ?: ps.setNull(6, java.sql.Types.DATE)
            ps.setObject(7, rule.seasonStartMonth)
            ps.setObject(8, rule.seasonStartDay)
            ps.setObject(9, rule.seasonEndMonth)
            ps.setObject(10, rule.seasonEndDay)
            ps.setBoolean(11, rule.active)
            ps.setString(12, rule.notes)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); rule.copy(id = rs.getLong(1)) }
        }
    }

    fun update(rule: MaintenanceRule) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE maintenance_rule
                   SET activity_type = ?, interval_days = ?, anchor_date = ?,
                       season_start_month = ?, season_start_day = ?,
                       season_end_month = ?, season_end_day = ?,
                       active = ?, notes = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, rule.activity.name)
                ps.setInt(2, rule.intervalDays)
                rule.anchorDate?.let { ps.setDate(3, Date.valueOf(it)) } ?: ps.setNull(3, java.sql.Types.DATE)
                ps.setObject(4, rule.seasonStartMonth)
                ps.setObject(5, rule.seasonStartDay)
                ps.setObject(6, rule.seasonEndMonth)
                ps.setObject(7, rule.seasonEndDay)
                ps.setBoolean(8, rule.active)
                ps.setString(9, rule.notes)
                ps.setLong(10, rule.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM maintenance_rule WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun query(sql: String, id: Long): List<MaintenanceRule> = ds.connection.use { conn ->
        conn.prepareStatement(sql).use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toRule()) } }
        }
    }

    private fun ResultSet.toRule() = MaintenanceRule(
        id = getLong("id"),
        orgId = getLong("org_id"),
        bedId = getObject("bed_id") as? Long,
        gardenAreaId = getObject("garden_area_id") as? Long,
        activity = MaintenanceActivity.parse(getString("activity_type")),
        intervalDays = getInt("interval_days"),
        anchorDate = getDate("anchor_date")?.toLocalDate(),
        seasonStartMonth = (getObject("season_start_month") as? Number)?.toInt(),
        seasonStartDay = (getObject("season_start_day") as? Number)?.toInt(),
        seasonEndMonth = (getObject("season_end_month") as? Number)?.toInt(),
        seasonEndDay = (getObject("season_end_day") as? Number)?.toInt(),
        active = getBoolean("active"),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
```

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.repository.MaintenanceRuleRepositoryTest" --no-daemon --console=plain
```
Expected: PASS, 8 tests.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/migration/V44__maintenance_rule.sql \
        backend/src/main/kotlin/app/verdant/entity/MaintenanceRule.kt \
        backend/src/main/kotlin/app/verdant/repository/MaintenanceRuleRepository.kt \
        backend/src/test/kotlin/app/verdant/repository/MaintenanceRuleRepositoryTest.kt
git commit -m "feat(backend): maintenance_rule schema, entity, and repository"
```

---

### Task 5: `ScheduledTask` carries an area and a rule

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/entity/ScheduledTask.kt`
- Modify: `backend/src/main/kotlin/app/verdant/repository/ScheduledTaskRepository.kt:53-82` (persist) and `:197-214` (row mapper)
- Modify: `backend/src/main/kotlin/app/verdant/dto/ScheduledTaskDtos.kt`
- Modify: `backend/src/main/kotlin/app/verdant/service/ScheduledTaskService.kt` (`buildResponses`)
- Test: `backend/src/test/kotlin/app/verdant/resource/ApiContractTest.kt`

**Interfaces:**
- Consumes: `garden_area` and `maintenance_rule` columns from Task 4's migration.
- Produces: `ScheduledTask.gardenAreaId: Long?` and `ScheduledTask.maintenanceRuleId: Long?`; `ScheduledTaskResponse.gardenAreaId`, `.gardenAreaName`, `.maintenanceRuleId`.

- [ ] **Step 1: Write the failing test**

Add to `backend/src/test/kotlin/app/verdant/resource/ApiContractTest.kt`, alongside the existing pinned DTOs:

```kotlin
    @Test
    fun `ScheduledTaskResponse fields are pinned`() = assertFields(
        ScheduledTaskResponse::class,
        "id", "speciesId", "speciesName", "bedId", "bedName", "gardenName",
        "gardenAreaId", "gardenAreaName", "maintenanceRuleId",
        "activityType", "earliestDate", "deadline", "targetCount",
        "remainingCount", "status", "notes", "seasonId", "successionScheduleId",
        "originGroupId", "originGroupName", "acceptableSpecies",
        "createdAt", "updatedAt",
    )

    @Test
    fun `GardenAreaResponse fields are pinned`() = assertFields(
        GardenAreaResponse::class,
        "id", "gardenId", "gardenName", "name", "description", "category",
        "boundaryJson", "sizeSqm", "createdAt", "updatedAt",
    )
```

Add the imports `app.verdant.dto.ScheduledTaskResponse` and `app.verdant.dto.GardenAreaResponse`.

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.resource.ApiContractTest" --no-daemon --console=plain
```
Expected: FAIL — the `ScheduledTaskResponse` assertion reports the three new field names as missing.

- [ ] **Step 3: Add the entity fields**

In `backend/src/main/kotlin/app/verdant/entity/ScheduledTask.kt`, add after `bedId`:

```kotlin
    val gardenAreaId: Long? = null,
    val maintenanceRuleId: Long? = null,
```

- [ ] **Step 4: Update the repository**

In `ScheduledTaskRepository.persist`, extend the column list and add the two parameters. The statement becomes:

```kotlin
                """INSERT INTO scheduled_task (org_id, species_id, bed_id, garden_area_id, maintenance_rule_id, activity_type, earliest_date, deadline, target_count, remaining_count, status, notes, season_id, succession_schedule_id, origin_group_id, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
```

with the bindings renumbered so that positions 4 and 5 are the new columns:

```kotlin
                ps.setLong(1, task.orgId)
                ps.setObject(2, task.speciesId)
                ps.setObject(3, task.bedId)
                ps.setObject(4, task.gardenAreaId)
                ps.setObject(5, task.maintenanceRuleId)
                ps.setString(6, task.activityType)
                if (task.earliestDate != null) ps.setDate(7, Date.valueOf(task.earliestDate))
                else ps.setNull(7, java.sql.Types.DATE)
                if (task.deadline != null) ps.setDate(8, Date.valueOf(task.deadline))
                else ps.setNull(8, java.sql.Types.DATE)
                ps.setInt(9, task.targetCount)
                ps.setInt(10, task.remainingCount)
                ps.setString(11, task.status.name)
                ps.setString(12, task.notes)
                ps.setObject(13, task.seasonId)
                ps.setObject(14, task.successionScheduleId)
                ps.setObject(15, task.originGroupId)
```

In the `ResultSet.toScheduledTask()` mapper, add after `bedId`:

```kotlin
        gardenAreaId = getObject("garden_area_id") as? Long,
        maintenanceRuleId = getObject("maintenance_rule_id") as? Long,
```

- [ ] **Step 5: Add the DTO fields**

In `ScheduledTaskDtos.kt`, add to `ScheduledTaskResponse` after `gardenName`:

```kotlin
    val gardenAreaId: Long?,
    val gardenAreaName: String?,
    val maintenanceRuleId: Long?,
```

- [ ] **Step 6: Populate them in `buildResponses`**

Add `private val gardenAreaRepository: GardenAreaRepository` to the `ScheduledTaskService` constructor and the matching import.

In `buildResponses`, the bed lookup currently reads:

```kotlin
        val bedIds = tasks.mapNotNull { it.bedId }.toSet()
        val bedsById = if (bedIds.isEmpty()) emptyMap() else
            bedIds.mapNotNull { bedRepository.findById(it) }.associateBy { it.id!! }
        val gardenIds = bedsById.values.map { it.gardenId }.toSet()
```

Extend it so areas are batch-loaded the same way and both contribute garden ids:

```kotlin
        val bedIds = tasks.mapNotNull { it.bedId }.toSet()
        val bedsById = if (bedIds.isEmpty()) emptyMap() else
            bedIds.mapNotNull { bedRepository.findById(it) }.associateBy { it.id!! }
        val areaIds = tasks.mapNotNull { it.gardenAreaId }.toSet()
        val areasById = if (areaIds.isEmpty()) emptyMap() else
            areaIds.mapNotNull { gardenAreaRepository.findById(it) }.associateBy { it.id!! }
        val gardenIds = bedsById.values.map { it.gardenId }.toSet() +
            areasById.values.map { it.gardenId }.toSet()
```

Then inside `tasks.map { task -> ... }`, replace the two local bindings:

```kotlin
            val bed = task.bedId?.let { bedsById[it] }
            val garden = bed?.gardenId?.let { gardensById[it] }
```

with:

```kotlin
            val bed = task.bedId?.let { bedsById[it] }
            val area = task.gardenAreaId?.let { areasById[it] }
            // An area task's garden name comes from the area, a bed task's from the bed.
            val garden = (bed?.gardenId ?: area?.gardenId)?.let { gardensById[it] }
```

and add the three new arguments to the `ScheduledTaskResponse(...)` construction, directly after `gardenName`:

```kotlin
                gardenAreaId = task.gardenAreaId,
                gardenAreaName = area?.name,
                maintenanceRuleId = task.maintenanceRuleId,
```

Every other construction site of `ScheduledTaskResponse` must pass the three new arguments too; the compiler will list them.

- [ ] **Step 7: Run the tests**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.resource.ApiContractTest" --tests "app.verdant.service.ScheduledTaskServiceTest" \
  --no-daemon --console=plain
```
Expected: PASS. `ScheduledTaskServiceTest` will need its `ScheduledTaskService(...)` construction updated with the new `GardenAreaRepository` mock.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/entity/ScheduledTask.kt \
        backend/src/main/kotlin/app/verdant/repository/ScheduledTaskRepository.kt \
        backend/src/main/kotlin/app/verdant/dto/ScheduledTaskDtos.kt \
        backend/src/main/kotlin/app/verdant/service/ScheduledTaskService.kt \
        backend/src/test/kotlin/app/verdant/resource/ApiContractTest.kt \
        backend/src/test/kotlin/app/verdant/service/ScheduledTaskServiceTest.kt
git commit -m "feat(backend): scheduled tasks can target a garden area and cite a rule"
```

---

### Task 6: Season windows and due-date arithmetic

The heart of the feature, and pure — no database, no mocks. Test it hard.

**Files:**
- Create: `backend/src/main/kotlin/app/verdant/service/MaintenanceDueCalculator.kt`
- Test: `backend/src/test/kotlin/app/verdant/service/MaintenanceDueCalculatorTest.kt`

**Interfaces:**
- Consumes: `MaintenanceRule` (Task 4).
- Produces: `data class SeasonWindow(start: MonthDay, end: MonthDay)` with `contains(LocalDate): Boolean` and `nextOpening(LocalDate): LocalDate`, plus `companion object { fun of(startMonth: Int?, startDay: Int?, endMonth: Int?, endDay: Int?): SeasonWindow? }`; `object MaintenanceDueCalculator` with `fun dueDate(lastDone: LocalDate?, intervalDays: Int, window: SeasonWindow?, today: LocalDate): LocalDate` and `fun windowOf(rule: MaintenanceRule): SeasonWindow?`.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/kotlin/app/verdant/service/MaintenanceDueCalculatorTest.kt`:

```kotlin
package app.verdant.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.MonthDay

class MaintenanceDueCalculatorTest {

    private val summer = SeasonWindow(MonthDay.of(4, 1), MonthDay.of(10, 15))
    private val winter = SeasonWindow(MonthDay.of(11, 1), MonthDay.of(3, 31))

    // --- SeasonWindow.contains -------------------------------------------

    @Test
    fun `summer window contains a midsummer date`() {
        assertTrue(summer.contains(LocalDate.of(2026, 6, 15)))
    }

    @Test
    fun `summer window excludes a winter date`() {
        assertFalse(summer.contains(LocalDate.of(2026, 1, 15)))
    }

    @Test
    fun `window bounds are inclusive at both ends`() {
        assertTrue(summer.contains(LocalDate.of(2026, 4, 1)))
        assertTrue(summer.contains(LocalDate.of(2026, 10, 15)))
    }

    @Test
    fun `wrap-around window contains dates on both sides of new year`() {
        assertTrue(winter.contains(LocalDate.of(2026, 12, 20)))
        assertTrue(winter.contains(LocalDate.of(2026, 2, 10)))
        assertFalse(winter.contains(LocalDate.of(2026, 6, 1)))
    }

    // --- SeasonWindow.nextOpening ----------------------------------------

    @Test
    fun `nextOpening returns the date itself when already inside the window`() {
        assertEquals(LocalDate.of(2026, 6, 15), summer.nextOpening(LocalDate.of(2026, 6, 15)))
    }

    @Test
    fun `nextOpening jumps to next spring for a date after the window closes`() {
        assertEquals(LocalDate.of(2027, 4, 1), summer.nextOpening(LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun `nextOpening jumps forward within the same year for a date before it opens`() {
        assertEquals(LocalDate.of(2026, 4, 1), summer.nextOpening(LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun `nextOpening on a wrap-around window returns the autumn opening`() {
        assertEquals(LocalDate.of(2026, 11, 1), winter.nextOpening(LocalDate.of(2026, 6, 15)))
    }

    // --- SeasonWindow.of --------------------------------------------------

    @Test
    fun `of returns null when no bounds are set`() {
        assertNull(SeasonWindow.of(null, null, null, null))
    }

    @Test
    fun `of builds a window from four bounds`() {
        assertEquals(summer, SeasonWindow.of(4, 1, 10, 15))
    }

    // --- dueDate ----------------------------------------------------------

    @Test
    fun `a rule never done is due today`() {
        assertEquals(
            LocalDate.of(2026, 6, 1),
            MaintenanceDueCalculator.dueDate(null, 21, null, LocalDate.of(2026, 6, 1)),
        )
    }

    @Test
    fun `due is last done plus the interval`() {
        assertEquals(
            LocalDate.of(2026, 6, 22),
            MaintenanceDueCalculator.dueDate(LocalDate.of(2026, 6, 1), 21, null, LocalDate.of(2026, 6, 10)),
        )
    }

    @Test
    fun `a due date inside the window is left alone`() {
        assertEquals(
            LocalDate.of(2026, 6, 22),
            MaintenanceDueCalculator.dueDate(LocalDate.of(2026, 6, 1), 21, summer, LocalDate.of(2026, 6, 10)),
        )
    }

    @Test
    fun `weeding done in october is next due when the window reopens in april`() {
        assertEquals(
            LocalDate.of(2027, 4, 1),
            MaintenanceDueCalculator.dueDate(LocalDate.of(2026, 10, 10), 21, summer, LocalDate.of(2026, 10, 20)),
        )
    }

    @Test
    fun `a never-done rule out of season waits for the window to open`() {
        assertEquals(
            LocalDate.of(2026, 4, 1),
            MaintenanceDueCalculator.dueDate(null, 21, summer, LocalDate.of(2026, 1, 15)),
        )
    }

    @Test
    fun `a never-done wrap-around rule in summer waits until november`() {
        assertEquals(
            LocalDate.of(2026, 11, 1),
            MaintenanceDueCalculator.dueDate(null, 30, winter, LocalDate.of(2026, 6, 15)),
        )
    }

    @Test
    fun `snow clearing done in december is due again in january`() {
        assertEquals(
            LocalDate.of(2027, 1, 14),
            MaintenanceDueCalculator.dueDate(LocalDate.of(2026, 12, 15), 30, winter, LocalDate.of(2026, 12, 20)),
        )
    }

    @Test
    fun `a february 29 window opening falls back to february 28 in a common year`() {
        val leapWindow = SeasonWindow(MonthDay.of(2, 29), MonthDay.of(3, 31))
        assertEquals(LocalDate.of(2027, 2, 28), leapWindow.nextOpening(LocalDate.of(2027, 1, 1)))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.MaintenanceDueCalculatorTest" --no-daemon --console=plain
```
Expected: FAIL — compilation error, `SeasonWindow` is unresolved.

- [ ] **Step 3: Write the implementation**

Create `backend/src/main/kotlin/app/verdant/service/MaintenanceDueCalculator.kt`:

```kotlin
package app.verdant.service

import app.verdant.entity.MaintenanceRule
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.Year

/**
 * A yearly-repeating window of dates, inclusive at both ends.
 *
 * When [start] is after [end] the window wraps the new year, so
 * `Nov 1 – Mar 31` is a single continuous winter season rather than an
 * empty range.
 */
data class SeasonWindow(val start: MonthDay, val end: MonthDay) {

    fun contains(date: LocalDate): Boolean {
        val md = MonthDay.from(date)
        return if (start <= end) md >= start && md <= end
        else md >= start || md <= end
    }

    /** The first date on or after [from] that falls inside the window. */
    fun nextOpening(from: LocalDate): LocalDate {
        if (contains(from)) return from
        val thisYear = start.atYearClamped(from.year)
        return if (thisYear >= from) thisYear else start.atYearClamped(from.year + 1)
    }

    /** Feb 29 in a common year lands on Feb 28 rather than throwing. */
    private fun MonthDay.atYearClamped(year: Int): LocalDate =
        if (month == Month.FEBRUARY && dayOfMonth == 29 && !Year.isLeap(year.toLong())) {
            LocalDate.of(year, 2, 28)
        } else {
            LocalDate.of(year, month, dayOfMonth)
        }

    companion object {
        /** Null unless all four bounds are present — the DB enforces all-or-none. */
        fun of(startMonth: Int?, startDay: Int?, endMonth: Int?, endDay: Int?): SeasonWindow? {
            if (startMonth == null || startDay == null || endMonth == null || endDay == null) return null
            return SeasonWindow(MonthDay.of(startMonth, startDay), MonthDay.of(endMonth, endDay))
        }
    }
}

object MaintenanceDueCalculator {

    /**
     * When the work next falls due.
     *
     * A rule that has never been done is due immediately, which is what a
     * gardener means by "weed the path every three weeks" typed in June.
     * A due date outside the season window slides to the window's next
     * opening, so nothing nags out of season and nothing piles up.
     */
    fun dueDate(
        lastDone: LocalDate?,
        intervalDays: Int,
        window: SeasonWindow?,
        today: LocalDate,
    ): LocalDate {
        val naive = lastDone?.plusDays(intervalDays.toLong()) ?: today
        return window?.nextOpening(naive) ?: naive
    }

    fun windowOf(rule: MaintenanceRule): SeasonWindow? = SeasonWindow.of(
        rule.seasonStartMonth, rule.seasonStartDay, rule.seasonEndMonth, rule.seasonEndDay,
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.MaintenanceDueCalculatorTest" --no-daemon --console=plain
```
Expected: PASS, 17 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/service/MaintenanceDueCalculator.kt \
        backend/src/test/kotlin/app/verdant/service/MaintenanceDueCalculatorTest.kt
git commit -m "feat(backend): season windows and maintenance due-date arithmetic"
```

---

### Task 7: Deriving "last done" from the event log

**Files:**
- Create: `backend/src/main/kotlin/app/verdant/service/LastDoneResolver.kt`
- Test: `backend/src/test/kotlin/app/verdant/service/LastDoneResolverTest.kt`

**Interfaces:**
- Consumes: `MaintenanceActivity` (Task 1), `GardenAreaEventRepository` (Task 2), `MaintenanceRule` (Task 4).
- Produces: `LastDoneResolver` with `fun resolve(rule: MaintenanceRule): LocalDate?`.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/kotlin/app/verdant/service/LastDoneResolverTest.kt`:

```kotlin
package app.verdant.service

import app.verdant.entity.*
import app.verdant.repository.*
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class LastDoneResolverTest {

    @Inject lateinit var resolver: LastDoneResolver
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var gardens: GardenRepository
    @Inject lateinit var beds: BedRepository
    @Inject lateinit var bedEvents: BedEventRepository
    @Inject lateinit var areas: GardenAreaRepository
    @Inject lateinit var areaEvents: GardenAreaEventRepository

    private var orgId: Long = 0
    private var bedId: Long = 0
    private var areaId: Long = 0

    @BeforeEach
    fun setUp() {
        orgId = orgs.persist(Organization(name = "Resolver org")).id!!
        val gardenId = gardens.persist(Garden(name = "Resolver garden", orgId = orgId)).id!!
        bedId = beds.persist(Bed(name = "Bädd 1", gardenId = gardenId)).id!!
        areaId = areas.persist(
            GardenArea(gardenId = gardenId, name = "Gång", category = GardenAreaCategory.WALKWAY)
        ).id!!
    }

    private fun bedRule(activity: MaintenanceActivity) =
        MaintenanceRule(orgId = orgId, bedId = bedId, activity = activity, intervalDays = 14)

    private fun areaRule(activity: MaintenanceActivity) =
        MaintenanceRule(orgId = orgId, gardenAreaId = areaId, activity = activity, intervalDays = 14)

    @Test
    fun `a bed never weeded resolves to null`() {
        assertNull(resolver.resolve(bedRule(MaintenanceActivity.WEED)))
    }

    @Test
    fun `bed weeding resolves to the newest WEEDED bed event`() {
        bedEvents.persist(BedEvent(bedId = bedId, eventType = PlantEventType.WEEDED, eventDate = LocalDate.of(2026, 5, 1)))
        bedEvents.persist(BedEvent(bedId = bedId, eventType = PlantEventType.WEEDED, eventDate = LocalDate.of(2026, 6, 1)))

        assertEquals(LocalDate.of(2026, 6, 1), resolver.resolve(bedRule(MaintenanceActivity.WEED)))
    }

    @Test
    fun `watering does not satisfy a weeding rule`() {
        bedEvents.persist(BedEvent(bedId = bedId, eventType = PlantEventType.WATERED, eventDate = LocalDate.of(2026, 6, 1)))
        assertNull(resolver.resolve(bedRule(MaintenanceActivity.WEED)))
    }

    @Test
    fun `area weeding resolves to the newest matching area event`() {
        areaEvents.persist(GardenAreaEvent(gardenAreaId = areaId, eventType = "WEED", eventDate = LocalDate.of(2026, 6, 10)))
        areaEvents.persist(GardenAreaEvent(gardenAreaId = areaId, eventType = "MOW", eventDate = LocalDate.of(2026, 7, 1)))

        assertEquals(LocalDate.of(2026, 6, 10), resolver.resolve(areaRule(MaintenanceActivity.WEED)))
        assertEquals(LocalDate.of(2026, 7, 1), resolver.resolve(areaRule(MaintenanceActivity.MOW)))
    }

    @Test
    fun `fertilizing resolves from an APPLIED_SUPPLY bed event`() {
        bedEvents.persist(
            BedEvent(bedId = bedId, eventType = PlantEventType.APPLIED_SUPPLY, eventDate = LocalDate.of(2026, 5, 20))
        )
        assertEquals(LocalDate.of(2026, 5, 20), resolver.resolve(bedRule(MaintenanceActivity.FERTILIZE)))
    }
}
```

Add the sixth test to the same class — it is the whole reason `resolve` reads two
sources, so it must not be skipped. It needs the fuller fixture (`app_user`,
`supply_type`, `supply_inventory`), so add these injections and fields to the
class alongside the existing ones:

```kotlin
    @Inject lateinit var users: UserRepository
    @Inject lateinit var supplyTypes: SupplyTypeRepository
    @Inject lateinit var supplyInventories: SupplyInventoryRepository
    @Inject lateinit var supplyApplications: SupplyApplicationRepository
```

and the test itself:

```kotlin
    @Test
    fun `fertilizing resolves from a supply application`() {
        val userId = users.persist(User(email = "resolver@test.com", displayName = "Resolver")).id!!
        val supplyTypeId = supplyTypes.persist(
            SupplyType(
                orgId = orgId, name = "Hönsgödsel",
                category = SupplyCategory.FERTILIZER, unit = SupplyUnit.KILOGRAMS,
            )
        ).id!!
        val inventoryId = supplyInventories.persist(
            SupplyInventory(orgId = orgId, supplyTypeId = supplyTypeId, quantity = BigDecimal("100.00"))
        ).id!!

        supplyApplications.insert(
            SupplyApplication(
                orgId = orgId,
                bedId = bedId,
                supplyInventoryId = inventoryId,
                supplyTypeId = supplyTypeId,
                quantity = BigDecimal("5.00"),
                targetScope = SupplyApplicationScope.BED,
                appliedAt = LocalDate.of(2026, 5, 20).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                appliedBy = userId,
            )
        )

        // No bed_event row at all — the clock has to move on the application alone.
        assertEquals(LocalDate.of(2026, 5, 20), resolver.resolve(bedRule(MaintenanceActivity.FERTILIZE)))
    }
```

with the extra imports `java.math.BigDecimal` and `java.time.ZoneId`. `SupplyApplicationRepository`'s
write method is named `insert`, not `persist`.

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.LastDoneResolverTest" --no-daemon --console=plain
```
Expected: FAIL — compilation error, `LastDoneResolver` is unresolved.

- [ ] **Step 3: Write the implementation**

Create `backend/src/main/kotlin/app/verdant/service/LastDoneResolver.kt`:

```kotlin
package app.verdant.service

import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import app.verdant.entity.MaintenanceTarget
import app.verdant.repository.GardenAreaEventRepository
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate

/**
 * Works out when a rule's activity was last carried out.
 *
 * Deliberately derived rather than stamped on the rule: weeding a path and
 * logging it the ordinary way must reset the reminder, whether the work was
 * logged by completing a task or by pressing the button on the detail screen.
 */
@ApplicationScoped
class LastDoneResolver(
    private val ds: AgroalDataSource,
    private val areaEvents: GardenAreaEventRepository,
) {
    fun resolve(rule: MaintenanceRule): LocalDate? = when (rule.target) {
        MaintenanceTarget.GARDEN_AREA -> areaEvents.findLatestDate(rule.gardenAreaId!!, rule.activity.name)
        MaintenanceTarget.BED -> resolveForBed(rule.bedId!!, rule.activity)
    }

    private fun resolveForBed(bedId: Long, activity: MaintenanceActivity): LocalDate? {
        val fromEvents = latestBedEvent(bedId, activity.bedEventType!!.name)
        // Bed fertilising is recorded in supply_application, not bed_event, so a
        // FERTILIZE rule has to look at both or its clock would never move.
        val fromApplications =
            if (activity == MaintenanceActivity.FERTILIZE) latestFertilizerApplication(bedId) else null
        return listOfNotNull(fromEvents, fromApplications).maxOrNull()
    }

    private fun latestBedEvent(bedId: Long, eventType: String): LocalDate? = ds.connection.use { conn ->
        conn.prepareStatement(
            "SELECT MAX(event_date) AS latest FROM bed_event WHERE bed_id = ? AND event_type = ?"
        ).use { ps ->
            ps.setLong(1, bedId)
            ps.setString(2, eventType)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getDate("latest")?.toLocalDate() else null }
        }
    }

    private fun latestFertilizerApplication(bedId: Long): LocalDate? = ds.connection.use { conn ->
        conn.prepareStatement(
            """SELECT MAX(sa.applied_at::date) AS latest
               FROM supply_application sa
               JOIN supply_type st ON sa.supply_type_id = st.id
               WHERE sa.bed_id = ? AND st.category = 'FERTILIZER'"""
        ).use { ps ->
            ps.setLong(1, bedId)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getDate("latest")?.toLocalDate() else null }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.LastDoneResolverTest" --no-daemon --console=plain
```
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/service/LastDoneResolver.kt \
        backend/src/test/kotlin/app/verdant/service/LastDoneResolverTest.kt
git commit -m "feat(backend): derive maintenance last-done from the event log"
```

---

### Task 8: Maintenance rule DTOs, service, and resource

**Files:**
- Create: `backend/src/main/kotlin/app/verdant/dto/MaintenanceDtos.kt`
- Create: `backend/src/main/kotlin/app/verdant/service/MaintenanceRuleService.kt`
- Create: `backend/src/main/kotlin/app/verdant/resource/MaintenanceRuleResource.kt`
- Test: `backend/src/test/kotlin/app/verdant/service/MaintenanceRuleServiceTest.kt`

**Interfaces:**
- Consumes: `MaintenanceRuleRepository` (Task 4), `LastDoneResolver` (Task 7), `MaintenanceDueCalculator` (Task 6), `GardenAreaService.requireArea` (Task 3), `BedRepository`, `GardenRepository`.
- Produces: `MaintenanceRuleResponse`, `CreateMaintenanceRuleRequest`, `UpdateMaintenanceRuleRequest`; `MaintenanceRuleService` with `listRules(bedId, areaId, orgId)`, `createRule(request, orgId)`, `updateRule(id, request, orgId)`, `deleteRule(id, orgId)`.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/kotlin/app/verdant/service/MaintenanceRuleServiceTest.kt`:

```kotlin
package app.verdant.service

import app.verdant.dto.CreateMaintenanceRuleRequest
import app.verdant.dto.UpdateMaintenanceRuleRequest
import app.verdant.entity.*
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.MaintenanceRuleRepository
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class MaintenanceRuleServiceTest {

    private val rules: MaintenanceRuleRepository = mock()
    private val beds: BedRepository = mock()
    private val gardens: GardenRepository = mock()
    private val areaService: GardenAreaService = mock()
    private val lastDone: LastDoneResolver = mock()
    private val service = MaintenanceRuleService(rules, beds, gardens, areaService, lastDone)

    private val orgId = 10L
    private val bedId = 3L
    private val areaId = 5L
    private val gardenId = 1L

    private val garden = Garden(id = gardenId, name = "Trädgården", orgId = orgId)
    private val bed = Bed(id = bedId, name = "Bädd 1", gardenId = gardenId)
    private val area = GardenArea(id = areaId, gardenId = gardenId, name = "Gång", category = GardenAreaCategory.WALKWAY)

    private fun areaRequest(activity: String = "WEED") = CreateMaintenanceRuleRequest(
        gardenAreaId = areaId, activityType = activity, intervalDays = 21,
    )

    @Test
    fun `creating an area rule persists it`() {
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(rules.persist(any())).thenAnswer { (it.arguments[0] as MaintenanceRule).copy(id = 7L) }
        whenever(lastDone.resolve(any())).thenReturn(null)

        val result = service.createRule(areaRequest(), orgId)

        assertEquals(7L, result.id)
        assertEquals("WEED", result.activityType)
        assertEquals(areaId, result.gardenAreaId)
        assertEquals(21, result.intervalDays)
    }

    @Test
    fun `a rule must name exactly one target`() {
        assertThrows<BadRequestException> {
            service.createRule(CreateMaintenanceRuleRequest(activityType = "WEED", intervalDays = 21), orgId)
        }
        assertThrows<BadRequestException> {
            service.createRule(
                CreateMaintenanceRuleRequest(bedId = bedId, gardenAreaId = areaId, activityType = "WEED", intervalDays = 21),
                orgId,
            )
        }
        verify(rules, never()).persist(any())
    }

    @Test
    fun `a bed rule rejects an activity that beds cannot receive`() {
        whenever(beds.findById(bedId)).thenReturn(bed)
        whenever(gardens.findById(gardenId)).thenReturn(garden)

        assertThrows<BadRequestException> {
            service.createRule(
                CreateMaintenanceRuleRequest(bedId = bedId, activityType = "MOW", intervalDays = 30),
                orgId,
            )
        }
        verify(rules, never()).persist(any())
    }

    @Test
    fun `an area rule rejects a bed-only activity`() {
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)

        assertThrows<BadRequestException> { service.createRule(areaRequest("FERTILIZE"), orgId) }
        verify(rules, never()).persist(any())
    }

    @Test
    fun `a bed in another org is hidden`() {
        whenever(beds.findById(bedId)).thenReturn(bed)
        whenever(gardens.findById(gardenId)).thenReturn(garden.copy(orgId = 99L))

        assertThrows<NotFoundException> {
            service.createRule(
                CreateMaintenanceRuleRequest(bedId = bedId, activityType = "WEED", intervalDays = 14),
                orgId,
            )
        }
    }

    @Test
    fun `a season window must be complete or absent`() {
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)

        assertThrows<BadRequestException> {
            service.createRule(areaRequest().copy(seasonStartMonth = 4, seasonStartDay = 1), orgId)
        }
    }

    @Test
    fun `the response carries the derived last-done and next-due dates`() {
        val persisted = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
        )
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(rules.persist(any())).thenReturn(persisted)
        whenever(lastDone.resolve(persisted)).thenReturn(LocalDate.of(2026, 6, 1))

        val result = service.createRule(areaRequest(), orgId)

        assertEquals(LocalDate.of(2026, 6, 1), result.lastDoneDate)
        assertEquals(LocalDate.of(2026, 6, 22), result.nextDueDate)
    }

    @Test
    fun `listing rejects both filters at once`() {
        assertThrows<BadRequestException> { service.listRules(bedId, areaId, orgId) }
    }

    @Test
    fun `updating a rule can deactivate it without touching the interval`() {
        val existing = MaintenanceRule(
            id = 7L, orgId = orgId, gardenAreaId = areaId,
            activity = MaintenanceActivity.WEED, intervalDays = 21,
        )
        whenever(rules.findById(7L)).thenReturn(existing)
        whenever(areaService.requireArea(areaId, orgId)).thenReturn(area)
        whenever(lastDone.resolve(any())).thenReturn(null)

        val result = service.updateRule(7L, UpdateMaintenanceRuleRequest(active = false), orgId)

        assertEquals(false, result.active)
        assertEquals(21, result.intervalDays)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.MaintenanceRuleServiceTest" --no-daemon --console=plain
```
Expected: FAIL — compilation error, `MaintenanceRuleService` is unresolved.

- [ ] **Step 3: Write the DTOs**

Create `backend/src/main/kotlin/app/verdant/dto/MaintenanceDtos.kt`:

```kotlin
package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class MaintenanceRuleResponse(
    val id: Long,
    val bedId: Long?,
    val bedName: String?,
    val gardenAreaId: Long?,
    val gardenAreaName: String?,
    val activityType: String,
    val intervalDays: Int,
    val anchorDate: LocalDate?,
    val seasonStartMonth: Int?,
    val seasonStartDay: Int?,
    val seasonEndMonth: Int?,
    val seasonEndDay: Int?,
    val active: Boolean,
    val notes: String?,
    /** Derived from the event log, not stored. Null when never done. */
    val lastDoneDate: LocalDate?,
    /** Derived: when the next task will be created. */
    val nextDueDate: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateMaintenanceRuleRequest(
    val bedId: Long? = null,
    val gardenAreaId: Long? = null,
    @field:NotBlank
    val activityType: String,
    @field:Min(1)
    val intervalDays: Int,
    val anchorDate: LocalDate? = null,
    val seasonStartMonth: Int? = null,
    val seasonStartDay: Int? = null,
    val seasonEndMonth: Int? = null,
    val seasonEndDay: Int? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateMaintenanceRuleRequest(
    val activityType: String? = null,
    @field:Min(1)
    val intervalDays: Int? = null,
    val anchorDate: LocalDate? = null,
    val seasonStartMonth: Int? = null,
    val seasonStartDay: Int? = null,
    val seasonEndMonth: Int? = null,
    val seasonEndDay: Int? = null,
    val active: Boolean? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)
```

- [ ] **Step 4: Write the service**

Create `backend/src/main/kotlin/app/verdant/service/MaintenanceRuleService.kt`:

```kotlin
package app.verdant.service

import app.verdant.dto.CreateMaintenanceRuleRequest
import app.verdant.dto.MaintenanceRuleResponse
import app.verdant.dto.UpdateMaintenanceRuleRequest
import app.verdant.entity.Bed
import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import app.verdant.entity.MaintenanceTarget
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.MaintenanceRuleRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.time.LocalDate

@ApplicationScoped
class MaintenanceRuleService(
    private val ruleRepository: MaintenanceRuleRepository,
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository,
    private val areaService: GardenAreaService,
    private val lastDoneResolver: LastDoneResolver,
) {
    fun listRules(bedId: Long?, areaId: Long?, orgId: Long): List<MaintenanceRuleResponse> {
        if (bedId != null && areaId != null) {
            throw BadRequestException("Supply at most one of bedId and areaId")
        }
        val rules = when {
            bedId != null -> { requireBed(bedId, orgId); ruleRepository.findByBedId(bedId) }
            areaId != null -> { areaService.requireArea(areaId, orgId); ruleRepository.findByAreaId(areaId) }
            else -> ruleRepository.findByOrgId(orgId)
        }
        return rules.map { it.toResponse(orgId) }
    }

    fun createRule(request: CreateMaintenanceRuleRequest, orgId: Long): MaintenanceRuleResponse {
        val target = resolveTarget(request.bedId, request.gardenAreaId)
        val activity = parseActivity(request.activityType, target)
        validateSeason(
            request.seasonStartMonth, request.seasonStartDay,
            request.seasonEndMonth, request.seasonEndDay,
        )
        when (target) {
            MaintenanceTarget.BED -> requireBed(request.bedId!!, orgId)
            MaintenanceTarget.GARDEN_AREA -> areaService.requireArea(request.gardenAreaId!!, orgId)
        }

        val saved = ruleRepository.persist(
            MaintenanceRule(
                orgId = orgId,
                bedId = request.bedId,
                gardenAreaId = request.gardenAreaId,
                activity = activity,
                intervalDays = request.intervalDays,
                anchorDate = request.anchorDate,
                seasonStartMonth = request.seasonStartMonth,
                seasonStartDay = request.seasonStartDay,
                seasonEndMonth = request.seasonEndMonth,
                seasonEndDay = request.seasonEndDay,
                notes = request.notes,
            )
        )
        return saved.toResponse(orgId)
    }

    fun updateRule(id: Long, request: UpdateMaintenanceRuleRequest, orgId: Long): MaintenanceRuleResponse {
        val rule = requireRule(id, orgId)
        val activity = request.activityType?.let { parseActivity(it, rule.target) } ?: rule.activity
        validateSeason(
            request.seasonStartMonth ?: rule.seasonStartMonth,
            request.seasonStartDay ?: rule.seasonStartDay,
            request.seasonEndMonth ?: rule.seasonEndMonth,
            request.seasonEndDay ?: rule.seasonEndDay,
        )
        val updated = rule.copy(
            activity = activity,
            intervalDays = request.intervalDays ?: rule.intervalDays,
            anchorDate = request.anchorDate ?: rule.anchorDate,
            seasonStartMonth = request.seasonStartMonth ?: rule.seasonStartMonth,
            seasonStartDay = request.seasonStartDay ?: rule.seasonStartDay,
            seasonEndMonth = request.seasonEndMonth ?: rule.seasonEndMonth,
            seasonEndDay = request.seasonEndDay ?: rule.seasonEndDay,
            active = request.active ?: rule.active,
            notes = request.notes ?: rule.notes,
        )
        ruleRepository.update(updated)
        return updated.toResponse(orgId)
    }

    fun deleteRule(id: Long, orgId: Long) {
        requireRule(id, orgId)
        ruleRepository.delete(id)
    }

    private fun requireRule(id: Long, orgId: Long): MaintenanceRule {
        val rule = ruleRepository.findById(id) ?: throw NotFoundException("Rule not found")
        if (rule.orgId != orgId) throw NotFoundException("Rule not found")
        // Re-check the target so a rule can never outlive its owner's org.
        when (rule.target) {
            MaintenanceTarget.BED -> requireBed(rule.bedId!!, orgId)
            MaintenanceTarget.GARDEN_AREA -> areaService.requireArea(rule.gardenAreaId!!, orgId)
        }
        return rule
    }

    private fun requireBed(bedId: Long, orgId: Long): Bed {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Bed not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        return bed
    }

    private fun resolveTarget(bedId: Long?, areaId: Long?): MaintenanceTarget = when {
        bedId != null && areaId != null -> throw BadRequestException("A rule targets a bed or an area, not both")
        bedId != null -> MaintenanceTarget.BED
        areaId != null -> MaintenanceTarget.GARDEN_AREA
        else -> throw BadRequestException("A rule must target a bed or an area")
    }

    private fun parseActivity(value: String, target: MaintenanceTarget): MaintenanceActivity {
        val activity = runCatching { MaintenanceActivity.parse(value) }
            .getOrElse { throw BadRequestException("Unknown activity: $value") }
        if (!activity.appliesTo(target)) {
            throw BadRequestException("Activity $value does not apply to $target")
        }
        return activity
    }

    private fun validateSeason(startMonth: Int?, startDay: Int?, endMonth: Int?, endDay: Int?) {
        val present = listOfNotNull(startMonth, startDay, endMonth, endDay).size
        if (present != 0 && present != 4) {
            throw BadRequestException("A season window needs all four bounds, or none")
        }
        if (present == 4) {
            runCatching {
                java.time.MonthDay.of(startMonth!!, startDay!!)
                java.time.MonthDay.of(endMonth!!, endDay!!)
            }.getOrElse { throw BadRequestException("Invalid season window bounds") }
        }
    }

    private fun MaintenanceRule.toResponse(orgId: Long): MaintenanceRuleResponse {
        val lastDone = lastDoneResolver.resolve(this)
        val nextDue = MaintenanceDueCalculator.dueDate(
            lastDone, intervalDays, MaintenanceDueCalculator.windowOf(this), LocalDate.now(),
        )
        return MaintenanceRuleResponse(
            id = id!!,
            bedId = bedId,
            bedName = bedId?.let { bedRepository.findById(it)?.name },
            gardenAreaId = gardenAreaId,
            gardenAreaName = gardenAreaId?.let { runCatching { areaService.requireArea(it, orgId).name }.getOrNull() },
            activityType = activity.name,
            intervalDays = intervalDays,
            anchorDate = anchorDate,
            seasonStartMonth = seasonStartMonth,
            seasonStartDay = seasonStartDay,
            seasonEndMonth = seasonEndMonth,
            seasonEndDay = seasonEndDay,
            active = active,
            notes = notes,
            lastDoneDate = lastDone,
            nextDueDate = nextDue,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
```

`anchorDate` uses `?:` coalescing on update, matching how `UpdateBedRequest` fields behave. That means an anchor date cannot be cleared once set — only replaced. This mirrors the existing convention; if clearing is ever needed it is a separate change, as the `earliestDate` comment in `ScheduledTaskService.updateTask` records for that field.

- [ ] **Step 5: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.MaintenanceRuleServiceTest" --no-daemon --console=plain
```
Expected: PASS, 9 tests.

- [ ] **Step 6: Write the resource**

Create `backend/src/main/kotlin/app/verdant/resource/MaintenanceRuleResource.kt`:

```kotlin
package app.verdant.resource

import app.verdant.dto.CreateMaintenanceRuleRequest
import app.verdant.dto.UpdateMaintenanceRuleRequest
import app.verdant.filter.OrgContext
import app.verdant.service.MaintenanceRuleService
import io.quarkus.security.Authenticated
import jakarta.validation.Valid
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/api/maintenance-rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
class MaintenanceRuleResource(
    private val ruleService: MaintenanceRuleService,
    private val orgContext: OrgContext,
) {
    @GET
    fun list(@QueryParam("bedId") bedId: Long?, @QueryParam("areaId") areaId: Long?) =
        ruleService.listRules(bedId, areaId, orgContext.orgId)

    @POST
    fun create(@Valid request: CreateMaintenanceRuleRequest): Response {
        val rule = ruleService.createRule(request, orgContext.orgId)
        return Response.status(Response.Status.CREATED).entity(rule).build()
    }

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: Long, @Valid request: UpdateMaintenanceRuleRequest) =
        ruleService.updateRule(id, request, orgContext.orgId)

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: Long): Response {
        ruleService.deleteRule(id, orgContext.orgId)
        return Response.noContent().build()
    }
}
```

- [ ] **Step 7: Run the full backend suite**

Run: `./scripts/run-tests.sh backend`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/dto/MaintenanceDtos.kt \
        backend/src/main/kotlin/app/verdant/service/MaintenanceRuleService.kt \
        backend/src/main/kotlin/app/verdant/resource/MaintenanceRuleResource.kt \
        backend/src/test/kotlin/app/verdant/service/MaintenanceRuleServiceTest.kt
git commit -m "feat(backend): maintenance rule CRUD API"
```

---

### Task 9: The daily scheduler

**Files:**
- Create: `backend/src/main/kotlin/app/verdant/service/MaintenanceScheduler.kt`
- Test: `backend/src/test/kotlin/app/verdant/service/MaintenanceSchedulerTest.kt`

**Interfaces:**
- Consumes: `MaintenanceRuleRepository.findActiveWithoutOpenTask` (Task 4), `LastDoneResolver.resolve` (Task 7), `MaintenanceDueCalculator.dueDate` (Task 6), `ScheduledTaskRepository.persist` (Task 5).
- Produces: `MaintenanceScheduler` with `@Scheduled fun materialiseDueTasks()` and `internal fun run(today: LocalDate): Int` returning the number of tasks created.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/kotlin/app/verdant/service/MaintenanceSchedulerTest.kt`:

```kotlin
package app.verdant.service

import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import app.verdant.entity.ScheduledTask
import app.verdant.repository.MaintenanceRuleRepository
import app.verdant.repository.ScheduledTaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.MonthDay

class MaintenanceSchedulerTest {

    private val rules: MaintenanceRuleRepository = mock()
    private val lastDone: LastDoneResolver = mock()
    private val tasks: ScheduledTaskRepository = mock()
    private val scheduler = MaintenanceScheduler(rules, lastDone, tasks)

    private val orgId = 10L
    private val areaId = 5L
    private val today = LocalDate.of(2026, 6, 25)

    private fun rule(
        id: Long = 1L,
        intervalDays: Int = 21,
        seasonStartMonth: Int? = null,
        seasonStartDay: Int? = null,
        seasonEndMonth: Int? = null,
        seasonEndDay: Int? = null,
    ) = MaintenanceRule(
        id = id, orgId = orgId, gardenAreaId = areaId,
        activity = MaintenanceActivity.WEED, intervalDays = intervalDays,
        seasonStartMonth = seasonStartMonth, seasonStartDay = seasonStartDay,
        seasonEndMonth = seasonEndMonth, seasonEndDay = seasonEndDay,
    )

    @Test
    fun `a rule due today produces one task`() {
        val r = rule()
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(LocalDate.of(2026, 6, 4))
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        assertEquals(1, scheduler.run(today))

        val captor = argumentCaptor<ScheduledTask>()
        verify(tasks).persist(captor.capture())
        val created = captor.firstValue
        assertEquals("WEED", created.activityType)
        assertEquals(areaId, created.gardenAreaId)
        assertEquals(1L, created.maintenanceRuleId)
        assertEquals(1, created.targetCount)
        assertEquals(1, created.remainingCount)
        assertEquals(LocalDate.of(2026, 6, 25), created.deadline)
        assertEquals(LocalDate.of(2026, 6, 25), created.earliestDate)
    }

    @Test
    fun `a rule not yet due produces nothing`() {
        val r = rule()
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(LocalDate.of(2026, 6, 20))

        assertEquals(0, scheduler.run(today))
        verify(tasks, never()).persist(any())
    }

    @Test
    fun `an overdue rule uses the due date as the deadline not today`() {
        val r = rule()
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(LocalDate.of(2026, 5, 1))
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        scheduler.run(today)

        val captor = argumentCaptor<ScheduledTask>()
        verify(tasks).persist(captor.capture())
        assertEquals(LocalDate.of(2026, 5, 22), captor.firstValue.deadline)
    }

    @Test
    fun `a rule never done is due immediately`() {
        val r = rule()
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(null)
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        assertEquals(1, scheduler.run(today))
    }

    @Test
    fun `a rule out of season produces nothing`() {
        val r = rule(seasonStartMonth = 11, seasonStartDay = 1, seasonEndMonth = 3, seasonEndDay = 31)
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(null)

        assertEquals(0, scheduler.run(today))
        verify(tasks, never()).persist(any())
    }

    @Test
    fun `a bed rule carries the bed id instead of the area id`() {
        val r = MaintenanceRule(
            id = 2L, orgId = orgId, bedId = 3L,
            activity = MaintenanceActivity.WATER, intervalDays = 7,
        )
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(r))
        whenever(lastDone.resolve(r)).thenReturn(null)
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        scheduler.run(today)

        val captor = argumentCaptor<ScheduledTask>()
        verify(tasks).persist(captor.capture())
        assertEquals(3L, captor.firstValue.bedId)
        assertEquals(null, captor.firstValue.gardenAreaId)
    }

    @Test
    fun `one failing rule does not stop the others`() {
        val bad = rule(id = 1L)
        val good = rule(id = 2L)
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(listOf(bad, good))
        whenever(lastDone.resolve(bad)).thenThrow(RuntimeException("boom"))
        whenever(lastDone.resolve(good)).thenReturn(null)
        whenever(tasks.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        assertEquals(1, scheduler.run(today))
    }

    @Test
    fun `a second run over an empty work list creates nothing`() {
        whenever(rules.findActiveWithoutOpenTask()).thenReturn(emptyList())
        assertEquals(0, scheduler.run(today))
        verify(tasks, never()).persist(any())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.MaintenanceSchedulerTest" --no-daemon --console=plain
```
Expected: FAIL — compilation error, `MaintenanceScheduler` is unresolved.

- [ ] **Step 3: Write the implementation**

Create `backend/src/main/kotlin/app/verdant/service/MaintenanceScheduler.kt`:

```kotlin
package app.verdant.service

import app.verdant.entity.ScheduledTask
import app.verdant.repository.MaintenanceRuleRepository
import app.verdant.repository.ScheduledTaskRepository
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 * Turns due maintenance rules into ordinary scheduled tasks once a day, so
 * recurring work shows up in the same task list as everything else.
 *
 * The only creator of rule-backed tasks. Completing a task does not chain
 * synchronously into the next one — the next run picks it up once the derived
 * last-done date has moved.
 */
@ApplicationScoped
class MaintenanceScheduler(
    private val rules: MaintenanceRuleRepository,
    private val lastDoneResolver: LastDoneResolver,
    private val tasks: ScheduledTaskRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 3 * * ?")
    fun materialiseDueTasks() {
        val created = run(LocalDate.now())
        if (created > 0) log.info("Maintenance scheduler created $created task(s)")
    }

    /**
     * Visible for testing so the date can be driven directly.
     * Returns how many tasks were created.
     */
    internal fun run(today: LocalDate): Int {
        var created = 0
        for (rule in rules.findActiveWithoutOpenTask()) {
            // A rule whose target vanished mid-run, or whose lookup fails, must
            // not stop the rest of the garden from being scheduled.
            runCatching {
                val due = MaintenanceDueCalculator.dueDate(
                    lastDone = lastDoneResolver.resolve(rule),
                    intervalDays = rule.intervalDays,
                    window = MaintenanceDueCalculator.windowOf(rule),
                    today = today,
                )
                if (due <= today) {
                    tasks.persist(
                        ScheduledTask(
                            orgId = rule.orgId,
                            speciesId = null,
                            bedId = rule.bedId,
                            gardenAreaId = rule.gardenAreaId,
                            maintenanceRuleId = rule.id,
                            activityType = rule.activity.name,
                            earliestDate = due,
                            deadline = due,
                            targetCount = 1,
                            remainingCount = 1,
                        )
                    )
                    created++
                }
            }.onFailure { log.warn("Maintenance rule ${rule.id} skipped: ${it.message}") }
        }
        return created
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.MaintenanceSchedulerTest" --no-daemon --console=plain
```
Expected: PASS, 8 tests.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/service/MaintenanceScheduler.kt \
        backend/src/test/kotlin/app/verdant/service/MaintenanceSchedulerTest.kt
git commit -m "feat(backend): daily job materialising due maintenance tasks"
```

---

### Task 10: Completing a maintenance task records the work

Closes the loop: without this the derived clock never moves and the same task is recreated forever.

**Files:**
- Modify: `backend/src/main/kotlin/app/verdant/service/ScheduledTaskService.kt` (`completePartially`, `BED_ACTIVITY_TYPES`, `createTask` validation)
- Test: `backend/src/test/kotlin/app/verdant/service/ScheduledTaskServiceTest.kt`

**Interfaces:**
- Consumes: `MaintenanceActivity`, `MaintenanceTarget` (Task 1); `GardenAreaEventRepository` (Task 2); `BedEventRepository`; `MaintenanceRuleRepository` (Task 4); `ScheduledTask.maintenanceRuleId` (Task 5).
- Produces: no new public API — behaviour change only.

- [ ] **Step 1: Write the failing test**

Add to `backend/src/test/kotlin/app/verdant/service/ScheduledTaskServiceTest.kt`:

```kotlin
    @Test
    fun `completing a rule-backed area task logs an area event`() {
        val task = ScheduledTask(
            id = 42L, orgId = orgId, speciesId = null, gardenAreaId = 5L,
            maintenanceRuleId = 7L, activityType = "WEED",
            deadline = deadline, targetCount = 1, remainingCount = 1,
        )
        whenever(taskRepository.findById(42L)).thenReturn(task, task.copy(remainingCount = 0))
        whenever(gardenAreaEventRepository.persist(any()))
            .thenAnswer { it.arguments[0] as GardenAreaEvent }

        service.completePartially(42L, speciesId = null, processedCount = 1, orgId = orgId)

        val captor = argumentCaptor<GardenAreaEvent>()
        verify(gardenAreaEventRepository).persist(captor.capture())
        assertEquals(5L, captor.firstValue.gardenAreaId)
        assertEquals("WEED", captor.firstValue.eventType)
    }

    @Test
    fun `completing a rule-backed bed task logs a bed event`() {
        val task = ScheduledTask(
            id = 43L, orgId = orgId, speciesId = null, bedId = 3L,
            maintenanceRuleId = 8L, activityType = "WEED",
            deadline = deadline, targetCount = 1, remainingCount = 1,
        )
        whenever(taskRepository.findById(43L)).thenReturn(task, task.copy(remainingCount = 0))

        service.completePartially(43L, speciesId = null, processedCount = 1, orgId = orgId)

        val captor = argumentCaptor<BedEvent>()
        verify(bedEventRepository).persist(captor.capture())
        assertEquals(3L, captor.firstValue.bedId)
        assertEquals(PlantEventType.WEEDED, captor.firstValue.eventType)
    }

    @Test
    fun `a partially completed task logs nothing yet`() {
        val task = ScheduledTask(
            id = 44L, orgId = orgId, speciesId = null, gardenAreaId = 5L,
            maintenanceRuleId = 9L, activityType = "WEED",
            deadline = deadline, targetCount = 3, remainingCount = 3,
        )
        whenever(taskRepository.findById(44L)).thenReturn(task, task.copy(remainingCount = 2))

        service.completePartially(44L, speciesId = null, processedCount = 1, orgId = orgId)

        verify(gardenAreaEventRepository, never()).persist(any())
    }

    @Test
    fun `completing a hand-made bed task logs nothing`() {
        val task = ScheduledTask(
            id = 45L, orgId = orgId, speciesId = null, bedId = 3L,
            maintenanceRuleId = null, activityType = "WEED",
            deadline = deadline, targetCount = 1, remainingCount = 1,
        )
        whenever(taskRepository.findById(45L)).thenReturn(task, task.copy(remainingCount = 0))

        service.completePartially(45L, speciesId = null, processedCount = 1, orgId = orgId)

        verify(bedEventRepository, never()).persist(any())
    }

    @Test
    fun `an area-only activity is accepted when creating an area task`() {
        whenever(gardenAreaRepository.findById(5L)).thenReturn(
            GardenArea(id = 5L, gardenId = 1L, name = "Gång", category = GardenAreaCategory.WALKWAY)
        )
        whenever(gardenRepository.findById(1L)).thenReturn(Garden(id = 1L, name = "T", orgId = orgId))
        whenever(taskRepository.persist(any())).thenAnswer { it.arguments[0] as ScheduledTask }

        val result = service.createTask(
            CreateScheduledTaskRequest(
                gardenAreaId = 5L, activityType = "MOW", deadline = deadline, targetCount = 1,
            ),
            orgId,
        )

        assertEquals("MOW", result.activityType)
    }
```

Add `gardenAreaEventRepository`, `bedEventRepository`, `gardenAreaRepository`, and `maintenanceRuleRepository` mocks to the existing field block and pass them into the `ScheduledTaskService(...)` construction. Add the matching imports.

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.ScheduledTaskServiceTest" --no-daemon --console=plain
```
Expected: FAIL — `CreateScheduledTaskRequest` has no `gardenAreaId`, and the service constructor does not take the new repositories.

- [ ] **Step 3: Add `gardenAreaId` to the create request**

In `ScheduledTaskDtos.kt`, add to `CreateScheduledTaskRequest` after `bedId`:

```kotlin
    val gardenAreaId: Long? = null,
```

- [ ] **Step 4: Widen the activity validation**

In `ScheduledTaskService`, replace the hardcoded set:

```kotlin
    private val BED_ACTIVITY_TYPES = setOf("WATER", "FERTILIZE", "WEED")
```

with values derived from the enum, so the two never drift:

```kotlin
    private val BED_ACTIVITY_TYPES =
        MaintenanceActivity.forTarget(MaintenanceTarget.BED).map { it.name }.toSet()
    private val AREA_ACTIVITY_TYPES =
        MaintenanceActivity.forTarget(MaintenanceTarget.GARDEN_AREA).map { it.name }.toSet()
```

Then in `createTask`, directly after the existing `if (request.bedId != null) { ... }` branch, add the area branch:

```kotlin
        // Area-scoped maintenance tasks don't carry species either.
        if (request.gardenAreaId != null) {
            if (request.activityType !in AREA_ACTIVITY_TYPES)
                throw BadRequestException("activityType ${request.activityType} cannot target an area")
            val area = gardenAreaRepository.findById(request.gardenAreaId)
                ?: throw NotFoundException("Area not found")
            val garden = gardenRepository.findById(area.gardenId)
                ?: throw NotFoundException("Area not found")
            if (garden.orgId != orgId) throw NotFoundException("Area not found")

            val task = taskRepository.persist(
                ScheduledTask(
                    orgId = orgId,
                    speciesId = null,
                    gardenAreaId = request.gardenAreaId,
                    activityType = request.activityType,
                    earliestDate = request.earliestDate,
                    deadline = request.deadline,
                    targetCount = request.targetCount!!,
                    remainingCount = request.targetCount!!,
                    notes = request.notes,
                    seasonId = request.seasonId,
                    successionScheduleId = null,
                    originGroupId = null,
                )
            )
            return buildResponses(listOf(task)).first()
        }
```

Also reject a request that names both a bed and an area, next to the existing `TODO` guards:

```kotlin
        if (request.bedId != null && request.gardenAreaId != null) {
            throw BadRequestException("A task targets a bed or an area, not both")
        }
```

- [ ] **Step 5: Log the work on completion**

In `completePartially`, extend the `when` so an area task skips species validation the way a bed task does:

```kotlin
            task.bedId != null || task.gardenAreaId != null -> {
                // Bed- and area-scoped maintenance tasks don't carry species.
            }
```

Then, after `val updated = taskRepository.findById(taskId)!!`, add:

```kotlin
        // A rule-backed task that has just been finished records the work, which
        // is what moves the rule's derived clock. Without this the scheduler
        // would recreate the same task tomorrow.
        if (updated.maintenanceRuleId != null && updated.remainingCount <= 0) {
            recordMaintenance(updated)
        }
```

and the private helper:

```kotlin
    private fun recordMaintenance(task: ScheduledTask) {
        val activity = runCatching { MaintenanceActivity.parse(task.activityType) }.getOrNull() ?: return
        val today = java.time.LocalDate.now()
        when {
            task.gardenAreaId != null -> gardenAreaEventRepository.persist(
                GardenAreaEvent(
                    gardenAreaId = task.gardenAreaId,
                    eventType = activity.name,
                    eventDate = today,
                    notes = task.notes,
                )
            )
            task.bedId != null -> activity.bedEventType?.let { eventType ->
                bedEventRepository.persist(
                    BedEvent(bedId = task.bedId, eventType = eventType, eventDate = today, notes = task.notes)
                )
            }
        }
    }
```

Add `gardenAreaEventRepository: GardenAreaEventRepository`, `bedEventRepository: BedEventRepository`, and `gardenAreaRepository: GardenAreaRepository` to the constructor, with the matching imports.

- [ ] **Step 6: Run test to verify it passes**

Run:
```bash
docker compose -f docker-compose.test.yml run --rm backend-tests \
  gradle test --tests "app.verdant.service.ScheduledTaskServiceTest" --no-daemon --console=plain
```
Expected: PASS — the five new tests plus every pre-existing one.

- [ ] **Step 7: Run the full backend suite**

Run: `./scripts/run-tests.sh backend`
Expected: PASS, everything green. This is the gate before the web plan starts.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/kotlin/app/verdant/dto/ScheduledTaskDtos.kt \
        backend/src/main/kotlin/app/verdant/service/ScheduledTaskService.kt \
        backend/src/test/kotlin/app/verdant/service/ScheduledTaskServiceTest.kt
git commit -m "feat(backend): completing a maintenance task records the work"
```

---

## What this plan deliberately leaves out

- Anything in `web/`, `android/`, or `admin/`. Those are the next two plans.
- Drawing or rendering `boundary_json`. The column and API carry it; no client draws it.
- Seeding default rules for any bed or area.
- Changes to the existing `POST /api/beds/{id}/weed` and `/water` endpoints.

## One deviation from the spec's testing section

The spec asks for org-isolation coverage "in a resource test". Resource tests in
this repo (see `WeatherResourceTest`) construct the resource with a **mocked**
service, so an org check that lives in the service would not actually be
exercised by one. The isolation tests therefore sit in `GardenAreaServiceTest`
and `MaintenanceRuleServiceTest`, against the real `requireArea` / `requireBed`
logic. Same coverage, at the layer that owns the check.
