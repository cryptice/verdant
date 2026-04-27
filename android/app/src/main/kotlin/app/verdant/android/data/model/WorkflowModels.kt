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

data class PlantWorkflowProgressResponse(
    @SerializedName("steps") val steps: List<SpeciesWorkflowStepResponse>,
    @SerializedName("completedStepIds") val completedStepIds: List<Long>,
    @SerializedName("currentStepId") val currentStepId: Long?,
    @SerializedName("activeSideBranches") val activeSideBranches: List<String>,
)

data class CompleteWorkflowStepRequest(
    @SerializedName("plantIds") val plantIds: List<Long>,
    @SerializedName("notes") val notes: String? = null,
)
