package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateSupplyInventoryRequest
import app.verdant.android.data.model.CreateSupplyTypeRequest
import app.verdant.android.data.model.DecrementSupplyRequest
import app.verdant.android.data.model.UpdateSupplyTypeRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Supply types, inventory lots, decrements. */
@Singleton
class SupplyRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun listTypes() = api.getSupplyTypes()
    suspend fun createType(request: CreateSupplyTypeRequest) = api.createSupplyType(request)
    suspend fun updateType(id: Long, request: UpdateSupplyTypeRequest) = api.updateSupplyType(id, request)
    suspend fun deleteType(id: Long) = api.deleteSupplyType(id)

    suspend fun listInventory() = api.getSupplyInventory()
    suspend fun createInventory(request: CreateSupplyInventoryRequest) = api.createSupplyInventory(request)
    suspend fun decrement(id: Long, quantity: Double) = api.decrementSupply(id, DecrementSupplyRequest(quantity))
}
