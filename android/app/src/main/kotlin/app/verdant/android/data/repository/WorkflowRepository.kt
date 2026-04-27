package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.CompleteWorkflowStepRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Species workflows + plant workflow progress. */
@Singleton
class WorkflowRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun speciesWorkflow(speciesId: Long) = api.getSpeciesWorkflow(speciesId)
    suspend fun plantProgress(plantId: Long) = api.getPlantWorkflowProgress(plantId)
    suspend fun completeStep(stepId: Long, request: CompleteWorkflowStepRequest) =
        api.completeWorkflowStep(stepId, request)
    suspend fun plantsAtStep(stepId: Long, speciesId: Long) = api.getPlantsAtStep(stepId, speciesId)
}
