package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateOutletRequest
import app.verdant.android.data.model.OutletResponse
import app.verdant.android.data.model.UpdateOutletRequest
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

/** Outlet CRUD scoped to the user's org context. Interface so ViewModels can be tested with a fake. */
interface OutletRepository {
    suspend fun list(): List<OutletResponse>
    suspend fun create(request: CreateOutletRequest): OutletResponse
    suspend fun update(id: Long, request: UpdateOutletRequest): OutletResponse
    suspend fun delete(id: Long): Response<Unit>
}

@Singleton
class OutletRepositoryImpl @Inject constructor(private val api: VerdantApi) : OutletRepository {
    override suspend fun list() = api.getOutlets()
    override suspend fun create(request: CreateOutletRequest) = api.createOutlet(request)
    override suspend fun update(id: Long, request: UpdateOutletRequest) = api.updateOutlet(id, request)
    override suspend fun delete(id: Long) = api.deleteOutlet(id)
}
