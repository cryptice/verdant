package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateSeedInventoryRequest
import app.verdant.android.data.model.DecrementSeedInventoryRequest
import app.verdant.android.data.model.UpdateSeedInventoryRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Seed stock. */
@Singleton
class SeedInventoryRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list(speciesId: Long? = null) = api.getSeedInventory(speciesId)
    suspend fun create(request: CreateSeedInventoryRequest) = api.createSeedInventory(request)
    suspend fun update(id: Long, request: UpdateSeedInventoryRequest) = api.updateSeedInventory(id, request)
    suspend fun decrement(id: Long, request: DecrementSeedInventoryRequest) = api.decrementSeedInventory(id, request)
    suspend fun delete(id: Long) = api.deleteSeedInventory(id)
}
