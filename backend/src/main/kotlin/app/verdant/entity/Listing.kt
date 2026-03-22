package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class Listing(
    val id: Long? = null,
    val userId: Long,
    val speciesId: Long,
    val title: String,
    val description: String? = null,
    val quantityAvailable: Int,
    val pricePerStemCents: Int,
    val availableFrom: LocalDate,
    val availableUntil: LocalDate,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
