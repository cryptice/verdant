package app.verdant.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class ListingResponse(
    val id: Long,
    val sellerName: String,
    val producerName: String,
    val speciesId: Long,
    val speciesName: String,
    val speciesNameSv: String?,
    val title: String,
    val description: String?,
    val quantityAvailable: Int,
    val pricePerStemSek: Int,
    val availableFrom: LocalDate,
    val availableUntil: LocalDate,
    val imageUrl: String?,
    val isActive: Boolean,
    val createdAt: Instant,
)

data class CreateListingRequest(
    @field:NotNull
    val speciesId: Long,
    @field:NotBlank @field:Size(max = 255)
    val title: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:Min(0)
    val quantityAvailable: Int,
    @field:Min(0)
    val pricePerStemSek: Int,
    @field:NotNull
    val availableFrom: LocalDate,
    @field:NotNull
    val availableUntil: LocalDate,
    val imageBase64: String? = null,
)

data class UpdateListingRequest(
    @field:Size(max = 255)
    val title: String? = null,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:Min(0)
    val quantityAvailable: Int? = null,
    @field:Min(0)
    val pricePerStemSek: Int? = null,
    val availableFrom: LocalDate? = null,
    val availableUntil: LocalDate? = null,
    val isActive: Boolean? = null,
)
