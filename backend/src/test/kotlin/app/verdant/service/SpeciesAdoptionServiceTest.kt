package app.verdant.service

import app.verdant.dto.CreateWorkflowStepRequest
import app.verdant.dto.CreateWorkflowTemplateRequest
import app.verdant.entity.MovementKind
import app.verdant.entity.Organization
import app.verdant.entity.Provider
import app.verdant.entity.SeedInventory
import app.verdant.entity.SeedStockMovement
import app.verdant.entity.Species
import app.verdant.entity.SpeciesProvider
import app.verdant.repository.OrganizationRepository
import app.verdant.repository.ProviderRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SeedStockMovementRepository
import app.verdant.repository.SpeciesProviderRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.WorkflowRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class SpeciesAdoptionServiceTest {

    @Inject lateinit var service: SpeciesAdoptionService
    @Inject lateinit var workflowService: WorkflowService
    @Inject lateinit var workflows: WorkflowRepository
    @Inject lateinit var species: SpeciesRepository
    @Inject lateinit var providers: ProviderRepository
    @Inject lateinit var speciesProviders: SpeciesProviderRepository
    @Inject lateinit var inventory: SeedInventoryRepository
    @Inject lateinit var movements: SeedStockMovementRepository
    @Inject lateinit var orgs: OrganizationRepository

    @Test
    fun `makes a catalogue variety your own, moving your batches and their purchases but not their sowings`() {
        val orgId = orgs.persist(Organization(name = "Adoption org")).id!!
        val otherOrg = orgs.persist(Organization(name = "Other adoption org")).id!!
        val catalogue = species.persist(Species(commonName = "Baby's breath", commonNameSv = "Slöjsilja", germinationRate = 85)).id!!
        val impecta = providers.persist(Provider(name = "Impecta", identifier = "impecta-${UUID.randomUUID()}")).id!!
        val link = speciesProviders.persist(SpeciesProvider(speciesId = catalogue, providerId = impecta, productUrl = "https://impecta.se/slojsilja")).id!!
        val batch = inventory.persist(SeedInventory(orgId = orgId, speciesId = catalogue, quantity = 700, speciesProviderId = link)).id!!
        val theirs = inventory.persist(SeedInventory(orgId = otherOrg, speciesId = catalogue, quantity = 50)).id!!
        val day = LocalDate.of(2026, 10, 1)
        val opening = movements.persist(SeedStockMovement(orgId = orgId, seedInventoryId = batch, speciesId = catalogue, kind = MovementKind.OPENING,
            quantity = 720, unitCostCents = 5, occurredOn = day, seasonId = null)).id!!
        val sown = movements.persist(SeedStockMovement(orgId = orgId, seedInventoryId = batch, speciesId = catalogue, kind = MovementKind.SOWN,
            quantity = 20, unitCostCents = 5, occurredOn = day, seasonId = null)).id!!
        val template = workflowService.createTemplate(CreateWorkflowTemplateRequest(name = "Slöjsilja"), orgId).id
        workflowService.addTemplateStep(template, CreateWorkflowStepRequest(name = "Sådd", eventType = "SEEDED", daysAfterPrevious = 0), orgId)
        workflowService.addTemplateStep(template, CreateWorkflowStepRequest(name = "Säljklar", daysAfterPrevious = 70, sortOrder = 1), orgId)

        val result = service.adopt(catalogue, template, orgId)

        val copy = result.species
        assertNotEquals(catalogue, copy.id)
        assertEquals("Slöjsilja", copy.commonNameSv)
        assertEquals(85, copy.germinationRate)
        assertEquals(false, copy.isSystem)
        assertEquals(listOf("Impecta"), copy.providers.map { it.providerName })
        assertEquals(listOf(batch), result.movedBatchIds)

        val moved = inventory.findById(batch)!!
        assertEquals(copy.id, moved.speciesId)
        assertEquals(copy.id, speciesProviders.findBySpeciesId(copy.id).single { it.id == moved.speciesProviderId }.speciesId)
        assertEquals(catalogue, inventory.findById(theirs)!!.speciesId, "another organization's batch stays")
        assertEquals(copy.id, movements.findById(opening)!!.speciesId, "the purchase follows the batch")
        assertEquals(catalogue, movements.findById(sown)!!.speciesId, "the sowing stays with its plants")
        assertEquals(listOf("Sådd", "Säljklar"), workflows.findStepsBySpeciesId(copy.id).sortedBy { it.sortOrder }.map { it.name })
        assertEquals(1, speciesProviders.findBySpeciesId(catalogue).size, "the catalogue keeps its supplier")
    }

    @Test
    fun `refuses to adopt a variety that is already an organization's own`() {
        val orgId = orgs.persist(Organization(name = "Adoption own org")).id!!
        val own = species.persist(Species(commonName = "Own zinnia", orgId = orgId)).id!!
        assertThrows<BadRequestException> { service.adopt(own, null, orgId) }
    }
}
