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
