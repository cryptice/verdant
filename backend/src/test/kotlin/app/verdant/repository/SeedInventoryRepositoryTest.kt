package app.verdant.repository

import app.verdant.entity.Organization
import app.verdant.entity.SeedInventory
import app.verdant.entity.Species
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@QuarkusTest
class SeedInventoryRepositoryTest {

    @Inject lateinit var inventory: SeedInventoryRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var species: SpeciesRepository

    private fun newBatch(seedsPerPackage: Int?) = SeedInventory(
        orgId = orgs.persist(Organization(name = "Seed test org")).id!!,
        speciesId = species.persist(Species(commonName = "Seed test chili")).id!!,
        quantity = 15,
        seedsPerPackage = seedsPerPackage,
    )

    @Test
    fun `package size round-trips through persist and update`() {
        val saved = inventory.persist(newBatch(seedsPerPackage = 5))
        assertEquals(5, inventory.findById(saved.id!!)!!.seedsPerPackage)

        inventory.update(saved.copy(seedsPerPackage = 10))
        assertEquals(10, inventory.findById(saved.id!!)!!.seedsPerPackage)
    }

    @Test
    fun `a batch without a package size reads back as null`() {
        val saved = inventory.persist(newBatch(seedsPerPackage = null))
        assertNull(inventory.findById(saved.id!!)!!.seedsPerPackage)
    }
}
