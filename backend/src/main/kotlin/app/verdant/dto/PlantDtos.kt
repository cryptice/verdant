package app.verdant.dto

import app.verdant.entity.PlantStatus
import java.time.Instant
import java.time.LocalDate

data class PlantResponse(
    val id: Long,
    val name: String,
    val speciesId: Long?,
    val speciesName: String?,
    val plantedDate: LocalDate?,
    val status: PlantStatus,
    val seedCount: Int?,
    val survivingCount: Int?,
    val bedId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreatePlantRequest(
    val name: String,
    val speciesId: Long? = null,
    val plantedDate: LocalDate? = null,
    val status: PlantStatus = PlantStatus.SEEDED,
    val seedCount: Int? = null,
    val survivingCount: Int? = null,
)

data class UpdatePlantRequest(
    val name: String? = null,
    val speciesId: Long? = null,
    val plantedDate: LocalDate? = null,
    val status: PlantStatus? = null,
    val seedCount: Int? = null,
    val survivingCount: Int? = null,
)

data class SpeciesPlantSummary(
    val speciesId: Long,
    val speciesName: String,
    val scientificName: String?,
    val activePlantCount: Int,
    val totalPlantCount: Int,
)

data class BatchSowRequest(
    val bedId: Long? = null,
    val speciesId: Long,
    val name: String,
    val seedCount: Int,
    val notes: String? = null,
    val imageBase64: String? = null,
)

data class BatchSowResponse(
    val plantIds: List<Long>,
    val count: Int,
)

data class BatchEventRequest(
    val speciesId: Long,
    val bedId: Long? = null,
    val plantedDate: String? = null,
    val status: String,
    val eventType: String,
    val count: Int,
    val notes: String? = null,
    val imageBase64: String? = null,
)

data class BatchEventResponse(
    val updatedCount: Int,
)

data class PlantGroupResponse(
    val speciesId: Long,
    val speciesName: String?,
    val bedId: Long?,
    val bedName: String?,
    val gardenName: String?,
    val plantedDate: String?,
    val status: String,
    val count: Int,
)

data class PlantLocationGroup(
    val gardenName: String,
    val bedName: String,
    val bedId: Long,
    val status: String,
    val count: Int,
    val year: Int,
)
