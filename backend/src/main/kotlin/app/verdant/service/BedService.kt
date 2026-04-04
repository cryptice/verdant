package app.verdant.service

import app.verdant.dto.*
import app.verdant.entity.Bed
import app.verdant.repository.BedRepository
import app.verdant.repository.GardenRepository
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotFoundException

@ApplicationScoped
class BedService(
    private val bedRepository: BedRepository,
    private val gardenRepository: GardenRepository,
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
            Bed(name = request.name, description = request.description, gardenId = gardenId, boundaryJson = request.boundaryJson)
        )
        return bed.toResponse()
    }

    fun updateBed(bedId: Long, request: UpdateBedRequest, orgId: Long): BedResponse {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        val updated = bed.copy(
            name = request.name ?: bed.name,
            description = request.description ?: bed.description,
            boundaryJson = request.boundaryJson ?: bed.boundaryJson,
        )
        bedRepository.update(updated)
        return updated.toResponse()
    }

    fun deleteBed(bedId: Long, orgId: Long) {
        val bed = bedRepository.findById(bedId) ?: throw NotFoundException("Bed not found")
        val garden = gardenRepository.findById(bed.gardenId) ?: throw NotFoundException("Garden not found")
        if (garden.orgId != orgId) throw NotFoundException("Bed not found")
        bedRepository.delete(bedId)
    }

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
                          sp.common_name AS species_name,
                          COUNT(DISTINCT p.id) AS plant_count,
                          COALESCE(SUM(pe.stem_count), 0) AS total_stems,
                          MAX(p.status) AS status
                   FROM plant p
                   JOIN species sp ON p.species_id = sp.id
                   LEFT JOIN season s ON p.season_id = s.id
                   LEFT JOIN plant_event pe ON pe.plant_id = p.id AND pe.event_type = 'HARVESTED'
                   WHERE p.bed_id = ?
                   GROUP BY p.season_id, s.name, s.year, sp.id, sp.common_name
                   ORDER BY s.year DESC NULLS LAST, s.name, sp.common_name"""
            ).use { ps ->
                ps.setLong(1, bedId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            val seasonId = rs.getLong("season_id").takeIf { !rs.wasNull() }
                            add(
                                Row(
                                    seasonId = seasonId,
                                    seasonName = rs.getString("season_name"),
                                    year = rs.getInt("season_year").takeIf { !rs.wasNull() },
                                    speciesId = rs.getLong("species_id"),
                                    speciesName = rs.getString("species_name"),
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

fun Bed.toResponse() = BedResponse(
    id = id!!, name = name, description = description,
    gardenId = gardenId, boundaryJson = boundaryJson,
    lengthMeters = lengthMeters, widthMeters = widthMeters,
    createdAt = createdAt, updatedAt = updatedAt
)
