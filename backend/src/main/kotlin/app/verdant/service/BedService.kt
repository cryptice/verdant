package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.*
import app.verdant.repository.BedPhotoRepository
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import app.verdant.repository.PlantEventRepository
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import java.time.Instant

@ApplicationScoped
class BedService(
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository,
    private val bedPhotoRepository: BedPhotoRepository,
    private val storageService: StorageService,
    private val plantEvents: PlantEventRepository,
    private val ds: AgroalDataSource,
) {
    fun getAllBedsForUser(orgId: Long): List<BedWithGardenResponse> {
        return bedRepository.findByOrgIdWithGardenName(orgId).map {
            BedWithGardenResponse(
                id = it.bed.id!!,
                name = it.bed.name,
                description = it.bed.description,
                gardenId = it.bed.gardenId,
                gardenName = it.gardenName,
                boundaryJson = it.bed.boundaryJson,
            )
        }
    }

    fun getBedsForGarden(gardenId: Long, orgId: Long): List<BedResponse> {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Garden not found")
        return bedRepository.findByGardenId(gardenId).map { it.toResponse() }
    }

    fun getBed(bedId: Long, orgId: Long): BedResponse {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        return bed.toResponse()
    }

    fun createBed(gardenId: Long, request: CreateBedRequest, orgId: Long): BedResponse {
        val garden = gardenRepository.findById(gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Garden not found")
        val bed = bedRepository.persist(
            Bed(
                name = request.name,
                description = request.description,
                gardenId = gardenId,
                boundaryJson = request.boundaryJson,
                lengthMeters = request.lengthMeters,
                widthMeters = request.widthMeters,
                soilType = request.soilType?.let { SoilType.valueOf(it) },
                soilPh = request.soilPh,
                sunExposure = request.sunExposure?.let { SunExposure.valueOf(it) },
                drainage = request.drainage?.let { Drainage.valueOf(it) },
                sunDirections = request.sunDirections?.map { CompassDirection.valueOf(it) } ?: emptyList(),
                irrigationType = request.irrigationType?.let { IrrigationType.valueOf(it) },
                protection = request.protection?.let { Protection.valueOf(it) },
                raisedBed = request.raisedBed,
            )
        )
        return bed.toResponse()
    }

    fun updateBed(bedId: Long, request: UpdateBedRequest, orgId: Long): BedResponse {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        // A clear* flag and a replacement value for the same field are
        // contradictory, so the request is rejected rather than one silently
        // winning. Mirrors MaintenanceRuleService's clearSeasonWindow check.
        rejectClearWithValue("lengthMeters", request.clearLengthMeters, request.lengthMeters)
        rejectClearWithValue("widthMeters", request.clearWidthMeters, request.widthMeters)
        rejectClearWithValue("soilType", request.clearSoilType, request.soilType)
        rejectClearWithValue("soilPh", request.clearSoilPh, request.soilPh)
        rejectClearWithValue("sunExposure", request.clearSunExposure, request.sunExposure)
        rejectClearWithValue("drainage", request.clearDrainage, request.drainage)
        rejectClearWithValue("sunDirections", request.clearSunDirections, request.sunDirections)
        rejectClearWithValue("irrigationType", request.clearIrrigationType, request.irrigationType)
        rejectClearWithValue("protection", request.clearProtection, request.protection)

        val updated = bed.copy(
            name = request.name ?: bed.name,
            // Empty/blank string means "clear" — null means "not in request, keep"
            description = if (request.description == null) bed.description
                else request.description.ifBlank { null },
            boundaryJson = request.boundaryJson ?: bed.boundaryJson,
            lengthMeters = if (request.clearLengthMeters) null else request.lengthMeters ?: bed.lengthMeters,
            widthMeters = if (request.clearWidthMeters) null else request.widthMeters ?: bed.widthMeters,
            soilType = if (request.clearSoilType) null
                else request.soilType?.let { SoilType.valueOf(it) } ?: bed.soilType,
            soilPh = if (request.clearSoilPh) null else request.soilPh ?: bed.soilPh,
            sunExposure = if (request.clearSunExposure) null
                else request.sunExposure?.let { SunExposure.valueOf(it) } ?: bed.sunExposure,
            drainage = if (request.clearDrainage) null
                else request.drainage?.let { Drainage.valueOf(it) } ?: bed.drainage,
            sunDirections = if (request.clearSunDirections) emptyList()
                else request.sunDirections?.map { CompassDirection.valueOf(it) } ?: bed.sunDirections,
            irrigationType = if (request.clearIrrigationType) null
                else request.irrigationType?.let { IrrigationType.valueOf(it) } ?: bed.irrigationType,
            protection = if (request.clearProtection) null
                else request.protection?.let { Protection.valueOf(it) } ?: bed.protection,
            raisedBed = request.raisedBed ?: bed.raisedBed,
        )
        bedRepository.update(updated)
        return updated.toResponse()
    }

    private fun rejectClearWithValue(field: String, clear: Boolean, value: Any?) {
        if (clear && value != null) {
            throw BadRequestException("Cannot clear $field and supply a new value at the same time")
        }
    }

    fun deleteBed(bedId: Long, orgId: Long) {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        bedRepository.delete(bedId)
    }

    fun listPhotos(bedId: Long, orgId: Long): List<BedPhotoResponse> {
        requireBedOwnership(bedId, orgId)
        return bedPhotoRepository.findByBedId(bedId).map { it.toResponse() }
    }

    fun addPhoto(bedId: Long, request: CreateBedPhotoRequest, orgId: Long): BedPhotoResponse {
        requireBedOwnership(bedId, orgId)
        val reason = parseReason(request.reason)
        // Persist first to get an id we can use as the storage path, then
        // patch the URL once the upload succeeds. Mirrors how plant event
        // photos are stored (see PlantService.addEvent).
        var photo = bedPhotoRepository.persist(
            BedPhoto(
                bedId = bedId,
                photoUrl = "",
                reason = reason,
                description = request.description?.takeIf { it.isNotBlank() },
                capturedAt = request.capturedAt ?: Instant.now(),
            )
        )
        val url = storageService.uploadBedPhoto(bedId, photo.id!!, request.imageBase64)
        bedPhotoRepository.updatePhotoUrl(photo.id, url)
        photo = photo.copy(photoUrl = url)
        return photo.toResponse()
    }

    fun deletePhoto(bedId: Long, photoId: Long, orgId: Long) {
        requireBedOwnership(bedId, orgId)
        val photo = bedPhotoRepository.findById(photoId)
            ?: throw NotFoundException("Bed photo not found")
        if (photo.bedId != bedId) throw NotFoundException("Bed photo not found")
        storageService.deleteByPath(photo.photoUrl)
        bedPhotoRepository.delete(photoId)
    }

    /** Total harvested stems for a bed, optionally scoped to one season. Bed must belong to [orgId]. */
    fun getBedHarvestStats(bedId: Long, orgId: Long, seasonId: Long?): HarvestStatsResponse {
        requireBedOwnership(bedId, orgId)
        return HarvestStatsResponse(totalStems = plantEvents.totalStemsByBed(bedId, seasonId))
    }

    private fun requireBedOwnership(bedId: Long, orgId: Long): Bed {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        return bed
    }

    private fun parseReason(value: String): BedPhotoReason =
        try { BedPhotoReason.valueOf(value) }
        catch (e: IllegalArgumentException) { throw BadRequestException("Unknown bed photo reason: $value") }

    fun getBedHistory(bedId: Long, orgId: Long): List<BedHistoryEntry> {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")

        data class Row(
            val seasonId: Long?,
            val seasonName: String?,
            val year: Int?,
            val speciesId: Long,
            val speciesName: String,
            val plantCount: Int,
            val totalStemsHarvested: Int,
            val status: String,
        )

        val rows = ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT p.season_id,
                          s.name AS season_name,
                          s.year AS season_year,
                          sp.id AS species_id,
                          COALESCE(sp.common_name_sv, sp.common_name) AS species_name,
                          COALESCE(sp.variant_name_sv, sp.variant_name) AS variant_name,
                          COUNT(DISTINCT p.id) AS plant_count,
                          COALESCE(SUM(pe.stem_count), 0) AS total_stems,
                          MAX(p.status) AS status
                   FROM plant p
                   JOIN species sp ON p.species_id = sp.id
                   LEFT JOIN season s ON p.season_id = s.id
                   LEFT JOIN plant_event pe ON pe.plant_id = p.id AND pe.event_type = 'HARVESTED'
                   WHERE p.bed_id = ?
                   GROUP BY p.season_id, s.name, s.year, sp.id, sp.common_name_sv, sp.common_name, sp.variant_name_sv, sp.variant_name
                   ORDER BY s.year DESC NULLS LAST, s.name, species_name"""
            ).use { ps ->
                ps.setLong(1, bedId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            val seasonId = rs.getLong("season_id").takeIf { !rs.wasNull() }
                            val name = rs.getString("species_name")
                            val variant = rs.getString("variant_name")
                            add(
                                Row(
                                    seasonId = seasonId,
                                    seasonName = rs.getString("season_name"),
                                    year = rs.getInt("season_year").takeIf { !rs.wasNull() },
                                    speciesId = rs.getLong("species_id"),
                                    speciesName = if (variant != null) "$name – $variant" else name,
                                    plantCount = rs.getInt("plant_count"),
                                    totalStemsHarvested = rs.getInt("total_stems"),
                                    status = rs.getString("status"),
                                )
                            )
                        }
                    }
                }
            }
        }

        return rows.groupBy { Triple(it.seasonId, it.seasonName, it.year) }
            .map { (key, speciesRows) ->
                BedHistoryEntry(
                    seasonId = key.first,
                    seasonName = key.second,
                    year = key.third,
                    species = speciesRows.map {
                        BedHistorySpecies(
                            speciesId = it.speciesId,
                            speciesName = it.speciesName,
                            plantCount = it.plantCount,
                            totalStemsHarvested = it.totalStemsHarvested,
                            status = it.status,
                        )
                    },
                )
            }
    }
}

fun BedPhoto.toResponse() = BedPhotoResponse(
    id = id!!,
    bedId = bedId,
    photoUrl = photoUrl,
    reason = reason.name,
    description = description,
    capturedAt = capturedAt,
    createdAt = createdAt,
)

fun Bed.toResponse() = BedResponse(
    id = id!!, name = name, description = description,
    gardenId = gardenId, boundaryJson = boundaryJson,
    lengthMeters = lengthMeters, widthMeters = widthMeters,
    soilType = soilType?.name,
    soilPh = soilPh,
    sunExposure = sunExposure?.name,
    drainage = drainage?.name,
    sunDirections = sunDirections.map { it.name },
    irrigationType = irrigationType?.name,
    protection = protection?.name,
    raisedBed = raisedBed,
    createdAt = createdAt, updatedAt = updatedAt
)
