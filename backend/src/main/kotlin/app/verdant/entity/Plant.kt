package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class Plant(
    val id: Long? = null,
    val name: String,
    val species: String? = null,
    val plantedDate: LocalDate? = null,
    val status: PlantStatus = PlantStatus.SEEDLING,
    val bedId: Long,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class PlantStatus { SEEDLING, GROWING, MATURE, HARVESTED, REMOVED }
