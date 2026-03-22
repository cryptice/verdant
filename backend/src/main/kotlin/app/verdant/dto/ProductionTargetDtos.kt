package app.verdant.dto

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
    val seasonId: Long,
    val speciesId: Long,
    val stemsPerWeek: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val notes: String? = null,
)

data class UpdateProductionTargetRequest(
    val seasonId: Long? = null,
    val speciesId: Long? = null,
    val stemsPerWeek: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val notes: String? = null,
)
