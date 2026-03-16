package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.SeedInventory
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SeedInventoryService(
    private val repo: SeedInventoryRepository,
    private val speciesRepo: SpeciesRepository,
) {
    fun getInventoryForUser(userId: Long, speciesId: Long? = null): List<SeedInventoryResponse> {
        val items = if (speciesId != null) {
            repo.findByUserIdAndSpeciesId(userId, speciesId)
        } else {
            repo.findByUserId(userId)
        }
        val speciesNames = items.map { it.speciesId }.distinct().associateWith { id ->
            speciesRepo.findById(id)?.commonName ?: "Unknown"
        }
        return items.map { it.toResponse(speciesNames[it.speciesId] ?: "Unknown") }
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
            )
        )
        return inventory.toResponse(species.commonName)
    }

    fun updateInventory(id: Long, request: UpdateSeedInventoryRequest, userId: Long): SeedInventoryResponse {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed inventory not found")
        if (inventory.userId != userId) throw ForbiddenException()
        val updated = inventory.copy(
            quantity = request.quantity ?: inventory.quantity,
            collectionDate = request.collectionDate ?: inventory.collectionDate,
            expirationDate = request.expirationDate ?: inventory.expirationDate,
        )
        repo.update(updated)
        val speciesName = speciesRepo.findById(updated.speciesId)?.commonName ?: "Unknown"
        return updated.toResponse(speciesName)
    }

    fun decrementInventory(id: Long, request: DecrementSeedInventoryRequest, userId: Long): SeedInventoryResponse {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed inventory not found")
        if (inventory.userId != userId) throw ForbiddenException()
        if (!repo.decrementQuantity(id, request.quantity)) {
            throw BadRequestException("Insufficient seeds (have ${inventory.quantity}, need ${request.quantity})")
        }
        val updated = repo.findById(id)!!
        val speciesName = speciesRepo.findById(updated.speciesId)?.commonName ?: "Unknown"
        return updated.toResponse(speciesName)
    }

    fun deleteInventory(id: Long, userId: Long) {
        val inventory = repo.findById(id) ?: throw NotFoundException("Seed inventory not found")
        if (inventory.userId != userId) throw ForbiddenException()
        repo.delete(id)
    }

    private fun SeedInventory.toResponse(speciesName: String) = SeedInventoryResponse(
        id = id!!,
        speciesId = speciesId,
        speciesName = speciesName,
        quantity = quantity,
        collectionDate = collectionDate,
        expirationDate = expirationDate,
        createdAt = createdAt,
    )
}
