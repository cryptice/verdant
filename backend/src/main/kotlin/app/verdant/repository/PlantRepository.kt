package app.verdant.repository

import app.verdant.dto.PlantLocationGroup
import app.verdant.dto.SpeciesPlantSummary
import app.verdant.entity.Plant
import app.verdant.entity.PlantStatus
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class PlantRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Plant? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM plant WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toPlant() else null }
            }
        }

    fun findByBedId(bedId: Long, seasonId: Long? = null): List<Plant> =
        ds.connection.use { conn ->
            val sql = buildString {
                append("SELECT * FROM plant WHERE bed_id = ?")
                if (seasonId != null) append(" AND season_id = ?")
                append(" ORDER BY id")
            }
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, bedId)
                if (seasonId != null) ps.setLong(2, seasonId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPlant()) }
                }
            }
        }

    fun findByOrgId(orgId: Long, status: PlantStatus? = null, seasonId: Long? = null, limit: Int = 50, offset: Int = 0): List<Plant> =
        ds.connection.use { conn ->
            val sql = buildString {
                append("SELECT p.* FROM plant p WHERE p.org_id = ?")
                if (status != null) append(" AND p.status = ?")
                if (seasonId != null) append(" AND p.season_id = ?")
                append(" ORDER BY p.id LIMIT ? OFFSET ?")
            }
            conn.prepareStatement(sql).use { ps ->
                var idx = 1
                ps.setLong(idx++, orgId)
                if (status != null) ps.setString(idx++, status.name)
                if (seasonId != null) ps.setLong(idx++, seasonId)
                ps.setInt(idx++, limit)
                ps.setInt(idx, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPlant()) }
                }
            }
        }

    fun countByGardenIds(gardenIds: Set<Long>): Map<Long, Int> {
        if (gardenIds.isEmpty()) return emptyMap()
        val placeholders = gardenIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT b.garden_id, COUNT(*) FROM plant p JOIN bed b ON p.bed_id = b.id WHERE b.garden_id IN ($placeholders) GROUP BY b.garden_id"
            ).use { ps ->
                gardenIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong("garden_id"), rs.getInt("count")) }
                }
            }
        }
    }

    fun countByGardenId(gardenId: Long): Long =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT count(*) FROM plant p JOIN bed b ON p.bed_id = b.id WHERE b.garden_id = ?"
            ).use { ps ->
                ps.setLong(1, gardenId)
                ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
            }
        }

    fun persist(plant: Plant): Plant {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO plant (name, species_id, planted_date, status, seed_count, surviving_count, bed_id, org_id, season_id, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, plant.name)
                ps.setObject(2, plant.speciesId)
                ps.setDate(3, plant.plantedDate?.let { Date.valueOf(it) })
                ps.setString(4, plant.status.name)
                ps.setObject(5, plant.seedCount)
                ps.setObject(6, plant.survivingCount)
                ps.setObject(7, plant.bedId)
                ps.setLong(8, plant.orgId)
                ps.setObject(9, plant.seasonId)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return plant.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(plant: Plant) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE plant SET name = ?, species_id = ?, planted_date = ?, status = ?,
                   seed_count = ?, surviving_count = ?, bed_id = ?, season_id = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, plant.name)
                ps.setObject(2, plant.speciesId)
                ps.setDate(3, plant.plantedDate?.let { Date.valueOf(it) })
                ps.setString(4, plant.status.name)
                ps.setObject(5, plant.seedCount)
                ps.setObject(6, plant.survivingCount)
                ps.setObject(7, plant.bedId)
                ps.setObject(8, plant.seasonId)
                ps.setLong(9, plant.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun speciesSummary(orgId: Long): List<SpeciesPlantSummary> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT p.species_id,
                          COALESCE(s.common_name_sv, s.common_name) as species_name,
                          COALESCE(s.variant_name_sv, s.variant_name) as variant_name,
                          s.scientific_name,
                          COUNT(*) as total_count,
                          COUNT(*) FILTER (WHERE p.status != 'REMOVED') as active_count
                   FROM plant p
                   JOIN species s ON p.species_id = s.id
                   WHERE p.org_id = ? AND p.species_id IS NOT NULL
                   GROUP BY p.species_id, s.common_name_sv, s.common_name, s.variant_name_sv, s.variant_name, s.scientific_name
                   ORDER BY species_name, variant_name"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            SpeciesPlantSummary(
                                speciesId = rs.getLong("species_id"),
                                speciesName = rs.getString("species_name"),
                                variantName = rs.getString("variant_name"),
                                scientificName = rs.getString("scientific_name"),
                                activePlantCount = rs.getInt("active_count"),
                                totalPlantCount = rs.getInt("total_count"),
                            )
                        )
                    }
                }
            }
        }

    fun speciesLocations(orgId: Long, speciesId: Long): List<PlantLocationGroup> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT g.name as garden_name, b.name as bed_name, p.bed_id as bed_id,
                          p.status, COUNT(*) as count,
                          EXTRACT(YEAR FROM p.created_at)::int as year
                   FROM plant p
                   LEFT JOIN bed b ON p.bed_id = b.id
                   LEFT JOIN garden g ON b.garden_id = g.id
                   WHERE p.org_id = ? AND p.species_id = ?
                   GROUP BY g.name, b.name, p.bed_id, p.status, EXTRACT(YEAR FROM p.created_at)
                   ORDER BY year DESC, g.name, b.name, p.status"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, speciesId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            PlantLocationGroup(
                                gardenName = rs.getString("garden_name"),
                                bedName = rs.getString("bed_name"),
                                bedId = rs.getObject("bed_id") as? Long,
                                status = rs.getString("status"),
                                count = rs.getInt("count"),
                                year = rs.getInt("year"),
                            )
                        )
                    }
                }
            }
        }

    /** Aggregate plant events for a species, optionally limited to tray plants
     *  (bed_id IS NULL). Returns rows of (eventType, eventDate, plantCount sum). */
    fun speciesEventSummary(orgId: Long, speciesId: Long, trayOnly: Boolean = false): List<app.verdant.dto.SpeciesEventSummaryEntry> =
        ds.connection.use { conn ->
            val sql = buildString {
                append("""SELECT pe.event_type, pe.event_date, COALESCE(SUM(pe.plant_count), COUNT(*))::int as count
                          FROM plant_event pe
                          JOIN plant p ON pe.plant_id = p.id
                          WHERE p.org_id = ? AND p.species_id = ?""")
                if (trayOnly) append(" AND p.bed_id IS NULL")
                append(" GROUP BY pe.event_type, pe.event_date ORDER BY pe.event_date, pe.event_type")
            }
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, speciesId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            app.verdant.dto.SpeciesEventSummaryEntry(
                                eventType = rs.getString("event_type"),
                                eventDate = rs.getDate("event_date").toLocalDate(),
                                count = rs.getInt("count"),
                            )
                        )
                    }
                }
            }
        }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM plant WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Plant not found")
            }
        }
    }

    fun traySummary(orgId: Long): List<app.verdant.dto.TraySummaryEntry> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT s.id AS species_id,
                          COALESCE(s.common_name_sv, s.common_name) as species_name,
                          COALESCE(s.variant_name_sv, s.variant_name) as variant_name,
                          p.status,
                          COUNT(*) as count
                   FROM plant p
                   LEFT JOIN species s ON p.species_id = s.id
                   WHERE p.org_id = ? AND p.bed_id IS NULL AND p.status != 'REMOVED'
                   GROUP BY s.id, s.common_name_sv, s.common_name, s.variant_name_sv, s.variant_name, p.status
                   ORDER BY species_name, variant_name, p.status"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            app.verdant.dto.TraySummaryEntry(
                                speciesId = rs.getObject("species_id") as? Long,
                                speciesName = rs.getString("species_name") ?: "Unknown",
                                variantName = rs.getString("variant_name"),
                                status = rs.getString("status"),
                                count = rs.getInt("count"),
                            )
                        )
                    }
                }
            }
        }

    fun findGroupedBySpecies(orgId: Long, status: PlantStatus, trayOnly: Boolean = false): List<Map<String, Any?>> =
        ds.connection.use { conn ->
            val sql = buildString {
                append("""SELECT p.species_id,
                          COALESCE(s.common_name_sv, s.common_name) as species_name,
                          COALESCE(s.variant_name_sv, s.variant_name) as variant_name,
                          p.bed_id, b.name as bed_name, g.name as garden_name,
                          p.planted_date, p.status, COUNT(*) as count
                   FROM plant p
                   LEFT JOIN species s ON p.species_id = s.id
                   LEFT JOIN bed b ON p.bed_id = b.id
                   LEFT JOIN garden g ON b.garden_id = g.id
                   WHERE p.org_id = ? AND p.status = ?""")
                if (trayOnly) append(" AND p.bed_id IS NULL")
                append(" GROUP BY p.species_id, s.common_name_sv, s.common_name, s.variant_name_sv, s.variant_name, p.bed_id, b.name, g.name, p.planted_date, p.status")
                append(" ORDER BY p.planted_date DESC")
            }
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, orgId)
                ps.setString(2, status.name)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(mapOf(
                            "speciesId" to (rs.getObject("species_id") as? Long),
                            "speciesName" to rs.getString("species_name"),
                            "variantName" to rs.getString("variant_name"),
                            "bedId" to (rs.getObject("bed_id") as? Long),
                            "bedName" to rs.getString("bed_name"),
                            "gardenName" to rs.getString("garden_name"),
                            "plantedDate" to rs.getDate("planted_date")?.toString(),
                            "status" to rs.getString("status"),
                            "count" to rs.getInt("count"),
                        ))
                    }
                }
            }
        }

    fun findByIds(ids: List<Long>): List<Plant> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM plant WHERE id IN ($placeholders)").use { ps ->
                ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPlant()) }
                }
            }
        }
    }

    fun findByGroup(orgId: Long, speciesId: Long, bedId: Long?, plantedDate: java.time.LocalDate?, status: PlantStatus, limit: Int): List<Plant> =
        ds.connection.use { conn ->
            val sql = buildString {
                append("SELECT * FROM plant WHERE org_id = ? AND species_id = ? AND status = ?")
                if (bedId != null) append(" AND bed_id = ?") else append(" AND bed_id IS NULL")
                if (plantedDate != null) append(" AND planted_date = ?")
                append(" ORDER BY id LIMIT ?")
            }
            conn.prepareStatement(sql).use { ps ->
                var idx = 1
                ps.setLong(idx++, orgId)
                ps.setLong(idx++, speciesId)
                ps.setString(idx++, status.name)
                if (bedId != null) ps.setLong(idx++, bedId)
                if (plantedDate != null) ps.setDate(idx++, java.sql.Date.valueOf(plantedDate))
                ps.setInt(idx, limit)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPlant()) }
                }
            }
        }

    private fun ResultSet.toPlant() = Plant(
        id = getLong("id"),
        name = getString("name"),
        speciesId = getObject("species_id") as? Long,
        plantedDate = getDate("planted_date")?.toLocalDate(),
        status = PlantStatus.valueOf(getString("status")),
        seedCount = getObject("seed_count") as? Int,
        survivingCount = getObject("surviving_count") as? Int,
        bedId = getObject("bed_id") as? Long,
        orgId = getLong("org_id"),
        seasonId = getObject("season_id") as? Long,
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
