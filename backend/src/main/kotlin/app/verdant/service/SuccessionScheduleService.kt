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
    private fun resolveSpeciesName(speciesId: Long): String? =
        speciesRepo.findById(speciesId)?.commonName

    fun getSchedulesForUser(userId: Long, seasonId: Long? = null): List<SuccessionScheduleResponse> {
        val schedules = if (seasonId != null) {
            repo.findBySeasonId(userId, seasonId)
        } else {
            repo.findByUserId(userId)
        }
        return schedules.map { it.toResponse() }
    }

    fun getSchedule(id: Long, userId: Long): SuccessionScheduleResponse {
        val schedule = repo.findById(id) ?: throw NotFoundException("Succession schedule not found")
        if (schedule.userId != userId) throw ForbiddenException()
        return schedule.toResponse()
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
        return schedule.toResponse()
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
        return updated.toResponse()
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

    private fun SuccessionSchedule.toResponse() = SuccessionScheduleResponse(
        id = id!!,
        seasonId = seasonId,
        speciesId = speciesId,
        speciesName = resolveSpeciesName(speciesId),
        bedId = bedId,
        firstSowDate = firstSowDate,
        intervalDays = intervalDays,
        totalSuccessions = totalSuccessions,
        seedsPerSuccession = seedsPerSuccession,
        notes = notes,
        createdAt = createdAt,
    )
}
