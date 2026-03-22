package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.ProductionTarget
import app.verdant.repository.ProductionTargetRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

@ApplicationScoped
class ProductionTargetService(
    private val repo: ProductionTargetRepository,
    private val speciesRepo: SpeciesRepository,
) {
    private fun resolveSpeciesName(speciesId: Long): String? =
        speciesRepo.findById(speciesId)?.commonName

    fun getTargetsForUser(userId: Long, seasonId: Long? = null): List<ProductionTargetResponse> {
        val targets = if (seasonId != null) {
            repo.findBySeasonId(userId, seasonId)
        } else {
            repo.findByUserId(userId)
        }
        return targets.map { it.toResponse() }
    }

    fun getTarget(id: Long, userId: Long): ProductionTargetResponse {
        val target = repo.findById(id) ?: throw NotFoundException("Production target not found")
        if (target.userId != userId) throw ForbiddenException()
        return target.toResponse()
    }

    fun createTarget(request: CreateProductionTargetRequest, userId: Long): ProductionTargetResponse {
        val target = repo.persist(
            ProductionTarget(
                userId = userId,
                seasonId = request.seasonId,
                speciesId = request.speciesId,
                stemsPerWeek = request.stemsPerWeek,
                startDate = request.startDate,
                endDate = request.endDate,
                notes = request.notes,
            )
        )
        return target.toResponse()
    }

    fun updateTarget(id: Long, request: UpdateProductionTargetRequest, userId: Long): ProductionTargetResponse {
        val target = repo.findById(id) ?: throw NotFoundException("Production target not found")
        if (target.userId != userId) throw ForbiddenException()
        val updated = target.copy(
            seasonId = request.seasonId ?: target.seasonId,
            speciesId = request.speciesId ?: target.speciesId,
            stemsPerWeek = request.stemsPerWeek ?: target.stemsPerWeek,
            startDate = request.startDate ?: target.startDate,
            endDate = request.endDate ?: target.endDate,
            notes = request.notes ?: target.notes,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    fun deleteTarget(id: Long, userId: Long) {
        val target = repo.findById(id) ?: throw NotFoundException("Production target not found")
        if (target.userId != userId) throw ForbiddenException()
        repo.delete(id)
    }

    fun calculateRequirements(id: Long, userId: Long): ProductionForecastResponse {
        val target = repo.findById(id) ?: throw NotFoundException("Production target not found")
        if (target.userId != userId) throw ForbiddenException()
        val species = speciesRepo.findById(target.speciesId)
            ?: throw NotFoundException("Species not found")

        val warnings = mutableListOf<String>()

        val weeks = ChronoUnit.WEEKS.between(target.startDate, target.endDate)
        val totalStemsNeeded = target.stemsPerWeek * weeks

        val stemsPerPlant = species.expectedStemsPerPlant ?: run {
            warnings.add("Species has no expectedStemsPerPlant set; defaulting to 1")
            1
        }
        val plantsNeeded = ceil(totalStemsNeeded.toDouble() / stemsPerPlant).toLong()

        val germRate = species.germinationRate ?: run {
            warnings.add("Species has no germinationRate set; defaulting to 80%")
            80
        }
        val seedsNeeded = ceil(plantsNeeded / (germRate / 100.0)).toLong()

        val daysToHarvest = species.daysToHarvest ?: run {
            warnings.add("Species has no daysToHarvest set; defaulting to 90 days")
            90
        }
        val suggestedSowDate = target.startDate.minusDays(daysToHarvest.toLong())

        return ProductionForecastResponse(
            targetId = target.id!!,
            speciesId = target.speciesId,
            speciesName = species.commonName,
            totalWeeks = weeks,
            totalStemsNeeded = totalStemsNeeded,
            stemsPerPlant = stemsPerPlant,
            plantsNeeded = plantsNeeded,
            germinationRate = germRate,
            seedsNeeded = seedsNeeded,
            daysToHarvest = daysToHarvest,
            suggestedSowDate = suggestedSowDate,
            warnings = warnings,
        )
    }

    private fun ProductionTarget.toResponse() = ProductionTargetResponse(
        id = id!!,
        seasonId = seasonId,
        speciesId = speciesId,
        speciesName = resolveSpeciesName(speciesId),
        stemsPerWeek = stemsPerWeek,
        startDate = startDate,
        endDate = endDate,
        notes = notes,
        createdAt = createdAt,
    )
}
