package app.verdant.service

import app.verdant.dto.CreateMaintenanceRuleRequest
import app.verdant.dto.MaintenanceRuleResponse
import app.verdant.dto.UpdateMaintenanceRuleRequest
import app.verdant.entity.Bed
import app.verdant.entity.MaintenanceActivity
import app.verdant.entity.MaintenanceRule
import app.verdant.entity.MaintenanceTarget
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.MaintenanceRuleRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.time.LocalDate

@ApplicationScoped
class MaintenanceRuleService(
    private val ruleRepository: MaintenanceRuleRepository,
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository,
    private val areaService: GardenAreaService,
    private val lastDoneResolver: LastDoneResolver,
) {
    fun listRules(bedId: Long?, areaId: Long?, orgId: Long): List<MaintenanceRuleResponse> {
        if (bedId != null && areaId != null) {
            throw BadRequestException("Supply at most one of bedId and areaId")
        }
        val rules = when {
            bedId != null -> { requireBed(bedId, orgId); ruleRepository.findByBedId(bedId) }
            areaId != null -> { areaService.requireArea(areaId, orgId); ruleRepository.findByAreaId(areaId) }
            else -> ruleRepository.findByOrgId(orgId)
        }
        return rules.map { it.toResponse(orgId) }
    }

    fun createRule(request: CreateMaintenanceRuleRequest, orgId: Long): MaintenanceRuleResponse {
        val target = resolveTarget(request.bedId, request.gardenAreaId)
        val activity = parseActivity(request.activityType, target)
        validateSeason(
            request.seasonStartMonth, request.seasonStartDay,
            request.seasonEndMonth, request.seasonEndDay,
        )
        when (target) {
            MaintenanceTarget.BED -> requireBed(request.bedId!!, orgId)
            MaintenanceTarget.GARDEN_AREA -> areaService.requireArea(request.gardenAreaId!!, orgId)
        }

        val saved = ruleRepository.persist(
            MaintenanceRule(
                orgId = orgId,
                bedId = request.bedId,
                gardenAreaId = request.gardenAreaId,
                activity = activity,
                intervalDays = request.intervalDays,
                anchorDate = request.anchorDate,
                seasonStartMonth = request.seasonStartMonth,
                seasonStartDay = request.seasonStartDay,
                seasonEndMonth = request.seasonEndMonth,
                seasonEndDay = request.seasonEndDay,
                notes = request.notes,
            )
        )
        return saved.toResponse(orgId)
    }

    fun updateRule(id: Long, request: UpdateMaintenanceRuleRequest, orgId: Long): MaintenanceRuleResponse {
        val rule = requireRule(id, orgId)
        val activity = request.activityType?.let { parseActivity(it, rule.target) } ?: rule.activity
        validateSeason(
            request.seasonStartMonth ?: rule.seasonStartMonth,
            request.seasonStartDay ?: rule.seasonStartDay,
            request.seasonEndMonth ?: rule.seasonEndMonth,
            request.seasonEndDay ?: rule.seasonEndDay,
        )
        val updated = rule.copy(
            activity = activity,
            intervalDays = request.intervalDays ?: rule.intervalDays,
            anchorDate = request.anchorDate ?: rule.anchorDate,
            seasonStartMonth = request.seasonStartMonth ?: rule.seasonStartMonth,
            seasonStartDay = request.seasonStartDay ?: rule.seasonStartDay,
            seasonEndMonth = request.seasonEndMonth ?: rule.seasonEndMonth,
            seasonEndDay = request.seasonEndDay ?: rule.seasonEndDay,
            active = request.active ?: rule.active,
            notes = request.notes ?: rule.notes,
        )
        ruleRepository.update(updated)
        return updated.toResponse(orgId)
    }

    fun deleteRule(id: Long, orgId: Long) {
        requireRule(id, orgId)
        ruleRepository.delete(id)
    }

    private fun requireRule(id: Long, orgId: Long): MaintenanceRule {
        val rule = ruleRepository.findById(id) ?: throw NotFoundException("Rule not found")
        if (rule.orgId != orgId) throw NotFoundException("Rule not found")
        // Re-check the target so a rule can never outlive its owner's org.
        when (rule.target) {
            MaintenanceTarget.BED -> requireBed(rule.bedId!!, orgId)
            MaintenanceTarget.GARDEN_AREA -> areaService.requireArea(rule.gardenAreaId!!, orgId)
        }
        return rule
    }

    private fun requireBed(bedId: Long, orgId: Long): Bed {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Bed not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        return bed
    }

    private fun resolveTarget(bedId: Long?, areaId: Long?): MaintenanceTarget = when {
        bedId != null && areaId != null -> throw BadRequestException("A rule targets a bed or an area, not both")
        bedId != null -> MaintenanceTarget.BED
        areaId != null -> MaintenanceTarget.GARDEN_AREA
        else -> throw BadRequestException("A rule must target a bed or an area")
    }

    private fun parseActivity(value: String, target: MaintenanceTarget): MaintenanceActivity {
        val activity = runCatching { MaintenanceActivity.parse(value) }
            .getOrElse { throw BadRequestException("Unknown activity: $value") }
        if (!activity.appliesTo(target)) {
            throw BadRequestException("Activity $value does not apply to $target")
        }
        return activity
    }

    private fun validateSeason(startMonth: Int?, startDay: Int?, endMonth: Int?, endDay: Int?) {
        val present = listOfNotNull(startMonth, startDay, endMonth, endDay).size
        if (present != 0 && present != 4) {
            throw BadRequestException("A season window needs all four bounds, or none")
        }
        if (present == 4) {
            runCatching {
                java.time.MonthDay.of(startMonth!!, startDay!!)
                java.time.MonthDay.of(endMonth!!, endDay!!)
            }.getOrElse { throw BadRequestException("Invalid season window bounds") }
        }
    }

    private fun MaintenanceRule.toResponse(orgId: Long): MaintenanceRuleResponse {
        val lastDone = lastDoneResolver.resolve(this)
        val nextDue = MaintenanceDueCalculator.dueDate(
            lastDone, intervalDays, MaintenanceDueCalculator.windowOf(this), LocalDate.now(),
        )
        return MaintenanceRuleResponse(
            id = id!!,
            bedId = bedId,
            bedName = bedId?.let { bedRepository.findById(it)?.name },
            gardenAreaId = gardenAreaId,
            gardenAreaName = gardenAreaId?.let { runCatching { areaService.requireArea(it, orgId).name }.getOrNull() },
            activityType = activity.name,
            intervalDays = intervalDays,
            anchorDate = anchorDate,
            seasonStartMonth = seasonStartMonth,
            seasonStartDay = seasonStartDay,
            seasonEndMonth = seasonEndMonth,
            seasonEndDay = seasonEndDay,
            active = active,
            notes = notes,
            lastDoneDate = lastDone,
            nextDueDate = nextDue,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
