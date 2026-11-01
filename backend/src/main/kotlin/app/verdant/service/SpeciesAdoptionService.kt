package app.verdant.service

import app.verdant.dto.AdoptSpeciesResponse
import app.verdant.entity.SpeciesProvider
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SeedStockMovementRepository
import app.verdant.repository.SpeciesGroupRepository
import app.verdant.repository.SpeciesProviderRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.time.Instant

/**
 * Turns a catalogue variety into the organization's own, so it can have a workflow: the copy takes the
 * catalogue's details, suppliers, tags and groups, and the organization's seed batches move to it with
 * their purchase history. Plants already sown stay on the catalogue variety, and so do their sowings.
 */
@ApplicationScoped
class SpeciesAdoptionService(
    private val speciesRepository: SpeciesRepository,
    private val speciesProviderRepository: SpeciesProviderRepository,
    private val groupRepository: SpeciesGroupRepository,
    private val inventoryRepository: SeedInventoryRepository,
    private val movementRepository: SeedStockMovementRepository,
    private val workflowService: WorkflowService,
    private val speciesService: SpeciesService,
) {
    @Transactional
    fun adopt(speciesId: Long, workflowTemplateId: Long?, orgId: Long): AdoptSpeciesResponse {
        val catalogue = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (catalogue.orgId != null) throw BadRequestException("Only a catalogue variety can be made your own")

        // Photos are not copied: deleting one from the copy would delete the file the catalogue shows.
        val copy = speciesRepository.persist(catalogue.copy(id = null, orgId = orgId, workflowTemplateId = null, createdAt = Instant.now()))
        val copyId = copy.id!!
        speciesRepository.findTagIdsBySpeciesIds(setOf(speciesId))[speciesId]?.takeIf { it.isNotEmpty() }
            ?.let { speciesRepository.setTagsForSpecies(copyId, it) }
        groupRepository.findGroupIdsBySpeciesId(speciesId).forEach { groupRepository.addSpeciesToGroup(copyId, it) }
        val providerMap = speciesProviderRepository.findBySpeciesId(speciesId).associate { sp ->
            sp.id!! to speciesProviderRepository.persist(sp.copy(id = null, speciesId = copyId, createdAt = Instant.now())).id!!
        }

        val moved = inventoryRepository.moveToSpecies(orgId, speciesId, copyId, providerMap)
        movementRepository.moveBatchesToSpecies(moved, copyId)

        if (workflowTemplateId != null) workflowService.assignTemplateToSpecies(copyId, workflowTemplateId, orgId)
        return AdoptSpeciesResponse(speciesService.getSpecies(copyId, orgId), moved)
    }
}
