package app.verdant.entity

import java.time.Instant

data class WorkflowTemplate(
    val id: Long? = null,
    val orgId: Long,
    val name: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class WorkflowTemplateStep(
    val id: Long? = null,
    val templateId: Long,
    val name: String,
    val description: String? = null,
    val eventType: String? = null,
    val daysAfterPrevious: Int? = null,
    val isOptional: Boolean = false,
    val isSideBranch: Boolean = false,
    val sideBranchName: String? = null,
    val sortOrder: Int = 0,
)

data class SpeciesWorkflowStep(
    val id: Long? = null,
    val speciesId: Long,
    val templateStepId: Long? = null,
    val name: String,
    val description: String? = null,
    val eventType: String? = null,
    val daysAfterPrevious: Int? = null,
    val isOptional: Boolean = false,
    val isSideBranch: Boolean = false,
    val sideBranchName: String? = null,
    val sortOrder: Int = 0,
)

data class PlantWorkflowProgress(
    val plantId: Long,
    val stepId: Long,
    val completedAt: Instant = Instant.now(),
)
