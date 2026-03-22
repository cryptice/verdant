package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Season
import app.verdant.repository.SeasonRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SeasonService(
    private val repo: SeasonRepository,
) {
    fun getSeasonsForUser(userId: Long): List<SeasonResponse> =
        repo.findByUserId(userId).map { it.toResponse() }

    fun getSeason(id: Long, userId: Long): SeasonResponse {
        val season = repo.findById(id) ?: throw NotFoundException("Season not found")
        if (season.userId != userId) throw ForbiddenException()
        return season.toResponse()
    }

    fun getActiveSeason(userId: Long): SeasonResponse? =
        repo.findActiveByUserId(userId).firstOrNull()?.toResponse()

    fun createSeason(request: CreateSeasonRequest, userId: Long): SeasonResponse {
        val season = repo.persist(
            Season(
                userId = userId,
                name = request.name,
                year = request.year,
                startDate = request.startDate,
                endDate = request.endDate,
                lastFrostDate = request.lastFrostDate,
                firstFrostDate = request.firstFrostDate,
                growingDegreeBaseC = request.growingDegreeBaseC,
                notes = request.notes,
                isActive = request.isActive,
            )
        )
        return season.toResponse()
    }

    fun updateSeason(id: Long, request: UpdateSeasonRequest, userId: Long): SeasonResponse {
        val season = repo.findById(id) ?: throw NotFoundException("Season not found")
        if (season.userId != userId) throw ForbiddenException()
        val updated = season.copy(
            name = request.name ?: season.name,
            year = request.year ?: season.year,
            startDate = request.startDate ?: season.startDate,
            endDate = request.endDate ?: season.endDate,
            lastFrostDate = request.lastFrostDate ?: season.lastFrostDate,
            firstFrostDate = request.firstFrostDate ?: season.firstFrostDate,
            growingDegreeBaseC = request.growingDegreeBaseC ?: season.growingDegreeBaseC,
            notes = request.notes ?: season.notes,
            isActive = request.isActive ?: season.isActive,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    fun deleteSeason(id: Long, userId: Long) {
        val season = repo.findById(id) ?: throw NotFoundException("Season not found")
        if (season.userId != userId) throw ForbiddenException()
        repo.delete(id)
    }

    private fun Season.toResponse() = SeasonResponse(
        id = id!!,
        name = name,
        year = year,
        startDate = startDate,
        endDate = endDate,
        lastFrostDate = lastFrostDate,
        firstFrostDate = firstFrostDate,
        growingDegreeBaseC = growingDegreeBaseC,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
