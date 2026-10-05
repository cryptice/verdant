package app.verdant.service

import app.verdant.entity.*
import app.verdant.repository.*
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class PlanningCatalogueServiceTest {
    @Inject lateinit var service: PlanningCatalogueService
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var groups: SpeciesGroupRepository
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var plans: HarvestPlanRepository

    @Test fun `group population is additive repeatable and only changes shared memberships`() {
        val org = orgs.persist(Organization(name = "Catalogue test ${UUID.randomUUID()}"))
        val shared = species.persist(Species(commonName = "Test ${UUID.randomUUID()}", scientificName = "Zinnia elegans"))
        val private = species.persist(Species(orgId = org.id, commonName = "Private", scientificName = "Zinnia elegans"))
        val priorGroup = groups.persist(SpeciesGroup(name = "Preserve ${UUID.randomUUID()}"))
        val privateGroup = groups.persist(SpeciesGroup(orgId = org.id, name = "Zinnior"))
        groups.addSpeciesToGroup(shared.id!!, priorGroup.id!!)
        groups.addSpeciesToGroup(private.id!!, privateGroup.id!!)
        val profile = HarvestPlanCalculatorTest.profile()
        plans.saveProfile(org.id!!, shared.id, profile)
        val before = species.findById(shared.id)
        service.apply()
        val names = groups.findGroupIdsBySpeciesId(shared.id).map { groups.findById(it)!!.name }
        assertTrue(names.containsAll(listOf(priorGroup.name, "Zinnior")))
        assertEquals(listOf(privateGroup.id), groups.findGroupIdsBySpeciesId(private.id))
        assertEquals(before, species.findById(shared.id))
        assertEquals(profile, plans.profile(org.id, shared.id))
        assertTrue(service.preview().none { it.speciesId == private.id })
        val repeat = service.apply()
        assertEquals(0, repeat.groupsCreated)
        assertEquals(0, repeat.membershipsAdded)
    }
}
