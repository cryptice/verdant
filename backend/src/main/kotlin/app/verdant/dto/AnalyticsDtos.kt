package app.verdant.dto

data class SeasonSummaryResponse(
    val seasonId: Long,
    val seasonName: String,
    val year: Int,
    val totalPlants: Int,
    val totalStemsHarvested: Int,
    val totalHarvestWeightGrams: Double,
    val speciesCount: Int,
    val topSpecies: List<SpeciesYieldSummary>,
)

data class SpeciesYieldSummary(
    val speciesId: Long,
    val speciesName: String,
    val plantCount: Int,
    val stemsHarvested: Int,
    val avgStemLength: Double?,
    val avgVaseLife: Double?,
    val qualityBreakdown: Map<String, Int>,
)

data class SpeciesComparisonResponse(
    val speciesId: Long,
    val speciesName: String,
    val seasons: List<SpeciesSeasonData>,
)

data class SpeciesSeasonData(
    val seasonId: Long,
    val seasonName: String,
    val year: Int,
    val plantCount: Int,
    val stemsHarvested: Int,
    val stemsPerPlant: Double?,
    val avgStemLength: Double?,
    val avgVaseLife: Double?,
)

data class YieldPerBedResponse(
    val bedId: Long,
    val bedName: String,
    val gardenName: String,
    val areaM2: Double?,
    val seasons: List<BedSeasonYield>,
)

data class BedSeasonYield(
    val seasonId: Long,
    val seasonName: String,
    val stemsHarvested: Int,
    val stemsPerM2: Double?,
)
