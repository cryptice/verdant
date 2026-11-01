package app.verdant.service

import app.verdant.dto.SeedBatchPlanRequest
import app.verdant.dto.SeedBatchPlanResponse
import app.verdant.dto.SeedBatchPlanStep
import app.verdant.entity.ScheduledTask
import app.verdant.entity.SeedInventory
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SeedInventoryRepository
import app.verdant.repository.WorkflowRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.time.LocalDate

/**
 * Plans a seed batch: counts back through the variety's workflow from the date the plants should be
 * ready, and creates one dated task per step. The sowing task draws the seeds; later steps handle the plants.
 */
@ApplicationScoped
class SeedBatchPlanService(
    private val inventoryRepository: SeedInventoryRepository,
    private val workflowRepository: WorkflowRepository,
    private val taskRepository: ScheduledTaskRepository,
) {
    fun preview(batchId: Long, request: SeedBatchPlanRequest, orgId: Long): SeedBatchPlanResponse {
        val batch = ownedBatch(batchId, orgId)
        return SeedBatchPlanResponse(batch.id!!, batch.speciesId, request.seeds, request.plants, request.readyDate, steps(batch, request))
    }

    @Transactional
    fun create(batchId: Long, request: SeedBatchPlanRequest, orgId: Long, today: LocalDate = LocalDate.now()): SeedBatchPlanResponse {
        val batch = ownedBatch(batchId, orgId)
        val steps = steps(batch, request)
        val start = steps.first().date
        if (start.isBefore(today)) throw BadRequestException("The plan would have to start on $start, which has already passed")
        val taskIds = steps.filterNot { it.optional }.map { step ->
            val task = taskRepository.persist(
                ScheduledTask(
                    orgId = orgId,
                    speciesId = batch.speciesId,
                    activityType = step.activityType,
                    deadline = step.date,
                    targetCount = step.count,
                    remainingCount = step.count,
                    notes = step.name,
                    seedInventoryId = batch.id,
                )
            )
            taskRepository.setAcceptableSpecies(task.id!!, listOf(batch.speciesId))
            task.id
        }
        return SeedBatchPlanResponse(batch.id!!, batch.speciesId, request.seeds, request.plants, request.readyDate, steps, taskIds)
    }

    private fun ownedBatch(batchId: Long, orgId: Long): SeedInventory {
        val batch = inventoryRepository.findById(batchId) ?: throw NotFoundException("Seed batch not found")
        if (batch.orgId != orgId) throw NotFoundException("Seed batch not found")
        return batch
    }

    private fun steps(batch: SeedInventory, request: SeedBatchPlanRequest): List<SeedBatchPlanStep> {
        if (request.plants > request.seeds) throw BadRequestException("More plants than seeds: ${request.plants} from ${request.seeds}")
        if (request.seeds > batch.quantity) throw BadRequestException("The batch holds only ${batch.quantity} seeds")
        val workflow = workflowRepository.findStepsBySpeciesId(batch.speciesId)
            .filterNot { it.isSideBranch }
            .sortedBy { it.sortOrder }
        if (workflow.isEmpty()) throw BadRequestException("The variety has no workflow to plan from")
        workflow.drop(1).firstOrNull { it.daysAfterPrevious == null }?.let {
            throw BadRequestException("The workflow step \"${it.name}\" has no number of days")
        }

        // The last step lands on the ready date; each earlier one comes the next step's interval before it.
        val dates = MutableList(workflow.size) { request.readyDate }
        for (i in workflow.lastIndex downTo 1) dates[i - 1] = dates[i].minusDays(workflow[i].daysAfterPrevious!!.toLong())

        return workflow.mapIndexed { i, step ->
            val activity = activityFor(step.eventType)
            SeedBatchPlanStep(
                name = step.name,
                activityType = activity,
                date = dates[i],
                count = if (activity == "SOW") request.seeds else request.plants,
                optional = step.isOptional,
            )
        }
    }

    private fun activityFor(eventType: String?): String = when (eventType) {
        "SEEDED" -> "SOW"
        "POTTED_UP" -> "POT_UP"
        "PLANTED_OUT" -> "PLANT"
        "HARVESTED" -> "HARVEST"
        "PINCHED" -> "PINCH"
        "DISBUDDED" -> "DISBUD"
        "WATERED" -> "WATER"
        "APPLIED_SUPPLY" -> "FERTILIZE"
        // Moving, buds, blooms and the final check have no activity of their own; the task notes name the step.
        else -> "INSPECT"
    }
}
