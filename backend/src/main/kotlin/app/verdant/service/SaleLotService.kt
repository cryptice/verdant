package app.verdant.service

import app.verdant.dto.*
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
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import java.time.LocalDate

@ApplicationScoped
class SaleLotService(
    private val lotRepo: SaleLotRepository,
    private val saleRepo: SaleRepository,
    private val eventRepo: SaleLotEventRepository,
    private val outletRepo: OutletRepository,
    private val plantRepo: PlantRepository,
    private val plantEventRepo: PlantEventRepository,
    private val bouquetRepo: BouquetRepository,
    private val customerRepo: CustomerRepository,
    private val objectMapper: ObjectMapper,
) {

    // ── Listing ──

    fun list(
        orgId: Long,
        status: SaleLotStatus? = null,
        sourceKind: SourceKind? = null,
        limit: Int = 200,
        offset: Int = 0,
    ): List<SaleLotResponse> {
        val lots = lotRepo.findByOrgId(orgId, status, sourceKind, limit, offset)
        return enrichLots(lots)
    }

    fun getDetail(lotId: Long, orgId: Long): SaleLotDetailResponse {
        val lot = requireLotInOrg(lotId, orgId)
        val sales = saleRepo.findByLotId(lotId)
        val events = eventRepo.findByLotId(lotId)
        val outletNames = outletRepo.findByIds(
            (sales.map { it.outletId } + lot.currentOutletId).toSet()
        ).mapValues { it.value.name }
        val customerNames = customerRepo.findByIds(
            sales.mapNotNull { it.customerId }.toSet()
        ).mapValues { it.value.name }
        return SaleLotDetailResponse(
            lot = lot.toResponse(
                outletName = outletNames[lot.currentOutletId] ?: "(unknown)",
                sourceSummary = sourceSummary(lot),
            ),
            sales = sales.map {
                it.toResponse(
                    outletName = outletNames[it.outletId] ?: "(unknown)",
                    customerName = it.customerId?.let { cid -> customerNames[cid] },
                )
            },
            events = events.map { it.toResponse() },
        )
    }

    // ── Create ──

    @Transactional
    fun createForPlant(request: CreateSaleLotForPlantRequest, orgId: Long, userId: Long): SaleLotResponse {
        val plant = plantRepo.findById(request.plantId) ?: throw NotFoundException("Plant not found")
        if (plant.orgId != orgId) throw NotFoundException("Plant not found")
        if (plant.status == PlantStatus.REMOVED) {
            throw ForbiddenException("Plant is removed; new sale lots cannot be created from it")
        }
        if (request.unitKind == UnitKind.STEM || request.unitKind == UnitKind.BUNCH || request.unitKind == UnitKind.BOUQUET) {
            throw BadRequestException("Unit kind ${request.unitKind} is not valid for plant-source lots")
        }
        validateOutletInOrg(request.currentOutletId, orgId)
        val available = lotRepo.availableForPlant(request.plantId)
        if (request.quantityTotal > available) {
            throw BadRequestException("Quantity $request.quantityTotal exceeds available $available for this plant")
        }
        val lot = lotRepo.persist(
            SaleLot(
                orgId = orgId,
                sourceKind = SourceKind.PLANT,
                plantId = request.plantId,
                unitKind = request.unitKind,
                quantityTotal = request.quantityTotal,
                quantityRemaining = request.quantityTotal,
                initialRequestedPriceCents = request.initialRequestedPriceCents,
                currentRequestedPriceCents = request.initialRequestedPriceCents,
                currentOutletId = request.currentOutletId,
            ),
        )
        recordEvent(lot.id!!, SaleLotEventType.CREATED, payloadFor("sourceKind" to "PLANT", "plantId" to request.plantId), userId)
        return lot.toResponseEnriched()
    }

    @Transactional
    fun createForHarvestEvent(request: CreateSaleLotForHarvestRequest, orgId: Long, userId: Long): SaleLotResponse {
        val event = plantEventRepo.findById(request.harvestEventId) ?: throw NotFoundException("Harvest event not found")
        val plant = plantRepo.findById(event.plantId) ?: throw NotFoundException("Harvest event not found")
        if (plant.orgId != orgId) throw NotFoundException("Harvest event not found")
        if (event.eventType.name != "HARVESTED") {
            throw BadRequestException("Sale lots can only be created from HARVESTED events")
        }
        if (request.unitKind != UnitKind.STEM && request.unitKind != UnitKind.BUNCH) {
            throw BadRequestException("Unit kind ${request.unitKind} is not valid for harvest-event-source lots; use STEM or BUNCH")
        }
        if (request.unitKind == UnitKind.BUNCH && (request.stemsPerUnit == null || request.stemsPerUnit < 1)) {
            throw BadRequestException("BUNCH unit kind requires stemsPerUnit ≥ 1")
        }
        if (request.unitKind == UnitKind.STEM && request.stemsPerUnit != null) {
            throw BadRequestException("STEM unit kind must not provide stemsPerUnit")
        }
        validateOutletInOrg(request.currentOutletId, orgId)
        val multiplier = request.stemsPerUnit ?: 1
        val stemsRequested = request.quantityTotal * multiplier
        val available = lotRepo.availableForHarvestEvent(request.harvestEventId)
        if (stemsRequested > available) {
            throw BadRequestException("Requested $stemsRequested stems exceed available $available for this harvest event")
        }
        val lot = lotRepo.persist(
            SaleLot(
                orgId = orgId,
                sourceKind = SourceKind.HARVEST_EVENT,
                harvestEventId = request.harvestEventId,
                unitKind = request.unitKind,
                stemsPerUnit = request.stemsPerUnit,
                quantityTotal = request.quantityTotal,
                quantityRemaining = request.quantityTotal,
                initialRequestedPriceCents = request.initialRequestedPriceCents,
                currentRequestedPriceCents = request.initialRequestedPriceCents,
                currentOutletId = request.currentOutletId,
            ),
        )
        recordEvent(lot.id!!, SaleLotEventType.CREATED, payloadFor("sourceKind" to "HARVEST_EVENT", "harvestEventId" to request.harvestEventId), userId)
        return lot.toResponseEnriched()
    }

    /**
     * Internal: called by [BouquetService.createBouquet] inside the same transaction.
     * Bouquet lots are always quantity 1, unit BOUQUET, and there can be at most
     * one non-NOT_SOLD lot per bouquet.
     */
    @Transactional
    fun createForBouquet(bouquetId: Long, requestedPriceCents: Int, outletId: Long, orgId: Long, userId: Long): SaleLot {
        val bouquet = bouquetRepo.findById(bouquetId) ?: throw NotFoundException("Bouquet not found")
        if (bouquet.orgId != orgId) throw NotFoundException("Bouquet not found")
        validateOutletInOrg(outletId, orgId)
        if (lotRepo.availableForBouquet(bouquetId) < 1) {
            throw BadRequestException("Bouquet already has an active sale lot")
        }
        val lot = lotRepo.persist(
            SaleLot(
                orgId = orgId,
                sourceKind = SourceKind.BOUQUET,
                bouquetId = bouquetId,
                unitKind = UnitKind.BOUQUET,
                quantityTotal = 1,
                quantityRemaining = 1,
                initialRequestedPriceCents = requestedPriceCents,
                currentRequestedPriceCents = requestedPriceCents,
                currentOutletId = outletId,
            ),
        )
        recordEvent(lot.id!!, SaleLotEventType.CREATED, payloadFor("sourceKind" to "BOUQUET", "bouquetId" to bouquetId), userId)
        return lot
    }

    // ── Sale flow ──

    @Transactional
    fun recordSale(lotId: Long, request: RecordSaleRequest, orgId: Long, userId: Long): SaleResponse {
        val lot = requireLotInOrg(lotId, orgId)
        if (lot.status != SaleLotStatus.OFFERED) {
            throw BadRequestException("Cannot record sale on a lot with status ${lot.status}")
        }
        if (request.quantity > lot.quantityRemaining) {
            throw BadRequestException("Sale quantity ${request.quantity} exceeds remaining ${lot.quantityRemaining}")
        }
        request.customerId?.let { validateCustomerInOrg(it, orgId) }

        val sale = saleRepo.persist(
            Sale(
                saleLotId = lotId,
                quantity = request.quantity,
                pricePerUnitCents = request.pricePerUnitCents,
                outletId = lot.currentOutletId,
                customerId = request.customerId,
                soldAt = request.soldAt ?: LocalDate.now(),
                recordedByUserId = userId,
                notes = request.notes,
            ),
        )
        val newRemaining = lot.quantityRemaining - request.quantity
        val newStatus = if (newRemaining == 0) SaleLotStatus.SOLD_OUT else lot.status
        lotRepo.update(lot.copy(quantityRemaining = newRemaining, status = newStatus))

        recordEvent(lotId, SaleLotEventType.SALE_RECORDED, payloadFor(
            "saleId" to sale.id,
            "quantity" to sale.quantity,
            "pricePerUnitCents" to sale.pricePerUnitCents,
        ), userId)
        if (newStatus == SaleLotStatus.SOLD_OUT) {
            recordEvent(lotId, SaleLotEventType.AUTO_SOLD_OUT, null, userId)
        }
        val outletName = outletRepo.findById(sale.outletId)?.name ?: "(unknown)"
        val customerName = sale.customerId?.let { customerRepo.findById(it)?.name }
        return sale.toResponse(outletName, customerName)
    }

    @Transactional
    fun editSale(saleId: Long, request: EditSaleRequest, orgId: Long, userId: Long): SaleResponse {
        val sale = saleRepo.findById(saleId) ?: throw NotFoundException("Sale not found")
        val lot = requireLotInOrg(sale.saleLotId, orgId)
        request.customerId?.let { validateCustomerInOrg(it, orgId) }

        val before = mapOf(
            "quantity" to sale.quantity,
            "pricePerUnitCents" to sale.pricePerUnitCents,
            "customerId" to sale.customerId,
            "soldAt" to sale.soldAt.toString(),
            "notes" to sale.notes,
        )
        val updated = sale.copy(
            quantity = request.quantity ?: sale.quantity,
            pricePerUnitCents = request.pricePerUnitCents ?: sale.pricePerUnitCents,
            customerId = request.customerId ?: sale.customerId,
            soldAt = request.soldAt ?: sale.soldAt,
            notes = request.notes ?: sale.notes,
        )
        // Validate edited quantity doesn't push other sales over capacity. Compute
        // new total = sum of all sale qtys with this sale's qty replaced.
        val totalIfApplied = saleRepo.sumQuantityForLot(sale.saleLotId) - sale.quantity + updated.quantity
        if (totalIfApplied > lot.quantityTotal) {
            throw BadRequestException("Edited quantity would exceed lot total ${lot.quantityTotal}")
        }
        saleRepo.update(updated)

        // Recompute remaining + status based on new sum.
        val newRemaining = lot.quantityTotal - totalIfApplied
        val newStatus = when {
            newRemaining == 0 && lot.status != SaleLotStatus.NOT_SOLD -> SaleLotStatus.SOLD_OUT
            newRemaining > 0 && lot.status == SaleLotStatus.SOLD_OUT -> SaleLotStatus.OFFERED
            else -> lot.status
        }
        lotRepo.update(lot.copy(quantityRemaining = newRemaining, status = newStatus))

        val after = mapOf(
            "quantity" to updated.quantity,
            "pricePerUnitCents" to updated.pricePerUnitCents,
            "customerId" to updated.customerId,
            "soldAt" to updated.soldAt.toString(),
            "notes" to updated.notes,
        )
        recordEvent(lot.id!!, SaleLotEventType.SALE_EDITED, payloadFor(
            "saleId" to saleId,
            "before" to before,
            "after" to after,
        ), userId)
        if (newStatus != lot.status && newStatus == SaleLotStatus.SOLD_OUT) {
            recordEvent(lot.id, SaleLotEventType.AUTO_SOLD_OUT, null, userId)
        }
        val outletName = outletRepo.findById(updated.outletId)?.name ?: "(unknown)"
        val customerName = updated.customerId?.let { customerRepo.findById(it)?.name }
        return updated.toResponse(outletName, customerName)
    }

    // ── Lot mutations ──

    @Transactional
    fun changePrice(lotId: Long, request: ChangePriceRequest, orgId: Long, userId: Long): SaleLotResponse {
        val lot = requireLotInOrg(lotId, orgId)
        if (lot.status != SaleLotStatus.OFFERED) {
            throw BadRequestException("Cannot change price on a lot with status ${lot.status}")
        }
        val before = lot.currentRequestedPriceCents
        lotRepo.update(lot.copy(currentRequestedPriceCents = request.newPriceCents))
        recordEvent(lotId, SaleLotEventType.PRICE_CHANGED, payloadFor("from" to before, "to" to request.newPriceCents), userId)
        return getDetail(lotId, orgId).lot
    }

    @Transactional
    fun changeOutlet(lotId: Long, request: ChangeOutletRequest, orgId: Long, userId: Long): SaleLotResponse {
        val lot = requireLotInOrg(lotId, orgId)
        if (lot.status != SaleLotStatus.OFFERED) {
            throw BadRequestException("Cannot change outlet on a lot with status ${lot.status}")
        }
        validateOutletInOrg(request.newOutletId, orgId)
        val before = lot.currentOutletId
        lotRepo.update(lot.copy(currentOutletId = request.newOutletId))
        recordEvent(lotId, SaleLotEventType.OUTLET_CHANGED, payloadFor("from" to before, "to" to request.newOutletId), userId)
        return getDetail(lotId, orgId).lot
    }

    @Transactional
    fun markReturnedFromOutlet(lotId: Long, request: ReturnFromOutletRequest, orgId: Long, userId: Long) {
        requireLotInOrg(lotId, orgId)
        // Audit-only — no quantity_remaining or status changes.
        recordEvent(lotId, SaleLotEventType.RETURNED_FROM_OUTLET, payloadFor("fromOutletId" to request.fromOutletId), userId)
    }

    @Transactional
    fun markNotSold(lotId: Long, orgId: Long, userId: Long): SaleLotResponse {
        val lot = requireLotInOrg(lotId, orgId)
        if (lot.status != SaleLotStatus.OFFERED) {
            throw BadRequestException("Only OFFERED lots can be marked NOT_SOLD")
        }
        if (lot.quantityRemaining == 0) {
            throw BadRequestException("Lot has no remaining quantity to mark NOT_SOLD")
        }
        lotRepo.update(lot.copy(status = SaleLotStatus.NOT_SOLD))
        recordEvent(lotId, SaleLotEventType.MARKED_NOT_SOLD, null, userId)
        return getDetail(lotId, orgId).lot
    }

    @Transactional
    fun delete(lotId: Long, orgId: Long) {
        val lot = requireLotInOrg(lotId, orgId)
        if (lot.status != SaleLotStatus.OFFERED) {
            throw BadRequestException("Only OFFERED lots with no sales can be deleted")
        }
        if (saleRepo.sumQuantityForLot(lotId) > 0) {
            throw BadRequestException("Lot has recorded sales and cannot be deleted; mark NOT_SOLD instead")
        }
        // Audit rows cascade via FK ON DELETE CASCADE on sale_lot_event.sale_lot_id.
        lotRepo.delete(lotId)
    }

    // ── Helpers ──

    private fun requireLotInOrg(lotId: Long, orgId: Long): SaleLot {
        val lot = lotRepo.findById(lotId) ?: throw NotFoundException("Sale lot not found")
        if (lot.orgId != orgId) throw NotFoundException("Sale lot not found")
        return lot
    }

    private fun validateOutletInOrg(outletId: Long, orgId: Long) {
        val outlet = outletRepo.findById(outletId) ?: throw NotFoundException("Outlet not found")
        if (outlet.orgId != orgId) throw NotFoundException("Outlet not found")
    }

    private fun validateCustomerInOrg(customerId: Long, orgId: Long) {
        val customer = customerRepo.findById(customerId) ?: throw NotFoundException("Customer not found")
        if (customer.orgId != orgId) throw NotFoundException("Customer not found")
    }

    private fun recordEvent(lotId: Long, type: SaleLotEventType, payload: String?, userId: Long) {
        eventRepo.persist(SaleLotEvent(saleLotId = lotId, eventType = type, payloadJson = payload, recordedByUserId = userId))
    }

    private fun payloadFor(vararg pairs: Pair<String, Any?>): String =
        objectMapper.writeValueAsString(mapOf(*pairs))

    private fun enrichLots(lots: List<SaleLot>): List<SaleLotResponse> {
        if (lots.isEmpty()) return emptyList()
        val outletIds = lots.map { it.currentOutletId }.toSet()
        val outletNames = outletRepo.findByIds(outletIds).mapValues { it.value.name }
        val plantIds = lots.mapNotNull { it.plantId }.toSet()
        val plantNames = if (plantIds.isEmpty()) emptyMap() else
            plantRepo.findByIds(plantIds.toList()).associate { it.id!! to it.name }
        val bouquetIds = lots.mapNotNull { it.bouquetId }.toSet()
        val bouquetNames = if (bouquetIds.isEmpty()) emptyMap() else
            bouquetIds.mapNotNull { id -> bouquetRepo.findById(id)?.let { id to it.name } }.toMap()
        val harvestSummaries = lots.mapNotNull { it.harvestEventId }.toSet().mapNotNull { id ->
            plantEventRepo.findById(id)?.let { e ->
                id to "${e.stemCount ?: 0} stems on ${e.eventDate}"
            }
        }.toMap()
        return lots.map { lot ->
            val summary = when (lot.sourceKind) {
                SourceKind.PLANT -> plantNames[lot.plantId]
                SourceKind.HARVEST_EVENT -> harvestSummaries[lot.harvestEventId]
                SourceKind.BOUQUET -> bouquetNames[lot.bouquetId]
            }
            lot.toResponse(
                outletName = outletNames[lot.currentOutletId] ?: "(unknown)",
                sourceSummary = summary,
            )
        }
    }

    private fun sourceSummary(lot: SaleLot): String? = when (lot.sourceKind) {
        SourceKind.PLANT -> lot.plantId?.let { plantRepo.findById(it)?.name }
        SourceKind.HARVEST_EVENT -> lot.harvestEventId?.let { id ->
            plantEventRepo.findById(id)?.let { "${it.stemCount ?: 0} stems on ${it.eventDate}" }
        }
        SourceKind.BOUQUET -> lot.bouquetId?.let { bouquetRepo.findById(it)?.name }
    }

    private fun SaleLot.toResponse(outletName: String, sourceSummary: String?) = SaleLotResponse(
        id = id!!,
        sourceKind = sourceKind,
        plantId = plantId,
        harvestEventId = harvestEventId,
        bouquetId = bouquetId,
        sourceSummary = sourceSummary,
        unitKind = unitKind,
        stemsPerUnit = stemsPerUnit,
        quantityTotal = quantityTotal,
        quantityRemaining = quantityRemaining,
        initialRequestedPriceCents = initialRequestedPriceCents,
        currentRequestedPriceCents = currentRequestedPriceCents,
        currentOutletId = currentOutletId,
        currentOutletName = outletName,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    /** Used by single-lot creation paths after persist. */
    private fun SaleLot.toResponseEnriched(): SaleLotResponse {
        val outletName = outletRepo.findById(currentOutletId)?.name ?: "(unknown)"
        return toResponse(outletName, sourceSummary(this))
    }

    private fun Sale.toResponse(outletName: String, customerName: String?) = SaleResponse(
        id = id!!,
        saleLotId = saleLotId,
        quantity = quantity,
        pricePerUnitCents = pricePerUnitCents,
        outletId = outletId,
        outletName = outletName,
        customerId = customerId,
        customerName = customerName,
        soldAt = soldAt,
        recordedByUserId = recordedByUserId,
        notes = notes,
        createdAt = createdAt,
    )

    private fun SaleLotEvent.toResponse() = SaleLotEventResponse(
        id = id!!,
        eventType = eventType,
        payloadJson = payloadJson,
        recordedByUserId = recordedByUserId,
        createdAt = createdAt,
    )
}
