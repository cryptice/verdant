package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateOutletRequest
import app.verdant.android.data.model.UpdateOutletRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Outlet CRUD scoped to the user's org context. */
@Singleton
class OutletRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun list() = api.getOutlets()
    suspend fun create(request: CreateOutletRequest) = api.createOutlet(request)
    suspend fun update(id: Long, request: UpdateOutletRequest) = api.updateOutlet(id, request)
    suspend fun delete(id: Long) = api.deleteOutlet(id)
}
