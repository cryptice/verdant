package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class ScheduledTaskService(
    private val taskRepository: ScheduledTaskRepository,
    private val speciesRepository: SpeciesRepository,
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
        val speciesNames = speciesRepository.findNamesByIds(tasks.map { it.speciesId }.toSet())
        return tasks.map { it.toResponse(speciesNames) }
    }

    fun getTask(taskId: Long, orgId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, orgId)
        val speciesNames = speciesRepository.findNamesByIds(setOf(task.speciesId))
        return task.toResponse(speciesNames)
    }

    fun createTask(request: CreateScheduledTaskRequest, orgId: Long): ScheduledTaskResponse {
        speciesRepository.findById(request.speciesId) ?: throw NotFoundException("Species not found")
        val task = taskRepository.persist(
            ScheduledTask(
                orgId = orgId,
                speciesId = request.speciesId,
                activityType = request.activityType,
                deadline = request.deadline,
                targetCount = request.targetCount,
                remainingCount = request.targetCount,
                notes = request.notes,
            )
        )
        val speciesNames = speciesRepository.findNamesByIds(setOf(task.speciesId))
        return task.toResponse(speciesNames)
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
            speciesId = request.speciesId ?: task.speciesId,
            activityType = request.activityType ?: task.activityType,
            deadline = request.deadline ?: task.deadline,
            targetCount = newTarget,
            remainingCount = newRemaining,
            status = newStatus,
            notes = request.notes ?: task.notes,
        )
        taskRepository.update(updated)
        val speciesNames = speciesRepository.findNamesByIds(setOf(updated.speciesId))
        return updated.toResponse(speciesNames)
    }

    fun completePartially(taskId: Long, processedCount: Int, orgId: Long): ScheduledTaskResponse {
        checkOwnership(taskId, orgId)
        taskRepository.decrementRemainingCount(taskId, processedCount)
        val task = taskRepository.findById(taskId)!!
        val speciesNames = speciesRepository.findNamesByIds(setOf(task.speciesId))
        return task.toResponse(speciesNames)
    }

    fun deleteTask(taskId: Long, orgId: Long) {
        checkOwnership(taskId, orgId)
        taskRepository.delete(taskId)
    }

    private fun ScheduledTask.toResponse(speciesNames: Map<Long, String>) = ScheduledTaskResponse(
        id = id!!,
        speciesId = speciesId,
        speciesName = speciesNames[speciesId] ?: "Unknown",
        activityType = activityType,
        deadline = deadline,
        targetCount = targetCount,
        remainingCount = remainingCount,
        status = status.name,
        notes = notes,
        seasonId = seasonId,
        successionScheduleId = successionScheduleId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
