package app.verdant.service

import app.verdant.dto.SeedBatchAllocation
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
 * Plans seed batches: counts back through the variety's workflow from the date the plants should be
 * ready, and creates one dated task per step. The seeds are drawn from the chosen batches in order, so
 * a plan can sow more than one batch holds; each batch drawn from gets its own sowing task, and later
 * steps handle the plants.
 */
@ApplicationScoped
class SeedBatchPlanService(
    private val inventoryRepository: SeedInventoryRepository,
    private val workflowRepository: WorkflowRepository,
    private val taskRepository: ScheduledTaskRepository,
) {
    fun preview(batchIds: List<Long>, request: SeedBatchPlanRequest, orgId: Long): SeedBatchPlanResponse {
        val batches = ownedBatches(batchIds, orgId)
        val allocations = allocate(batches, request.seeds)
        return response(batches, request, steps(batches.first().speciesId, request), allocations)
    }

    @Transactional
    fun create(batchIds: List<Long>, request: SeedBatchPlanRequest, orgId: Long, today: LocalDate = LocalDate.now()): SeedBatchPlanResponse {
        val batches = ownedBatches(batchIds, orgId)
        val allocations = allocate(batches, request.seeds)
        val steps = steps(batches.first().speciesId, request)
        val start = steps.first().date
        if (start.isBefore(today)) throw BadRequestException("The plan would have to start on $start, which has already passed")

        val speciesId = batches.first().speciesId
        val firstBatchId = allocations.first().seedBatchId
        val taskIds = steps.filterNot { it.optional }.flatMap { step ->
            val parts = if (step.activityType == "SOW") allocations.map { it.seedBatchId to it.seeds } else listOf(firstBatchId to step.count)
            parts.map { (batchId, count) ->
                val task = taskRepository.persist(
                    ScheduledTask(
                        orgId = orgId,
                        speciesId = speciesId,
                        activityType = step.activityType,
                        deadline = step.date,
                        targetCount = count,
                        remainingCount = count,
                        notes = step.name,
                        seedInventoryId = batchId,
                    )
                )
                taskRepository.setAcceptableSpecies(task.id!!, listOf(speciesId))
                task.id
            }
        }
        return response(batches, request, steps, allocations, taskIds)
    }

    private fun response(
        batches: List<SeedInventory>,
        request: SeedBatchPlanRequest,
        steps: List<SeedBatchPlanStep>,
        allocations: List<SeedBatchAllocation>,
        taskIds: List<Long> = emptyList(),
    ) = SeedBatchPlanResponse(
        seedBatchId = allocations.first().seedBatchId,
        speciesId = batches.first().speciesId,
        seeds = request.seeds,
        plants = request.plants,
        readyDate = request.readyDate,
        steps = steps,
        allocations = allocations,
        taskIds = taskIds,
    )

    private fun ownedBatches(batchIds: List<Long>, orgId: Long): List<SeedInventory> {
        if (batchIds.isEmpty()) throw BadRequestException("Choose at least one seed batch")
        val batches = batchIds.distinct().map { id ->
            inventoryRepository.findById(id)?.takeIf { it.orgId == orgId } ?: throw NotFoundException("Seed batch not found")
        }
        if (batches.map { it.speciesId }.distinct().size > 1) throw BadRequestException("The seed batches belong to different varieties")
        return batches
    }

    /** Draws the seeds from the batches in order; a batch the seeds don't reach is left out. */
    private fun allocate(batches: List<SeedInventory>, seeds: Int): List<SeedBatchAllocation> {
        val available = batches.sumOf { it.quantity }
        if (seeds > available) {
            throw BadRequestException(if (batches.size == 1) "The batch holds only $available seeds" else "The batches hold only $available seeds")
        }
        var left = seeds
        return batches.mapNotNull { batch ->
            val take = minOf(left, batch.quantity)
            left -= take
            if (take > 0) SeedBatchAllocation(batch.id!!, take) else null
        }
    }

    private fun steps(speciesId: Long, request: SeedBatchPlanRequest): List<SeedBatchPlanStep> {
        if (request.plants > request.seeds) throw BadRequestException("More plants than seeds: ${request.plants} from ${request.seeds}")
        val workflow = workflowRepository.findStepsBySpeciesId(speciesId)
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
