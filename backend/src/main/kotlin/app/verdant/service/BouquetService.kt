package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Bouquet
import app.verdant.entity.BouquetItem
import app.verdant.repository.BouquetRecipeRepository
import app.verdant.repository.BouquetRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import java.time.Instant

@ApplicationScoped
class BouquetService(
    private val repo: BouquetRepository,
    private val recipeRepo: BouquetRecipeRepository,
    private val speciesRepo: SpeciesRepository,
) {
    fun getBouquetsForUser(orgId: Long, limit: Int = 100, offset: Int = 0): List<BouquetResponse> =
        repo.findByOrgId(orgId, limit, offset).map { it.toResponseWithItems() }

    fun getBouquet(id: Long, orgId: Long): BouquetResponse {
        val bouquet = repo.findById(id) ?: throw NotFoundException("Bouquet not found")
        if (bouquet.orgId != orgId) throw NotFoundException("Bouquet not found")
        return bouquet.toResponseWithItems()
    }

    @Transactional
    fun createBouquet(request: CreateBouquetRequest, orgId: Long): BouquetResponse {
        // If sourceRecipeId is supplied, verify the recipe belongs to this org.
        request.sourceRecipeId?.let { recipeId ->
            val recipe = recipeRepo.findById(recipeId)
                ?: throw NotFoundException("Bouquet recipe not found")
            if (recipe.orgId != orgId) throw NotFoundException("Bouquet recipe not found")
        }
        val bouquet = repo.persist(
            Bouquet(
                orgId = orgId,
                sourceRecipeId = request.sourceRecipeId,
                name = request.name,
                description = request.description,
                priceCents = request.priceCents,
                assembledAt = request.assembledAt ?: Instant.now(),
                notes = request.notes,
            )
        )
        for (itemReq in request.items) {
            repo.persistItem(
                BouquetItem(
                    bouquetId = bouquet.id!!,
                    speciesId = itemReq.speciesId,
                    stemCount = itemReq.stemCount,
                    role = itemReq.role,
                    notes = itemReq.notes,
                )
            )
        }
        return bouquet.toResponseWithItems()
    }

    @Transactional
    fun updateBouquet(id: Long, request: UpdateBouquetRequest, orgId: Long): BouquetResponse {
        val bouquet = repo.findById(id) ?: throw NotFoundException("Bouquet not found")
        if (bouquet.orgId != orgId) throw NotFoundException("Bouquet not found")
        request.sourceRecipeId?.let { recipeId ->
            val recipe = recipeRepo.findById(recipeId)
                ?: throw NotFoundException("Bouquet recipe not found")
            if (recipe.orgId != orgId) throw NotFoundException("Bouquet recipe not found")
        }
        val updated = bouquet.copy(
            sourceRecipeId = request.sourceRecipeId ?: bouquet.sourceRecipeId,
            name = request.name ?: bouquet.name,
            description = request.description ?: bouquet.description,
            priceCents = request.priceCents ?: bouquet.priceCents,
            assembledAt = request.assembledAt ?: bouquet.assembledAt,
            notes = request.notes ?: bouquet.notes,
        )
        repo.update(updated)
        if (request.items != null) {
            repo.deleteItemsByBouquetId(id)
            for (itemReq in request.items) {
                repo.persistItem(
                    BouquetItem(
                        bouquetId = id,
                        speciesId = itemReq.speciesId,
                        stemCount = itemReq.stemCount,
                        role = itemReq.role,
                        notes = itemReq.notes,
                    )
                )
            }
        }
        return updated.toResponseWithItems()
    }

    fun deleteBouquet(id: Long, orgId: Long) {
        val bouquet = repo.findById(id) ?: throw NotFoundException("Bouquet not found")
        if (bouquet.orgId != orgId) throw NotFoundException("Bouquet not found")
        repo.deleteItemsByBouquetId(id)
        repo.delete(id)
    }

    private fun Bouquet.toResponseWithItems(): BouquetResponse {
        val items = repo.findItemsByBouquetId(id!!)
        val speciesNames = speciesRepo.findNamesByIds(items.map { it.speciesId }.toSet())
        val recipeName = sourceRecipeId?.let { recipeRepo.findById(it)?.name }
        return BouquetResponse(
            id = id,
            sourceRecipeId = sourceRecipeId,
            sourceRecipeName = recipeName,
            name = name,
            description = description,
            imageUrl = imageUrl,
            priceCents = priceCents,
            assembledAt = assembledAt,
            notes = notes,
            items = items.map { item ->
                BouquetItemResponse(
                    id = item.id!!,
                    speciesId = item.speciesId,
                    speciesName = speciesNames[item.speciesId],
                    stemCount = item.stemCount,
                    role = item.role,
                    notes = item.notes,
                )
            },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
