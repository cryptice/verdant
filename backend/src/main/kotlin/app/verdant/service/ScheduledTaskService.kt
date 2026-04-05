package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
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
                ?: throw BadRequestException("Either speciesId, speciesGroupId, or speciesIds must be provided")
            speciesRepository.findById(singleId) ?: throw NotFoundException("Species not found")
            acceptableSpeciesIds = listOf(singleId)
        }

        val task = taskRepository.persist(
            ScheduledTask(
                orgId = orgId,
                speciesId = if (acceptableSpeciesIds.size == 1) acceptableSpeciesIds.first() else null,
                activityType = request.activityType,
                deadline = request.deadline,
                targetCount = request.targetCount,
                remainingCount = request.targetCount,
                notes = request.notes,
                seasonId = request.seasonId,
                successionScheduleId = request.successionScheduleId,
                originGroupId = originGroupId,
            )
        )
        taskRepository.setAcceptableSpecies(task.id!!, acceptableSpeciesIds)
        return buildResponses(listOf(task)).first()
    }

    fun updateTask(taskId: Long, request: UpdateScheduledTaskRequest, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)

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
            deadline = request.deadline ?: task.deadline,
            targetCount = newTarget,
            remainingCount = newRemaining,
            status = newStatus,
            notes = request.notes ?: task.notes,
        )
        taskRepository.update(updated)
        return buildResponses(listOf(updated)).first()
    }

    fun completePartially(taskId: Long, speciesId: Long, processedCount: Int, orgId: Long): ScheduledTaskResponse {
        checkOwnership(taskId, orgId)
        val acceptableIds = taskRepository.findAcceptableSpeciesIds(taskId)
        if (speciesId !in acceptableIds) {
            throw BadRequestException("Species $speciesId is not in the acceptable species list for this task")
        }
        taskRepository.decrementRemainingCount(taskId, processedCount)
        val task = taskRepository.findById(taskId)!!
        return buildResponses(listOf(task)).first()
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

        return tasks.map { task ->
            val myAcceptable = acceptableByTask[task.id] ?: emptyList()
            ScheduledTaskResponse(
                id = task.id!!,
                speciesId = task.speciesId,
                speciesName = task.speciesId?.let { speciesNames[it] },
                activityType = task.activityType,
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
