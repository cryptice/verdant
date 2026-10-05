package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Species
import app.verdant.entity.UnitType
import app.verdant.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.NotFoundException
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId

@ApplicationScoped
class HarvestPlanService(
    private val plans: HarvestPlanRepository,
    private val species: SpeciesRepository,
    private val groups: SpeciesGroupRepository,
    private val seasons: SeasonRepository,
    private val workflows: WorkflowRepository,
    private val calculator: HarvestPlanCalculator,
    private val mapper: ObjectMapper,
) {
    private fun ownedSpecies(orgId: Long, id: Long): Species = species.findById(id)
        ?.takeIf { it.orgId == null || it.orgId == orgId } ?: throw NotFoundException("Species not found")

    fun candidates(orgId: Long, speciesId: Long?, groupId: Long?): List<PlanningSpecies> {
        if ((speciesId == null) == (groupId == null)) throw BadRequestException("Choose one species or one group")
        val members = if (speciesId != null) listOf(ownedSpecies(orgId, speciesId)) else {
            groups.findById(groupId!!)?.takeIf { it.orgId == null || it.orgId == orgId }
                ?: throw NotFoundException("Species group not found")
            species.findByGroupId(groupId).filter { it.orgId == null || it.orgId == orgId }
        }
        if (members.size > 100) throw BadRequestException("Use a planning group with at most 100 species")
        return members.sortedBy { it.id }.map { planningSpecies(orgId, it) }
    }

    private fun planningSpecies(orgId: Long, sp: Species): PlanningSpecies {
        val profile = plans.profile(orgId, sp.id!!)
        return PlanningSpecies(sp.id, listOfNotNull(sp.commonName, sp.variantName).joinToString(" — "),
            profile ?: defaultProfile(sp), profile != null)
    }

    private fun defaultProfile(sp: Species): ProductionProfile {
        val propagation = if (sp.defaultUnitType == UnitType.SEED) "SOW" else "PLANT"
        val steps = workflows.findStepsBySpeciesId(sp.id!!).filter { !it.isOptional && !it.isSideBranch }.sortedBy { it.sortOrder }
        val harvestIndex = steps.indexOfFirst { activity(it.eventType) == "HARVEST" }
        val main = if (harvestIndex >= 0) steps.take(harvestIndex + 1) else emptyList()
        // Relative workflow timings can only be imported when every interval up to harvest is known.
        val imported = if (main.isNotEmpty() && main.drop(1).all { it.daysAfterPrevious != null && it.daysAfterPrevious in 0..3650 } &&
            main.any { activity(it.eventType) == propagation }) main.mapIndexed { index, step ->
                LifecycleStep("workflow-${step.id}", step.name,
                    activity(step.eventType),
                    main.drop(index + 1).sumOf { it.daysAfterPrevious!! },
                    when (activity(step.eventType)) {
                        "HARVEST" -> PlanningQuantityBasis.OUTPUT
                        propagation -> PlanningQuantityBasis.START
                        else -> PlanningQuantityBasis.PLANT
                    })
            } else listOf(
                LifecycleStep("start", if (propagation == "SOW") "Sow" else "Plant starting material", propagation, sp.daysToHarvestMin, PlanningQuantityBasis.START),
                LifecycleStep("harvest", "Harvest", "HARVEST", 0, PlanningQuantityBasis.OUTPUT),
            )
        return ProductionProfile(startingUnit = sp.defaultUnitType,
            establishmentPercent = if (sp.defaultUnitType == UnitType.SEED) sp.germinationRate?.toBigDecimal() else null,
            steps = listOf(LifecycleStep("purchase", "Purchase starting material", "PURCHASE", null, PlanningQuantityBasis.START)) + imported)
    }

    private fun activity(event: String?): String = when (event) {
        "SEEDED" -> "SOW"
        "POTTED_UP" -> "POT_UP"
        "PLANTED_OUT" -> "PLANT"
        "HARVESTED" -> "HARVEST"
        "PINCHED" -> "PINCH"
        "WATERED" -> "WATER"
        else -> event?.takeIf { it in HarvestPlanCalculator.ACTIVITIES } ?: "TODO"
    }

    fun saveProfile(orgId: Long, id: Long, profile: ProductionProfile): PlanningSpecies {
        val sp = ownedSpecies(orgId, id)
        val issues = calculator.profileIssues(profile)
        if (issues.isNotEmpty()) throw BadRequestException(issues.joinToString(" "))
        plans.saveProfile(orgId, id, profile)
        return planningSpecies(orgId, sp)
    }

    fun preview(orgId: Long, request: HarvestPlanRequest): HarvestPlanPreview {
        seasons.findById(request.seasonId)?.takeIf { it.orgId == orgId } ?: throw NotFoundException("Season not found")
        val result = calculator.calculate(request.copy(allocations = request.allocations.toSortedMap()),
            candidates(orgId, request.speciesId, request.groupId), LocalDate.now(ZoneId.of("Europe/Stockholm")))
        val digest = MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(result))
        return result.copy(fingerprint = digest.joinToString("") { "%02x".format(it) })
    }

    fun create(orgId: Long, request: CreateHarvestPlanRequest): HarvestPlanResponse {
        plans.existing(orgId, request)?.let { return it }
        val preview = preview(orgId, request.target)
        if (!preview.canSave) throw BadRequestException("Resolve unallocated output before saving the plan")
        if (preview.fingerprint != request.fingerprint) throw ClientErrorException("Planning assumptions changed. Preview again before saving.", 409)
        return plans.create(orgId, request, preview)
    }

    fun list(orgId: Long, limit: Int, offset: Int) = plans.list(orgId, limit.coerceIn(1, 100), offset.coerceAtLeast(0))
    fun get(orgId: Long, id: Long) = plans.get(orgId, id)
    fun cancel(orgId: Long, id: Long) = plans.cancel(orgId, id)
    fun complete(orgId: Long, id: Long, taskId: Long, request: CompleteHarvestPlanTaskRequest): HarvestPlanResponse {
        if (request.processedCount < 1 || request.processedCount > request.expectedRemaining)
            throw BadRequestException("Processed quantity must be between one and the remaining quantity")
        return plans.complete(orgId, id, taskId, request)
    }
}
