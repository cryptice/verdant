package app.verdant.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class WorkflowTemplateResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val steps: List<WorkflowStepResponse>,
    val createdAt: Instant,
)

data class WorkflowStepResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val eventType: String?,
    val daysAfterPrevious: Int?,
    @get:JsonProperty("isOptional") val isOptional: Boolean,
    @get:JsonProperty("isSideBranch") val isSideBranch: Boolean,
    val sideBranchName: String?,
    val sortOrder: Int,
)

data class CreateWorkflowTemplateRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
)

data class UpdateWorkflowTemplateRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
)

data class CreateWorkflowStepRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Size(max = 2000)
    val description: String? = null,
    val eventType: String? = null,
    val daysAfterPrevious: Int? = null,
    @JsonProperty("isOptional") val isOptional: Boolean = false,
    @JsonProperty("isSideBranch") val isSideBranch: Boolean = false,
    @field:Size(max = 255)
    val sideBranchName: String? = null,
    val sortOrder: Int = 0,
)

data class UpdateWorkflowStepRequest(
    @field:Size(max = 255)
    val name: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    val eventType: String? = null,
    val daysAfterPrevious: Int? = null,
    @JsonProperty("isOptional") val isOptional: Boolean? = null,
    @JsonProperty("isSideBranch") val isSideBranch: Boolean? = null,
    @field:Size(max = 255)
    val sideBranchName: String? = null,
    val sortOrder: Int? = null,
)

data class SpeciesWorkflowResponse(
    val templateId: Long?,
    val templateName: String?,
    val steps: List<SpeciesWorkflowStepResponse>,
)

data class SpeciesWorkflowStepResponse(
    val id: Long,
    val templateStepId: Long?,
    val name: String,
    val description: String?,
    val eventType: String?,
    val daysAfterPrevious: Int?,
    @get:JsonProperty("isOptional") val isOptional: Boolean,
    @get:JsonProperty("isSideBranch") val isSideBranch: Boolean,
    val sideBranchName: String?,
    val sortOrder: Int,
)

data class PlantWorkflowProgressResponse(
    val steps: List<SpeciesWorkflowStepResponse>,
    val completedStepIds: List<Long>,
    val currentStepId: Long?,
    val activeSideBranches: List<String>,
)

data class CompleteWorkflowStepRequest(
    @field:NotNull
    val plantIds: List<Long>,
    val notes: String? = null,
)

data class AssignWorkflowTemplateRequest(
    @field:NotNull
    val templateId: Long,
)

data class StartSideBranchRequest(
    @field:NotNull
    val plantIds: List<Long>,
    @field:NotBlank
    val sideBranchName: String,
)
