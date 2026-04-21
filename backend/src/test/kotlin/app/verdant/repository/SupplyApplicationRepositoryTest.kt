package app.verdant.repository

import app.verdant.dto.CreateSupplyApplicationRequest
import app.verdant.entity.Bed
import app.verdant.entity.Garden
import app.verdant.entity.Organization
import app.verdant.entity.Plant
import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantStatus
import app.verdant.entity.SupplyApplication
import app.verdant.entity.SupplyApplicationScope
import app.verdant.entity.SupplyCategory
import app.verdant.entity.SupplyInventory
import app.verdant.entity.SupplyType
import app.verdant.entity.SupplyUnit
import app.verdant.entity.User
import app.verdant.service.SupplyApplicationService
import io.agroal.api.AgroalDataSource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@QuarkusTest
class SupplyApplicationRepositoryTest {

    @Inject lateinit var repo: SupplyApplicationRepository
    @Inject lateinit var orgRepo: OrganizationRepository
    @Inject lateinit var gardenRepo: GardenRepository
    @Inject lateinit var bedRepo: BedRepository
    @Inject lateinit var supplyTypeRepo: SupplyTypeRepository
    @Inject lateinit var supplyInventoryRepo: SupplyInventoryRepository
    @Inject lateinit var plantRepo: PlantRepository
    @Inject lateinit var plantEventRepo: PlantEventRepository
    @Inject lateinit var userRepo: UserRepository
    @Inject lateinit var service: SupplyApplicationService
    @Inject lateinit var ds: AgroalDataSource

    private var userId: Long = 0
    private var orgId: Long = 0
    private var gardenId: Long = 0
    private var bedId: Long = 0
    private var supplyTypeId: Long = 0
    private var inventoryId: Long = 0

    @BeforeEach
    fun setup() {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM supply_application").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM plant_event").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM plant_workflow_progress").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM plant").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM supply_inventory").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM supply_type").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM bed").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM garden").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM organization").use { it.executeUpdate() }
            conn.prepareStatement("DELETE FROM app_user").use { it.executeUpdate() }
        }

        val user = userRepo.persist(User(email = "test@test.com", displayName = "Test User"))
        userId = user.id!!

        val org = orgRepo.persist(Organization(name = "Test Org"))
        orgId = org.id!!

        val garden = gardenRepo.persist(Garden(name = "Test Garden", orgId = orgId))
        gardenId = garden.id!!

        val bed = bedRepo.persist(Bed(name = "Bed A", gardenId = gardenId))
        bedId = bed.id!!

        val supplyType = supplyTypeRepo.persist(
            SupplyType(orgId = orgId, name = "Fertilizer A", category = SupplyCategory.FERTILIZER, unit = SupplyUnit.KILOGRAMS)
        )
        supplyTypeId = supplyType.id!!

        val inventory = supplyInventoryRepo.persist(
            SupplyInventory(orgId = orgId, supplyTypeId = supplyTypeId, quantity = BigDecimal("100.00"))
        )
        inventoryId = inventory.id!!
    }

    @Test
    fun `insert returns row with id set and findById returns it`() {
        val app = SupplyApplication(
            orgId = orgId,
            bedId = bedId,
            supplyInventoryId = inventoryId,
            supplyTypeId = supplyTypeId,
            quantity = BigDecimal("10.00"),
            targetScope = SupplyApplicationScope.BED,
            appliedBy = userId,
            notes = "Test note",
        )

        val saved = repo.insert(app)

        assertNotNull(saved.id)
        assertTrue(saved.id!! > 0)

        val found = repo.findById(saved.id!!)
        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
        assertEquals(orgId, found.orgId)
        assertEquals(bedId, found.bedId)
        assertEquals(inventoryId, found.supplyInventoryId)
        assertEquals(supplyTypeId, found.supplyTypeId)
        assertEquals(0, BigDecimal("10.00").compareTo(found.quantity))
        assertEquals(SupplyApplicationScope.BED, found.targetScope)
        assertEquals(userId, found.appliedBy)
        assertEquals("Test note", found.notes)
        assertNull(found.workflowStepId)
    }

    @Test
    fun `findById returns null for nonexistent id`() {
        val result = repo.findById(99999L)
        assertNull(result)
    }

    @Test
    fun `findByBed orders by applied_at DESC and respects limit`() {
        val base = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val oldest = base.minus(2, ChronoUnit.HOURS)
        val middle = base.minus(1, ChronoUnit.HOURS)
        val newest = base

        fun insertWithTime(appliedAt: Instant): SupplyApplication {
            val app = SupplyApplication(
                orgId = orgId,
                bedId = bedId,
                supplyInventoryId = inventoryId,
                supplyTypeId = supplyTypeId,
                quantity = BigDecimal("5.00"),
                targetScope = SupplyApplicationScope.BED,
                appliedAt = appliedAt,
                appliedBy = userId,
            )
            return repo.insert(app)
        }

        val a1 = insertWithTime(oldest)
        val a2 = insertWithTime(middle)
        val a3 = insertWithTime(newest)

        val results = repo.findByBed(bedId, limit = 2)

        assertEquals(2, results.size)
        assertEquals(a3.id, results[0].id)
        assertEquals(a2.id, results[1].id)
    }

    @Test
    fun `findByGarden joins via bed and returns only applications from that garden`() {
        // Set up a second garden with its own bed and application
        val org2 = orgRepo.persist(Organization(name = "Other Org"))
        val garden2 = gardenRepo.persist(Garden(name = "Other Garden", orgId = org2.id!!))
        val bed2 = bedRepo.persist(Bed(name = "Other Bed", gardenId = garden2.id!!))
        val supplyType2 = supplyTypeRepo.persist(
            SupplyType(orgId = org2.id, name = "Other Fertilizer", category = SupplyCategory.FERTILIZER, unit = SupplyUnit.LITERS)
        )
        val inventory2 = supplyInventoryRepo.persist(
            SupplyInventory(orgId = org2.id, supplyTypeId = supplyType2.id!!, quantity = BigDecimal("50.00"))
        )

        val appInGarden1 = repo.insert(
            SupplyApplication(
                orgId = orgId,
                bedId = bedId,
                supplyInventoryId = inventoryId,
                supplyTypeId = supplyTypeId,
                quantity = BigDecimal("10.00"),
                targetScope = SupplyApplicationScope.BED,
                appliedBy = userId,
            )
        )

        val appInGarden2 = repo.insert(
            SupplyApplication(
                orgId = org2.id,
                bedId = bed2.id!!,
                supplyInventoryId = inventory2.id!!,
                supplyTypeId = supplyType2.id,
                quantity = BigDecimal("5.00"),
                targetScope = SupplyApplicationScope.BED,
                appliedBy = userId,
            )
        )

        val garden1Results = repo.findByGarden(gardenId)
        assertEquals(1, garden1Results.size)
        assertEquals(appInGarden1.id, garden1Results[0].id)

        val garden2Results = repo.findByGarden(garden2.id!!)
        assertEquals(1, garden2Results.size)
        assertEquals(appInGarden2.id, garden2Results[0].id)
    }

    @Test
    fun `findPlantIdsForApplication returns plant ids from plant_event rows with matching supply_application_id`() {
        val application = repo.insert(
            SupplyApplication(
                orgId = orgId,
                bedId = bedId,
                supplyInventoryId = inventoryId,
                supplyTypeId = supplyTypeId,
                quantity = BigDecimal("15.00"),
                targetScope = SupplyApplicationScope.PLANTS,
                appliedBy = userId,
            )
        )

        val plant1 = plantRepo.persist(Plant(name = "Plant 1", orgId = orgId, bedId = bedId, status = PlantStatus.GROWING))
        val plant2 = plantRepo.persist(Plant(name = "Plant 2", orgId = orgId, bedId = bedId, status = PlantStatus.GROWING))
        val plant3 = plantRepo.persist(Plant(name = "Plant 3", orgId = orgId, bedId = bedId, status = PlantStatus.GROWING))
        val unrelatedPlant = plantRepo.persist(Plant(name = "Plant 4", orgId = orgId, bedId = bedId, status = PlantStatus.GROWING))

        // Three events linked to the application
        plantEventRepo.persist(PlantEvent(plantId = plant1.id!!, eventType = PlantEventType.APPLIED_SUPPLY, supplyApplicationId = application.id))
        plantEventRepo.persist(PlantEvent(plantId = plant2.id!!, eventType = PlantEventType.APPLIED_SUPPLY, supplyApplicationId = application.id))
        plantEventRepo.persist(PlantEvent(plantId = plant3.id!!, eventType = PlantEventType.APPLIED_SUPPLY, supplyApplicationId = application.id))

        // One unrelated event
        plantEventRepo.persist(PlantEvent(plantId = unrelatedPlant.id!!, eventType = PlantEventType.NOTE))

        val plantIds = repo.findPlantIdsForApplication(application.id!!)

        assertEquals(3, plantIds.size)
        assertTrue(plantIds.contains(plant1.id))
        assertTrue(plantIds.contains(plant2.id))
        assertTrue(plantIds.contains(plant3.id))
        assertTrue(!plantIds.contains(unrelatedPlant.id))
    }

    @Test
    fun `create with quantity exceeding inventory rolls back transaction`() {
        // Seed a fresh inventory row with quantity = 10
        val inventory = supplyInventoryRepo.persist(
            SupplyInventory(orgId = orgId, supplyTypeId = supplyTypeId, quantity = BigDecimal("10.00"))
        )

        val request = CreateSupplyApplicationRequest(
            bedId = bedId,
            supplyInventoryId = inventory.id!!,
            quantity = BigDecimal("50.00"),
            targetScope = "BED",
        )

        assertThrows(BadRequestException::class.java) {
            service.create(request, orgId, userId)
        }

        // supply_application table must still be empty
        val appCount = ds.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM supply_application").use { ps ->
                ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
            }
        }
        assertEquals(0L, appCount, "supply_application should have no rows after rollback")

        // supply_inventory quantity must remain 10
        val refreshed = supplyInventoryRepo.findById(inventory.id!!)!!
        assertEquals(0, BigDecimal("10.00").compareTo(refreshed.quantity), "inventory quantity must remain 10 after rollback")
    }
}
