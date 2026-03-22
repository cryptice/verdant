package app.verdant.dto

data class BedHistoryEntry(
    val seasonId: Long?,
    val seasonName: String?,
    val year: Int?,
    val species: List<BedHistorySpecies>,
)

data class BedHistorySpecies(
    val speciesId: Long,
    val speciesName: String,
    val plantCount: Int,
    val totalStemsHarvested: Int,
    val status: String,
)
