package app.verdant.service

import app.verdant.dto.CreateSeedInventoryRequest
import app.verdant.dto.UpdateSeedInventoryRequest
import app.verdant.entity.Organization
import app.verdant.entity.Provider
import app.verdant.entity.Species
import app.verdant.entity.SpeciesProvider
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.ProviderRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SpeciesProviderRepository
import app.verdant.repository.SpeciesRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

/** A batch can name any supplier; an own variety is linked to it, a shared catalogue variety is not. */
@QuarkusTest
class SeedBatchSupplierTest {
    @Inject lateinit var service: SeedInventoryService
    @Inject lateinit var orgs: OrganizationRepository
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var providers: ProviderRepository
    @Inject lateinit var links: SpeciesProviderRepository
    @Inject lateinit var inventory: SeedInventoryRepository

    private fun provider(name: String) = providers.persist(Provider(name = name, identifier = "$name-${UUID.randomUUID()}")).id!!
    private fun request(speciesId: Long, providerId: Long? = null, speciesProviderId: Long? = null) =
        CreateSeedInventoryRequest(speciesId = speciesId, quantity = 40, unitType = "SEED", providerId = providerId, speciesProviderId = speciesProviderId)

    @Test
    fun `an own variety is linked to a supplier chosen for its batch`() {
        val orgId = orgs.persist(Organization(name = "Supplier org")).id!!
        val own = species.persist(Species(commonName = "Supplier dahlia", orgId = orgId)).id!!
        val wexthuset = provider("Wexthuset")

        val batch = service.createInventory(request(own, providerId = wexthuset), orgId)

        assertEquals(wexthuset, batch.providerId)
        assertEquals("Wexthuset", batch.providerName)
        val link = links.findBySpeciesId(own).single()
        assertEquals(wexthuset, link.providerId)
        assertEquals(link.id, batch.speciesProviderId)

        // Choosing it again reuses the link.
        service.createInventory(request(own, providerId = wexthuset), orgId)
        assertEquals(1, links.findBySpeciesId(own).size)
    }

    @Test
    fun `a catalogue variety keeps its shared supplier links, and the supplier stays on the batch`() {
        val orgId = orgs.persist(Organization(name = "Catalogue supplier org")).id!!
        val catalogue = species.persist(Species(commonName = "Catalogue carnation")).id!!
        val impecta = provider("Impecta")
        val impectaLink = links.persist(SpeciesProvider(speciesId = catalogue, providerId = impecta)).id!!
        val florea = provider("Florea")

        val fromFlorea = service.createInventory(request(catalogue, providerId = florea), orgId)
        assertEquals(florea, fromFlorea.providerId)
        assertEquals("Florea", fromFlorea.providerName)
        assertNull(fromFlorea.speciesProviderId)
        assertEquals(listOf(impecta), links.findBySpeciesId(catalogue).map { it.providerId })

        val fromImpecta = service.createInventory(request(catalogue, providerId = impecta), orgId)
        assertEquals(impectaLink, fromImpecta.speciesProviderId)
    }

    @Test
    fun `an older client's supplier link still names the supplier, and the supplier can change later`() {
        val orgId = orgs.persist(Organization(name = "Legacy supplier org")).id!!
        val own = species.persist(Species(commonName = "Legacy zinnia", orgId = orgId)).id!!
        val impecta = provider("Impecta")
        val link = links.persist(SpeciesProvider(speciesId = own, providerId = impecta)).id!!

        val batch = service.createInventory(request(own, speciesProviderId = link), orgId)
        assertEquals(impecta, batch.providerId)
        assertEquals("Impecta", batch.providerName)

        val florea = provider("Florea")
        val changed = service.updateInventory(batch.id, UpdateSeedInventoryRequest(providerId = florea), orgId)
        assertEquals("Florea", changed.providerName)
        assertEquals(florea, inventory.findById(batch.id)!!.providerId)

        val other = species.persist(Species(commonName = "Other zinnia", orgId = orgId)).id!!
        assertThrows<BadRequestException> { service.createInventory(request(other, speciesProviderId = link), orgId) }
    }
}
