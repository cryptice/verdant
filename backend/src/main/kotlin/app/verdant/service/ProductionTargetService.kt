package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.ProductionTarget
import app.verdant.repository.ProductionTargetRepository
import app.verdant.repository.SpeciesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

@ApplicationScoped
class ProductionTargetService(
    private val repo: ProductionTargetRepository,
    private val speciesRepo: SpeciesRepository,
) {
    private fun resolveSpeciesName(speciesId: Long): String? =
        speciesRepo.findNamesByIds(setOf(speciesId))[speciesId]

    fun getTargetsForUser(orgId: Long, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<ProductionTargetResponse> {
        val targets = if (seasonId != null) {
            repo.findBySeasonId(orgId, seasonId, limit, offset)
        } else {
            repo.findByOrgId(orgId, limit, offset)
        }
        return targets.map { it.toResponse() }
    }

    fun getTarget(id: Long, orgId: Long): ProductionTargetResponse {
        val target = repo.findById(id) ?: throw NotFoundException("Production target not found")
        if (target.orgId != orgId) throw NotFoundException("Production target not found")
        return target.toResponse()
    }

    fun createTarget(request: CreateProductionTargetRequest, orgId: Long): ProductionTargetResponse {
        val target = repo.persist(
            ProductionTarget(
                orgId = orgId,
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

    fun updateTarget(id: Long, request: UpdateProductionTargetRequest, orgId: Long): ProductionTargetResponse {
        val target = repo.findById(id) ?: throw NotFoundException("Production target not found")
        if (target.orgId != orgId) throw NotFoundException("Production target not found")
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

    fun deleteTarget(id: Long, orgId: Long) {
        val target = repo.findById(id) ?: throw NotFoundException("Production target not found")
        if (target.orgId != orgId) throw NotFoundException("Production target not found")
        repo.delete(id)
    }

    fun calculateRequirements(id: Long, orgId: Long): ProductionForecastResponse {
        val target = repo.findById(id) ?: throw NotFoundException("Production target not found")
        if (target.orgId != orgId) throw NotFoundException("Production target not found")
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

        val daysToHarvest = species.daysToHarvestMin ?: run {
            warnings.add("Species has no daysToHarvestMin set; defaulting to 90 days")
            90
        }
        val suggestedSowDate = target.startDate.minusDays(daysToHarvest.toLong())

        return ProductionForecastResponse(
            targetId = target.id!!,
            speciesId = target.speciesId,
            speciesName = resolveSpeciesName(target.speciesId) ?: species.commonName,
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
