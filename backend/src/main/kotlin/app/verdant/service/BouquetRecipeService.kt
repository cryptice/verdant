package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.BouquetRecipe
import app.verdant.entity.BouquetRecipeItem
import app.verdant.repository.BouquetRecipeRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class BouquetRecipeService(
    private val repo: BouquetRecipeRepository,
    private val speciesRepo: SpeciesRepository,
    private val storageService: StorageService,
) {
    fun getRecipesForUser(userId: Long, limit: Int = 50, offset: Int = 0): List<BouquetRecipeResponse> =
        repo.findByUserId(userId, limit, offset).map { it.toResponseWithItems() }

    fun getRecipe(id: Long, userId: Long): BouquetRecipeResponse {
        val recipe = repo.findById(id) ?: throw NotFoundException("Bouquet recipe not found")
        if (recipe.userId != userId) throw ForbiddenException()
        return recipe.toResponseWithItems()
    }

    @Transactional
    fun createRecipe(request: CreateBouquetRecipeRequest, userId: Long): BouquetRecipeResponse {
        var recipe = repo.persist(
            BouquetRecipe(
                userId = userId,
                name = request.name,
                description = request.description,
                priceCents = request.priceCents,
            )
        )
        if (request.imageBase64 != null) {
            val imageUrl = storageService.uploadImage(
                request.imageBase64,
                "bouquet/$userId/${recipe.id}.jpg"
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
    fun updateRecipe(id: Long, request: UpdateBouquetRecipeRequest, userId: Long): BouquetRecipeResponse {
        val recipe = repo.findById(id) ?: throw NotFoundException("Bouquet recipe not found")
        if (recipe.userId != userId) throw ForbiddenException()
        var imageUrl = recipe.imageUrl
        if (request.imageBase64 != null) {
            imageUrl = storageService.uploadImage(
                request.imageBase64,
                "bouquet/$userId/$id.jpg"
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

    fun deleteRecipe(id: Long, userId: Long) {
        val recipe = repo.findById(id) ?: throw NotFoundException("Bouquet recipe not found")
        if (recipe.userId != userId) throw ForbiddenException()
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
