package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.BedEvent
import app.verdant.entity.GardenAreaEvent
import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceTarget
import app.verdant.entity.PlantEventType
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import app.verdant.repository.BedEventRepository
import app.verdant.repository.GardenAreaEventRepository
import app.verdant.repository.GardenAreaRepository
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SpeciesGroupRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class ScheduledTaskService(
    private val taskRepository: ScheduledTaskRepository,
    private val speciesRepository: SpeciesRepository,
    private val speciesGroupRepository: SpeciesGroupRepository,
    private val bedRepository: app.verdant.repository.BedRepository,
    private val gardenRepository: app.verdant.repository.GardenRepository,
    private val gardenAreaRepository: GardenAreaRepository,
    private val gardenAreaEventRepository: GardenAreaEventRepository,
    private val bedEventRepository: BedEventRepository,
    private val plantService: PlantService,
) {
    private fun checkOwnership(taskId: Long, orgId: Long): ScheduledTask {
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        if (task.orgId != orgId) throw NotFoundException("Task not found")
        return task
    }

    fun getTasksForUser(orgId: Long, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<ScheduledTaskResponse> {
        val tasks = if (seasonId != null) {
            taskRepository.findBySeasonId(orgId, seasonId, limit, offset)
        } else {
            taskRepository.findByOrgId(orgId, limit, offset)
        }
        return buildResponses(tasks)
    }

    fun getTask(taskId: Long, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)
        return buildResponses(listOf(task)).first()
    }

    fun createTask(request: CreateScheduledTaskRequest, orgId: Long): ScheduledTaskResponse {
        if (request.activityType == TODO_ACTIVITY_TYPE) {
            if (request.speciesId != null || request.speciesIds != null ||
                request.speciesGroupId != null || request.bedId != null) {
                throw BadRequestException("TODO tasks cannot reference species, group, or bed")
            }
            if (request.notes.isNullOrBlank()) {
                throw BadRequestException("TODO tasks require a description in notes")
            }
            val task = taskRepository.persist(
                ScheduledTask(
                    orgId = orgId,
                    speciesId = null,
                    bedId = null,
                    activityType = TODO_ACTIVITY_TYPE,
                    earliestDate = request.earliestDate,
                    deadline = request.deadline,
                    targetCount = 1,
                    remainingCount = 1,
                    notes = request.notes,
                    seasonId = request.seasonId,
                    successionScheduleId = null,
                    originGroupId = null,
                )
            )
            return buildResponses(listOf(task)).first()
        }

        // Non-TODO tasks must declare deadline and target_count; the DTO
        // makes them nullable so the TODO branch can omit them.
        if (request.activityType != TODO_ACTIVITY_TYPE) {
            if (request.deadline == null) throw BadRequestException("deadline is required")
            if (request.targetCount == null || request.targetCount < 1)
                throw BadRequestException("targetCount must be >= 1")
        }

        if (request.bedId != null && request.gardenAreaId != null) {
            throw BadRequestException("A task targets a bed or an area, not both")
        }

        // Bed-scoped maintenance tasks (WATER, FERTILIZE, WEED) don't carry species.
        if (request.bedId != null) {
            if (request.activityType !in BED_ACTIVITY_TYPES)
                throw BadRequestException("activityType ${request.activityType} cannot target a bed")
            val bed = bedRepository.findById(request.bedId)
                ?: throw NotFoundException("Bed not found")
            val garden = gardenRepository.findById(bed.gardenId)
                ?: throw NotFoundException("Bed not found")
            if (garden.orgId != orgId) throw NotFoundException("Bed not found")

            val task = taskRepository.persist(
                ScheduledTask(
                    orgId = orgId,
                    speciesId = null,
                    bedId = request.bedId,
                    activityType = request.activityType,
                    earliestDate = request.earliestDate,
                    deadline = request.deadline,
                    targetCount = request.targetCount!!,
                    remainingCount = request.targetCount!!,
                    notes = request.notes,
                    seasonId = request.seasonId,
                    successionScheduleId = null,
                    originGroupId = null,
                )
            )
            return buildResponses(listOf(task)).first()
        }

        // Area-scoped maintenance tasks don't carry species either.
        if (request.gardenAreaId != null) {
            if (request.activityType !in AREA_ACTIVITY_TYPES)
                throw BadRequestException("activityType ${request.activityType} cannot target an area")
            val area = gardenAreaRepository.findById(request.gardenAreaId)
                ?: throw NotFoundException("Area not found")
            val garden = gardenRepository.findById(area.gardenId)
                ?: throw NotFoundException("Area not found")
            if (garden.orgId != orgId) throw NotFoundException("Area not found")

            val task = taskRepository.persist(
                ScheduledTask(
                    orgId = orgId,
                    speciesId = null,
                    gardenAreaId = request.gardenAreaId,
                    activityType = request.activityType,
                    earliestDate = request.earliestDate,
                    deadline = request.deadline,
                    targetCount = request.targetCount!!,
                    remainingCount = request.targetCount!!,
                    notes = request.notes,
                    seasonId = request.seasonId,
                    successionScheduleId = null,
                    originGroupId = null,
                )
            )
            return buildResponses(listOf(task)).first()
        }

        val acceptableSpeciesIds: List<Long>
        var originGroupId: Long? = null

        if (request.speciesGroupId != null) {
            speciesGroupRepository.findById(request.speciesGroupId)
                ?: throw NotFoundException("Species group not found")
            originGroupId = request.speciesGroupId
            val groupSpecies = speciesRepository.findByGroupId(request.speciesGroupId)
            if (groupSpecies.isEmpty()) throw BadRequestException("Species group is empty")

            acceptableSpeciesIds = if (request.speciesIds != null) {
                val groupIds = groupSpecies.map { it.id!! }.toSet()
                val invalid = request.speciesIds.filter { it !in groupIds }
                if (invalid.isNotEmpty()) throw BadRequestException("Species not in group: $invalid")
                request.speciesIds
            } else {
                groupSpecies.map { it.id!! }
            }
        } else if (request.speciesIds != null && request.speciesIds.size > 1) {
            val found = speciesRepository.findByIds(request.speciesIds.toSet())
            if (found.size != request.speciesIds.size) throw NotFoundException("One or more species not found")
            acceptableSpeciesIds = request.speciesIds
        } else {
            val singleId = request.speciesId ?: request.speciesIds?.firstOrNull()
                ?: throw BadRequestException("Either speciesId, speciesGroupId, speciesIds, or bedId must be provided")
            speciesRepository.findById(singleId) ?: throw NotFoundException("Species not found")
            acceptableSpeciesIds = listOf(singleId)
        }

        val task = taskRepository.persist(
            ScheduledTask(
                orgId = orgId,
                speciesId = if (acceptableSpeciesIds.size == 1) acceptableSpeciesIds.first() else null,
                activityType = request.activityType,
                earliestDate = request.earliestDate,
                deadline = request.deadline,
                targetCount = request.targetCount!!,
                remainingCount = request.targetCount!!,
                notes = request.notes,
                seasonId = request.seasonId,
                successionScheduleId = request.successionScheduleId,
                originGroupId = originGroupId,
            )
        )
        taskRepository.setAcceptableSpecies(task.id!!, acceptableSpeciesIds)
        return buildResponses(listOf(task)).first()
    }

    private val BED_ACTIVITY_TYPES =
        MaintenanceActivity.forTarget(MaintenanceTarget.BED).map { it.name }.toSet()
    private val AREA_ACTIVITY_TYPES =
        MaintenanceActivity.forTarget(MaintenanceTarget.GARDEN_AREA).map { it.name }.toSet()
    private val TODO_ACTIVITY_TYPE = "TODO"

    fun updateTask(taskId: Long, request: UpdateScheduledTaskRequest, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)

        if (task.activityType == TODO_ACTIVITY_TYPE &&
            request.activityType != null && request.activityType != TODO_ACTIVITY_TYPE) {
            throw BadRequestException("Cannot convert a TODO task to another type")
        }
        if (task.activityType != TODO_ACTIVITY_TYPE &&
            request.activityType == TODO_ACTIVITY_TYPE) {
            throw BadRequestException("Cannot convert a non-TODO task to TODO")
        }
        if (task.activityType == TODO_ACTIVITY_TYPE &&
            request.notes != null && request.notes.isBlank()) {
            throw BadRequestException("TODO tasks require a non-blank description")
        }

        val newTarget = request.targetCount ?: task.targetCount
        val newRemaining = if (request.targetCount != null) {
            val completed = task.targetCount - task.remainingCount
            maxOf(newTarget - completed, 0)
        } else {
            task.remainingCount
        }
        val newStatus = if (newRemaining <= 0) ScheduledTaskStatus.COMPLETED else ScheduledTaskStatus.PENDING

        val updated = task.copy(
            activityType = request.activityType ?: task.activityType,
            // earliestDate intentionally not coalesced — the Android/web
            // forms always re-send the current value, so a `null` request
            // field means the user has cleared the date.
            earliestDate = request.earliestDate,
            deadline = request.deadline ?: task.deadline,
            targetCount = newTarget,
            remainingCount = newRemaining,
            status = newStatus,
            notes = request.notes ?: task.notes,
        )
        taskRepository.update(updated)
        return buildResponses(listOf(updated)).first()
    }

    fun completePartially(taskId: Long, speciesId: Long?, processedCount: Int, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)
        when {
            task.activityType == TODO_ACTIVITY_TYPE -> {
                // TODOs are done-or-not; speciesId is irrelevant.
            }
            task.bedId != null || task.gardenAreaId != null -> {
                // Bed- and area-scoped maintenance tasks don't carry species.
            }
            else -> {
                if (speciesId == null) {
                    throw BadRequestException("speciesId is required for species-scoped tasks")
                }
                val acceptableIds = taskRepository.findAcceptableSpeciesIds(taskId)
                if (speciesId !in acceptableIds) {
                    throw BadRequestException("Species $speciesId is not in the acceptable species list for this task")
                }
            }
        }
        taskRepository.decrementRemainingCount(taskId, processedCount)
        val updated = taskRepository.findById(taskId)!!

        // A rule-backed task that has just been finished records the work, which
        // is what moves the rule's derived clock. Without this the scheduler
        // would recreate the same task tomorrow.
        if (updated.maintenanceRuleId != null && updated.remainingCount <= 0) {
            recordMaintenance(updated)
        }

        return buildResponses(listOf(updated)).first()
    }

    /**
     * Records the completion of a rule-backed maintenance task as an event.
     *
     * WEED and WATER on a bed route through [PlantService] so a completed
     * maintenance task is indistinguishable from a tapped bed button in bed
     * history — including the per-plant fan-out. FERTILIZE has no
     * `fertilizeBed` equivalent (quantities are unknown, so a
     * `supply_application` row can't be honestly fabricated), so it logs a
     * bare bed event directly. Area activities always log directly, since
     * there is no per-plant equivalent for an area.
     */
    private fun recordMaintenance(task: ScheduledTask) {
        val activity = runCatching { MaintenanceActivity.parse(task.activityType) }.getOrNull() ?: return
        val today = java.time.LocalDate.now()
        when {
            task.gardenAreaId != null -> gardenAreaEventRepository.persist(
                GardenAreaEvent(
                    gardenAreaId = task.gardenAreaId,
                    eventType = activity.name,
                    eventDate = today,
                    notes = task.notes,
                )
            )
            task.bedId != null -> when (activity) {
                MaintenanceActivity.WEED -> plantService.weedBed(task.bedId, task.orgId)
                MaintenanceActivity.WATER -> plantService.waterBed(task.bedId, task.orgId)
                MaintenanceActivity.FERTILIZE -> bedEventRepository.persist(
                    BedEvent(
                        bedId = task.bedId,
                        eventType = PlantEventType.APPLIED_SUPPLY,
                        eventDate = today,
                        notes = task.notes,
                    )
                )
                else -> {
                    // No other activity can currently target a bed (BED_ACTIVITY_TYPES
                    // guards createTask); nothing to record.
                }
            }
        }
    }

    fun addSpeciesToTask(taskId: Long, speciesId: Long, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)
        speciesRepository.findById(speciesId) ?: throw NotFoundException("Species not found")
        taskRepository.addAcceptableSpecies(taskId, speciesId)
        return buildResponses(listOf(taskRepository.findById(taskId)!!)).first()
    }

    fun syncTaskWithGroup(taskId: Long, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)
        val groupId = task.originGroupId
            ?: throw BadRequestException("Task is not associated with a group")
        val currentGroupSpeciesIds = speciesRepository.findByGroupId(groupId).map { it.id!! }.toSet()
        val currentTaskSpeciesIds = taskRepository.findAcceptableSpeciesIds(taskId).toSet()
        val toAdd = currentGroupSpeciesIds - currentTaskSpeciesIds
        if (toAdd.isEmpty()) throw BadRequestException("No new species to add from group")
        for (speciesId in toAdd) {
            taskRepository.addAcceptableSpecies(taskId, speciesId)
        }
        return buildResponses(listOf(taskRepository.findById(taskId)!!)).first()
    }

    fun deleteTask(taskId: Long, orgId: Long) {
        checkOwnership(taskId, orgId)
        taskRepository.delete(taskId)
    }

    private fun buildResponses(tasks: List<ScheduledTask>): List<ScheduledTaskResponse> {
        if (tasks.isEmpty()) return emptyList()
        val taskIds = tasks.map { it.id!! }.toSet()
        val acceptableByTask = taskRepository.findAcceptableSpeciesIdsByTaskIds(taskIds)
        val allSpeciesIds = acceptableByTask.values.flatten().toSet() +
            tasks.mapNotNull { it.speciesId }.toSet()
        val speciesNames = speciesRepository.findNamesByIds(allSpeciesIds)
        val speciesById = speciesRepository.findByIds(allSpeciesIds)

        val groupIds = tasks.mapNotNull { it.originGroupId }.toSet()
        val groupNames = speciesGroupRepository.findNamesByIds(groupIds)

        val bedIds = tasks.mapNotNull { it.bedId }.toSet()
        val bedsById = if (bedIds.isEmpty()) emptyMap() else
            bedIds.mapNotNull { bedRepository.findById(it) }.associateBy { it.id!! }
        val areaIds = tasks.mapNotNull { it.gardenAreaId }.toSet()
        val areasById = if (areaIds.isEmpty()) emptyMap() else
            areaIds.mapNotNull { gardenAreaRepository.findById(it) }.associateBy { it.id!! }
        val gardenIds = bedsById.values.map { it.gardenId }.toSet() +
            areasById.values.map { it.gardenId }.toSet()
        val gardensById = if (gardenIds.isEmpty()) emptyMap() else
            gardenIds.mapNotNull { gardenRepository.findById(it) }.associateBy { it.id!! }

        return tasks.map { task ->
            val myAcceptable = acceptableByTask[task.id] ?: emptyList()
            val bed = task.bedId?.let { bedsById[it] }
            val area = task.gardenAreaId?.let { areasById[it] }
            // An area task's garden name comes from the area, a bed task's from the bed.
            val garden = (bed?.gardenId ?: area?.gardenId)?.let { gardensById[it] }
            ScheduledTaskResponse(
                id = task.id!!,
                speciesId = task.speciesId,
                speciesName = task.speciesId?.let { speciesNames[it] },
                bedId = task.bedId,
                bedName = bed?.name,
                gardenName = garden?.name,
                gardenAreaId = task.gardenAreaId,
                gardenAreaName = area?.name,
                maintenanceRuleId = task.maintenanceRuleId,
                activityType = task.activityType,
                earliestDate = task.earliestDate,
                deadline = task.deadline,
                targetCount = task.targetCount,
                remainingCount = task.remainingCount,
                status = task.status.name,
                notes = task.notes,
                seasonId = task.seasonId,
                successionScheduleId = task.successionScheduleId,
                originGroupId = task.originGroupId,
                originGroupName = task.originGroupId?.let { groupNames[it] },
                acceptableSpecies = myAcceptable.map { sid ->
                    val sp = speciesById[sid]
                    AcceptableSpeciesEntry(
                        speciesId = sid,
                        speciesName = speciesNames[sid] ?: "Unknown",
                        commonName = sp?.commonName ?: "Unknown",
                        variantName = sp?.variantName,
                        commonNameSv = sp?.commonNameSv,
                        variantNameSv = sp?.variantNameSv,
                    )
                },
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
            )
        }
    }
}
