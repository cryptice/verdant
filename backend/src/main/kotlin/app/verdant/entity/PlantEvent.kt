package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class PlantEvent(
    val id: Long? = null,
    val plantId: Long,
    val eventType: PlantEventType,
    val eventDate: LocalDate = LocalDate.now(),
    val plantCount: Int? = null,
    val weightGrams: Double? = null,
    val quantity: Int? = null,
    val notes: String? = null,
    val imageUrl: String? = null,
    val aiSuggestions: String? = null,
    val stemCount: Int? = null,
    val stemLengthCm: Int? = null,
    val qualityGrade: String? = null,
    val vaseLifeDays: Int? = null,
    val harvestDestinationId: Long? = null,
    val supplyApplicationId: Long? = null,
    val createdAt: Instant = Instant.now(),
)

enum class PlantEventType { SEEDED, POTTED_UP, PLANTED_OUT, HARVESTED, RECOVERED, REMOVED, NOTE, BUDDING, FIRST_BLOOM, PEAK_BLOOM, LAST_BLOOM, LIFTED, DIVIDED, STORED, PINCHED, DISBUDDED, APPLIED_SUPPLY, WATERED, MOVED }
