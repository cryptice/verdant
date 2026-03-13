package app.verdant.dto

import app.verdant.entity.PlantStatus
import java.time.Instant
import java.time.LocalDate

data class PlantResponse(
    val id: Long,
    val name: String,
    val species: String?,
    val plantedDate: LocalDate?,
    val status: PlantStatus,
    val bedId: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreatePlantRequest(
    val name: String,
    val species: String? = null,
    val plantedDate: LocalDate? = null,
    val status: PlantStatus = PlantStatus.SEEDLING
)

data class UpdatePlantRequest(
    val name: String? = null,
    val species: String? = null,
    val plantedDate: LocalDate? = null,
    val status: PlantStatus? = null
)
