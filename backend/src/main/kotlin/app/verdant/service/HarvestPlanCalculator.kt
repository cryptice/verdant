package app.verdant.service

import app.verdant.dto.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@ApplicationScoped
class HarvestPlanCalculator {
    fun profileIssues(profile: ProductionProfile, allowIncomplete: Boolean = false): List<String> = buildList {
        if ((profile.outputPerPlant == null && !allowIncomplete) || profile.outputPerPlant?.let {
            it < BigDecimal("0.01") || it > BigDecimal("1000000") } == true)
            add("Set sellable output per plant for the harvest date (0.01–1,000,000).")
        if ((profile.establishmentPercent == null && !allowIncomplete) || profile.establishmentPercent?.let {
            it < BigDecimal("0.01") || it > BigDecimal(100) } == true) add("Set establishment success (0.01–100%).")
        val steps = profile.steps
        if (steps.size !in 3..30) add("Include purchase, propagation and harvest, with at most 30 steps.")
        if (steps.map { it.key }.distinct().size != steps.size || steps.any { !it.key.matches(Regex("[a-zA-Z0-9_-]{1,40}")) })
            add("Each lifecycle step needs a unique key.")
        if (steps.any { it.name.isBlank() || it.name.length > 255 || it.activityType !in ACTIVITIES })
            add("Each step needs a name and a supported activity.")
        if (steps.any { (it.daysBeforeHarvest == null && !allowIncomplete) || it.daysBeforeHarvest?.let { d -> d !in 0..3650 } == true })
            add("Set days before harvest for every step (0–3650).")
        if (steps.mapNotNull { it.daysBeforeHarvest }.zipWithNext().any { (a, b) -> a < b })
            add("Lifecycle steps must run in order from purchase to harvest.")
        if (steps.firstOrNull()?.let { it.activityType == "PURCHASE" && it.quantityBasis == PlanningQuantityBasis.START } != true)
            add("The first step must purchase starting material.")
        if (steps.count { it.activityType == "HARVEST" } != 1 || steps.lastOrNull()?.let {
                it.activityType == "HARVEST" && it.daysBeforeHarvest == 0 && it.quantityBasis == PlanningQuantityBasis.OUTPUT
            } != true) add("The last step must harvest sellable output on the target date.")
        val propagation = if (profile.startingUnit.name == "SEED") "SOW" else "PLANT"
        if (steps.none { it.activityType == propagation && it.quantityBasis == PlanningQuantityBasis.START })
            add("Include a $propagation step measured in starting material.")
    }

    fun calculate(request: HarvestPlanRequest, candidates: List<PlanningSpecies>, today: LocalDate): HarvestPlanPreview {
        if (request.quantity !in 1..1_000_000 || request.harvestDate.year !in 1900..2200)
            throw BadRequestException("Choose a quantity from 1 to 1,000,000 and a harvest date between 1900 and 2200")
        if (request.allocations.keys.any { id -> candidates.none { it.speciesId == id } } ||
            request.allocations.values.any { it < 0 } || request.allocations.values.sumOf { it.toLong() } > request.quantity)
            throw BadRequestException("Fixed allocations must belong to the target and total no more than its quantity")
        val issues = mutableListOf<PlanningIssue>()
        val eligible = candidates.sortedBy { it.speciesId }.filter { species ->
            val reasons = profileIssues(species.profile).toMutableList()
            species.defaultSchedule?.takeIf { !species.customized }?.let { schedule ->
                if (schedule.reviewRequired) reasons.add("Review and save the suggested lifecycle for this species before planning its harvest.")
                if (schedule.harvestMonths.isNotEmpty() && request.harvestDate.monthValue !in schedule.harvestMonths)
                    reasons.add("The target is outside the default flowering season. Choose a suitable date or configure a profile for your growing conditions.")
                val planting = (species.profile.steps.lastOrNull { it.activityType == "PLANT" }
                    ?: species.profile.steps.firstOrNull { it.activityType == "SOW" })?.daysBeforeHarvest
                if (planting != null && schedule.plantingMonths.isNotEmpty() &&
                    request.harvestDate.minusDays(planting.toLong()).monthValue !in schedule.plantingMonths)
                    reasons.add("The default would plant outside the crop's planting season. Adjust the target or configure its lifecycle.")
            }
            if (species.profile.sellableUnit != request.sellableUnit) reasons.add("Sellable unit does not match the target.")
            if (species.profile.steps.any { it.daysBeforeHarvest != null && it.daysBeforeHarvest in 0..3650 &&
                    request.harvestDate.minusDays(it.daysBeforeHarvest.toLong()).isBefore(today) })
                reasons.add("The lifecycle would need to start in the past. Choose a later harvest date.")
            reasons.forEach { issues.add(PlanningIssue(species.speciesId, it)) }
            reasons.isEmpty()
        }
        val unpinned = eligible.filter { it.speciesId !in request.allocations }
        val remainder = request.quantity - request.allocations.values.sum()
        val suggested = unpinned.mapIndexed { index, sp ->
            sp.speciesId to (remainder / unpinned.size + if (index < remainder % unpinned.size) 1 else 0)
        }.toMap()
        val allocations = eligible.mapNotNull { species ->
            val output = request.allocations[species.speciesId] ?: suggested[species.speciesId] ?: 0
            if (output == 0) return@mapNotNull null
            val profile = species.profile
            val plants = BigDecimal(output).divide(profile.outputPerPlant!!, 0, RoundingMode.CEILING)
            val starting = plants.multiply(BigDecimal(100)).divide(profile.establishmentPercent!!, 0, RoundingMode.CEILING)
            if (plants > BigDecimal(10_000_000) || starting > BigDecimal(10_000_000)) {
                issues.add(PlanningIssue(species.speciesId, "This allocation requires more than 10,000,000 plants or starting units."))
                return@mapNotNull null
            }
            SpeciesAllocation(species.speciesId, species.speciesName, output, plants.toInt(), starting.toInt(), profile,
                profile.steps.map { step ->
                    val (count, unit) = when (step.quantityBasis) {
                        PlanningQuantityBasis.START -> starting.toInt() to profile.startingUnit.name
                        PlanningQuantityBasis.PLANT -> plants.toInt() to "PLANT"
                        PlanningQuantityBasis.OUTPUT -> output to profile.sellableUnit.name
                    }
                    PlannedStep("${species.speciesId}:${step.key}", step.name, step.activityType,
                        request.harvestDate.minusDays(step.daysBeforeHarvest!!.toLong()), count, unit)
                })
        }
        val unallocated = request.quantity - allocations.sumOf { it.outputQuantity }
        if (unallocated > 0) issues.add(PlanningIssue(null, "$unallocated sellable units remain unallocated. Configure profiles or adjust fixed quantities."))
        return HarvestPlanPreview(request, allocations, issues, unallocated, unallocated == 0)
    }

    companion object {
        val ACTIVITIES = setOf("PURCHASE", "SOW", "POT_UP", "PLANT", "PINCH", "SUPPORT", "WATER", "FERTILIZE", "HARVEST", "TODO")
    }
}
