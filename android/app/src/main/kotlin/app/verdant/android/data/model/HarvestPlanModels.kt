package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

data class LifecycleStep(
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("daysBeforeHarvest") val daysBeforeHarvest: Int?,
    @SerializedName("quantityBasis") val quantityBasis: String
)

data class ProductionProfile(
    @SerializedName("sellableUnit") val sellableUnit: String,
    @SerializedName("outputPerPlant") val outputPerPlant: Double?,
    @SerializedName("establishmentPercent") val establishmentPercent: Double?,
    @SerializedName("startingUnit") val startingUnit: String,
    @SerializedName("steps") val steps: List<LifecycleStep>
)

data class PlanningSpecies(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("profile") val profile: ProductionProfile,
    @SerializedName("customized") val customized: Boolean,
    @SerializedName("defaultSchedule") val defaultSchedule: DefaultScheduleInfo? = null,
)

data class PlanningSource(@SerializedName("title") val title: String, @SerializedName("url") val url: String)
data class DefaultScheduleInfo(
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String,
    @SerializedName("version") val version: String,
    @SerializedName("climate") val climate: String,
    @SerializedName("description") val description: String,
    @SerializedName("reviewRequired") val reviewRequired: Boolean,
    @SerializedName("harvestMonths") val harvestMonths: List<Int>,
    @SerializedName("plantingMonths") val plantingMonths: List<Int>,
    @SerializedName("sources") val sources: List<PlanningSource>,
)

data class HarvestPlanRequest(
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("speciesId") val speciesId: Long? = null,
    @SerializedName("groupId") val groupId: Long? = null,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("sellableUnit") val sellableUnit: String,
    @SerializedName("harvestDate") val harvestDate: String,
    @SerializedName("allocations") val allocations: Map<Long, Int> = emptyMap()
)

data class PlannedStep(
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String,
    @SerializedName("activityType") val activityType: String,
    @SerializedName("date") val date: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit") val unit: String
)

data class SpeciesAllocation(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("outputQuantity") val outputQuantity: Int,
    @SerializedName("plantsNeeded") val plantsNeeded: Int,
    @SerializedName("startingQuantity") val startingQuantity: Int,
    @SerializedName("profile") val profile: ProductionProfile,
    @SerializedName("steps") val steps: List<PlannedStep>
)

data class PlanningIssue(
    @SerializedName("speciesId") val speciesId: Long?,
    @SerializedName("message") val message: String
)

data class HarvestPlanPreview(
    @SerializedName("request") val request: HarvestPlanRequest,
    @SerializedName("allocations") val allocations: List<SpeciesAllocation>,
    @SerializedName("issues") val issues: List<PlanningIssue>,
    @SerializedName("unallocatedQuantity") val unallocatedQuantity: Int,
    @SerializedName("canSave") val canSave: Boolean,
    @SerializedName("fingerprint") val fingerprint: String
)

data class CreateHarvestPlanRequest(
    @SerializedName("requestKey") val requestKey: String,
    @SerializedName("fingerprint") val fingerprint: String,
    @SerializedName("target") val target: HarvestPlanRequest
)

data class HarvestPlanTask(
    @SerializedName("id") val id: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("stepKey") val stepKey: String,
    @SerializedName("remainingCount") val remainingCount: Int,
    @SerializedName("status") val status: String
)

data class HarvestPlanResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("status") val status: String,
    @SerializedName("snapshot") val snapshot: HarvestPlanPreview,
    @SerializedName("tasks") val tasks: List<HarvestPlanTask>,
    @SerializedName("createdAt") val createdAt: String
)

data class CompleteHarvestPlanTaskRequest(
    @SerializedName("expectedRemaining") val expectedRemaining: Int,
    @SerializedName("processedCount") val processedCount: Int
)

internal fun planningUnit(unit: String) = when (unit) {
    "STEM" -> "stjälkar"; "FLOWER" -> "blommor"; "SEED" -> "frön"; "PLUG" -> "pluggplantor"
    "BULB" -> "lökar"; "TUBER" -> "knölar"; "PLANT" -> "plantor"; else -> unit
}


internal fun planningActivity(activity: String) = when (activity) {
    "PURCHASE" -> "Inköp"; "SOW" -> "Sådd"; "POT_UP" -> "Omskolning"; "PLANT" -> "Plantering"
    "PINCH" -> "Toppning"; "SUPPORT" -> "Stöd"; "WATER" -> "Vattning"; "FERTILIZE" -> "Gödsling"
    "HARVEST" -> "Skörd"; "TODO" -> "Att göra"; else -> activity
}
