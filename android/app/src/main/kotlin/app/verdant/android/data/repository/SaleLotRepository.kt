package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.ChangeOutletRequest
import app.verdant.android.data.model.ChangePriceRequest
import app.verdant.android.data.model.CreateSaleLotForHarvestRequest
import app.verdant.android.data.model.CreateSaleLotForPlantRequest
import app.verdant.android.data.model.ReturnFromOutletRequest
import app.verdant.android.data.model.SaleLotDetailResponse
import app.verdant.android.data.model.SaleLotResponse
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Sale-lot CRUD + actions. Interface so ViewModels can be tested with a fake. */
interface SaleLotRepository {
    suspend fun list(status: String? = null, sourceKind: String? = null): List<SaleLotResponse>
    suspend fun detail(id: Long): SaleLotDetailResponse
    suspend fun createForPlant(request: CreateSaleLotForPlantRequest): SaleLotResponse
    suspend fun createForHarvest(request: CreateSaleLotForHarvestRequest): SaleLotResponse
    suspend fun changePrice(id: Long, newPriceCents: Int): SaleLotResponse
    suspend fun changeOutlet(id: Long, newOutletId: Long): SaleLotResponse
    suspend fun markReturnedFromOutlet(id: Long, fromOutletId: Long): Response<Unit>
    suspend fun markNotSold(id: Long): SaleLotResponse
    suspend fun delete(id: Long): Response<Unit>
    suspend fun availableForPlant(plantId: Long): Int
    suspend fun availableForHarvestEvent(eventId: Long): Int
    suspend fun availableForBouquet(bouquetId: Long): Int
}

@Singleton
class SaleLotRepositoryImpl @Inject constructor(private val api: VerdantApi) : SaleLotRepository {
    override suspend fun list(status: String?, sourceKind: String?) =
        api.getSaleLots(status = status, sourceKind = sourceKind)

    override suspend fun detail(id: Long) = api.getSaleLotDetail(id)

    override suspend fun createForPlant(request: CreateSaleLotForPlantRequest) = api.createSaleLotForPlant(request)
    override suspend fun createForHarvest(request: CreateSaleLotForHarvestRequest) = api.createSaleLotForHarvest(request)

    override suspend fun changePrice(id: Long, newPriceCents: Int) =
        api.changeSaleLotPrice(id, ChangePriceRequest(newPriceCents))

    override suspend fun changeOutlet(id: Long, newOutletId: Long) =
        api.changeSaleLotOutlet(id, ChangeOutletRequest(newOutletId))

    override suspend fun markReturnedFromOutlet(id: Long, fromOutletId: Long) =
        api.markSaleLotReturned(id, ReturnFromOutletRequest(fromOutletId))

    override suspend fun markNotSold(id: Long) = api.markSaleLotNotSold(id)

    override suspend fun delete(id: Long) = api.deleteSaleLot(id)

    override suspend fun availableForPlant(plantId: Long) = api.availableForPlant(plantId).available
    override suspend fun availableForHarvestEvent(eventId: Long) = api.availableForHarvestEvent(eventId).available
    override suspend fun availableForBouquet(bouquetId: Long) = api.availableForBouquet(bouquetId).available
}
