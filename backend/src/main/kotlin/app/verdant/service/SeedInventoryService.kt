package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.SeedInventory
import app.verdant.entity.UnitType
import app.verdant.repository.ProviderRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SpeciesProviderRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SeedInventoryService(
    private val repo: SeedInventoryRepository,
    private val speciesRepo: SpeciesRepository,
    private val speciesProviderRepo: SpeciesProviderRepository,
    private val providerRepo: ProviderRepository,
) {
    fun getInventoryForUser(orgId: Long, speciesId: Long? = null, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<SeedInventoryResponse> {
        val items = if (speciesId != null) {
            repo.findByOrgIdAndSpeciesId(orgId, speciesId)
        } else if (seasonId != null) {
            repo.findBySeasonId(orgId, seasonId, limit, offset)
        } else {
            repo.findByOrgId(orgId, limit, offset)
        }
        val speciesNames = speciesRepo.findNamesByIds(items.map { it.speciesId }.toSet())
        val providerNames = resolveProviderNames(items.mapNotNull { it.speciesProviderId }.distinct())
        return items.map { it.toResponse(speciesNames[it.speciesId] ?: "Unknown", providerNames) }
    }

    fun createInventory(request: CreateSeedInventoryRequest, orgId: Long): SeedInventoryResponse {
        val species = speciesRepo.findById(request.speciesId) ?: throw NotFoundException("Species not found")
        if (species.orgId != null && species.orgId != orgId) throw NotFoundException("Species not found")
        val inventory = repo.persist(
            SeedInventory(
                orgId = orgId,
                speciesId = request.speciesId,
                quantity = request.quantity,
                collectionDate = request.collectionDate,
                expirationDate = request.expirationDate,
                costPerUnitCents = request.costPerUnitCents,
                unitType = UnitType.valueOf(request.unitType),
                seasonId = request.seasonId,
                speciesProviderId = request.speciesProviderId,
            )
        )
        val providerNames = resolveProviderNames(listOfNotNull(inventory.speciesProviderId))
        val speciesName = speciesRepo.findNamesByIds(setOf(inventory.speciesId))[inventory.speciesId]
            ?: species.commonName
        return inventory.toResponse(speciesName, providerNames)
    }

    fun updateInventory(id: Long, request: UpdateSeedInventoryRequest, orgId: Long): SeedInventoryResponse {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed stock not found")
        if (inventory.orgId != orgId) throw NotFoundException("Seed stock not found")
        val updated = inventory.copy(
            quantity = request.quantity ?: inventory.quantity,
            collectionDate = request.collectionDate ?: inventory.collectionDate,
            expirationDate = request.expirationDate ?: inventory.expirationDate,
            speciesProviderId = if (request.speciesProviderId != null) request.speciesProviderId else inventory.speciesProviderId,
        )
        repo.update(updated)
        val speciesName = speciesRepo.findNamesByIds(setOf(updated.speciesId))[updated.speciesId] ?: "Unknown"
        val providerNames = resolveProviderNames(listOfNotNull(updated.speciesProviderId))
        return updated.toResponse(speciesName, providerNames)
    }

    fun decrementInventory(id: Long, request: DecrementSeedInventoryRequest, orgId: Long): SeedInventoryResponse {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed stock not found")
        if (inventory.orgId != orgId) throw NotFoundException("Seed stock not found")
        if (!repo.decrementQuantity(id, request.quantity)) {
            throw BadRequestException("Insufficient seeds (have ${inventory.quantity}, need ${request.quantity})")
        }
        val updated = repo.findById(id)!!
        val speciesName = speciesRepo.findNamesByIds(setOf(updated.speciesId))[updated.speciesId] ?: "Unknown"
        val providerNames = resolveProviderNames(listOfNotNull(updated.speciesProviderId))
        return updated.toResponse(speciesName, providerNames)
    }

    fun deleteInventory(id: Long, orgId: Long) {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed stock not found")
        if (inventory.orgId != orgId) throw NotFoundException("Seed stock not found")
        repo.delete(id)
    }

    private fun resolveProviderNames(speciesProviderIds: List<Long>): Map<Long, String> {
        if (speciesProviderIds.isEmpty()) return emptyMap()
        return speciesProviderIds.mapNotNull { spId ->
            val sp = speciesProviderRepo.findById(spId) ?: return@mapNotNull null
            val provider = providerRepo.findById(sp.providerId) ?: return@mapNotNull null
            spId to provider.name
        }.toMap()
    }

    private fun SeedInventory.toResponse(speciesName: String, providerNames: Map<Long, String> = emptyMap()) = SeedInventoryResponse(
        id = id!!,
        speciesId = speciesId,
        speciesName = speciesName,
        quantity = quantity,
        collectionDate = collectionDate,
        expirationDate = expirationDate,
        costPerUnitCents = costPerUnitCents,
        unitType = unitType.name,
        seasonId = seasonId,
        speciesProviderId = speciesProviderId,
        providerName = speciesProviderId?.let { providerNames[it] },
        createdAt = createdAt,
    )
}
