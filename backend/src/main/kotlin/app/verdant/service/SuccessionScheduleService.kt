package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.ScheduledTask
import app.verdant.entity.SuccessionSchedule
import app.verdant.repository.ScheduledTaskRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.SuccessionScheduleRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SuccessionScheduleService(
    private val repo: SuccessionScheduleRepository,
    private val speciesRepo: SpeciesRepository,
    private val taskRepo: ScheduledTaskRepository,
) {
    fun getSchedulesForUser(userId: Long, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<SuccessionScheduleResponse> {
        val schedules = if (seasonId != null) {
            repo.findBySeasonId(userId, seasonId, limit, offset)
        } else {
            repo.findByUserId(userId, limit, offset)
        }
        val speciesNames = speciesRepo.findNamesByIds(schedules.map { it.speciesId }.toSet())
        return schedules.map { it.toResponse(speciesNames) }
    }

    fun getSchedule(id: Long, userId: Long): SuccessionScheduleResponse {
        val schedule = repo.findById(id) ?: throw NotFoundException("Succession schedule not found")
        if (schedule.userId != userId) throw ForbiddenException()
        val speciesNames = speciesRepo.findNamesByIds(setOf(schedule.speciesId))
        return schedule.toResponse(speciesNames)
    }

    fun createSchedule(request: CreateSuccessionScheduleRequest, userId: Long): SuccessionScheduleResponse {
        val schedule = repo.persist(
            SuccessionSchedule(
                userId = userId,
                seasonId = request.seasonId,
                speciesId = request.speciesId,
                bedId = request.bedId,
                firstSowDate = request.firstSowDate,
                intervalDays = request.intervalDays,
                totalSuccessions = request.totalSuccessions,
                seedsPerSuccession = request.seedsPerSuccession,
                notes = request.notes,
            )
        )
        val speciesNames = speciesRepo.findNamesByIds(setOf(schedule.speciesId))
        return schedule.toResponse(speciesNames)
    }

    fun updateSchedule(id: Long, request: UpdateSuccessionScheduleRequest, userId: Long): SuccessionScheduleResponse {
        val schedule = repo.findById(id) ?: throw NotFoundException("Succession schedule not found")
        if (schedule.userId != userId) throw ForbiddenException()
        val updated = schedule.copy(
            seasonId = request.seasonId ?: schedule.seasonId,
            speciesId = request.speciesId ?: schedule.speciesId,
            bedId = request.bedId ?: schedule.bedId,
            firstSowDate = request.firstSowDate ?: schedule.firstSowDate,
            intervalDays = request.intervalDays ?: schedule.intervalDays,
            totalSuccessions = request.totalSuccessions ?: schedule.totalSuccessions,
            seedsPerSuccession = request.seedsPerSuccession ?: schedule.seedsPerSuccession,
            notes = request.notes ?: schedule.notes,
        )
        repo.update(updated)
        val speciesNames = speciesRepo.findNamesByIds(setOf(updated.speciesId))
        return updated.toResponse(speciesNames)
    }

    fun deleteSchedule(id: Long, userId: Long) {
        val schedule = repo.findById(id) ?: throw NotFoundException("Succession schedule not found")
        if (schedule.userId != userId) throw ForbiddenException()
        repo.delete(id)
    }

    fun generateTasks(id: Long, userId: Long): List<Long> {
        val schedule = repo.findById(id) ?: throw NotFoundException("Succession schedule not found")
        if (schedule.userId != userId) throw ForbiddenException()
        val taskIds = mutableListOf<Long>()
        for (i in 0 until schedule.totalSuccessions) {
            val deadline = schedule.firstSowDate.plusDays((i.toLong() * schedule.intervalDays))
            val task = taskRepo.persist(
                ScheduledTask(
                    userId = userId,
                    speciesId = schedule.speciesId,
                    activityType = "SOW",
                    deadline = deadline,
                    targetCount = schedule.seedsPerSuccession,
                    remainingCount = schedule.seedsPerSuccession,
                    seasonId = schedule.seasonId,
                    successionScheduleId = schedule.id,
                )
            )
            taskIds.add(task.id!!)
        }
        return taskIds
    }

    private fun SuccessionSchedule.toResponse(speciesNames: Map<Long, String>) = SuccessionScheduleResponse(
        id = id!!,
        seasonId = seasonId,
        speciesId = speciesId,
        speciesName = speciesNames[speciesId],
        bedId = bedId,
        firstSowDate = firstSowDate,
        intervalDays = intervalDays,
        totalSuccessions = totalSuccessions,
        seedsPerSuccession = seedsPerSuccession,
        notes = notes,
        createdAt = createdAt,
    )
}
