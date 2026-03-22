package app.verdant.dto

import java.time.Instant
import java.time.LocalDate

data class MarketOrderResponse(
    val id: Long,
    val purchaserId: Long,
    val purchaserName: String,
    val producerId: Long,
    val producerName: String,
    val status: String,
    val deliveryDate: LocalDate?,
    val totalCents: Int,
    val notes: String?,
    val items: List<OrderItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class OrderItemResponse(
    val id: Long,
    val listingId: Long,
    val speciesId: Long,
    val speciesName: String,
    val quantity: Int,
    val pricePerStemCents: Int,
)

data class CreateMarketOrderRequest(
    val deliveryDate: LocalDate? = null,
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>,
)

data class CreateOrderItemRequest(
    val listingId: Long,
    val quantity: Int,
)

data class UpdateOrderStatusRequest(
    val status: String,
)
