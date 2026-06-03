package app.verdant.repository

import app.verdant.entity.Bed
import app.verdant.entity.Garden
import app.verdant.entity.Organization
import app.verdant.entity.Plant
import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantStatus
import app.verdant.entity.Season
import io.agroal.api.AgroalDataSource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.IsoFields

/**
 * Aggregation correctness for the harvest-analytics queries added for the
 * dashboard / bed / garden harvest cards. Seeds two orgs so org-scoping is
 * exercised, and harvest events across ISO weeks / seasons / beds.
 */
@QuarkusTest
class PlantEventHarvestStatsTest {

    @Inject lateinit var repo: PlantEventRepository
    @Inject lateinit var orgRepo: OrganizationRepository
    @Inject lateinit var gardenRepo: GardenRepository
    @Inject lateinit var bedRepo: BedRepository
    @Inject lateinit var seasonRepo: SeasonRepository
    @Inject lateinit var plantRepo: PlantRepository
    @Inject lateinit var ds: AgroalDataSource

    private var orgId = 0L
    private var otherOrgId = 0L
    private var gardenId = 0L
    private var bedAId = 0L
    private var bedBId = 0L
    private var season2025Id = 0L
    private var season2024Id = 0L

    @BeforeEach
    fun setup() {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM plant_event").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM plant_workflow_progress").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM plant").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM bed").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM season").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM garden").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM organization").use { it.executeUpdate() }
        }

        orgId = orgRepo.persist(Organization(name = "Org")).id!!
        otherOrgId = orgRepo.persist(Organization(name = "Other Org")).id!!
        gardenId = gardenRepo.persist(Garden(name = "Garden", orgId = orgId)).id!!
        bedAId = bedRepo.persist(Bed(name = "Bed A", gardenId = gardenId)).id!!
        bedBId = bedRepo.persist(Bed(name = "Bed B", gardenId = gardenId)).id!!
        season2025Id = seasonRepo.persist(Season(orgId = orgId, name = "2025", year = 2025)).id!!
        season2024Id = seasonRepo.persist(Season(orgId = orgId, name = "2024", year = 2024)).id!!
    }

    /** Seeds a plant + a HARVESTED event in one statement and returns the plant id. */
    private fun seedHarvest(
        orgId: Long,
        seasonId: Long?,
        bedId: Long?,
        date: LocalDate,
        stems: Int?,
        eventType: PlantEventType = PlantEventType.HARVESTED,
    ) {
        val plant = plantRepo.persist(
            Plant(name = "p", orgId = orgId, bedId = bedId, seasonId = seasonId, status = PlantStatus.GROWING)
        )
        repo.persist(PlantEvent(plantId = plant.id!!, eventType = eventType, eventDate = date, stemCount = stems))
    }

    private fun isoWeekOf(date: LocalDate) = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    @Test
    fun `weekly buckets group by ISO week and sum stems per week`() {
        val wk32a = LocalDate.of(2025, 8, 4)   // Monday, ISO week 32
        val wk32b = LocalDate.of(2025, 8, 6)   // same ISO week
        val wk20 = LocalDate.of(2025, 5, 12)   // earlier ISO week

        seedHarvest(orgId, season2025Id, bedAId, wk32a, 10)
        seedHarvest(orgId, season2025Id, bedAId, wk32b, 5)
        seedHarvest(orgId, season2025Id, bedBId, wk20, 7)

        val buckets = repo.harvestWeeklyBucketsBySeason(orgId, season2025Id)

        assertEquals(2, buckets.size)
        // Ordered by iso_week ascending
        assertEquals(isoWeekOf(wk20), buckets[0].isoWeek)
        assertEquals(7, buckets[0].stems)
        assertEquals(isoWeekOf(wk32a), buckets[1].isoWeek)
        assertEquals(15, buckets[1].stems, "two harvests in the same ISO week sum")
    }

    @Test
    fun `weekly buckets exclude other orgs, other seasons and non-harvest events`() {
        seedHarvest(orgId, season2025Id, bedAId, LocalDate.of(2025, 8, 4), 10)
        seedHarvest(otherOrgId, season2025Id, bedAId, LocalDate.of(2025, 8, 4), 99)            // other org
        seedHarvest(orgId, season2024Id, bedAId, LocalDate.of(2024, 8, 4), 99)                 // other season
        seedHarvest(orgId, season2025Id, bedAId, LocalDate.of(2025, 8, 11), 99, PlantEventType.NOTE) // not a harvest

        val buckets = repo.harvestWeeklyBucketsBySeason(orgId, season2025Id)

        assertEquals(1, buckets.size)
        assertEquals(10, buckets[0].stems)
    }

    @Test
    fun `weekly buckets skip weeks with no stems`() {
        seedHarvest(orgId, season2025Id, bedAId, LocalDate.of(2025, 8, 4), null) // null stems → no bucket
        seedHarvest(orgId, season2025Id, bedAId, LocalDate.of(2025, 8, 11), 0)   // zero stems → no bucket
        seedHarvest(orgId, season2025Id, bedAId, LocalDate.of(2025, 8, 18), 4)

        val buckets = repo.harvestWeeklyBucketsBySeason(orgId, season2025Id)

        assertEquals(1, buckets.size)
        assertEquals(4, buckets[0].stems)
    }

    @Test
    fun `totalStemsByOrgYear sums across seasons of that year and excludes other orgs`() {
        seedHarvest(orgId, season2024Id, bedAId, LocalDate.of(2024, 6, 1), 12)
        seedHarvest(orgId, season2025Id, bedAId, LocalDate.of(2025, 6, 1), 50)   // different year
        seedHarvest(otherOrgId, season2024Id, bedAId, LocalDate.of(2024, 6, 1), 99) // other org, but season belongs to orgId anyway

        assertEquals(12, repo.totalStemsByOrgYear(orgId, 2024))
        assertEquals(0, repo.totalStemsByOrgYear(orgId, 2023))
    }

    @Test
    fun `totalStemsByBed scopes to bed and optional season`() {
        seedHarvest(orgId, season2025Id, bedAId, LocalDate.of(2025, 6, 1), 10)
        seedHarvest(orgId, season2024Id, bedAId, LocalDate.of(2024, 6, 1), 3)
        seedHarvest(orgId, season2025Id, bedBId, LocalDate.of(2025, 6, 1), 99)

        assertEquals(13, repo.totalStemsByBed(bedAId, null), "all seasons")
        assertEquals(10, repo.totalStemsByBed(bedAId, season2025Id), "season-scoped")
        assertEquals(99, repo.totalStemsByBed(bedBId, null))
    }

    @Test
    fun `totalStemsByGarden sums across the garden's beds and respects season filter`() {
        seedHarvest(orgId, season2025Id, bedAId, LocalDate.of(2025, 6, 1), 10)
        seedHarvest(orgId, season2025Id, bedBId, LocalDate.of(2025, 6, 1), 20)
        seedHarvest(orgId, season2024Id, bedAId, LocalDate.of(2024, 6, 1), 5)

        // A plant with no bed (in a tray) must not count toward the garden total.
        seedHarvest(orgId, season2025Id, null, LocalDate.of(2025, 6, 1), 1000)

        assertEquals(35, repo.totalStemsByGarden(gardenId, null), "all seasons, both beds")
        assertEquals(30, repo.totalStemsByGarden(gardenId, season2025Id), "season-scoped")
    }

    @Test
    fun `empty harvest yields no buckets and zero totals`() {
        assertEquals(0, repo.harvestWeeklyBucketsBySeason(orgId, season2025Id).size)
        assertEquals(0, repo.totalStemsByOrgYear(orgId, 2025))
        assertEquals(0, repo.totalStemsByBed(bedAId, null))
        assertEquals(0, repo.totalStemsByGarden(gardenId, null))
        assertNull(repo.harvestWeeklyBucketsBySeason(orgId, season2025Id).maxByOrNull { it.stems })
    }
}
