package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.VarietyTrial
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.VarietyTrialRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class VarietyTrialService(
    private val repo: VarietyTrialRepository,
    private val speciesRepo: SpeciesRepository,
) {
    private fun resolveSpeciesName(speciesId: Long): String? =
        speciesRepo.findNamesByIds(setOf(speciesId))[speciesId]

    fun getTrialsForUser(orgId: Long, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<VarietyTrialResponse> {
        val trials = if (seasonId != null) {
            repo.findBySeasonId(orgId, seasonId, limit, offset)
        } else {
            repo.findByOrgId(orgId, limit, offset)
        }
        return trials.map { it.toResponse() }
    }

    fun getTrialsBySpecies(orgId: Long, speciesId: Long): List<VarietyTrialResponse> =
        repo.findBySpeciesId(orgId, speciesId).map { it.toResponse() }

    fun getTrial(id: Long, orgId: Long): VarietyTrialResponse {
        val trial = repo.findById(id) ?: throw NotFoundException("Variety trial not found")
        if (trial.orgId != orgId) throw NotFoundException("Variety trial not found")
        return trial.toResponse()
    }

    fun createTrial(request: CreateVarietyTrialRequest, orgId: Long): VarietyTrialResponse {
        val trial = repo.persist(
            VarietyTrial(
                orgId = orgId,
                seasonId = request.seasonId,
                speciesId = request.speciesId,
                bedId = request.bedId,
                plantCount = request.plantCount,
                stemYield = request.stemYield,
                avgStemLengthCm = request.avgStemLengthCm,
                avgVaseLifeDays = request.avgVaseLifeDays,
                qualityScore = request.qualityScore,
                customerReception = request.customerReception,
                verdict = request.verdict,
                notes = request.notes,
            )
        )
        return trial.toResponse()
    }

    fun updateTrial(id: Long, request: UpdateVarietyTrialRequest, orgId: Long): VarietyTrialResponse {
        val trial = repo.findById(id) ?: throw NotFoundException("Variety trial not found")
        if (trial.orgId != orgId) throw NotFoundException("Variety trial not found")
        val updated = trial.copy(
            seasonId = request.seasonId ?: trial.seasonId,
            speciesId = request.speciesId ?: trial.speciesId,
            bedId = request.bedId ?: trial.bedId,
            plantCount = request.plantCount ?: trial.plantCount,
            stemYield = request.stemYield ?: trial.stemYield,
            avgStemLengthCm = request.avgStemLengthCm ?: trial.avgStemLengthCm,
            avgVaseLifeDays = request.avgVaseLifeDays ?: trial.avgVaseLifeDays,
            qualityScore = request.qualityScore ?: trial.qualityScore,
            customerReception = request.customerReception ?: trial.customerReception,
            verdict = request.verdict ?: trial.verdict,
            notes = request.notes ?: trial.notes,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    fun deleteTrial(id: Long, orgId: Long) {
        val trial = repo.findById(id) ?: throw NotFoundException("Variety trial not found")
        if (trial.orgId != orgId) throw NotFoundException("Variety trial not found")
        repo.delete(id)
    }

    private fun VarietyTrial.toResponse() = VarietyTrialResponse(
        id = id!!,
        seasonId = seasonId,
        speciesId = speciesId,
        speciesName = resolveSpeciesName(speciesId),
        bedId = bedId,
        plantCount = plantCount,
        stemYield = stemYield,
        avgStemLengthCm = avgStemLengthCm,
        avgVaseLifeDays = avgVaseLifeDays,
        qualityScore = qualityScore,
        customerReception = customerReception,
        verdict = verdict,
        notes = notes,
        createdAt = createdAt,
    )
}
