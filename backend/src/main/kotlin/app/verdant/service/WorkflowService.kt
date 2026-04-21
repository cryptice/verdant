package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.SpeciesWorkflowStep
import app.verdant.entity.WorkflowTemplate
import app.verdant.entity.WorkflowTemplateStep
import app.verdant.repository.PlantRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.WorkflowRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class WorkflowService(
    private val workflowRepository: WorkflowRepository,
    private val speciesRepository: SpeciesRepository,
    private val plantRepository: PlantRepository,
) {

    // ── Template CRUD ──

    fun getTemplates(orgId: Long): List<WorkflowTemplateResponse> {
        val templates = workflowRepository.findTemplatesByOrgId(orgId)
        return templates.map { template ->
            val steps = workflowRepository.findStepsByTemplateId(template.id!!)
            template.toResponse(steps)
        }
    }

    fun getTemplate(templateId: Long, orgId: Long): WorkflowTemplateResponse {
        val template = checkTemplateOwnership(templateId, orgId)
        val steps = workflowRepository.findStepsByTemplateId(template.id!!)
        return template.toResponse(steps)
    }

    fun createTemplate(request: CreateWorkflowTemplateRequest, orgId: Long): WorkflowTemplateResponse {
        val template = workflowRepository.persistTemplate(
            WorkflowTemplate(
                orgId = orgId,
                name = request.name,
                description = request.description,
            )
        )
        return template.toResponse(emptyList())
    }

    fun updateTemplate(templateId: Long, request: UpdateWorkflowTemplateRequest, orgId: Long): WorkflowTemplateResponse {
        val template = checkTemplateOwnership(templateId, orgId)
        val updated = template.copy(
            name = request.name ?: template.name,
            description = request.description ?: template.description,
        )
        workflowRepository.updateTemplate(updated)
        val steps = workflowRepository.findStepsByTemplateId(templateId)
        return updated.toResponse(steps)
    }

    fun deleteTemplate(templateId: Long, orgId: Long) {
        checkTemplateOwnership(templateId, orgId)
        workflowRepository.deleteTemplate(templateId)
    }

    fun addTemplateStep(templateId: Long, request: CreateWorkflowStepRequest, orgId: Long): WorkflowStepResponse {
        checkTemplateOwnership(templateId, orgId)
        val step = workflowRepository.persistStep(
            WorkflowTemplateStep(
                templateId = templateId,
                name = request.name,
                description = request.description,
                eventType = request.eventType,
                daysAfterPrevious = request.daysAfterPrevious,
                isOptional = request.isOptional,
                isSideBranch = request.isSideBranch,
                sideBranchName = request.sideBranchName,
                sortOrder = request.sortOrder,
                suggestedSupplyTypeId = request.suggestedSupplyTypeId,
                suggestedQuantity = request.suggestedQuantity,
            )
        )
        return step.toResponse()
    }

    fun updateTemplateStep(stepId: Long, request: UpdateWorkflowStepRequest, orgId: Long): WorkflowStepResponse {
        val step = findTemplateStepById(stepId, orgId)
        val clear = request.clearSuggestedSupply == true
        val updated = step.copy(
            name = request.name ?: step.name,
            description = request.description ?: step.description,
            eventType = request.eventType ?: step.eventType,
            daysAfterPrevious = request.daysAfterPrevious ?: step.daysAfterPrevious,
            isOptional = request.isOptional ?: step.isOptional,
            isSideBranch = request.isSideBranch ?: step.isSideBranch,
            sideBranchName = request.sideBranchName ?: step.sideBranchName,
            sortOrder = request.sortOrder ?: step.sortOrder,
            suggestedSupplyTypeId = if (clear) null else request.suggestedSupplyTypeId ?: step.suggestedSupplyTypeId,
            suggestedQuantity = if (clear) null else request.suggestedQuantity ?: step.suggestedQuantity,
        )
        workflowRepository.updateStep(updated)
        return updated.toResponse()
    }

    fun deleteTemplateStep(stepId: Long, orgId: Long) {
        findTemplateStepById(stepId, orgId)
        workflowRepository.deleteStep(stepId)
    }

    // ── Species Workflow ──

    fun assignTemplateToSpecies(speciesId: Long, templateId: Long, orgId: Long): SpeciesWorkflowResponse {
        val template = checkTemplateOwnership(templateId, orgId)
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.orgId != orgId) throw NotFoundException("Species not found")

        // Delete existing species steps
        workflowRepository.deleteStepsBySpeciesId(speciesId)

        // Copy template steps to species workflow steps
        val templateSteps = workflowRepository.findStepsByTemplateId(templateId)
        val speciesSteps = templateSteps.map { ts ->
            workflowRepository.persistSpeciesStep(
                SpeciesWorkflowStep(
                    speciesId = speciesId,
                    templateStepId = ts.id,
                    name = ts.name,
                    description = ts.description,
                    eventType = ts.eventType,
                    daysAfterPrevious = ts.daysAfterPrevious,
                    isOptional = ts.isOptional,
                    isSideBranch = ts.isSideBranch,
                    sideBranchName = ts.sideBranchName,
                    sortOrder = ts.sortOrder,
                    suggestedSupplyTypeId = ts.suggestedSupplyTypeId,
                    suggestedQuantity = ts.suggestedQuantity,
                )
            )
        }

        // Update species.workflow_template_id
        speciesRepository.updateWorkflowTemplateId(speciesId, templateId)

        return SpeciesWorkflowResponse(
            templateId = templateId,
            templateName = template.name,
            steps = speciesSteps.map { it.toSpeciesStepResponse() },
        )
    }

    fun getSpeciesWorkflow(speciesId: Long, orgId: Long): SpeciesWorkflowResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.orgId != orgId) throw NotFoundException("Species not found")

        val steps = workflowRepository.findStepsBySpeciesId(speciesId)
        val templateName = species.workflowTemplateId?.let { workflowRepository.findTemplateById(it)?.name }

        return SpeciesWorkflowResponse(
            templateId = species.workflowTemplateId,
            templateName = templateName,
            steps = steps.map { it.toSpeciesStepResponse() },
        )
    }

    fun addSpeciesStep(speciesId: Long, request: CreateWorkflowStepRequest, orgId: Long): SpeciesWorkflowStepResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.orgId != orgId) throw NotFoundException("Species not found")

        val step = workflowRepository.persistSpeciesStep(
            SpeciesWorkflowStep(
                speciesId = speciesId,
                name = request.name,
                description = request.description,
                eventType = request.eventType,
                daysAfterPrevious = request.daysAfterPrevious,
                isOptional = request.isOptional,
                isSideBranch = request.isSideBranch,
                sideBranchName = request.sideBranchName,
                sortOrder = request.sortOrder,
                suggestedSupplyTypeId = request.suggestedSupplyTypeId,
                suggestedQuantity = request.suggestedQuantity,
            )
        )
        return step.toSpeciesStepResponse()
    }

    fun updateSpeciesStep(stepId: Long, request: UpdateWorkflowStepRequest, orgId: Long): SpeciesWorkflowStepResponse {
        val step = findSpeciesStepById(stepId, orgId)
        val clear = request.clearSuggestedSupply == true
        val updated = step.copy(
            name = request.name ?: step.name,
            description = request.description ?: step.description,
            eventType = request.eventType ?: step.eventType,
            daysAfterPrevious = request.daysAfterPrevious ?: step.daysAfterPrevious,
            isOptional = request.isOptional ?: step.isOptional,
            isSideBranch = request.isSideBranch ?: step.isSideBranch,
            sideBranchName = request.sideBranchName ?: step.sideBranchName,
            sortOrder = request.sortOrder ?: step.sortOrder,
            suggestedSupplyTypeId = if (clear) null else request.suggestedSupplyTypeId ?: step.suggestedSupplyTypeId,
            suggestedQuantity = if (clear) null else request.suggestedQuantity ?: step.suggestedQuantity,
        )
        workflowRepository.updateSpeciesStep(updated)
        return updated.toSpeciesStepResponse()
    }

    fun deleteSpeciesStep(stepId: Long, orgId: Long) {
        findSpeciesStepById(stepId, orgId)
        workflowRepository.deleteSpeciesStep(stepId)
    }

    fun syncSpeciesWithTemplate(speciesId: Long, orgId: Long): SpeciesWorkflowResponse {
        val species = speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        if (species.orgId != orgId) throw NotFoundException("Species not found")
        val templateId = species.workflowTemplateId ?: throw NotFoundException("Species has no workflow template")

        val template = workflowRepository.findTemplateById(templateId) ?: throw NotFoundException("Workflow template not found")
        val templateSteps = workflowRepository.findStepsByTemplateId(templateId)
        val currentSteps = workflowRepository.findStepsBySpeciesId(speciesId)

        val existingTemplateStepIds = currentSteps.mapNotNull { it.templateStepId }.toSet()

        // Add new steps from template that are not already in species steps
        val newSteps = templateSteps.filter { it.id !in existingTemplateStepIds }
        for (ts in newSteps) {
            workflowRepository.persistSpeciesStep(
                SpeciesWorkflowStep(
                    speciesId = speciesId,
                    templateStepId = ts.id,
                    name = ts.name,
                    description = ts.description,
                    eventType = ts.eventType,
                    daysAfterPrevious = ts.daysAfterPrevious,
                    isOptional = ts.isOptional,
                    isSideBranch = ts.isSideBranch,
                    sideBranchName = ts.sideBranchName,
                    sortOrder = ts.sortOrder,
                )
            )
        }

        val allSteps = workflowRepository.findStepsBySpeciesId(speciesId)
        return SpeciesWorkflowResponse(
            templateId = templateId,
            templateName = template.name,
            steps = allSteps.map { it.toSpeciesStepResponse() },
        )
    }

    // ── Plant Progress ──

    fun getPlantProgress(plantId: Long, orgId: Long): PlantWorkflowProgressResponse {
        val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant not found")
        if (plant.orgId != orgId) throw NotFoundException("Plant not found")

        val speciesId = plant.speciesId ?: return PlantWorkflowProgressResponse(
            steps = emptyList(),
            completedStepIds = emptyList(),
            currentStepId = null,
            activeSideBranches = emptyList(),
        )

        val steps = workflowRepository.findStepsBySpeciesId(speciesId)
        val progress = workflowRepository.findProgressByPlantId(plantId)
        val completedStepIds = progress.map { it.stepId }
        val completedSet = completedStepIds.toSet()

        // Current step = first main-flow required step not completed
        val currentStepId = steps
            .filter { !it.isSideBranch && !it.isOptional }
            .firstOrNull { it.id!! !in completedSet }
            ?.id

        // Active side-branches = side-branch names where at least 1 step completed but not all
        val sideBranchSteps = steps.filter { it.isSideBranch && it.sideBranchName != null }
            .groupBy { it.sideBranchName!! }
        val activeSideBranches = sideBranchSteps.filter { (_, branchSteps) ->
            val branchIds = branchSteps.map { it.id!! }.toSet()
            val completedInBranch = branchIds.intersect(completedSet)
            completedInBranch.isNotEmpty() && completedInBranch.size < branchIds.size
        }.keys.toList()

        return PlantWorkflowProgressResponse(
            steps = steps.map { it.toSpeciesStepResponse() },
            completedStepIds = completedStepIds,
            currentStepId = currentStepId,
            activeSideBranches = activeSideBranches,
        )
    }

    fun completeStep(stepId: Long, request: CompleteWorkflowStepRequest, orgId: Long): Int {
        val step = findSpeciesStepById(stepId, orgId)

        for (plantId in request.plantIds) {
            val plant = plantRepository.findById(plantId) ?: throw NotFoundException("Plant $plantId not found")
            if (plant.orgId != orgId) throw NotFoundException("Plant $plantId not found")
            workflowRepository.recordProgress(plantId, stepId)
        }

        return request.plantIds.size
    }

    fun getPlantsAtStep(speciesId: Long, stepId: Long, orgId: Long): List<Long> {
        return workflowRepository.findPlantIdsByIncompleteStep(speciesId, stepId, orgId)
    }

    fun startSideBranch(request: StartSideBranchRequest, orgId: Long) {
        if (request.plantIds.isEmpty()) return

        // Get species from first plant
        val firstPlant = plantRepository.findById(request.plantIds.first())
            ?: throw NotFoundException("Plant not found")
        if (firstPlant.orgId != orgId) throw NotFoundException("Plant not found")
        val speciesId = firstPlant.speciesId ?: throw NotFoundException("Plant has no species")

        val steps = workflowRepository.findStepsBySpeciesId(speciesId)
        val firstBranchStep = steps
            .filter { it.isSideBranch && it.sideBranchName == request.sideBranchName }
            .minByOrNull { it.sortOrder }
            ?: throw NotFoundException("Side branch '${request.sideBranchName}' not found")

        workflowRepository.recordProgressBatch(request.plantIds, firstBranchStep.id!!)
    }

    // ── Helpers ──

    private fun checkTemplateOwnership(templateId: Long, orgId: Long): WorkflowTemplate {
        val template = workflowRepository.findTemplateById(templateId) ?: throw NotFoundException("Workflow template not found")
        if (template.orgId != orgId) throw NotFoundException("Workflow template not found")
        return template
    }

    private fun findTemplateStepById(stepId: Long, orgId: Long): WorkflowTemplateStep {
        val step = workflowRepository.findTemplateStepById(stepId) ?: throw NotFoundException("Workflow template step not found")
        val template = workflowRepository.findTemplateById(step.templateId) ?: throw NotFoundException("Workflow template not found")
        if (template.orgId != orgId) throw NotFoundException("Workflow template step not found")
        return step
    }

    private fun findSpeciesStepById(stepId: Long, orgId: Long): SpeciesWorkflowStep {
        val step = workflowRepository.findSpeciesStepById(stepId) ?: throw NotFoundException("Species workflow step not found")
        val species = speciesRepository.findById(step.speciesId) ?: throw NotFoundException("Species not found")
        if (species.orgId != orgId) throw NotFoundException("Species workflow step not found")
        return step
    }

    // ── Mapping ──

    private fun WorkflowTemplate.toResponse(steps: List<WorkflowTemplateStep>) = WorkflowTemplateResponse(
        id = id!!,
        name = name,
        description = description,
        steps = steps.map { it.toResponse() },
        createdAt = createdAt,
    )

    private fun WorkflowTemplateStep.toResponse() = WorkflowStepResponse(
        id = id!!,
        name = name,
        description = description,
        eventType = eventType,
        daysAfterPrevious = daysAfterPrevious,
        isOptional = isOptional,
        isSideBranch = isSideBranch,
        sideBranchName = sideBranchName,
        sortOrder = sortOrder,
        suggestedSupplyTypeId = suggestedSupplyTypeId,
        suggestedQuantity = suggestedQuantity,
    )

    private fun SpeciesWorkflowStep.toSpeciesStepResponse() = SpeciesWorkflowStepResponse(
        id = id!!,
        templateStepId = templateStepId,
        name = name,
        description = description,
        eventType = eventType,
        daysAfterPrevious = daysAfterPrevious,
        isOptional = isOptional,
        isSideBranch = isSideBranch,
        sideBranchName = sideBranchName,
        sortOrder = sortOrder,
        suggestedSupplyTypeId = suggestedSupplyTypeId,
        suggestedQuantity = suggestedQuantity,
    )
}
