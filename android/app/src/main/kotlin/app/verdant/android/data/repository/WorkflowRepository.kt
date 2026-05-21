package app.verdant.android.data.repository

import app.verdant.android.data.api.VerdantApi
import app.verdant.android.data.model.AssignWorkflowTemplateRequest
import app.verdant.android.data.model.CompleteWorkflowStepRequest
import app.verdant.android.data.model.CreateWorkflowStepRequest
import app.verdant.android.data.model.UpdateWorkflowStepRequest
import javax.inject.Inject
import javax.inject.Singleton

/** Species workflows + plant workflow progress + per-plant step CRUD. */
@Singleton
class WorkflowRepository @Inject constructor(private val api: VerdantApi) {
    suspend fun templates() = api.getWorkflowTemplates()
    suspend fun speciesWorkflow(speciesId: Long) = api.getSpeciesWorkflow(speciesId)
    suspend fun assignTemplate(speciesId: Long, templateId: Long) =
        api.assignSpeciesWorkflow(speciesId, AssignWorkflowTemplateRequest(templateId))
    suspend fun syncSpecies(speciesId: Long) = api.syncSpeciesWorkflow(speciesId)

    suspend fun plantProgress(plantId: Long) = api.getPlantWorkflowProgress(plantId)
    suspend fun addPlantStep(plantId: Long, request: CreateWorkflowStepRequest) =
        api.addPlantWorkflowStep(plantId, request)
    suspend fun updatePlantStep(stepId: Long, request: UpdateWorkflowStepRequest) =
        api.updatePlantWorkflowStep(stepId, request)
    suspend fun deletePlantStep(stepId: Long) = api.deletePlantWorkflowStep(stepId)
    suspend fun completePlantStep(stepId: Long) = api.completePlantWorkflowStep(stepId)
    suspend fun resyncPlant(plantId: Long) = api.resyncPlantWorkflow(plantId)

    suspend fun completeStep(stepId: Long, request: CompleteWorkflowStepRequest) =
        api.completeWorkflowStep(stepId, request)
    suspend fun plantsAtStep(stepId: Long, speciesId: Long) = api.getPlantsAtStep(stepId, speciesId)
}
