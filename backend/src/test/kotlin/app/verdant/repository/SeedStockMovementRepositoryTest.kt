package app.verdant.repository

import app.verdant.entity.MovementKind
import app.verdant.entity.Organization
import app.verdant.entity.SeedInventory
import app.verdant.entity.SeedStockMovement
import app.verdant.entity.Species
import io.agroal.api.AgroalDataSource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class SeedStockMovementRepositoryTest {

    @Inject lateinit var movements: SeedStockMovementRepository
    @Inject lateinit var inventory: SeedInventoryRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var ds: AgroalDataSource

    private data class Fixture(val orgId: Long, val speciesId: Long, val batchId: Long)

    private fun fixture(): Fixture {
        val orgId = orgs.persist(Organization(name = "Ledger test org")).id!!
        val speciesId = species.persist(Species(commonName = "Ledger test chili")).id!!
        val batch = inventory.persist(SeedInventory(orgId = orgId, speciesId = speciesId, quantity = 10))
        return Fixture(orgId, speciesId, batch.id!!)
    }

    @Test
    fun `a movement round-trips with every field`() {
        val f = fixture()
        val saved = movements.persist(
            SeedStockMovement(
                orgId = f.orgId, seedInventoryId = f.batchId, speciesId = f.speciesId,
                kind = MovementKind.SOWN, quantity = -4, shortfall = 2, unitCostCents = 35,
                occurredOn = LocalDate.of(2026, 4, 2), seasonId = null, plantCount = 3,
            )
        )
        assertNotNull(saved.id)

        val read = movements.findBySeedInventoryId(f.batchId).single()
        assertEquals(saved.id, read.id)
        assertEquals(f.orgId, read.orgId)
        assertEquals(f.batchId, read.seedInventoryId)
        assertEquals(f.speciesId, read.speciesId)
        assertEquals(MovementKind.SOWN, read.kind)
        assertEquals(-4, read.quantity)
        assertEquals(2, read.shortfall)
        assertEquals(35, read.unitCostCents)
        assertEquals(LocalDate.of(2026, 4, 2), read.occurredOn)
        assertNull(read.seasonId)
        assertNull(read.firstPlantId)
        assertEquals(3, read.plantCount)
    }

    @Test
    fun `deleting the batch keeps its movements with the batch reference cleared`() {
        val f = fixture()
        movements.persist(
            SeedStockMovement(
                orgId = f.orgId, seedInventoryId = f.batchId, speciesId = f.speciesId,
                kind = MovementKind.ADDED, quantity = 10, unitCostCents = 50,
                occurredOn = LocalDate.of(2026, 3, 1), seasonId = null,
            )
        )
        inventory.delete(f.batchId)

        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT seed_inventory_id, species_id, unit_cost_cents FROM seed_stock_movement WHERE org_id = ?"
            ).use { ps ->
                ps.setLong(1, f.orgId)
                ps.executeQuery().use { rs ->
                    assertTrue(rs.next())
                    assertNull(rs.getObject("seed_inventory_id"))
                    assertEquals(f.speciesId, rs.getLong("species_id"))
                    assertEquals(50, rs.getInt("unit_cost_cents"))
                    assertFalse(rs.next())
                }
            }
        }
    }

    @Test
    fun `the ledger start date is known once migrations have run`() {
        assertNotNull(movements.ledgerStartDate())
    }
}
