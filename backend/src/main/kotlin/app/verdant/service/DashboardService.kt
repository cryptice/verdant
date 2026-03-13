package app.verdant.service

import app.verdant.dto.*
import app.verdant.repository.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class DashboardService(
    private val userRepository: UserRepository,
    private val gardenRepository: GardenRepository,
    private val bedRepository: BedRepository,
    private val plantRepository: PlantRepository
) {
    fun getDashboard(userId: Long): DashboardResponse {
        val user = userRepository.findById(userId) ?: throw NotFoundException("User not found")
        val gardens = gardenRepository.findByOwnerId(userId)

        val gardenSummaries = gardens.map { garden ->
            val beds = bedRepository.findByGardenId(garden.id!!)
            val plantCount = plantRepository.countByGardenId(garden.id).toInt()
            GardenSummary(
                id = garden.id,
                name = garden.name,
                emoji = garden.emoji,
                bedCount = beds.size,
                plantCount = plantCount
            )
        }

        return DashboardResponse(
            user = user.toResponse(),
            gardens = gardenSummaries,
            stats = DashboardStats(
                totalGardens = gardens.size,
                totalBeds = gardenSummaries.sumOf { it.bedCount },
                totalPlants = gardenSummaries.sumOf { it.plantCount }
            )
        )
    }
}
