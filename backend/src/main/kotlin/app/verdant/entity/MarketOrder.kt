package app.verdant.entity

import java.time.Instant
import java.time.LocalDate

data class MarketOrder(
    val id: Long? = null,
    val purchaserOrgId: Long,
    val producerOrgId: Long,
    val status: OrderStatus = OrderStatus.PLACED,
    val deliveryDate: LocalDate? = null,
    val totalSek: Int = 0,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

enum class OrderStatus { PLACED, ACCEPTED, FULFILLED, DELIVERED, CANCELLED }

data class OrderItem(
    val id: Long? = null,
    val orderId: Long,
    val listingId: Long,
    val speciesId: Long,
    val speciesName: String,
    val quantity: Int,
    val pricePerStemSek: Int,
)
