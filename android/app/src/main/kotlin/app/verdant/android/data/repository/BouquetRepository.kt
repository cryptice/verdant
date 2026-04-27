package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateBouquetRecipeRequest
import app.verdant.android.data.model.CreateBouquetRequest
import app.verdant.android.data.model.UpdateBouquetRecipeRequest
import app.verdant.android.data.model.UpdateBouquetRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Bouquet recipes + bouquet instances. */
@Singleton
class BouquetRepository @Inject constructor(private val api: VerdantApi) {
    // Recipes
    suspend fun listRecipes() = api.getBouquetRecipes()
    suspend fun getRecipe(id: Long) = api.getBouquetRecipe(id)
    suspend fun createRecipe(request: CreateBouquetRecipeRequest) = api.createBouquetRecipe(request)
    suspend fun updateRecipe(id: Long, request: UpdateBouquetRecipeRequest) = api.updateBouquetRecipe(id, request)
    suspend fun deleteRecipe(id: Long) = api.deleteBouquetRecipe(id)

    // Instances
    suspend fun list() = api.getBouquets()
    suspend fun get(id: Long) = api.getBouquet(id)
    suspend fun create(request: CreateBouquetRequest) = api.createBouquet(request)
    suspend fun update(id: Long, request: UpdateBouquetRequest) = api.updateBouquet(id, request)
    suspend fun delete(id: Long) = api.deleteBouquet(id)
}
