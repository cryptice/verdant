package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class Plant(
    val id: Long? = null,
    val name: String,
    val speciesId: Long? = null,
    val plantedDate: LocalDate? = null,
    val status: PlantStatus = PlantStatus.SEEDED,
    val seedCount: Int? = null,
    val survivingCount: Int? = null,
    val bedId: Long,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class PlantStatus { SEEDED, POTTED_UP, PLANTED_OUT, GROWING, HARVESTED, RECOVERED, REMOVED }
