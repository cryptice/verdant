package app.verdant.service

import app.verdant.dto.BulkLocationActionResponse
import app.verdant.dto.BulkLocationNoteRequest
import app.verdant.dto.CreateTrayLocationRequest
import app.verdant.dto.MoveTrayLocationRequest
import app.verdant.dto.TrayLocationResponse
import app.verdant.dto.UpdateTrayLocationRequest
import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import app.verdant.entity.TrayLocation
import app.verdant.repository.PlantEventRepository
import app.verdant.repository.PlantRepository
import app.verdant.repository.TrayLocationRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class TrayLocationService(
    private val repo: TrayLocationRepository,
    private val plantRepository: PlantRepository,
    private val plantEventRepository: PlantEventRepository,
) {

    fun list(orgId: Long): List<TrayLocationResponse> =
        repo.findByOrgId(orgId).map { it.toResponse() }

    @Transactional
    fun create(request: CreateTrayLocationRequest, orgId: Long): TrayLocationResponse =
        repo.persist(TrayLocation(orgId = orgId, name = request.name.trim())).toResponse()

    @Transactional
    fun update(id: Long, request: UpdateTrayLocationRequest, orgId: Long): TrayLocationResponse {
        val loc = repo.findById(id) ?: throw NotFoundException("Tray location not found")
        if (loc.orgId != orgId) throw NotFoundException("Tray location not found")
        val updated = loc.copy(
            name = request.name?.trim()?.takeIf { it.isNotBlank() } ?: loc.name,
        )
        repo.update(updated)
        return updated.toResponse()
    }

    /** Emits MOVED audit events (to_tray_location_id = NULL) for every active plant
     *  before removing the location. The FK is ON DELETE SET NULL so the plants stay. */
    @Transactional
    fun delete(id: Long, orgId: Long): BulkLocationActionResponse {
        val loc = repo.findById(id) ?: throw NotFoundException("Tray location not found")
        if (loc.orgId != orgId) throw NotFoundException("Tray location not found")
        val plants = plantRepository.findActiveByTrayLocation(orgId, id)
        val today = java.time.LocalDate.now()
        plants.forEach { plant ->
            plantEventRepository.persist(
                PlantEvent(
                    plantId = plant.id!!,
                    eventType = PlantEventType.MOVED,
                    eventDate = today,
                    plantCount = 1,
                    fromTrayLocationId = id,
                    toTrayLocationId = null,
                )
            )
        }
        repo.delete(id)
        return BulkLocationActionResponse(plantsAffected = plants.size)
    }

    @Transactional
    fun move(locationId: Long, orgId: Long, request: MoveTrayLocationRequest): BulkLocationActionResponse {
        val source = repo.findById(locationId) ?: throw NotFoundException("Source location not found")
        if (source.orgId != orgId) throw NotFoundException("Source location not found")
        request.targetLocationId?.let { tid ->
            val target = repo.findById(tid) ?: throw NotFoundException("Target location not found")
            if (target.orgId != orgId) throw NotFoundException("Target location not found")
        }
        val plants = plantRepository.findActiveByTrayLocationFiltered(
            orgId = orgId,
            locationId = locationId,
            speciesId = request.speciesId,
            status = request.status,
            limit = if (request.count < 0) Int.MAX_VALUE else request.count,
        )
        val today = java.time.LocalDate.now()
        plants.forEach { plant ->
            plantRepository.update(plant.copy(trayLocationId = request.targetLocationId))
            plantEventRepository.persist(
                PlantEvent(
                    plantId = plant.id!!,
                    eventType = PlantEventType.MOVED,
                    eventDate = today,
                    plantCount = 1,
                    fromTrayLocationId = locationId,
                    toTrayLocationId = request.targetLocationId,
                )
            )
        }
        return BulkLocationActionResponse(plantsAffected = plants.size)
    }

    @Transactional
    fun water(locationId: Long, orgId: Long): BulkLocationActionResponse =
        bulkEvent(locationId, orgId, PlantEventType.WATERED, notes = null)

    @Transactional
    fun note(locationId: Long, orgId: Long, request: BulkLocationNoteRequest): BulkLocationActionResponse =
        bulkEvent(locationId, orgId, PlantEventType.NOTE, notes = request.text)

    private fun bulkEvent(
        locationId: Long,
        orgId: Long,
        eventType: PlantEventType,
        notes: String?,
    ): BulkLocationActionResponse {
        val loc = repo.findById(locationId) ?: throw NotFoundException("Tray location not found")
        if (loc.orgId != orgId) throw NotFoundException("Tray location not found")
        val plants = plantRepository.findActiveByTrayLocation(orgId, locationId)
        val today = java.time.LocalDate.now()
        plants.forEach { plant ->
            plantEventRepository.persist(
                PlantEvent(
                    plantId = plant.id!!,
                    eventType = eventType,
                    eventDate = today,
                    plantCount = 1,
                    notes = notes,
                )
            )
        }
        return BulkLocationActionResponse(plantsAffected = plants.size)
    }

    private fun TrayLocation.toResponse() = TrayLocationResponse(
        id = id!!,
        name = name,
        activePlantCount = repo.countActivePlants(id),
        createdAt = createdAt,
    )
}
