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
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SeedInventoryService(
    private val repo: SeedInventoryRepository,
    private val speciesRepo: SpeciesRepository,
    private val speciesProviderRepo: SpeciesProviderRepository,
    private val providerRepo: ProviderRepository,
) {
    fun getInventoryForUser(userId: Long, speciesId: Long? = null, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<SeedInventoryResponse> {
        val items = if (speciesId != null) {
            repo.findByUserIdAndSpeciesId(userId, speciesId)
        } else if (seasonId != null) {
            repo.findBySeasonId(userId, seasonId, limit, offset)
        } else {
            repo.findByUserId(userId, limit, offset)
        }
        val speciesNames = items.map { it.speciesId }.distinct().associateWith { id ->
            speciesRepo.findById(id)?.commonName ?: "Unknown"
        }
        val providerNames = resolveProviderNames(items.mapNotNull { it.speciesProviderId }.distinct())
        return items.map { it.toResponse(speciesNames[it.speciesId] ?: "Unknown", providerNames) }
    }

    fun createInventory(request: CreateSeedInventoryRequest, userId: Long): SeedInventoryResponse {
        val species = speciesRepo.findById(request.speciesId) ?: throw NotFoundException("Species not found")
        if (species.userId != null && species.userId != userId) throw ForbiddenException()
        val inventory = repo.persist(
            SeedInventory(
                userId = userId,
                speciesId = request.speciesId,
                quantity = request.quantity,
                collectionDate = request.collectionDate,
                expirationDate = request.expirationDate,
                costPerUnitSek = request.costPerUnitSek,
                unitType = UnitType.valueOf(request.unitType),
                seasonId = request.seasonId,
                speciesProviderId = request.speciesProviderId,
            )
        )
        val providerNames = resolveProviderNames(listOfNotNull(inventory.speciesProviderId))
        return inventory.toResponse(species.commonName, providerNames)
    }

    fun updateInventory(id: Long, request: UpdateSeedInventoryRequest, userId: Long): SeedInventoryResponse {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed stock not found")
        if (inventory.userId != userId) throw ForbiddenException()
        val updated = inventory.copy(
            quantity = request.quantity ?: inventory.quantity,
            collectionDate = request.collectionDate ?: inventory.collectionDate,
            expirationDate = request.expirationDate ?: inventory.expirationDate,
            speciesProviderId = if (request.speciesProviderId != null) request.speciesProviderId else inventory.speciesProviderId,
        )
        repo.update(updated)
        val speciesName = speciesRepo.findById(updated.speciesId)?.commonName ?: "Unknown"
        val providerNames = resolveProviderNames(listOfNotNull(updated.speciesProviderId))
        return updated.toResponse(speciesName, providerNames)
    }

    fun decrementInventory(id: Long, request: DecrementSeedInventoryRequest, userId: Long): SeedInventoryResponse {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed stock not found")
        if (inventory.userId != userId) throw ForbiddenException()
        if (!repo.decrementQuantity(id, request.quantity)) {
            throw BadRequestException("Insufficient seeds (have ${inventory.quantity}, need ${request.quantity})")
        }
        val updated = repo.findById(id)!!
        val speciesName = speciesRepo.findById(updated.speciesId)?.commonName ?: "Unknown"
        val providerNames = resolveProviderNames(listOfNotNull(updated.speciesProviderId))
        return updated.toResponse(speciesName, providerNames)
    }

    fun deleteInventory(id: Long, userId: Long) {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed stock not found")
        if (inventory.userId != userId) throw ForbiddenException()
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
        costPerUnitSek = costPerUnitSek,
        unitType = unitType.name,
        seasonId = seasonId,
        speciesProviderId = speciesProviderId,
        providerName = speciesProviderId?.let { providerNames[it] },
        createdAt = createdAt,
    )
}
