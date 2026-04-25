package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.BouquetRecipe
import app.verdant.entity.BouquetRecipeItem
import app.verdant.repository.BouquetRecipeRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class BouquetRecipeService(
    private val repo: BouquetRecipeRepository,
    private val speciesRepo: SpeciesRepository,
    private val storageService: StorageService,
) {
    fun getRecipesForUser(orgId: Long, limit: Int = 50, offset: Int = 0): List<BouquetRecipeResponse> =
        repo.findByOrgId(orgId, limit, offset).map { it.toResponseWithItems() }

    fun getRecipe(id: Long, orgId: Long): BouquetRecipeResponse {
        val recipe = repo.findById(id) ?: throw NotFoundException("Bouquet recipe not found")
        if (recipe.orgId != orgId) throw NotFoundException("Bouquet recipe not found")
        return recipe.toResponseWithItems()
    }

    @Transactional
    fun createRecipe(request: CreateBouquetRecipeRequest, orgId: Long): BouquetRecipeResponse {
        var recipe = repo.persist(
            BouquetRecipe(
                orgId = orgId,
                name = request.name,
                description = request.description,
                priceCents = request.priceCents,
            )
        )
        if (request.imageBase64 != null) {
            val imageUrl = storageService.uploadImage(
                request.imageBase64,
                "bouquet/$orgId/${recipe.id}.jpg"
            )
            recipe = recipe.copy(imageUrl = imageUrl)
            repo.update(recipe)
        }
        for (itemReq in request.items) {
            repo.persistItem(
                BouquetRecipeItem(
                    recipeId = recipe.id!!,
                    speciesId = itemReq.speciesId,
                    stemCount = itemReq.stemCount,
                    role = itemReq.role,
                    notes = itemReq.notes,
                )
            )
        }
        return recipe.toResponseWithItems()
    }

    @Transactional
    fun updateRecipe(id: Long, request: UpdateBouquetRecipeRequest, orgId: Long): BouquetRecipeResponse {
        val recipe = repo.findById(id) ?: throw NotFoundException("Bouquet recipe not found")
        if (recipe.orgId != orgId) throw NotFoundException("Bouquet recipe not found")
        var imageUrl = recipe.imageUrl
        if (request.imageBase64 != null) {
            imageUrl = storageService.uploadImage(
                request.imageBase64,
                "bouquet/$orgId/$id.jpg"
            )
        }
        val updated = recipe.copy(
            name = request.name ?: recipe.name,
            description = request.description ?: recipe.description,
            imageUrl = imageUrl,
            priceCents = request.priceCents ?: recipe.priceCents,
        )
        repo.update(updated)
        if (request.items != null) {
            repo.deleteItemsByRecipeId(id)
            for (itemReq in request.items) {
                repo.persistItem(
                    BouquetRecipeItem(
                        recipeId = id,
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

    fun deleteRecipe(id: Long, orgId: Long) {
        val recipe = repo.findById(id) ?: throw NotFoundException("Bouquet recipe not found")
        if (recipe.orgId != orgId) throw NotFoundException("Bouquet recipe not found")
        repo.deleteItemsByRecipeId(id)
        repo.delete(id)
    }

    private fun BouquetRecipe.toResponseWithItems(): BouquetRecipeResponse {
        val items = repo.findItemsByRecipeId(id!!)
        val speciesNames = speciesRepo.findNamesByIds(items.map { it.speciesId }.toSet())
        return BouquetRecipeResponse(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            priceCents = priceCents,
            items = items.map { item ->
                BouquetRecipeItemResponse(
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
