package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.*
import app.verdant.repository.GardenAreaEventRepository
import app.verdant.repository.GardenAreaPhotoRepository
import app.verdant.repository.GardenAreaRepository
import app.verdant.repository.GardenRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.time.Instant
import java.time.LocalDate

@ApplicationScoped
class GardenAreaService(
    private val areaRepository: GardenAreaRepository,
    private val eventRepository: GardenAreaEventRepository,
    private val photoRepository: GardenAreaPhotoRepository,
    private val gardenRepository: GardenRepository,
    private val storageService: StorageService,
) {
    /** Loads an area, or 404s if it does not exist or belongs to another org. */
    fun requireArea(areaId: Long, orgId: Long): GardenArea {
        val area = areaRepository.findById(areaId) ?: throw NotFoundException("Area not found")
        val garden = gardenRepository.findById(area.gardenId) ?: throw NotFoundException("Area not found")
        if (garden.orgId != orgId) throw NotFoundException("Area not found")
        return area
    }

    private fun requireGarden(gardenId: Long, orgId: Long): Garden {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Garden not found")
        return garden
    }

    private fun parseCategory(value: String): GardenAreaCategory =
        runCatching { GardenAreaCategory.valueOf(value) }
            .getOrElse { throw BadRequestException("Unknown area category: $value") }

    fun getAreasForGarden(gardenId: Long, orgId: Long): List<GardenAreaResponse> {
        val garden = requireGarden(gardenId, orgId)
        return areaRepository.findByGardenId(gardenId).map { it.toResponse(garden.name) }
    }

    fun getArea(areaId: Long, orgId: Long): GardenAreaResponse {
        val area = requireArea(areaId, orgId)
        return area.toResponse(gardenRepository.findById(area.gardenId)?.name)
    }

    fun createArea(gardenId: Long, request: CreateGardenAreaRequest, orgId: Long): GardenAreaResponse {
        val garden = requireGarden(gardenId, orgId)
        val category = parseCategory(request.category)
        val saved = areaRepository.persist(
            GardenArea(
                gardenId = gardenId,
                name = request.name,
                description = request.description,
                category = category,
                boundaryJson = request.boundaryJson,
                sizeSqm = request.sizeSqm,
            )
        )
        return saved.toResponse(garden.name)
    }

    fun updateArea(areaId: Long, request: UpdateGardenAreaRequest, orgId: Long): GardenAreaResponse {
        val area = requireArea(areaId, orgId)
        val updated = area.copy(
            name = request.name ?: area.name,
            description = request.description ?: area.description,
            category = request.category?.let { parseCategory(it) } ?: area.category,
            boundaryJson = request.boundaryJson ?: area.boundaryJson,
            sizeSqm = request.sizeSqm ?: area.sizeSqm,
        )
        areaRepository.update(updated)
        return updated.toResponse(gardenRepository.findById(area.gardenId)?.name)
    }

    fun deleteArea(areaId: Long, orgId: Long) {
        requireArea(areaId, orgId)
        areaRepository.delete(areaId)
    }

    fun listEvents(areaId: Long, orgId: Long, limit: Int = 50): List<GardenAreaEventResponse> {
        requireArea(areaId, orgId)
        return eventRepository.findByAreaId(areaId, limit).map { it.toResponse() }
    }

    fun logEvent(areaId: Long, request: CreateGardenAreaEventRequest, orgId: Long): GardenAreaEventResponse {
        requireArea(areaId, orgId)
        val eventType = validateAreaEventType(request.activityType)
        val saved = eventRepository.persist(
            GardenAreaEvent(
                gardenAreaId = areaId,
                eventType = eventType,
                eventDate = request.eventDate ?: LocalDate.now(),
                notes = request.notes,
            )
        )
        return saved.toResponse()
    }

    fun listPhotos(areaId: Long, orgId: Long): List<GardenAreaPhotoResponse> {
        requireArea(areaId, orgId)
        return photoRepository.findByAreaId(areaId).map { it.toResponse() }
    }

    fun addPhoto(areaId: Long, request: CreateGardenAreaPhotoRequest, orgId: Long): GardenAreaPhotoResponse {
        requireArea(areaId, orgId)
        val reason = runCatching { BedPhotoReason.valueOf(request.reason) }
            .getOrElse { throw BadRequestException("Unknown photo reason: ${request.reason}") }
        val saved = photoRepository.persist(
            GardenAreaPhoto(
                gardenAreaId = areaId,
                photoUrl = request.photoUrl,
                reason = reason,
                description = request.description,
                capturedAt = request.capturedAt ?: Instant.now(),
            )
        )
        return saved.toResponse()
    }

    fun deletePhoto(areaId: Long, photoId: Long, orgId: Long) {
        requireArea(areaId, orgId)
        val photo = photoRepository.findById(photoId) ?: throw NotFoundException("Photo not found")
        if (photo.gardenAreaId != areaId) throw NotFoundException("Photo not found")
        storageService.deleteByPath(photo.photoUrl)
        photoRepository.delete(photoId)
    }

    companion object {
        /**
         * Area events hold a MaintenanceActivity that applies to areas, or a
         * plain NOTE. A bed-only activity such as FERTILIZE is rejected so the
         * log cannot record work an area can't receive.
         */
        fun validateAreaEventType(value: String): String {
            if (value == AREA_EVENT_NOTE) return AREA_EVENT_NOTE
            val activity = runCatching { MaintenanceActivity.parse(value) }
                .getOrElse { throw BadRequestException("Unknown activity: $value") }
            if (!activity.appliesTo(MaintenanceTarget.GARDEN_AREA)) {
                throw BadRequestException("Activity $value does not apply to garden areas")
            }
            return activity.name
        }
    }
}

private fun GardenArea.toResponse(gardenName: String?) = GardenAreaResponse(
    id = id!!,
    gardenId = gardenId,
    gardenName = gardenName,
    name = name,
    description = description,
    category = category.name,
    boundaryJson = boundaryJson,
    sizeSqm = sizeSqm,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun GardenAreaEvent.toResponse() = GardenAreaEventResponse(
    id = id ?: 0,
    gardenAreaId = gardenAreaId,
    eventType = eventType,
    eventDate = eventDate,
    notes = notes,
    createdAt = createdAt,
)

private fun GardenAreaPhoto.toResponse() = GardenAreaPhotoResponse(
    id = id!!,
    gardenAreaId = gardenAreaId,
    photoUrl = photoUrl,
    reason = reason.name,
    description = description,
    capturedAt = capturedAt,
    createdAt = createdAt,
)
