package app.verdant.service

import app.verdant.dto.BatchSowRequest
import app.verdant.dto.CreatePlantEventRequest
import app.verdant.dto.DeleteSpeciesEventRequest
import app.verdant.entity.MovementKind
import app.verdant.entity.Organization
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantStatus
import app.verdant.entity.SeedInventory
import app.verdant.entity.Species
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.PlantRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SeedStockMovementRepository
import app.verdant.repository.SpeciesRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class SowingCorrectionTest {

    @Inject lateinit var plantService: PlantService
    @Inject lateinit var plants: PlantRepository
    @Inject lateinit var inventory: SeedInventoryRepository
    @Inject lateinit var movements: SeedStockMovementRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var species: SpeciesRepository

    private val sownOn = LocalDate.of(2026, 10, 25)

    private data class Fixture(val orgId: Long, val speciesId: Long)

    private fun fixture(): Fixture {
        val orgId = orgs.persist(Organization(name = "Sowing correction org")).id!!
        val speciesId = species.persist(Species(commonName = "Sowing correction chili")).id!!
        return Fixture(orgId, speciesId)
    }

    private fun batch(f: Fixture, quantity: Int) =
        inventory.persist(SeedInventory(orgId = f.orgId, speciesId = f.speciesId, quantity = quantity, costPerUnitCents = 1510)).id!!

    private fun sow(f: Fixture, count: Int, batchId: Long) =
        plantService.batchSow(
            BatchSowRequest(speciesId = f.speciesId, name = "Naga", seedCount = count, seedBatchId = batchId,
                allowShortfall = true, plantedDate = sownOn),
            f.orgId,
        )

    private fun removeSowings(f: Fixture, count: Int) =
        plantService.deleteSpeciesEvent(
            f.orgId, f.speciesId,
            DeleteSpeciesEventRequest(eventType = "SEEDED", eventDate = sownOn, count = count, currentStatus = "SEEDED", trayOnly = true),
        )

    private fun remainingPlants(f: Fixture) = plants.findByOrgId(f.orgId, limit = 1000)

    @Test
    fun `removing sowings deletes the plants and returns their seed to the batch`() {
        val f = fixture()
        val batchId = batch(f, 30)
        sow(f, 10, batchId)

        val result = removeSowings(f, 4)

        assertEquals(4, result.plantsDeleted)
        assertEquals(4, result.seedsReturned)
        assertEquals(6, remainingPlants(f).size)
        assertEquals(24, inventory.findById(batchId)!!.quantity)
        val correction = movements.findBySeedInventoryId(batchId).single { it.correctsMovementId != null }
        assertEquals(MovementKind.SOWN, correction.kind)
        assertEquals(-4, correction.quantity)
        assertEquals(sownOn, correction.occurredOn)
        assertEquals(6, movements.findBySeedInventoryId(batchId).filter { it.kind == MovementKind.SOWN }.sumOf { it.quantity })
    }

    @Test
    fun `a double registration beyond the batch is undone without inventing seed`() {
        val f = fixture()
        val batchId = batch(f, 15)
        val first = sow(f, 16, batchId)
        sow(f, 15, batchId)

        val result = removeSowings(f, 16)

        assertEquals(16, result.plantsDeleted)
        assertEquals(0, result.seedsReturned)
        assertEquals(0, inventory.findById(batchId)!!.quantity)
        assertEquals(first.plantIds.take(15).toSet(), remainingPlants(f).map { it.id }.toSet())
        assertEquals(15, movements.findBySeedInventoryId(batchId).filter { it.kind == MovementKind.SOWN }.sumOf { it.quantity })
    }

    @Test
    fun `shortfall unwound by an earlier correction is not unwound again`() {
        val f = fixture()
        val batchId = batch(f, 8)
        sow(f, 10, batchId)

        assertEquals(1, removeSowings(f, 3).seedsReturned)
        assertEquals(3, removeSowings(f, 3).seedsReturned)
        assertEquals(4, inventory.findById(batchId)!!.quantity)
    }

    @Test
    fun `a plant with more recorded than the sowing is kept and its seed stays sown`() {
        val f = fixture()
        val batchId = batch(f, 5)
        val sown = sow(f, 2, batchId)
        sown.plantIds.forEach {
            plantService.addEvent(it, CreatePlantEventRequest(eventType = PlantEventType.NOTE, eventDate = sownOn, notes = "Grodde"), f.orgId)
        }

        val result = removeSowings(f, 2)

        assertEquals(0, result.plantsDeleted)
        assertEquals(0, result.seedsReturned)
        assertEquals(2, remainingPlants(f).size)
        assertEquals(3, inventory.findById(batchId)!!.quantity)
    }

    @Test
    fun `removing another kind of event still marks emptied plants removed`() {
        val f = fixture()
        val batchId = batch(f, 5)
        val plantId = sow(f, 1, batchId).plantIds.single()
        plantService.addEvent(plantId, CreatePlantEventRequest(eventType = PlantEventType.NOTE, eventDate = sownOn), f.orgId)
        plantService.deleteSpeciesEvent(
            f.orgId, f.speciesId,
            DeleteSpeciesEventRequest(eventType = "SEEDED", eventDate = sownOn, count = 1, trayOnly = true),
        )

        val result = plantService.deleteSpeciesEvent(
            f.orgId, f.speciesId,
            DeleteSpeciesEventRequest(eventType = "NOTE", eventDate = sownOn, count = 1, trayOnly = true),
        )

        assertEquals(1, result.plantsRemoved)
        assertEquals(0, result.plantsDeleted)
        assertEquals(PlantStatus.REMOVED, plants.findById(plantId)!!.status)
        assertEquals(4, inventory.findById(batchId)!!.quantity)
    }
}
