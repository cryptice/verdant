package app.verdant.dto

import app.verdant.entity.PlantStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
    val seasonId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreatePlantRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    val speciesId: Long? = null,
    val plantedDate: LocalDate? = null,
    val status: PlantStatus = PlantStatus.SEEDED,
    @field:Min(0)
    val seedCount: Int? = null,
    @field:Min(0)
    val survivingCount: Int? = null,
    val seasonId: Long? = null,
)

data class UpdatePlantRequest(
    @field:Size(max = 255)
    val name: String? = null,
    val speciesId: Long? = null,
    val plantedDate: LocalDate? = null,
    val status: PlantStatus? = null,
    @field:Min(0)
    val seedCount: Int? = null,
    @field:Min(0)
    val survivingCount: Int? = null,
    val seasonId: Long? = null,
)

data class SpeciesPlantSummary(
    val speciesId: Long,
    val speciesName: String,
    val variantName: String?,
    val scientificName: String?,
    val activePlantCount: Int,
    val totalPlantCount: Int,
)

data class BatchSowRequest(
    val bedId: Long? = null,
    @field:NotNull
    val speciesId: Long,
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:Min(1)
    val seedCount: Int,
    @field:Size(max = 2000)
    val notes: String? = null,
    val imageBase64: String? = null,
    /** When set, the plants are recorded as if they were sown on this date
     *  rather than today. Used for backdating when registering existing plants. */
    val plantedDate: java.time.LocalDate? = null,
)

data class BatchSowResponse(
    val plantIds: List<Long>,
    val count: Int,
)

data class BatchEventRequest(
    @field:NotNull
    val speciesId: Long,
    val bedId: Long? = null,
    val plantedDate: String? = null,
    @field:NotBlank
    val status: String,
    @field:NotBlank
    val eventType: String,
    @field:Min(1)
    val count: Int,
    @field:Size(max = 2000)
    val notes: String? = null,
    val imageBase64: String? = null,
    val targetBedId: Long? = null,
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

data class TraySummaryEntry(
    val speciesId: Long?,
    val speciesName: String,
    val variantName: String?,
    val status: String,
    val count: Int,
)

data class PlantLocationGroup(
    val gardenName: String?,
    val bedName: String?,
    val bedId: Long?,
    val status: String,
    val count: Int,
    val year: Int,
)
