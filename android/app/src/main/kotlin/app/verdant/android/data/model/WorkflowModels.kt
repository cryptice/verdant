package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Workflows ──

data class SpeciesWorkflowResponse(
    @SerializedName("templateId") val templateId: Long?,
    @SerializedName("templateName") val templateName: String?,
    @SerializedName("steps") val steps: List<SpeciesWorkflowStepResponse>,
)

data class SpeciesWorkflowStepResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("templateStepId") val templateStepId: Long?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("eventType") val eventType: String?,
    @SerializedName("daysAfterPrevious") val daysAfterPrevious: Int?,
    @SerializedName("isOptional") val isOptional: Boolean,
    @SerializedName("isSideBranch") val isSideBranch: Boolean,
    @SerializedName("sideBranchName") val sideBranchName: String?,
    @SerializedName("sortOrder") val sortOrder: Int,
    @SerializedName("suggestedSupplyTypeId") val suggestedSupplyTypeId: Long? = null,
    @SerializedName("suggestedQuantity") val suggestedQuantity: Double? = null,
)

data class PlantWorkflowStepResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("plantId") val plantId: Long,
    @SerializedName("speciesStepId") val speciesStepId: Long?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("eventType") val eventType: String?,
    @SerializedName("daysAfterPrevious") val daysAfterPrevious: Int?,
    @SerializedName("isOptional") val isOptional: Boolean,
    @SerializedName("isSideBranch") val isSideBranch: Boolean,
    @SerializedName("sideBranchName") val sideBranchName: String?,
    @SerializedName("sortOrder") val sortOrder: Int,
    @SerializedName("suggestedSupplyTypeId") val suggestedSupplyTypeId: Long? = null,
    @SerializedName("suggestedQuantity") val suggestedQuantity: Double? = null,
)

data class PlantWorkflowProgressResponse(
    @SerializedName("steps") val steps: List<PlantWorkflowStepResponse>,
    @SerializedName("completedStepIds") val completedStepIds: List<Long>,
    @SerializedName("currentStepId") val currentStepId: Long?,
    @SerializedName("activeSideBranches") val activeSideBranches: List<String>,
)

data class CompleteWorkflowStepRequest(
    @SerializedName("plantIds") val plantIds: List<Long>,
    @SerializedName("notes") val notes: String? = null,
)

data class AssignWorkflowTemplateRequest(
    @SerializedName("templateId") val templateId: Long,
)

data class CreateWorkflowStepRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("eventType") val eventType: String? = null,
    @SerializedName("daysAfterPrevious") val daysAfterPrevious: Int? = null,
    @SerializedName("isOptional") val isOptional: Boolean = false,
    @SerializedName("isSideBranch") val isSideBranch: Boolean = false,
    @SerializedName("sideBranchName") val sideBranchName: String? = null,
    @SerializedName("sortOrder") val sortOrder: Int = 0,
)

data class UpdateWorkflowStepRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("eventType") val eventType: String? = null,
    @SerializedName("daysAfterPrevious") val daysAfterPrevious: Int? = null,
    @SerializedName("isOptional") val isOptional: Boolean? = null,
)

data class WorkflowTemplateResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
)
