package app.verdant.dto

import app.verdant.entity.PlantType
import app.verdant.entity.UnitType

data class SharedSchedule(
    val key: String,
    val name: String,
    val priority: Int = 1000,
    val revision: Int = 0,
    val autoMatch: Boolean = false,
    val useSpeciesTiming: Boolean = true,
    val version: String,
    val climate: String,
    val description: String,
    val reviewRequired: Boolean = true,
    val harvestMonths: List<Int> = emptyList(),
    val plantingMonths: List<Int> = emptyList(),
    val sources: List<PlanningSource> = emptyList(),
    val genera: List<String> = emptyList(),
    val scientificPrefixes: List<String> = emptyList(),
    val plantTypes: List<PlantType> = emptyList(),
    val startingUnits: List<UnitType>,
    val profile: ProductionProfile,
)

data class AdminSpeciesGroup(val id: Long, val name: String, val speciesIds: List<Long>)
data class GroupMembersRequest(val speciesIds: Set<Long>)
data class ScheduleAssignmentRequest(val scheduleKey: String?, val speciesIds: Set<Long>)
data class AdminPlanningSpecies(
    val id: Long, val name: String, val scientificName: String?, val startingUnit: UnitType,
    val assignedScheduleKey: String?, val resolvedScheduleKey: String,
    val reviewRequired: Boolean, val workflowTemplateId: Long?,
)
