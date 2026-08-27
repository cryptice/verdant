package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CreateMaintenanceRuleRequest
import app.verdant.android.data.model.MaintenanceRuleResponse
import app.verdant.android.data.model.UpdateMaintenanceRuleRequest
import javax.inject.Inject
import javax.inject.Singleton

interface MaintenanceRuleRepository {
    /** Pass exactly one of [bedId] / [areaId], or neither for the whole org. */
    suspend fun list(bedId: Long? = null, areaId: Long? = null): List<MaintenanceRuleResponse>
    suspend fun create(request: CreateMaintenanceRuleRequest): MaintenanceRuleResponse
    suspend fun update(id: Long, request: UpdateMaintenanceRuleRequest): MaintenanceRuleResponse
    suspend fun delete(id: Long)
}

@Singleton
class DefaultMaintenanceRuleRepository @Inject constructor(
    private val api: VerdantApi,
) : MaintenanceRuleRepository {
    override suspend fun list(bedId: Long?, areaId: Long?) = api.getMaintenanceRules(bedId, areaId)
    override suspend fun create(request: CreateMaintenanceRuleRequest) =
        api.createMaintenanceRule(request)
    override suspend fun update(id: Long, request: UpdateMaintenanceRuleRequest) =
        api.updateMaintenanceRule(id, request)
    override suspend fun delete(id: Long) = api.deleteMaintenanceRule(id)
}
