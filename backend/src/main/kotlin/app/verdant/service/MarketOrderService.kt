package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.MarketOrder
import app.verdant.entity.OrderItem
import app.verdant.entity.OrderStatus
import app.verdant.repository.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class MarketOrderService(
    private val orderRepo: MarketOrderRepository,
    private val itemRepo: OrderItemRepository,
    private val listingRepo: ListingRepository,
    private val speciesRepo: SpeciesRepository,
    private val userRepo: UserRepository,
) {

    @Transactional
    fun placeOrder(request: CreateMarketOrderRequest, purchaserId: Long): MarketOrderResponse {
        if (request.items.isEmpty()) throw BadRequestException("Order must have at least one item")

        // Resolve all listings, validate, and decrement quantities atomically under FOR UPDATE locks.
        // Each findByIdForUpdate acquires a row-level lock that is held for the duration of the
        // transaction, ensuring no concurrent transaction can check or modify the same quantity
        // between our check and our decrement.
        data class ResolvedItem(val listingId: Long, val speciesId: Long, val quantity: Int, val pricePerStemSek: Int, val producerId: Long)
        val resolvedItems = request.items.map { itemReq ->
            val listing = listingRepo.findByIdForUpdate(itemReq.listingId)
                ?: throw BadRequestException("Listing ${itemReq.listingId} not found")
            if (!listing.isActive) throw BadRequestException("Listing ${listing.id} is not active")
            if (itemReq.quantity <= 0) throw BadRequestException("Quantity must be positive")
            if (itemReq.quantity > listing.quantityAvailable)
                throw BadRequestException("Requested ${itemReq.quantity} but only ${listing.quantityAvailable} available for listing ${listing.id}")
            // Decrement immediately while holding the FOR UPDATE lock
            listingRepo.update(listing.copy(quantityAvailable = listing.quantityAvailable - itemReq.quantity))
            ResolvedItem(
                listingId = listing.id!!,
                speciesId = listing.speciesId,
                quantity = itemReq.quantity,
                pricePerStemSek = listing.pricePerStemSek,
                producerId = listing.userId,
            )
        }

        // All items must belong to the same producer
        val producerIds = resolvedItems.map { it.producerId }.toSet()
        if (producerIds.size != 1) throw BadRequestException("All items in an order must be from the same producer")
        val producerId = producerIds.first()

        if (producerId == purchaserId) throw BadRequestException("Cannot place an order for your own listings")

        val totalSek = resolvedItems.sumOf { it.pricePerStemSek * it.quantity }

        val order = orderRepo.persist(
            MarketOrder(
                purchaserId = purchaserId,
                producerId = producerId,
                deliveryDate = request.deliveryDate,
                totalSek = totalSek,
                notes = request.notes,
            )
        )

        for (item in resolvedItems) {
            val species = speciesRepo.findById(item.speciesId)
            itemRepo.persist(
                OrderItem(
                    orderId = order.id!!,
                    listingId = item.listingId,
                    speciesId = item.speciesId,
                    speciesName = species?.commonName ?: "Unknown",
                    quantity = item.quantity,
                    pricePerStemSek = item.pricePerStemSek,
                )
            )
        }

        return getOrder(order.id!!)
    }

    fun getOrdersForPurchaser(userId: Long, limit: Int = 50, offset: Int = 0): List<MarketOrderResponse> =
        orderRepo.findByPurchaserId(userId, limit, offset).map { it.toResponse() }

    fun getOrdersForProducer(userId: Long, limit: Int = 50, offset: Int = 0): List<MarketOrderResponse> =
        orderRepo.findByProducerId(userId, limit, offset).map { it.toResponse() }

    fun getOrder(id: Long): MarketOrderResponse {
        val order = orderRepo.findById(id) ?: throw NotFoundException("Order not found")
        return order.toResponse()
    }

    fun getOrderForUser(id: Long, userId: Long): MarketOrderResponse {
        val order = orderRepo.findById(id) ?: throw NotFoundException("Order not found")
        if (order.purchaserId != userId && order.producerId != userId) throw ForbiddenException()
        return order.toResponse()
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, request: UpdateOrderStatusRequest, userId: Long): MarketOrderResponse {
        val order = orderRepo.findById(orderId) ?: throw NotFoundException("Order not found")
        val newStatus = try {
            OrderStatus.valueOf(request.status)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid status: ${request.status}")
        }

        when (newStatus) {
            OrderStatus.CANCELLED -> {
                if (order.purchaserId != userId) throw ForbiddenException("Only the purchaser can cancel")
                if (order.status != OrderStatus.PLACED) throw BadRequestException("Can only cancel orders with status PLACED")
                // Restore listing quantities
                val items = itemRepo.findByOrderId(orderId)
                for (item in items) {
                    val listing = listingRepo.findById(item.listingId)
                    if (listing != null) {
                        listingRepo.update(listing.copy(quantityAvailable = listing.quantityAvailable + item.quantity))
                    }
                }
            }
            OrderStatus.ACCEPTED, OrderStatus.FULFILLED, OrderStatus.DELIVERED -> {
                if (order.producerId != userId) throw ForbiddenException("Only the producer can update to $newStatus")
            }
            OrderStatus.PLACED -> throw BadRequestException("Cannot set status back to PLACED")
        }

        orderRepo.updateStatus(orderId, newStatus)
        return getOrder(orderId)
    }

    private fun MarketOrder.toResponse(): MarketOrderResponse {
        val purchaser = userRepo.findById(purchaserId)
        val producer = userRepo.findById(producerId)
        val items = itemRepo.findByOrderId(id!!).map { item ->
            OrderItemResponse(
                id = item.id!!,
                listingId = item.listingId,
                speciesId = item.speciesId,
                speciesName = item.speciesName,
                quantity = item.quantity,
                pricePerStemSek = item.pricePerStemSek,
            )
        }
        return MarketOrderResponse(
            id = id,
            purchaserId = purchaserId,
            purchaserName = purchaser?.displayName ?: "Unknown",
            producerId = producerId,
            producerName = producer?.displayName ?: "Unknown",
            status = status.name,
            deliveryDate = deliveryDate,
            totalSek = totalSek,
            notes = notes,
            items = items,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
