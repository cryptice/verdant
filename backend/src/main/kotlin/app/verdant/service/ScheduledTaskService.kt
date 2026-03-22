package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.ScheduledTask
import app.verdant.entity.ScheduledTaskStatus
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class ScheduledTaskService(
    private val taskRepository: ScheduledTaskRepository,
    private val speciesRepository: SpeciesRepository,
) {
    private fun checkOwnership(taskId: Long, userId: Long): ScheduledTask {
        val task = taskRepository.findById(taskId) ?: throw NotFoundException("Task not found")
        if (task.userId != userId) throw ForbiddenException()
        return task
    }

    private fun resolveSpeciesName(speciesId: Long): String =
        speciesRepository.findById(speciesId)?.commonName ?: "Unknown"

    fun getTasksForUser(userId: Long, seasonId: Long? = null): List<ScheduledTaskResponse> {
        val tasks = if (seasonId != null) {
            taskRepository.findBySeasonId(userId, seasonId)
        } else {
            taskRepository.findByUserId(userId)
        }
        return tasks.map { it.toResponse() }
    }

    fun getTask(taskId: Long, userId: Long): ScheduledTaskResponse =
        checkOwnership(taskId, userId).toResponse()

    fun createTask(request: CreateScheduledTaskRequest, userId: Long): ScheduledTaskResponse {
        speciesRepository.findById(request.speciesId) ?: throw NotFoundException("Species not found")
        val task = taskRepository.persist(
            ScheduledTask(
                userId = userId,
                speciesId = request.speciesId,
                activityType = request.activityType,
                deadline = request.deadline,
                targetCount = request.targetCount,
                remainingCount = request.targetCount,
                notes = request.notes,
            )
        )
        return task.toResponse()
    }

    fun updateTask(taskId: Long, request: UpdateScheduledTaskRequest, userId: Long): ScheduledTaskResponse {
        val task = checkOwnership(taskId, userId)

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
        return updated.toResponse()
    }

    fun completePartially(taskId: Long, processedCount: Int, userId: Long): ScheduledTaskResponse {
        checkOwnership(taskId, userId)
        taskRepository.decrementRemainingCount(taskId, processedCount)
        return taskRepository.findById(taskId)!!.toResponse()
    }

    fun deleteTask(taskId: Long, userId: Long) {
        checkOwnership(taskId, userId)
        taskRepository.delete(taskId)
    }

    private fun ScheduledTask.toResponse() = ScheduledTaskResponse(
        id = id!!,
        speciesId = speciesId,
        speciesName = resolveSpeciesName(speciesId),
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
