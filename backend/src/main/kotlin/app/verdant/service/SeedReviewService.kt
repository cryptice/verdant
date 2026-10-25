package app.verdant.service

import app.verdant.dto.ReviewStep
import app.verdant.dto.SaveOrderListRequest
import app.verdant.dto.SeasonSummary
import app.verdant.dto.SeedReviewResponse
import app.verdant.dto.VarietyReview
import app.verdant.entity.Season
import app.verdant.entity.Species
import app.verdant.repository.SeasonRepository
import app.verdant.repository.SeedOrderLineRepository
import app.verdant.repository.SeedReviewRepository
import app.verdant.repository.SeedStockMovementRepository
import app.verdant.repository.SoldFigures
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.WorkflowRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import kotlin.math.roundToLong

/**
 * Season review of seed usage per variety, and the order list saved against it.
 * Everything counts by date within the season's effective ranges. A season that
 * started before the seed stock ledger went live is estimated as a whole.
 */
@ApplicationScoped
class SeedReviewService(
    private val seasonRepo: SeasonRepository,
    private val reviewRepo: SeedReviewRepository,
    private val orderLineRepo: SeedOrderLineRepository,
    private val movementRepo: SeedStockMovementRepository,
    private val workflowRepo: WorkflowRepository,
    private val speciesRepo: SpeciesRepository,
) {

    fun review(seasonId: Long, orgId: Long): SeedReviewResponse {
        val season = ownedSeason(seasonId, orgId)
        val ranges = SeasonRanges.effectiveRanges(season, seasonRepo.findByOrgId(orgId))
        val ledgerStart = movementRepo.ledgerStartDate()
        val estimated = ledgerStart == null || SeasonRanges.rangeOf(season).start < ledgerStart

        val sown = reviewRepo.sownBySpecies(orgId, ranges)
        val eventCounts = reviewRepo.eventPlantCountsBySpecies(orgId, ranges)
        val harvested = reviewRepo.harvestedStemsBySpecies(orgId, ranges)
        val sales = reviewRepo.salesBySpecies(orgId, ranges)
        val stock = reviewRepo.stockBySpecies(orgId)
        val added = reviewRepo.addedBySpecies(orgId, ranges)
        val saved = orderLineRepo.findBySeason(orgId, seasonId)
        val seedCost = if (estimated) estimatedSeedCost(orgId, sown) else reviewRepo.sownCostBySpecies(orgId, ranges)

        val speciesIds = buildSet {
            sown.filterValues { it != 0 }.keys.let(::addAll)
            added.filterValues { it != 0 }.keys.let(::addAll)
            harvested.filterValues { it != 0 }.keys.let(::addAll)
            sales.filterValues { it.stems != 0 }.keys.let(::addAll)
        }
        val species = speciesRepo.findByIds(speciesIds)

        val varieties = speciesIds.mapNotNull { id ->
            val sp = species[id] ?: return@mapNotNull null
            val sownCount = sown[id] ?: 0
            val inStock = stock[id] ?: 0
            val sold = sales[id] ?: SoldFigures(0, 0)
            VarietyReview(
                speciesId = id,
                speciesName = sp.displayName(),
                unitType = sp.defaultUnitType.name,
                steps = steps(id, eventCounts[id].orEmpty()),
                sown = sownCount,
                bought = if (estimated) inStock + sownCount else added[id] ?: 0,
                harvestedStems = harvested[id] ?: 0,
                soldStems = sold.stems,
                revenueCents = sold.revenueCents,
                seedCostCents = seedCost[id] ?: 0,
                leftInStock = inStock,
                suggestedOrder = maxOf(sownCount - inStock, 0),
                savedOrder = saved[id],
            )
        }.sortedWith(compareByDescending<VarietyReview> { it.revenueCents }.thenBy { it.speciesName })

        return SeedReviewResponse(season.toSummary(), estimated, varieties)
    }

    fun saveOrderList(seasonId: Long, request: SaveOrderListRequest, orgId: Long): SeedReviewResponse {
        ownedSeason(seasonId, orgId)
        val speciesIds = request.lines.map { it.speciesId }
        if (speciesIds.size != speciesIds.toSet().size) throw BadRequestException("Each species may appear only once")
        val known = speciesRepo.findByIds(speciesIds.toSet())
        if (speciesIds.any { id -> known[id].let { it == null || (it.orgId != null && it.orgId != orgId) } }) {
            throw BadRequestException("Unknown species")
        }
        orderLineRepo.replaceForSeason(orgId, seasonId, request.lines.associate { it.speciesId to it.quantity })
        return review(seasonId, orgId)
    }

    private fun ownedSeason(seasonId: Long, orgId: Long): Season {
        val season = seasonRepo.findById(seasonId) ?: throw NotFoundException("Season not found")
        if (season.orgId != orgId) throw NotFoundException("Season not found")
        return season
    }

    /** Before the ledger, only the batches' average unit cost is known. */
    private fun estimatedSeedCost(orgId: Long, sown: Map<Long, Int>): Map<Long, Long> {
        val averageCost = reviewRepo.averageUnitCostBySpecies(orgId)
        return sown.mapValues { (id, count) -> (count * (averageCost[id] ?: 0.0)).roundToLong() }
    }

    /**
     * The species' workflow steps that record an event, in order, outside side branches.
     * Without such a workflow, the standard steps that actually occurred.
     */
    private fun steps(speciesId: Long, counts: Map<String, Int>): List<ReviewStep> {
        val workflow = workflowRepo.findStepsBySpeciesId(speciesId)
            .filter { !it.isSideBranch && it.eventType != null }
            .sortedBy { it.sortOrder }
        if (workflow.isNotEmpty()) {
            return workflow.map { ReviewStep(it.name, it.eventType!!, counts[it.eventType] ?: 0) }
        }
        return DEFAULT_STEPS
            .map { (eventType, name) -> ReviewStep(name, eventType, counts[eventType] ?: 0) }
            .filter { it.count > 0 }
    }

    private fun Species.displayName(): String {
        val name = commonNameSv ?: commonName
        val variant = variantNameSv ?: variantName
        return if (variant != null) "$name – $variant" else name
    }

    private fun Season.toSummary() = SeasonSummary(id!!, name, year, startDate, endDate)

    private companion object {
        val DEFAULT_STEPS = listOf(
            "SEEDED" to "Sått",
            "POTTED_UP" to "Omskolat",
            "PLANTED_OUT" to "Utplanterat",
            "HARVESTED" to "Skördat",
        )
    }
}
