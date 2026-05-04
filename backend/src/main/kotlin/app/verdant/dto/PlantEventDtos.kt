package app.verdant.dto

import app.verdant.entity.PlantEventType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class PlantEventResponse(
    val id: Long,
    val plantId: Long,
    val eventType: PlantEventType,
    val eventDate: LocalDate,
    val plantCount: Int?,
    val weightGrams: Double?,
    val quantity: Int?,
    val notes: String?,
    val imageUrl: String?,
    val aiSuggestions: String?,
    val stemCount: Int?,
    val stemLengthCm: Int?,
    val qualityGrade: String?,
    val vaseLifeDays: Int?,
    val supplyApplicationId: Long?,
    val createdAt: Instant,
)

data class CreatePlantEventRequest(
    @field:NotNull
    val eventType: PlantEventType,
    val eventDate: LocalDate = LocalDate.now(),
    @field:Min(0)
    val plantCount: Int? = null,
    val weightGrams: Double? = null,
    @field:Min(0)
    val quantity: Int? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
    val imageBase64: String? = null,
    @field:Size(max = 2000)
    val aiSuggestions: String? = null,
    @field:Min(0)
    val stemCount: Int? = null,
    @field:Min(0)
    val stemLengthCm: Int? = null,
    @field:Size(max = 255)
    val qualityGrade: String? = null,
    @field:Min(0)
    val vaseLifeDays: Int? = null,
)

data class IdentifyPlantRequest(
    @field:NotNull
    val imageBase64: String,
)

data class CropBox(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 1.0,
    val height: Double = 1.0,
)

data class PlantSuggestion(
    val species: String,
    val commonName: String,
    val confidence: Double,
    val cropBox: CropBox? = null,
)

data class HarvestStatRow(
    val species: String,
    val totalWeightGrams: Double,
    val totalQuantity: Int,
    val harvestCount: Int,
    val totalStems: Int,
)
