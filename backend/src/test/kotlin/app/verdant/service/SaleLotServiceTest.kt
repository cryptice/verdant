package app.verdant.service

import app.verdant.dto.ChangeOutletRequest
import app.verdant.dto.ChangePriceRequest
import app.verdant.dto.CreateSaleLotForHarvestRequest
import app.verdant.dto.CreateSaleLotForPlantRequest
import app.verdant.dto.EditSaleRequest
import app.verdant.dto.RecordSaleRequest
import app.verdant.dto.ReturnFromOutletRequest
import app.verdant.entity.Bouquet
import app.verdant.entity.Channel
import app.verdant.entity.Outlet
import app.verdant.entity.Plant
import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import app.verdant.entity.PlantStatus
import app.verdant.entity.Sale
import app.verdant.entity.SaleLot
import app.verdant.entity.SaleLotEvent
import app.verdant.entity.SaleLotEventType
import app.verdant.entity.SaleLotStatus
import app.verdant.entity.SourceKind
import app.verdant.entity.UnitKind
import app.verdant.repository.BouquetRepository
import app.verdant.repository.CustomerRepository
import app.verdant.repository.OutletRepository
import app.verdant.repository.PlantEventRepository
import app.verdant.repository.PlantRepository
import app.verdant.repository.SaleLotEventRepository
import app.verdant.repository.SaleLotRepository
import app.verdant.repository.SaleRepository
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDate

class SaleLotServiceTest {

    private lateinit var lotRepo: SaleLotRepository
    private lateinit var saleRepo: SaleRepository
    private lateinit var eventRepo: SaleLotEventRepository
    private lateinit var outletRepo: OutletRepository
    private lateinit var plantRepo: PlantRepository
    private lateinit var plantEventRepo: PlantEventRepository
    private lateinit var bouquetRepo: BouquetRepository
    private lateinit var customerRepo: CustomerRepository
    private lateinit var service: SaleLotService

    private val orgId = 42L
    private val userId = 7L
    private val outletId = 100L

    @BeforeEach
    fun setup() {
        lotRepo = mock()
        saleRepo = mock()
        eventRepo = mock()
        outletRepo = mock()
        plantRepo = mock()
        plantEventRepo = mock()
        bouquetRepo = mock()
        customerRepo = mock()
        service = SaleLotService(
            lotRepo, saleRepo, eventRepo, outletRepo,
            plantRepo, plantEventRepo, bouquetRepo, customerRepo,
            ObjectMapper(),
        )
        // Default: outlet exists in org. Override per-test if needed.
        whenever(outletRepo.findById(outletId)).thenReturn(makeOutlet(outletId))
    }

    // ── Helpers ──

    private fun makeOutlet(id: Long, channel: Channel = Channel.FLORIST) =
        Outlet(id = id, orgId = orgId, name = "Outlet $id", channel = channel)

    private fun makePlant(id: Long, status: PlantStatus = PlantStatus.GROWING, surviving: Int? = 50) =
        Plant(id = id, name = "Plant $id", status = status, seedCount = 50, survivingCount = surviving, orgId = orgId)

    private fun makeHarvestEvent(id: Long, plantId: Long, stems: Int = 100) =
        PlantEvent(id = id, plantId = plantId, eventType = PlantEventType.HARVESTED, eventDate = LocalDate.now(), stemCount = stems)

    private fun makeBouquet(id: Long) =
        Bouquet(id = id, orgId = orgId, name = "Bouquet $id")

    private fun makeLot(
        id: Long = 1L,
        sourceKind: SourceKind = SourceKind.HARVEST_EVENT,
        harvestEventId: Long? = 200L,
        plantId: Long? = null,
        bouquetId: Long? = null,
        unitKind: UnitKind = UnitKind.STEM,
        stemsPerUnit: Int? = null,
        total: Int = 20,
        remaining: Int = 20,
        status: SaleLotStatus = SaleLotStatus.OFFERED,
        currentOutletId: Long = outletId,
        currentRequestedPriceCents: Int = 2500,
        initialRequestedPriceCents: Int = 2500,
    ) = SaleLot(
        id = id, orgId = orgId, sourceKind = sourceKind,
        plantId = plantId, harvestEventId = harvestEventId, bouquetId = bouquetId,
        unitKind = unitKind, stemsPerUnit = stemsPerUnit,
        quantityTotal = total, quantityRemaining = remaining,
        initialRequestedPriceCents = initialRequestedPriceCents,
        currentRequestedPriceCents = currentRequestedPriceCents,
        currentOutletId = currentOutletId, status = status,
    )

    // ── createForPlant ──

    @Test
    fun `createForPlant rejects REMOVED plant`() {
        whenever(plantRepo.findById(1L)).thenReturn(makePlant(1L, status = PlantStatus.REMOVED))
        assertThrows<ForbiddenException> {
            service.createForPlant(
                CreateSaleLotForPlantRequest(plantId = 1L, unitKind = UnitKind.PLUG, quantityTotal = 5, initialRequestedPriceCents = 1000, currentOutletId = outletId),
                orgId, userId,
            )
        }
    }

    @Test
    fun `createForPlant rejects quantity exceeding available`() {
        whenever(plantRepo.findById(1L)).thenReturn(makePlant(1L, surviving = 50))
        whenever(lotRepo.availableForPlant(1L)).thenReturn(10)
        assertThrows<BadRequestException> {
            service.createForPlant(
                CreateSaleLotForPlantRequest(plantId = 1L, unitKind = UnitKind.PLUG, quantityTotal = 11, initialRequestedPriceCents = 1000, currentOutletId = outletId),
                orgId, userId,
            )
        }
    }

    @Test
    fun `createForPlant rejects STEM and BUNCH unit kinds`() {
        whenever(plantRepo.findById(1L)).thenReturn(makePlant(1L))
        whenever(lotRepo.availableForPlant(1L)).thenReturn(50)
        for (bad in listOf(UnitKind.STEM, UnitKind.BUNCH, UnitKind.BOUQUET)) {
            assertThrows<BadRequestException> {
                service.createForPlant(
                    CreateSaleLotForPlantRequest(plantId = 1L, unitKind = bad, quantityTotal = 5, initialRequestedPriceCents = 1000, currentOutletId = outletId),
                    orgId, userId,
                )
            }
        }
    }

    @Test
    fun `createForPlant happy path persists and writes CREATED audit`() {
        whenever(plantRepo.findById(1L)).thenReturn(makePlant(1L))
        whenever(lotRepo.availableForPlant(1L)).thenReturn(50)
        whenever(lotRepo.persist(any())).thenAnswer { (it.arguments[0] as SaleLot).copy(id = 99L) }

        service.createForPlant(
            CreateSaleLotForPlantRequest(plantId = 1L, unitKind = UnitKind.PLUG, quantityTotal = 10, initialRequestedPriceCents = 1500, currentOutletId = outletId),
            orgId, userId,
        )

        verify(lotRepo).persist(check { lot ->
            assertEquals(SourceKind.PLANT, lot.sourceKind)
            assertEquals(10, lot.quantityTotal)
            assertEquals(10, lot.quantityRemaining)
            assertEquals(SaleLotStatus.OFFERED, lot.status)
        })
        verify(eventRepo).persist(check { ev ->
            assertEquals(99L, ev.saleLotId)
            assertEquals(SaleLotEventType.CREATED, ev.eventType)
        })
    }

    // ── createForHarvestEvent ──

    @Test
    fun `createForHarvestEvent BUNCH requires stemsPerUnit`() {
        whenever(plantEventRepo.findById(200L)).thenReturn(makeHarvestEvent(200L, 1L))
        whenever(plantRepo.findById(1L)).thenReturn(makePlant(1L))
        assertThrows<BadRequestException> {
            service.createForHarvestEvent(
                CreateSaleLotForHarvestRequest(harvestEventId = 200L, unitKind = UnitKind.BUNCH, stemsPerUnit = null, quantityTotal = 5, initialRequestedPriceCents = 5000, currentOutletId = outletId),
                orgId, userId,
            )
        }
    }

    @Test
    fun `createForHarvestEvent STEM rejects stemsPerUnit`() {
        whenever(plantEventRepo.findById(200L)).thenReturn(makeHarvestEvent(200L, 1L))
        whenever(plantRepo.findById(1L)).thenReturn(makePlant(1L))
        assertThrows<BadRequestException> {
            service.createForHarvestEvent(
                CreateSaleLotForHarvestRequest(harvestEventId = 200L, unitKind = UnitKind.STEM, stemsPerUnit = 5, quantityTotal = 5, initialRequestedPriceCents = 5000, currentOutletId = outletId),
                orgId, userId,
            )
        }
    }

    @Test
    fun `createForHarvestEvent BUNCH inventory math multiplies by stemsPerUnit`() {
        // 100 stems available; 8 bunches × 10 stems = 80 stems requested → fits.
        whenever(plantEventRepo.findById(200L)).thenReturn(makeHarvestEvent(200L, 1L, stems = 100))
        whenever(plantRepo.findById(1L)).thenReturn(makePlant(1L))
        whenever(lotRepo.availableForHarvestEvent(200L)).thenReturn(100)
        whenever(lotRepo.persist(any())).thenAnswer { (it.arguments[0] as SaleLot).copy(id = 50L) }

        service.createForHarvestEvent(
            CreateSaleLotForHarvestRequest(harvestEventId = 200L, unitKind = UnitKind.BUNCH, stemsPerUnit = 10, quantityTotal = 8, initialRequestedPriceCents = 5000, currentOutletId = outletId),
            orgId, userId,
        )

        verify(lotRepo).persist(check { lot ->
            assertEquals(UnitKind.BUNCH, lot.unitKind)
            assertEquals(10, lot.stemsPerUnit)
            assertEquals(8, lot.quantityTotal)
        })
    }

    @Test
    fun `createForHarvestEvent rejects when bunched stems exceed available`() {
        // 50 stems; 8 bunches × 10 = 80 stems → over.
        whenever(plantEventRepo.findById(200L)).thenReturn(makeHarvestEvent(200L, 1L, stems = 50))
        whenever(plantRepo.findById(1L)).thenReturn(makePlant(1L))
        whenever(lotRepo.availableForHarvestEvent(200L)).thenReturn(50)

        assertThrows<BadRequestException> {
            service.createForHarvestEvent(
                CreateSaleLotForHarvestRequest(harvestEventId = 200L, unitKind = UnitKind.BUNCH, stemsPerUnit = 10, quantityTotal = 8, initialRequestedPriceCents = 5000, currentOutletId = outletId),
                orgId, userId,
            )
        }
    }

    // ── createForBouquet ──

    @Test
    fun `createForBouquet rejects when bouquet already has active lot`() {
        whenever(bouquetRepo.findById(300L)).thenReturn(makeBouquet(300L))
        whenever(lotRepo.availableForBouquet(300L)).thenReturn(0)
        assertThrows<BadRequestException> {
            service.createForBouquet(bouquetId = 300L, requestedPriceCents = 15000, outletId = outletId, orgId = orgId, userId = userId)
        }
    }

    // ── recordSale ──

    @Test
    fun `recordSale decrements remaining and stays OFFERED when remaining greater than zero`() {
        val lot = makeLot(id = 1L, total = 20, remaining = 20)
        whenever(lotRepo.findById(1L)).thenReturn(lot)
        whenever(saleRepo.persist(any())).thenAnswer { (it.arguments[0] as Sale).copy(id = 500L) }

        service.recordSale(
            lotId = 1L,
            request = RecordSaleRequest(quantity = 12, pricePerUnitCents = 2500),
            orgId = orgId, userId = userId,
        )

        verify(lotRepo).update(check { updated ->
            assertEquals(8, updated.quantityRemaining)
            assertEquals(SaleLotStatus.OFFERED, updated.status)
        })
        // SALE_RECORDED only — no AUTO_SOLD_OUT.
        verify(eventRepo).persist(check { ev -> assertEquals(SaleLotEventType.SALE_RECORDED, ev.eventType) })
    }

    @Test
    fun `recordSale flips to SOLD_OUT and writes AUTO_SOLD_OUT audit when remaining hits zero`() {
        val lot = makeLot(id = 1L, total = 20, remaining = 5)
        whenever(lotRepo.findById(1L)).thenReturn(lot)
        whenever(saleRepo.persist(any())).thenAnswer { (it.arguments[0] as Sale).copy(id = 501L) }

        service.recordSale(
            lotId = 1L,
            request = RecordSaleRequest(quantity = 5, pricePerUnitCents = 1500),
            orgId = orgId, userId = userId,
        )

        verify(lotRepo).update(check { updated ->
            assertEquals(0, updated.quantityRemaining)
            assertEquals(SaleLotStatus.SOLD_OUT, updated.status)
        })
        val captor = argumentCaptor<SaleLotEvent>()
        verify(eventRepo, times(2)).persist(captor.capture())
        val types = captor.allValues.map { it.eventType }
        assert(types.contains(SaleLotEventType.SALE_RECORDED))
        assert(types.contains(SaleLotEventType.AUTO_SOLD_OUT))
    }

    @Test
    fun `recordSale rejects when quantity exceeds remaining`() {
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L, total = 20, remaining = 5))
        assertThrows<BadRequestException> {
            service.recordSale(1L, RecordSaleRequest(quantity = 6, pricePerUnitCents = 1000), orgId, userId)
        }
    }

    @Test
    fun `recordSale rejects on non-OFFERED lot`() {
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L, status = SaleLotStatus.SOLD_OUT, remaining = 0))
        assertThrows<BadRequestException> {
            service.recordSale(1L, RecordSaleRequest(quantity = 1, pricePerUnitCents = 1000), orgId, userId)
        }
    }

    // ── editSale ──

    @Test
    fun `editSale revives SOLD_OUT to OFFERED on downward edit`() {
        // Lot was SOLD_OUT (12 sold of 12); user edits the sale down to 10. New remaining = 2 → OFFERED.
        val lot = makeLot(id = 1L, total = 12, remaining = 0, status = SaleLotStatus.SOLD_OUT)
        val sale = Sale(id = 700L, saleLotId = 1L, quantity = 12, pricePerUnitCents = 2500, outletId = outletId, recordedByUserId = userId, soldAt = LocalDate.now())
        whenever(saleRepo.findById(700L)).thenReturn(sale)
        whenever(lotRepo.findById(1L)).thenReturn(lot)
        whenever(saleRepo.sumQuantityForLot(1L)).thenReturn(12) // total before edit

        service.editSale(700L, EditSaleRequest(quantity = 10), orgId, userId)

        verify(lotRepo).update(check { updated ->
            assertEquals(2, updated.quantityRemaining)
            assertEquals(SaleLotStatus.OFFERED, updated.status)
        })
        verify(eventRepo).persist(check { ev -> assertEquals(SaleLotEventType.SALE_EDITED, ev.eventType) })
    }

    @Test
    fun `editSale rejects when total would exceed lot capacity`() {
        val lot = makeLot(id = 1L, total = 12, remaining = 5)
        val sale = Sale(id = 700L, saleLotId = 1L, quantity = 7, pricePerUnitCents = 2500, outletId = outletId, recordedByUserId = userId, soldAt = LocalDate.now())
        whenever(saleRepo.findById(700L)).thenReturn(sale)
        whenever(lotRepo.findById(1L)).thenReturn(lot)
        whenever(saleRepo.sumQuantityForLot(1L)).thenReturn(7)

        // Bumping qty from 7 to 13 makes total = 13 > capacity 12.
        assertThrows<BadRequestException> {
            service.editSale(700L, EditSaleRequest(quantity = 13), orgId, userId)
        }
    }

    // ── changePrice / changeOutlet ──

    @Test
    fun `changePrice updates current and writes PRICE_CHANGED audit on OFFERED lot`() {
        val lot = makeLot(id = 1L, currentRequestedPriceCents = 2500, initialRequestedPriceCents = 2500)
        whenever(lotRepo.findById(1L)).thenReturn(lot)

        service.changePrice(1L, ChangePriceRequest(newPriceCents = 1500), orgId, userId)

        verify(lotRepo).update(check { updated ->
            assertEquals(1500, updated.currentRequestedPriceCents)
            assertEquals(2500, updated.initialRequestedPriceCents) // unchanged
        })
        verify(eventRepo).persist(check { ev -> assertEquals(SaleLotEventType.PRICE_CHANGED, ev.eventType) })
    }

    @Test
    fun `changePrice rejects when lot is not OFFERED`() {
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L, status = SaleLotStatus.NOT_SOLD))
        assertThrows<BadRequestException> {
            service.changePrice(1L, ChangePriceRequest(newPriceCents = 1500), orgId, userId)
        }
    }

    @Test
    fun `changeOutlet updates current outlet and writes OUTLET_CHANGED audit`() {
        val newOutletId = 999L
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L, currentOutletId = outletId))
        whenever(outletRepo.findById(newOutletId)).thenReturn(makeOutlet(newOutletId))

        service.changeOutlet(1L, ChangeOutletRequest(newOutletId = newOutletId), orgId, userId)

        verify(lotRepo).update(check { updated -> assertEquals(newOutletId, updated.currentOutletId) })
        verify(eventRepo).persist(check { ev -> assertEquals(SaleLotEventType.OUTLET_CHANGED, ev.eventType) })
    }

    // ── returns / NOT_SOLD / delete ──

    @Test
    fun `markReturnedFromOutlet writes audit and does not change quantity or status`() {
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L, total = 20, remaining = 8))

        service.markReturnedFromOutlet(1L, ReturnFromOutletRequest(fromOutletId = outletId), orgId, userId)

        verify(lotRepo, never()).update(any())
        verify(eventRepo).persist(check { ev -> assertEquals(SaleLotEventType.RETURNED_FROM_OUTLET, ev.eventType) })
    }

    @Test
    fun `markNotSold rejects when lot has no remaining quantity`() {
        // Even an OFFERED lot at remaining=0 (transient before SOLD_OUT auto-flip) must not be NOT_SOLD.
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L, total = 20, remaining = 0))
        assertThrows<BadRequestException> {
            service.markNotSold(1L, orgId, userId)
        }
    }

    @Test
    fun `markNotSold rejects on already-terminal lot`() {
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L, status = SaleLotStatus.SOLD_OUT))
        assertThrows<BadRequestException> {
            service.markNotSold(1L, orgId, userId)
        }
    }

    @Test
    fun `delete blocked when sales exist on the lot`() {
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L))
        whenever(saleRepo.sumQuantityForLot(1L)).thenReturn(3)
        assertThrows<BadRequestException> { service.delete(1L, orgId) }
        verify(lotRepo, never()).delete(any())
    }

    @Test
    fun `delete blocked on non-OFFERED lot`() {
        whenever(lotRepo.findById(1L)).thenReturn(makeLot(id = 1L, status = SaleLotStatus.NOT_SOLD))
        assertThrows<BadRequestException> { service.delete(1L, orgId) }
        verify(lotRepo, never()).delete(any())
    }
}
