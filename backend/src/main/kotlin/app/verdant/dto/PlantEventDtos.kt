package app.verdant.dto

import app.verdant.entity.PlantEventType
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
    val harvestDestinationId: Long?,
    val customerName: String?,
    val createdAt: Instant,
)

data class CreatePlantEventRequest(
    val eventType: PlantEventType,
    val eventDate: LocalDate = LocalDate.now(),
    val plantCount: Int? = null,
    val weightGrams: Double? = null,
    val quantity: Int? = null,
    val notes: String? = null,
    val imageBase64: String? = null,
    val aiSuggestions: String? = null,
    val stemCount: Int? = null,
    val stemLengthCm: Int? = null,
    val qualityGrade: String? = null,
    val vaseLifeDays: Int? = null,
    val harvestDestinationId: Long? = null,
)

data class IdentifyPlantRequest(
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
)
