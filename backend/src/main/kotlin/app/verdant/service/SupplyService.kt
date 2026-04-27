package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.SupplyInventory
import app.verdant.entity.SupplyType
import app.verdant.repository.SupplyInventoryRepository
import app.verdant.repository.SupplyTypeRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class SupplyService(
    private val typeRepository: SupplyTypeRepository,
    private val inventoryRepository: SupplyInventoryRepository,
    private val objectMapper: ObjectMapper,
) {
    // ── Supply Types ──

    fun getTypesForUser(orgId: Long): List<SupplyTypeResponse> =
        typeRepository.findByOrgId(orgId).map { it.toResponse() }

    fun createType(request: CreateSupplyTypeRequest, orgId: Long): SupplyTypeResponse {
        val type = typeRepository.persist(
            SupplyType(
                orgId = orgId,
                name = request.name,
                category = request.category,
                unit = request.unit,
                properties = objectMapper.writeValueAsString(request.properties),
                inexhaustible = request.inexhaustible,
            )
        )
        return type.toResponse()
    }

    fun updateType(typeId: Long, request: UpdateSupplyTypeRequest, orgId: Long): SupplyTypeResponse {
        val type = typeRepository.findById(typeId) ?: throw NotFoundException("Supply type not found")
        if (type.orgId != orgId) throw NotFoundException("Supply type not found")
        val updated = type.copy(
            name = request.name ?: type.name,
            category = request.category ?: type.category,
            unit = request.unit ?: type.unit,
            properties = if (request.properties != null) objectMapper.writeValueAsString(request.properties) else type.properties,
            inexhaustible = request.inexhaustible ?: type.inexhaustible,
        )
        typeRepository.update(updated)
        return updated.toResponse()
    }

    fun deleteType(typeId: Long, orgId: Long) {
        val type = typeRepository.findById(typeId) ?: throw NotFoundException("Supply type not found")
        if (type.orgId != orgId) throw NotFoundException("Supply type not found")
        val batches = inventoryRepository.findBySupplyTypeId(typeId)
        if (batches.isNotEmpty()) throw BadRequestException("Cannot delete supply type with existing inventory batches")
        typeRepository.delete(typeId)
    }

    // ── Supply Inventory ──

    fun getInventoryForUser(orgId: Long): List<SupplyInventoryResponse> {
        val items = inventoryRepository.findByOrgId(orgId)
        return buildInventoryResponses(items)
    }

    fun createInventory(request: CreateSupplyInventoryRequest, orgId: Long): SupplyInventoryResponse {
        val type = typeRepository.findById(request.supplyTypeId) ?: throw NotFoundException("Supply type not found")
        if (type.orgId != orgId) throw NotFoundException("Supply type not found")
        val item = inventoryRepository.persist(
            SupplyInventory(
                orgId = orgId,
                supplyTypeId = request.supplyTypeId,
                quantity = request.quantity,
                costCents = request.costCents,
                seasonId = request.seasonId,
                notes = request.notes,
            )
        )
        return buildInventoryResponses(listOf(item)).first()
    }

    fun updateInventory(itemId: Long, request: UpdateSupplyInventoryRequest, orgId: Long): SupplyInventoryResponse {
        val item = inventoryRepository.findById(itemId) ?: throw NotFoundException("Supply inventory not found")
        if (item.orgId != orgId) throw NotFoundException("Supply inventory not found")
        val updated = item.copy(
            quantity = request.quantity ?: item.quantity,
            costCents = request.costCents ?: item.costCents,
            seasonId = request.seasonId ?: item.seasonId,
            notes = request.notes ?: item.notes,
        )
        inventoryRepository.update(updated)
        return buildInventoryResponses(listOf(updated)).first()
    }

    fun decrementInventory(itemId: Long, quantity: java.math.BigDecimal, orgId: Long) {
        val item = inventoryRepository.findById(itemId) ?: throw NotFoundException("Supply inventory not found")
        if (item.orgId != orgId) throw NotFoundException("Supply inventory not found")
        if (quantity > item.quantity) throw BadRequestException("Insufficient quantity")
        inventoryRepository.decrementQuantity(itemId, quantity)
    }

    fun deleteInventory(itemId: Long, orgId: Long) {
        val item = inventoryRepository.findById(itemId) ?: throw NotFoundException("Supply inventory not found")
        if (item.orgId != orgId) throw NotFoundException("Supply inventory not found")
        inventoryRepository.delete(itemId)
    }

    // ── Mapping ──

    private fun buildInventoryResponses(items: List<SupplyInventory>): List<SupplyInventoryResponse> {
        if (items.isEmpty()) return emptyList()
        val typeIds = items.map { it.supplyTypeId }.toSet()
        val types = typeIds.associateWith { typeRepository.findById(it) }
        return items.map { item ->
            val type = types[item.supplyTypeId]
            SupplyInventoryResponse(
                id = item.id!!,
                supplyTypeId = item.supplyTypeId,
                supplyTypeName = type?.name ?: "Unknown",
                category = type?.category?.name ?: "OTHER",
                unit = type?.unit?.name ?: "COUNT",
                properties = type?.let { parseProperties(it.properties) } ?: emptyMap(),
                quantity = item.quantity,
                costCents = item.costCents,
                seasonId = item.seasonId,
                notes = item.notes,
                createdAt = item.createdAt,
            )
        }
    }

    private fun SupplyType.toResponse() = SupplyTypeResponse(
        id = id!!,
        name = name,
        category = category.name,
        unit = unit.name,
        properties = parseProperties(properties),
        inexhaustible = inexhaustible,
        createdAt = createdAt,
    )

    private fun parseProperties(json: String): Map<String, Any?> =
        try {
            objectMapper.readValue(json, object : TypeReference<Map<String, Any?>>() {})
        } catch (_: Exception) {
            emptyMap()
        }
}
