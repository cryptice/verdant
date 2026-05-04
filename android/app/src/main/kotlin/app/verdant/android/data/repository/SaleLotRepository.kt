package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.ChangeOutletRequest
import app.verdant.android.data.model.ChangePriceRequest
import app.verdant.android.data.model.CreateSaleLotForHarvestRequest
import app.verdant.android.data.model.CreateSaleLotForPlantRequest
import app.verdant.android.data.model.ReturnFromOutletRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Sale-lot CRUD + actions (markdown, change outlet, return, NOT_SOLD, delete). */
@Singleton
class SaleLotRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list(status: String? = null, sourceKind: String? = null) =
        api.getSaleLots(status = status, sourceKind = sourceKind)

    suspend fun detail(id: Long) = api.getSaleLotDetail(id)

    suspend fun createForPlant(request: CreateSaleLotForPlantRequest) = api.createSaleLotForPlant(request)
    suspend fun createForHarvest(request: CreateSaleLotForHarvestRequest) = api.createSaleLotForHarvest(request)

    suspend fun changePrice(id: Long, newPriceCents: Int) =
        api.changeSaleLotPrice(id, ChangePriceRequest(newPriceCents))

    suspend fun changeOutlet(id: Long, newOutletId: Long) =
        api.changeSaleLotOutlet(id, ChangeOutletRequest(newOutletId))

    suspend fun markReturnedFromOutlet(id: Long, fromOutletId: Long) =
        api.markSaleLotReturned(id, ReturnFromOutletRequest(fromOutletId))

    suspend fun markNotSold(id: Long) = api.markSaleLotNotSold(id)

    suspend fun delete(id: Long) = api.deleteSaleLot(id)

    suspend fun availableForPlant(plantId: Long) = api.availableForPlant(plantId).available
    suspend fun availableForHarvestEvent(eventId: Long) = api.availableForHarvestEvent(eventId).available
    suspend fun availableForBouquet(bouquetId: Long) = api.availableForBouquet(bouquetId).available
}
