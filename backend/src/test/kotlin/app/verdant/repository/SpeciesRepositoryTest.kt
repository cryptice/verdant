package app.verdant.repository

import app.verdant.entity.Organization
import app.verdant.entity.Species
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@QuarkusTest
class SpeciesRepositoryTest {

    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var orgs: OrganizationRepository

    @Test
    fun `varieties sort in Swedish order with Å, Ä and Ö after Z`() {
        val orgId = orgs.persist(Organization(name = "Collation test org")).id!!
        listOf("Ört", "Zinnia", "Åkervädd", "Amarant", "Ängsull", "Oregano").forEach {
            species.persist(Species(orgId = orgId, commonName = it, commonNameSv = it))
        }

        val names = species.findByOrgId(orgId, limit = 10_000).filter { it.orgId == orgId }.map { it.commonNameSv }

        assertEquals(listOf("Amarant", "Oregano", "Zinnia", "Åkervädd", "Ängsull", "Ört"), names)
    }

    @Test
    fun `a variety's package size round-trips through persist and update`() {
        val saved = species.persist(Species(commonName = "Package zinnia", costPerSeedCents = 78, seedsPerPackage = 50))
        assertEquals(50, species.findById(saved.id!!)!!.seedsPerPackage)

        species.update(saved.copy(seedsPerPackage = 25))
        assertEquals(25, species.findById(saved.id!!)!!.seedsPerPackage)
        assertEquals(78, species.findById(saved.id!!)!!.costPerSeedCents)
    }
}
