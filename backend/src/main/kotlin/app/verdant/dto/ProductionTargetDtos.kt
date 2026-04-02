package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class ProductionTargetResponse(
    val id: Long,
    val seasonId: Long,
    val speciesId: Long,
    val speciesName: String?,
    val stemsPerWeek: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val notes: String?,
    val createdAt: Instant,
)

data class CreateProductionTargetRequest(
    @field:NotNull
    val seasonId: Long,
    @field:NotNull
    val speciesId: Long,
    @field:Min(1)
    val stemsPerWeek: Int,
    @field:NotNull
    val startDate: LocalDate,
    @field:NotNull
    val endDate: LocalDate,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class UpdateProductionTargetRequest(
    val seasonId: Long? = null,
    val speciesId: Long? = null,
    @field:Min(1)
    val stemsPerWeek: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class ProductionForecastResponse(
    val targetId: Long,
    val speciesId: Long,
    val speciesName: String?,
    val totalWeeks: Long,
    val totalStemsNeeded: Long,
    val stemsPerPlant: Int,
    val plantsNeeded: Long,
    val germinationRate: Int,
    val seedsNeeded: Long,
    val daysToHarvest: Int,
    val suggestedSowDate: LocalDate,
    val warnings: List<String>,
)
