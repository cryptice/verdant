package app.verdant.service

import app.verdant.dto.BatchSowRequest
import app.verdant.entity.MovementKind
import app.verdant.entity.Organization
import app.verdant.entity.PlantOrigin
import app.verdant.entity.Season
import app.verdant.entity.SeedInventory
import app.verdant.entity.Species
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.PlantRepository
import app.verdant.repository.SeasonRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SeedStockMovementRepository
import app.verdant.repository.SpeciesRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@QuarkusTest
class BatchSowLedgerTest {

    @Inject lateinit var plantService: PlantService
    @Inject lateinit var plants: PlantRepository
    @Inject lateinit var inventory: SeedInventoryRepository
    @Inject lateinit var movements: SeedStockMovementRepository
    @Inject lateinit var seasons: SeasonRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var species: SpeciesRepository

    private data class Fixture(val orgId: Long, val speciesId: Long)

    private fun fixture(): Fixture {
        val orgId = orgs.persist(Organization(name = "Sow ledger org")).id!!
        val speciesId = species.persist(Species(commonName = "Sow ledger cosmos")).id!!
        return Fixture(orgId, speciesId)
    }

    private fun batch(f: Fixture, quantity: Int) =
        inventory.persist(SeedInventory(orgId = f.orgId, speciesId = f.speciesId, quantity = quantity, costPerUnitCents = 20)).id!!

    private fun sow(f: Fixture, count: Int, batchId: Long?, allowShortfall: Boolean = false,
                    origin: PlantOrigin = PlantOrigin.SOWN, date: LocalDate? = null) =
        plantService.batchSow(
            BatchSowRequest(
                speciesId = f.speciesId, name = "Cosmos", seedCount = count, seedBatchId = batchId,
                allowShortfall = allowShortfall, origin = origin, plantedDate = date,
            ),
            f.orgId,
        )

    private fun plantCount(f: Fixture) = plants.findByOrgId(f.orgId, limit = 1000).size

    @Test
    fun `sowing from a batch deducts it and links the movement to the plants`() {
        val f = fixture()
        val date = LocalDate.of(2026, 4, 10)
        val season = seasons.persist(
            Season(orgId = f.orgId, name = "Sow ledger 2026", year = 2026,
                startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 12, 31))
        )
        val batchId = batch(f, 30)

        val response = sow(f, 10, batchId, date = date)

        assertEquals(20, inventory.findById(batchId)!!.quantity)
        val m = movements.findBySeedInventoryId(batchId).single { it.kind == MovementKind.SOWN }
        assertEquals(10, m.quantity)
        assertEquals(response.plantIds.first(), m.firstPlantId)
        assertEquals(10, m.plantCount)
        assertEquals(season.id, m.seasonId)
        assertEquals(date, m.occurredOn)
    }

    @Test
    fun `sowing past the batch without consent creates nothing`() {
        val f = fixture()
        val batchId = batch(f, 5)

        assertThrows<BadRequestException> { sow(f, 10, batchId) }

        assertEquals(0, plantCount(f))
        assertEquals(5, inventory.findById(batchId)!!.quantity)
    }

    @Test
    fun `consented shortfall empties the batch and records it`() {
        val f = fixture()
        val batchId = batch(f, 5)

        sow(f, 10, batchId, allowShortfall = true)

        assertEquals(0, inventory.findById(batchId)!!.quantity)
        val m = movements.findBySeedInventoryId(batchId).single { it.kind == MovementKind.SOWN }
        assertEquals(10, m.quantity)
        assertEquals(5, m.shortfall)
    }

    @Test
    fun `a batch from another organization or species is refused`() {
        val f = fixture()
        val other = fixture()
        val foreignBatch = batch(other, 20)
        assertThrows<NotFoundException> { sow(f, 3, foreignBatch) }

        val otherSpecies = species.persist(Species(commonName = "Sow ledger other")).id!!
        val wrongSpecies = inventory.persist(SeedInventory(orgId = f.orgId, speciesId = otherSpecies, quantity = 20)).id!!
        assertThrows<BadRequestException> { sow(f, 3, wrongSpecies) }

        assertEquals(0, plantCount(f))
        assertEquals(20, inventory.findById(foreignBatch)!!.quantity)
        assertEquals(20, inventory.findById(wrongSpecies)!!.quantity)
    }

    @Test
    fun `registered plants are stored as registered`() {
        val f = fixture()

        val response = sow(f, 3, null, origin = PlantOrigin.REGISTERED)

        val created = plants.findByIds(response.plantIds)
        assertEquals(3, created.size)
        assertTrue(created.all { it.origin == PlantOrigin.REGISTERED })
        assertTrue(movements.findBySeedInventoryId(-1).isEmpty())
    }
}
