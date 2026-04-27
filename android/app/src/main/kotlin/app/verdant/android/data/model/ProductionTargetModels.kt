package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Production Targets ──

data class ProductionTargetResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("stemsPerWeek") val stemsPerWeek: Int,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class ProductionForecastResponse(
    @SerializedName("targetId") val targetId: Long,
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String?,
    @SerializedName("totalWeeks") val totalWeeks: Long,
    @SerializedName("totalStemsNeeded") val totalStemsNeeded: Long,
    @SerializedName("stemsPerPlant") val stemsPerPlant: Int,
    @SerializedName("plantsNeeded") val plantsNeeded: Long,
    @SerializedName("germinationRate") val germinationRate: Int,
    @SerializedName("seedsNeeded") val seedsNeeded: Long,
    @SerializedName("daysToHarvest") val daysToHarvest: Int,
    @SerializedName("suggestedSowDate") val suggestedSowDate: String?,
    @SerializedName("warnings") val warnings: List<String>,
)
