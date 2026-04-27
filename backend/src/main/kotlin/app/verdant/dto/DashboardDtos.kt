package app.verdant.dto

data class DashboardResponse(
    val user: UserResponse,
    val gardens: List<GardenSummary>,
    val stats: DashboardStats
)

data class GardenSummary(
    val id: Long,
    val name: String,
    val emoji: String?,
    val bedCount: Int,
    val plantCount: Int
)

data class DashboardStats(
    val totalGardens: Int,
    val totalBeds: Int,
    val totalPlants: Int,
    val totalActivePlants: Int = 0,
    val totalActiveSpecies: Int = 0,
)
