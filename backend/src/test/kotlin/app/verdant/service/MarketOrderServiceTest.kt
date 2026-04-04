package app.verdant.service

import app.verdant.dto.CreateMarketOrderRequest
import app.verdant.dto.CreateOrderItemRequest
import app.verdant.dto.OrderItemResponse
import app.verdant.dto.MarketOrderResponse
import app.verdant.dto.UpdateOrderStatusRequest
import app.verdant.entity.Listing
import app.verdant.entity.MarketOrder
import app.verdant.entity.OrderItem
import app.verdant.entity.OrderStatus
import app.verdant.entity.Species
import app.verdant.entity.User
import app.verdant.repository.ListingRepository
import app.verdant.repository.MarketOrderRepository
import app.verdant.repository.OrderItemRepository
import app.verdant.repository.SpeciesRepository
import app.verdant.repository.UserRepository
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import java.time.Instant
import java.time.LocalDate

class MarketOrderServiceTest {

    private lateinit var orderRepo: MarketOrderRepository
    private lateinit var itemRepo: OrderItemRepository
    private lateinit var listingRepo: ListingRepository
    private lateinit var speciesRepo: SpeciesRepository
    private lateinit var userRepo: UserRepository
    private lateinit var service: MarketOrderService

    private val now = Instant.now()
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        orderRepo = mock(MarketOrderRepository::class.java)
        itemRepo = mock(OrderItemRepository::class.java)
        listingRepo = mock(ListingRepository::class.java)
        speciesRepo = mock(SpeciesRepository::class.java)
        userRepo = mock(UserRepository::class.java)
        service = MarketOrderService(orderRepo, itemRepo, listingRepo, speciesRepo, userRepo)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun makeListing(
        id: Long,
        orgId: Long,
        speciesId: Long = 10L,
        quantity: Int = 20,
        pricePerStemSek: Int = 150,
        isActive: Boolean = true,
    ) = Listing(
        id = id,
        orgId = orgId,
        speciesId = speciesId,
        title = "Test listing",
        quantityAvailable = quantity,
        pricePerStemSek = pricePerStemSek,
        availableFrom = today.minusDays(1),
        availableUntil = today.plusDays(30),
        isActive = isActive,
        createdAt = now,
        updatedAt = now,
    )

    private fun makeOrder(
        id: Long,
        purchaserOrgId: Long,
        producerOrgId: Long,
        status: OrderStatus = OrderStatus.PLACED,
        totalSek: Int = 0,
    ) = MarketOrder(
        id = id,
        purchaserOrgId = purchaserOrgId,
        producerOrgId = producerOrgId,
        status = status,
        totalSek = totalSek,
        createdAt = now,
        updatedAt = now,
    )

    private fun makeOrderItem(
        id: Long,
        orderId: Long,
        listingId: Long,
        speciesId: Long = 10L,
        quantity: Int = 3,
        pricePerStemSek: Int = 150,
    ) = OrderItem(
        id = id,
        orderId = orderId,
        listingId = listingId,
        speciesId = speciesId,
        speciesName = "Rose",
        quantity = quantity,
        pricePerStemSek = pricePerStemSek,
    )

    private fun makeUser(id: Long, displayName: String = "User $id") =
        User(id = id, email = "user$id@test.com", displayName = displayName, createdAt = now, updatedAt = now)

    private fun makeSpecies(id: Long, name: String = "Rose") =
        Species(id = id, commonName = name, createdAt = now)

    /** Stubs the full chain that toResponse() needs after an order is persisted. */
    private fun stubGetOrder(order: MarketOrder, items: List<OrderItem>) {
        `when`(orderRepo.findById(order.id!!)).thenReturn(order)
        `when`(itemRepo.findByOrderId(order.id!!)).thenReturn(items)
        `when`(userRepo.findById(order.purchaserOrgId)).thenReturn(makeUser(order.purchaserOrgId, "Buyer"))
        `when`(userRepo.findById(order.producerOrgId)).thenReturn(makeUser(order.producerOrgId, "Seller"))
    }

    // ── 1. placeOrder succeeds with valid data ────────────────────────────────

    @Test
    fun `placeOrder succeeds with single item and sufficient quantity`() {
        val purchaserId = 1L
        val producerId = 2L
        val listingId = 100L
        val speciesId = 10L
        val quantity = 5
        val pricePerStemSek = 200

        val listing = makeListing(listingId, producerId, speciesId, quantity = 10, pricePerStemSek = pricePerStemSek)
        val savedOrder = makeOrder(1L, purchaserId, producerId, totalSek = pricePerStemSek * quantity)
        val savedItem = makeOrderItem(1L, 1L, listingId, speciesId, quantity, pricePerStemSek)
        val species = makeSpecies(speciesId)

        `when`(listingRepo.findByIdForUpdate(listingId)).thenReturn(listing)
        `when`(orderRepo.persist(any())).thenReturn(savedOrder)
        `when`(itemRepo.persist(any())).thenReturn(savedItem)
        `when`(speciesRepo.findById(speciesId)).thenReturn(species)
        stubGetOrder(savedOrder, listOf(savedItem))

        val request = CreateMarketOrderRequest(
            deliveryDate = today.plusDays(7),
            items = listOf(CreateOrderItemRequest(listingId = listingId, quantity = quantity)),
        )

        val result = service.placeOrder(request, purchaserId)

        assertNotNull(result)
        assertEquals(savedOrder.id, result.id)
        assertEquals(purchaserId, result.purchaserId)
        assertEquals(producerId, result.producerId)
        assertEquals("PLACED", result.status)
    }

    // ── 2. placeOrder rejects ordering own listing ────────────────────────────

    @Test
    fun `placeOrder throws when purchaser is the same org as the listing owner`() {
        val orgId = 1L
        val listingId = 100L
        val listing = makeListing(listingId, orgId)

        `when`(listingRepo.findByIdForUpdate(listingId)).thenReturn(listing)

        val request = CreateMarketOrderRequest(
            items = listOf(CreateOrderItemRequest(listingId = listingId, quantity = 3)),
        )

        assertThrows<BadRequestException> {
            service.placeOrder(request, orgId)
        }

        verify(orderRepo, never()).persist(any())
    }

    // ── 3. placeOrder rejects insufficient quantity ───────────────────────────

    @Test
    fun `placeOrder throws when requested quantity exceeds available`() {
        val purchaserId = 1L
        val producerId = 2L
        val listingId = 100L
        val listing = makeListing(listingId, producerId, quantity = 5)

        `when`(listingRepo.findByIdForUpdate(listingId)).thenReturn(listing)

        val request = CreateMarketOrderRequest(
            items = listOf(CreateOrderItemRequest(listingId = listingId, quantity = 10)),
        )

        assertThrows<BadRequestException> {
            service.placeOrder(request, purchaserId)
        }

        verify(orderRepo, never()).persist(any())
    }

    // ── 4. placeOrder rejects items from multiple producers ───────────────────

    @Test
    fun `placeOrder throws when items belong to different producers`() {
        val purchaserId = 1L
        val producerA = 2L
        val producerB = 3L
        val listingA = makeListing(100L, producerA, speciesId = 10L, quantity = 10)
        val listingB = makeListing(101L, producerB, speciesId = 11L, quantity = 10)

        `when`(listingRepo.findByIdForUpdate(100L)).thenReturn(listingA)
        `when`(listingRepo.findByIdForUpdate(101L)).thenReturn(listingB)

        val request = CreateMarketOrderRequest(
            items = listOf(
                CreateOrderItemRequest(listingId = 100L, quantity = 2),
                CreateOrderItemRequest(listingId = 101L, quantity = 2),
            ),
        )

        assertThrows<BadRequestException> {
            service.placeOrder(request, purchaserId)
        }

        verify(orderRepo, never()).persist(any())
    }

    // ── 5. placeOrder decrements listing quantity ─────────────────────────────

    @Test
    fun `placeOrder decrements listing quantityAvailable by ordered amount`() {
        val purchaserId = 1L
        val producerId = 2L
        val listingId = 100L
        val speciesId = 10L
        val initialQuantity = 10
        val orderedQuantity = 3

        val listing = makeListing(listingId, producerId, speciesId, quantity = initialQuantity)
        val savedOrder = makeOrder(1L, purchaserId, producerId)
        val savedItem = makeOrderItem(1L, 1L, listingId, speciesId, orderedQuantity)

        `when`(listingRepo.findByIdForUpdate(listingId)).thenReturn(listing)
        `when`(orderRepo.persist(any())).thenReturn(savedOrder)
        `when`(itemRepo.persist(any())).thenReturn(savedItem)
        `when`(speciesRepo.findById(speciesId)).thenReturn(makeSpecies(speciesId))
        stubGetOrder(savedOrder, listOf(savedItem))

        val request = CreateMarketOrderRequest(
            items = listOf(CreateOrderItemRequest(listingId = listingId, quantity = orderedQuantity)),
        )
        service.placeOrder(request, purchaserId)

        val listingCaptor = argumentCaptor<Listing>()
        verify(listingRepo).update(listingCaptor.capture())
        assertEquals(initialQuantity - orderedQuantity, listingCaptor.firstValue.quantityAvailable)
    }

    // ── 6. Order status transitions ───────────────────────────────────────────

    @Test
    fun `producer can accept a PLACED order`() {
        val producerOrgId = 2L
        val order = makeOrder(1L, purchaserOrgId = 1L, producerOrgId = producerOrgId, status = OrderStatus.PLACED)
        val updatedOrder = order.copy(status = OrderStatus.ACCEPTED)

        `when`(orderRepo.findById(1L)).thenReturn(order).thenReturn(updatedOrder)
        `when`(itemRepo.findByOrderId(1L)).thenReturn(emptyList())
        `when`(userRepo.findById(any())).thenReturn(makeUser(1L))

        service.updateOrderStatus(1L, UpdateOrderStatusRequest("ACCEPTED"), producerOrgId)

        verify(orderRepo).updateStatus(1L, OrderStatus.ACCEPTED)
    }

    @Test
    fun `producer can mark an ACCEPTED order as FULFILLED`() {
        val producerOrgId = 2L
        val order = makeOrder(1L, purchaserOrgId = 1L, producerOrgId = producerOrgId, status = OrderStatus.ACCEPTED)
        val updatedOrder = order.copy(status = OrderStatus.FULFILLED)

        `when`(orderRepo.findById(1L)).thenReturn(order).thenReturn(updatedOrder)
        `when`(itemRepo.findByOrderId(1L)).thenReturn(emptyList())
        `when`(userRepo.findById(any())).thenReturn(makeUser(1L))

        service.updateOrderStatus(1L, UpdateOrderStatusRequest("FULFILLED"), producerOrgId)

        verify(orderRepo).updateStatus(1L, OrderStatus.FULFILLED)
    }

    @Test
    fun `producer can mark a FULFILLED order as DELIVERED`() {
        val producerOrgId = 2L
        val order = makeOrder(1L, purchaserOrgId = 1L, producerOrgId = producerOrgId, status = OrderStatus.FULFILLED)
        val updatedOrder = order.copy(status = OrderStatus.DELIVERED)

        `when`(orderRepo.findById(1L)).thenReturn(order).thenReturn(updatedOrder)
        `when`(itemRepo.findByOrderId(1L)).thenReturn(emptyList())
        `when`(userRepo.findById(any())).thenReturn(makeUser(1L))

        service.updateOrderStatus(1L, UpdateOrderStatusRequest("DELIVERED"), producerOrgId)

        verify(orderRepo).updateStatus(1L, OrderStatus.DELIVERED)
    }

    @Test
    fun `purchaser can cancel a PLACED order`() {
        val purchaserOrgId = 1L
        val order = makeOrder(1L, purchaserOrgId = purchaserOrgId, producerOrgId = 2L, status = OrderStatus.PLACED)
        val updatedOrder = order.copy(status = OrderStatus.CANCELLED)

        `when`(orderRepo.findById(1L)).thenReturn(order).thenReturn(updatedOrder)
        `when`(itemRepo.findByOrderId(1L)).thenReturn(emptyList())
        `when`(userRepo.findById(any())).thenReturn(makeUser(1L))

        service.updateOrderStatus(1L, UpdateOrderStatusRequest("CANCELLED"), purchaserOrgId)

        verify(orderRepo).updateStatus(1L, OrderStatus.CANCELLED)
    }

    @Test
    fun `non-purchaser cannot cancel an order`() {
        val order = makeOrder(1L, purchaserOrgId = 1L, producerOrgId = 2L, status = OrderStatus.PLACED)
        `when`(orderRepo.findById(1L)).thenReturn(order)

        assertThrows<ForbiddenException> {
            service.updateOrderStatus(1L, UpdateOrderStatusRequest("CANCELLED"), orgId = 99L)
        }

        verify(orderRepo, never()).updateStatus(any(), any())
    }

    @Test
    fun `non-producer cannot accept an order`() {
        val order = makeOrder(1L, purchaserOrgId = 1L, producerOrgId = 2L, status = OrderStatus.PLACED)
        `when`(orderRepo.findById(1L)).thenReturn(order)

        assertThrows<ForbiddenException> {
            service.updateOrderStatus(1L, UpdateOrderStatusRequest("ACCEPTED"), orgId = 99L)
        }

        verify(orderRepo, never()).updateStatus(any(), any())
    }

    @Test
    fun `cannot set status back to PLACED`() {
        val order = makeOrder(1L, purchaserOrgId = 1L, producerOrgId = 2L, status = OrderStatus.ACCEPTED)
        `when`(orderRepo.findById(1L)).thenReturn(order)

        assertThrows<BadRequestException> {
            service.updateOrderStatus(1L, UpdateOrderStatusRequest("PLACED"), orgId = 2L)
        }

        verify(orderRepo, never()).updateStatus(any(), any())
    }

    @Test
    fun `cannot cancel an order that is not in PLACED status`() {
        val order = makeOrder(1L, purchaserOrgId = 1L, producerOrgId = 2L, status = OrderStatus.ACCEPTED)
        `when`(orderRepo.findById(1L)).thenReturn(order)

        assertThrows<BadRequestException> {
            service.updateOrderStatus(1L, UpdateOrderStatusRequest("CANCELLED"), orgId = 1L)
        }

        verify(orderRepo, never()).updateStatus(any(), any())
    }

    @Test
    fun `invalid status string throws BadRequestException`() {
        val order = makeOrder(1L, purchaserOrgId = 1L, producerOrgId = 2L, status = OrderStatus.PLACED)
        `when`(orderRepo.findById(1L)).thenReturn(order)

        assertThrows<BadRequestException> {
            service.updateOrderStatus(1L, UpdateOrderStatusRequest("BOGUS_STATUS"), orgId = 2L)
        }
    }

    // ── 7. cancelOrder restores listing quantity ──────────────────────────────

    @Test
    fun `cancelling an order restores listing quantities`() {
        val purchaserOrgId = 1L
        val listingId = 100L
        val orderedQuantity = 3
        val currentQuantity = 7

        val order = makeOrder(1L, purchaserOrgId = purchaserOrgId, producerOrgId = 2L, status = OrderStatus.PLACED)
        val item = makeOrderItem(1L, 1L, listingId, quantity = orderedQuantity)
        val listing = makeListing(listingId, 2L, quantity = currentQuantity)
        val updatedOrder = order.copy(status = OrderStatus.CANCELLED)

        `when`(orderRepo.findById(1L)).thenReturn(order).thenReturn(updatedOrder)
        `when`(itemRepo.findByOrderId(1L)).thenReturn(listOf(item))
        `when`(listingRepo.findById(listingId)).thenReturn(listing)
        `when`(userRepo.findById(any())).thenReturn(makeUser(1L))

        service.updateOrderStatus(1L, UpdateOrderStatusRequest("CANCELLED"), purchaserOrgId)

        val listingCaptor = argumentCaptor<Listing>()
        verify(listingRepo).update(listingCaptor.capture())
        assertEquals(currentQuantity + orderedQuantity, listingCaptor.firstValue.quantityAvailable)
    }

    // ── 8. placeOrder calculates correct total ────────────────────────────────

    @Test
    fun `placeOrder calculates total as sum of pricePerStem times quantity for all items`() {
        val purchaserId = 1L
        val producerId = 2L
        val speciesId = 10L

        // Two listings from the same producer
        val listing1 = makeListing(100L, producerId, speciesId = speciesId, quantity = 20, pricePerStemSek = 100)
        val listing2 = makeListing(101L, producerId, speciesId = speciesId, quantity = 20, pricePerStemSek = 250)

        // Expected total: (100 * 4) + (250 * 6) = 400 + 1500 = 1900
        val expectedTotal = 100 * 4 + 250 * 6

        val savedOrder = makeOrder(1L, purchaserId, producerId, totalSek = expectedTotal)
        val savedItem1 = makeOrderItem(1L, 1L, 100L, speciesId, quantity = 4, pricePerStemSek = 100)
        val savedItem2 = makeOrderItem(2L, 1L, 101L, speciesId, quantity = 6, pricePerStemSek = 250)
        val species = makeSpecies(speciesId)

        `when`(listingRepo.findByIdForUpdate(100L)).thenReturn(listing1)
        `when`(listingRepo.findByIdForUpdate(101L)).thenReturn(listing2)
        `when`(speciesRepo.findById(speciesId)).thenReturn(species)
        `when`(itemRepo.persist(any()))
            .thenReturn(savedItem1)
            .thenReturn(savedItem2)

        val orderCaptor = argumentCaptor<MarketOrder>()
        `when`(orderRepo.persist(orderCaptor.capture())).thenReturn(savedOrder)

        stubGetOrder(savedOrder, listOf(savedItem1, savedItem2))

        val request = CreateMarketOrderRequest(
            items = listOf(
                CreateOrderItemRequest(listingId = 100L, quantity = 4),
                CreateOrderItemRequest(listingId = 101L, quantity = 6),
            ),
        )

        service.placeOrder(request, purchaserId)

        assertEquals(expectedTotal, orderCaptor.firstValue.totalSek)
    }
}
