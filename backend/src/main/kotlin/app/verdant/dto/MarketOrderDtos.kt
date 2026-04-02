package app.verdant.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
    @field:Size(max = 2000)
    val notes: String? = null,
    @field:Size(min = 1, max = 100)
    @field:Valid
    val items: List<CreateOrderItemRequest>,
)

data class CreateOrderItemRequest(
    @field:NotNull
    val listingId: Long,
    @field:Min(1)
    val quantity: Int,
)

data class UpdateOrderStatusRequest(
    @field:NotNull
    val status: String,
)
