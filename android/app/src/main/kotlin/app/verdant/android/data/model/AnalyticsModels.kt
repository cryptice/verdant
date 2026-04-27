package app.verdant.android.data.model

import com.google.gson.annotations.SerializedName

// ── Analytics ──

data class SeasonSummaryResponse(
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("seasonName") val seasonName: String,
    @SerializedName("year") val year: Int,
    @SerializedName("totalPlants") val totalPlants: Int,
    @SerializedName("totalStemsHarvested") val totalStemsHarvested: Int,
    @SerializedName("totalHarvestWeightGrams") val totalHarvestWeightGrams: Double,
    @SerializedName("speciesCount") val speciesCount: Int,
    @SerializedName("topSpecies") val topSpecies: List<SpeciesYieldSummary>,
)

data class SpeciesYieldSummary(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("plantCount") val plantCount: Int,
    @SerializedName("stemsHarvested") val stemsHarvested: Int,
    @SerializedName("avgStemLength") val avgStemLength: Double?,
    @SerializedName("avgVaseLife") val avgVaseLife: Double?,
    @SerializedName("qualityBreakdown") val qualityBreakdown: Map<String, Int>,
)

data class SpeciesComparisonResponse(
    @SerializedName("speciesId") val speciesId: Long,
    @SerializedName("speciesName") val speciesName: String,
    @SerializedName("seasons") val seasons: List<SpeciesSeasonData>,
)

data class SpeciesSeasonData(
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("seasonName") val seasonName: String,
    @SerializedName("year") val year: Int,
    @SerializedName("plantCount") val plantCount: Int,
    @SerializedName("stemsHarvested") val stemsHarvested: Int,
    @SerializedName("stemsPerPlant") val stemsPerPlant: Double?,
    @SerializedName("avgStemLength") val avgStemLength: Double?,
    @SerializedName("avgVaseLife") val avgVaseLife: Double?,
)

data class YieldPerBedResponse(
    @SerializedName("bedId") val bedId: Long,
    @SerializedName("bedName") val bedName: String,
    @SerializedName("gardenName") val gardenName: String,
    @SerializedName("areaM2") val areaM2: Double?,
    @SerializedName("seasons") val seasons: List<BedSeasonYield>,
)

data class BedSeasonYield(
    @SerializedName("seasonId") val seasonId: Long,
    @SerializedName("seasonName") val seasonName: String,
    @SerializedName("stemsHarvested") val stemsHarvested: Int,
    @SerializedName("stemsPerM2") val stemsPerM2: Double?,
)
