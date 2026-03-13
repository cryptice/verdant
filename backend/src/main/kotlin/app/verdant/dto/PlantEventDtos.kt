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
    val imageBase64: String?,
    val aiSuggestions: String?,
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
)

data class IdentifyPlantRequest(
    val imageBase64: String,
)

data class PlantSuggestion(
    val species: String,
    val commonName: String,
    val confidence: Double,
)

data class HarvestStatRow(
    val species: String,
    val totalWeightGrams: Double,
    val totalQuantity: Int,
    val harvestCount: Int,
)
