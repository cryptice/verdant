package app.verdant.dto

import java.time.Instant
import java.time.LocalDate

data class ListingResponse(
    val id: Long,
    val userId: Long,
    val producerName: String,
    val speciesId: Long,
    val speciesName: String,
    val speciesNameSv: String?,
    val title: String,
    val description: String?,
    val quantityAvailable: Int,
    val pricePerStemCents: Int,
    val availableFrom: LocalDate,
    val availableUntil: LocalDate,
    val imageUrl: String?,
    val isActive: Boolean,
    val createdAt: Instant,
)

data class CreateListingRequest(
    val speciesId: Long,
    val title: String,
    val description: String? = null,
    val quantityAvailable: Int,
    val pricePerStemCents: Int,
    val availableFrom: LocalDate,
    val availableUntil: LocalDate,
    val imageBase64: String? = null,
)

data class UpdateListingRequest(
    val title: String? = null,
    val description: String? = null,
    val quantityAvailable: Int? = null,
    val pricePerStemCents: Int? = null,
    val availableFrom: LocalDate? = null,
    val availableUntil: LocalDate? = null,
    val isActive: Boolean? = null,
)
