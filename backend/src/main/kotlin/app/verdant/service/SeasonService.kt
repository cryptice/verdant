package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Season
import app.verdant.repository.SeasonRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SeasonService(
    private val repo: SeasonRepository,
) {
    fun getSeasonsForUser(orgId: Long): List<SeasonResponse> =
        repo.findByOrgId(orgId).map { it.toResponse() }

    fun getSeason(id: Long, orgId: Long): SeasonResponse {
        val season = repo.findById(id) ?: throw NotFoundException("Season not found")
        if (season.orgId != orgId) throw NotFoundException("Season not found")
        return season.toResponse()
    }

    fun getActiveSeason(orgId: Long): SeasonResponse? =
        repo.findActiveByOrgId(orgId).firstOrNull()?.toResponse()

    fun createSeason(request: CreateSeasonRequest, orgId: Long): SeasonResponse {
        val season = repo.persist(
            Season(
                orgId = orgId,
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

    fun updateSeason(id: Long, request: UpdateSeasonRequest, orgId: Long): SeasonResponse {
        val season = repo.findById(id) ?: throw NotFoundException("Season not found")
        if (season.orgId != orgId) throw NotFoundException("Season not found")
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

    fun deleteSeason(id: Long, orgId: Long) {
        val season = repo.findById(id) ?: throw NotFoundException("Season not found")
        if (season.orgId != orgId) throw NotFoundException("Season not found")
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
