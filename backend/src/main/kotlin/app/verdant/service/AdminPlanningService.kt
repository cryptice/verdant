package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.SpeciesGroup
import app.verdant.repository.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.NotFoundException
import java.net.URI

@ApplicationScoped
class AdminPlanningService(
    private val schedules: SharedScheduleRepository,
    private val species: SpeciesRepository,
    private val groups: SpeciesGroupRepository,
    private val defaults: PlanningDefaults,
    private val calculator: HarvestPlanCalculator,
) {
    fun listSchedules() = schedules.findAll()
    fun schedule(key: String) = listSchedules().find { it.key == key } ?: throw NotFoundException("Schedule not found")
    private fun sharedSpecies(id: Long) = species.findById(id)?.takeIf { it.orgId == null }
        ?: throw NotFoundException("Shared species not found")
    private fun sharedGroup(id: Long) = groups.findById(id)?.takeIf { it.orgId == null }
        ?: throw NotFoundException("Shared group not found")

    fun planningSpecies(): List<AdminPlanningSpecies> {
        val catalogue = defaults.catalogue()
        return species.findAll().filter { it.orgId == null }.map { sp ->
            val resolved = defaults.suggest(sp, catalogue)
            AdminPlanningSpecies(sp.id!!, listOfNotNull(sp.commonNameSv ?: sp.commonName, sp.variantName).joinToString(" — "),
                sp.scientificName, sp.defaultUnitType, catalogue.assignments[sp.id], resolved.info.key,
                resolved.info.reviewRequired, sp.workflowTemplateId)
        }
    }

    private fun validate(schedule: SharedSchedule) {
        val errors = calculator.profileIssues(schedule.profile, allowIncomplete = schedule.reviewRequired).toMutableList()
        if (!schedule.key.matches(Regex("[a-z0-9][a-z0-9-]{0,79}")) || schedule.key in setOf("unassigned", "new")) errors.add("Use a unique lowercase schedule key (letters, digits and hyphens); 'new' and 'unassigned' are reserved.")
        if (schedule.name.isBlank() || schedule.name.length > 255) errors.add("Name is required (maximum 255 characters).")
        if (schedule.version.isBlank() || schedule.version.length > 80 || schedule.climate.isBlank() || schedule.climate.length > 1000 || schedule.description.length > 10000)
            errors.add("Set a version and climate, within their length limits.")
        if (schedule.priority !in 0..100000) errors.add("Priority must be between 0 and 100000.")
        if ((schedule.harvestMonths + schedule.plantingMonths).any { it !in 1..12 }) errors.add("Months must be between 1 and 12.")
        if (schedule.startingUnits != listOf(schedule.profile.startingUnit)) errors.add("Matching starting material must equal the lifecycle's starting material.")
        if ((schedule.genera + schedule.scientificPrefixes).any { it.isBlank() || it.length > 255 } || schedule.genera.size > 100 || schedule.scientificPrefixes.size > 100)
            errors.add("Use at most 100 nonempty botanical names per rule.")
        if (schedule.sources.size > 30 || schedule.sources.any { source ->
                source.title.isBlank() || source.title.length > 255 || source.url.length > 2000 ||
                    runCatching { URI(source.url).let { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() && it.userInfo == null } }.getOrDefault(false).not()
            }) errors.add("Sources need a title and a valid http(s) URL.")
        if (errors.isNotEmpty()) throw BadRequestException(errors.joinToString(" "))
    }

    @Transactional
    fun createSchedule(request: SharedSchedule): SharedSchedule {
        schedules.lock()
        validate(request)
        if (listSchedules().any { it.key == request.key }) throw ClientErrorException("A schedule with this key already exists.", 409)
        schedules.create(request.copy(revision = 0))
        return schedule(request.key)
    }

    @Transactional
    fun updateSchedule(key: String, request: SharedSchedule): SharedSchedule {
        schedules.lock()
        schedule(key)
        if (key != request.key) throw BadRequestException("A schedule key cannot be changed.")
        validate(request)
        val assigned = schedules.assignments().filterValues { it == key }.keys
        if (species.findAll().any { it.id in assigned && it.defaultUnitType != request.profile.startingUnit })
            throw BadRequestException("Remove incompatible species assignments before changing starting material.")
        schedules.update(request)
        return schedule(key)
    }

    @Transactional
    fun deleteSchedule(key: String, revision: Int) {
        schedules.lock()
        schedule(key)
        if (schedules.assignments().containsValue(key)) throw ClientErrorException("Remove explicit species assignments before deleting this schedule.", 409)
        schedules.delete(key, revision)
    }

    @Transactional
    fun assign(request: ScheduleAssignmentRequest) {
        schedules.lock()
        if (request.speciesIds.isEmpty() || request.speciesIds.size > 2000) throw BadRequestException("Select 1–2000 species.")
        val schedule = request.scheduleKey?.let(::schedule)
        val selected = request.speciesIds.map(::sharedSpecies)
        if (schedule != null && selected.any { it.defaultUnitType != schedule.profile.startingUnit })
            throw BadRequestException("All selected species must use the schedule's starting material.")
        selected.forEach { schedules.assign(it.id!!, request.scheduleKey) }
    }

    fun listGroups(): List<AdminSpeciesGroup> {
        val sharedIds = species.findAll().filter { it.orgId == null }.map { it.id!! }.toSet()
        val memberships = groups.findGroupIdsBySpeciesIds(sharedIds)
        val members = mutableMapOf<Long, MutableList<Long>>()
        memberships.forEach { (sp, ids) -> ids.forEach { members.getOrPut(it) { mutableListOf() }.add(sp) } }
        return groups.findAll().filter { it.orgId == null }.map { AdminSpeciesGroup(it.id!!, it.name, members[it.id].orEmpty().sorted()) }
    }

    @Transactional
    fun saveGroup(id: Long?, name: String): AdminSpeciesGroup {
        groups.lockPlanningCatalogue()
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 255) throw BadRequestException("Group name is required (maximum 255 characters).")
        if (groups.findAll().any { it.orgId == null && it.id != id && it.name.equals(trimmed, ignoreCase = true) })
            throw ClientErrorException("A shared group with this name already exists.", 409)
        val group = if (id == null) groups.persist(SpeciesGroup(name = trimmed))
            else sharedGroup(id).copy(name = trimmed).also(groups::update)
        return AdminSpeciesGroup(group.id!!, group.name, groups.findSpeciesIdsByGroupId(group.id))
    }

    @Transactional
    fun deleteGroup(id: Long) {
        groups.lockPlanningCatalogue()
        sharedGroup(id)
        groups.delete(id)
    }

    @Transactional
    fun addMembers(id: Long, request: GroupMembersRequest) {
        groups.lockPlanningCatalogue()
        sharedGroup(id)
        if (request.speciesIds.isEmpty() || request.speciesIds.size > 2000) throw BadRequestException("Select 1–2000 species.")
        request.speciesIds.forEach(::sharedSpecies)
        request.speciesIds.forEach { groups.addSpeciesToGroup(it, id) }
    }

    @Transactional
    fun removeMember(id: Long, speciesId: Long) {
        groups.lockPlanningCatalogue()
        sharedGroup(id); sharedSpecies(speciesId)
        groups.removeSpeciesFromGroup(speciesId, id)
    }
}
