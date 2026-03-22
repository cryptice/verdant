package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.PestDiseaseLog
import app.verdant.repository.PestDiseaseLogRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class PestDiseaseService(
    private val repo: PestDiseaseLogRepository,
    private val storageService: StorageService,
) {
    fun getLogsForUser(userId: Long, seasonId: Long? = null): List<PestDiseaseLogResponse> {
        val logs = if (seasonId != null) {
            repo.findBySeasonId(userId, seasonId)
        } else {
            repo.findByUserId(userId)
        }
        return logs.map { it.toResponse() }
    }

    fun getLog(id: Long, userId: Long): PestDiseaseLogResponse {
        val log = repo.findById(id) ?: throw NotFoundException("Pest/disease log not found")
        if (log.userId != userId) throw ForbiddenException()
        return log.toResponse()
    }

    fun createLog(request: CreatePestDiseaseLogRequest, userId: Long): PestDiseaseLogResponse {
        var imageUrl: String? = null
        val log = repo.persist(
            PestDiseaseLog(
                userId = userId,
                seasonId = request.seasonId,
                bedId = request.bedId,
                speciesId = request.speciesId,
                observedDate = request.observedDate,
                category = request.category,
                name = request.name,
                severity = request.severity,
                treatment = request.treatment,
                outcome = request.outcome,
                notes = request.notes,
                imageUrl = imageUrl,
            )
        )
        if (request.imageBase64 != null) {
            imageUrl = storageService.uploadImage(
                request.imageBase64,
                "pest-disease/$userId/${log.id}.jpg"
            )
            val updated = log.copy(imageUrl = imageUrl)
            repo.update(updated)
            return updated.toResponse()
        }
        return log.toResponse()
    }

    fun updateLog(id: Long, request: UpdatePestDiseaseLogRequest, userId: Long): PestDiseaseLogResponse {
        val log = repo.findById(id) ?: throw NotFoundException("Pest/disease log not found")
        if (log.userId != userId) throw ForbiddenException()
        var imageUrl = log.imageUrl
        if (request.imageBase64 != null) {
            imageUrl = storageService.uploadImage(
                request.imageBase64,
                "pest-disease/$userId/$id.jpg"
            )
        }
        val updated = log.copy(
            seasonId = request.seasonId ?: log.seasonId,
            bedId = request.bedId ?: log.bedId,
            speciesId = request.speciesId ?: log.speciesId,
            observedDate = request.observedDate ?: log.observedDate,
            category = request.category ?: log.category,
            name = request.name ?: log.name,
            severity = request.severity ?: log.severity,
            treatment = request.treatment ?: log.treatment,
            outcome = request.outcome ?: log.outcome,
            notes = request.notes ?: log.notes,
            imageUrl = imageUrl,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    fun deleteLog(id: Long, userId: Long) {
        val log = repo.findById(id) ?: throw NotFoundException("Pest/disease log not found")
        if (log.userId != userId) throw ForbiddenException()
        repo.delete(id)
    }

    private fun PestDiseaseLog.toResponse() = PestDiseaseLogResponse(
        id = id!!,
        seasonId = seasonId,
        bedId = bedId,
        speciesId = speciesId,
        observedDate = observedDate,
        category = category,
        name = name,
        severity = severity,
        treatment = treatment,
        outcome = outcome,
        notes = notes,
        imageUrl = imageUrl,
        createdAt = createdAt,
    )
}
