package app.verdant.repository

import app.verdant.entity.PlantEvent
import app.verdant.entity.PlantEventType
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.Date
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class PlantEventRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): PlantEvent? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM plant_event WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toPlantEvent() else null }
            }
        }

    fun findByPlantId(plantId: Long): List<PlantEvent> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM plant_event WHERE plant_id = ? ORDER BY event_date, id").use { ps ->
                ps.setLong(1, plantId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPlantEvent()) }
                }
            }
        }

    fun persist(event: PlantEvent): PlantEvent {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO plant_event (plant_id, event_type, event_date, plant_count, weight_grams, quantity, notes, image_url, ai_suggestions,
                   stem_count, stem_length_cm, quality_grade, vase_life_days, supply_application_id,
                   from_tray_location_id, to_tray_location_id, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, event.plantId)
                ps.setString(2, event.eventType.name)
                ps.setDate(3, Date.valueOf(event.eventDate))
                ps.setObject(4, event.plantCount)
                ps.setObject(5, event.weightGrams)
                ps.setObject(6, event.quantity)
                ps.setString(7, event.notes)
                ps.setString(8, event.imageUrl)
                ps.setString(9, event.aiSuggestions)
                ps.setObject(10, event.stemCount)
                ps.setObject(11, event.stemLengthCm)
                ps.setString(12, event.qualityGrade)
                ps.setObject(13, event.vaseLifeDays)
                event.supplyApplicationId?.let { ps.setLong(14, it) } ?: ps.setNull(14, java.sql.Types.BIGINT)
                event.fromTrayLocationId?.let { ps.setLong(15, it) } ?: ps.setNull(15, java.sql.Types.BIGINT)
                event.toTrayLocationId?.let { ps.setLong(16, it) } ?: ps.setNull(16, java.sql.Types.BIGINT)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return event.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun updateImageUrl(id: Long, imageUrl: String) {
        ds.connection.use { conn ->
            conn.prepareStatement("UPDATE plant_event SET image_url = ? WHERE id = ?").use { ps ->
                ps.setString(1, imageUrl)
                ps.setLong(2, id)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM plant_event WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Plant event not found")
            }
        }
    }

    /** Harvest stats grouped by species. Only includes plants with a linked species. */
    fun harvestStatsBySpecies(orgId: Long): List<HarvestStatResult> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT s.common_name as species,
                          COALESCE(SUM(pe.weight_grams), 0) as total_weight,
                          COALESCE(SUM(pe.quantity), 0) as total_quantity,
                          COUNT(pe.id) as harvest_count,
                          COALESCE(SUM(pe.stem_count), 0) as total_stems
                   FROM plant_event pe
                   JOIN plant p ON pe.plant_id = p.id
                   JOIN species s ON p.species_id = s.id
                   WHERE pe.event_type = 'HARVESTED'
                     AND p.org_id = ?
                   GROUP BY s.common_name
                   ORDER BY total_weight DESC"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            HarvestStatResult(
                                species = rs.getString("species"),
                                totalWeightGrams = rs.getDouble("total_weight"),
                                totalQuantity = rs.getInt("total_quantity"),
                                harvestCount = rs.getInt("harvest_count"),
                                totalStems = rs.getInt("total_stems"),
                            )
                        )
                    }
                }
            }
        }

    /**
     * Harvested stems for one season, bucketed by ISO-8601 week of the harvest event.
     * Org-scoped via `plant.org_id`, season-scoped via `plant.season_id`. Weeks with no
     * stems are excluded so callers can treat the result as "weeks that produced stems".
     */
    fun harvestWeeklyBucketsBySeason(orgId: Long, seasonId: Long): List<HarvestWeekBucket> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT EXTRACT(WEEK FROM pe.event_date)::int AS iso_week,
                          COALESCE(SUM(pe.stem_count), 0)::int AS stems
                   FROM plant_event pe
                   JOIN plant p ON pe.plant_id = p.id
                   WHERE pe.event_type = 'HARVESTED'
                     AND p.org_id = ?
                     AND p.season_id = ?
                   GROUP BY iso_week
                   HAVING COALESCE(SUM(pe.stem_count), 0) > 0
                   ORDER BY iso_week"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setLong(2, seasonId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(HarvestWeekBucket(rs.getInt("iso_week"), rs.getInt("stems")))
                    }
                }
            }
        }

    /** Total harvested stems for an org across every season of a given calendar year. */
    fun totalStemsByOrgYear(orgId: Long, year: Int): Int =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT COALESCE(SUM(pe.stem_count), 0)::int AS stems
                   FROM plant_event pe
                   JOIN plant p ON pe.plant_id = p.id
                   JOIN season s ON p.season_id = s.id
                   WHERE pe.event_type = 'HARVESTED'
                     AND p.org_id = ?
                     AND s.year = ?"""
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setInt(2, year)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("stems") else 0 }
            }
        }

    /** Total harvested stems for a single bed, optionally scoped to one season. */
    fun totalStemsByBed(bedId: Long, seasonId: Long?): Int =
        ds.connection.use { conn ->
            val seasonFilter = if (seasonId != null) " AND p.season_id = ?" else ""
            conn.prepareStatement(
                """SELECT COALESCE(SUM(pe.stem_count), 0)::int AS stems
                   FROM plant_event pe
                   JOIN plant p ON pe.plant_id = p.id
                   WHERE pe.event_type = 'HARVESTED'
                     AND p.bed_id = ?$seasonFilter"""
            ).use { ps ->
                ps.setLong(1, bedId)
                if (seasonId != null) ps.setLong(2, seasonId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("stems") else 0 }
            }
        }

    /** Total harvested stems for a whole garden (across all its beds), optionally scoped to one season. */
    fun totalStemsByGarden(gardenId: Long, seasonId: Long?): Int =
        ds.connection.use { conn ->
            val seasonFilter = if (seasonId != null) " AND p.season_id = ?" else ""
            conn.prepareStatement(
                """SELECT COALESCE(SUM(pe.stem_count), 0)::int AS stems
                   FROM plant_event pe
                   JOIN plant p ON pe.plant_id = p.id
                   JOIN bed b ON p.bed_id = b.id
                   WHERE pe.event_type = 'HARVESTED'
                     AND b.garden_id = ?$seasonFilter"""
            ).use { ps ->
                ps.setLong(1, gardenId)
                if (seasonId != null) ps.setLong(2, seasonId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("stems") else 0 }
            }
        }

    private fun ResultSet.toPlantEvent() = PlantEvent(
        id = getLong("id"),
        plantId = getLong("plant_id"),
        eventType = PlantEventType.valueOf(getString("event_type")),
        eventDate = getDate("event_date").toLocalDate(),
        plantCount = getObject("plant_count") as? Int,
        weightGrams = getObject("weight_grams") as? Double,
        quantity = getObject("quantity") as? Int,
        notes = getString("notes"),
        imageUrl = getString("image_url"),
        aiSuggestions = getString("ai_suggestions"),
        stemCount = getObject("stem_count") as? Int,
        stemLengthCm = getObject("stem_length_cm") as? Int,
        qualityGrade = getString("quality_grade"),
        vaseLifeDays = getObject("vase_life_days") as? Int,
        supplyApplicationId = getLong("supply_application_id").takeIf { !wasNull() },
        fromTrayLocationId = getLong("from_tray_location_id").takeIf { !wasNull() },
        toTrayLocationId = getLong("to_tray_location_id").takeIf { !wasNull() },
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

data class HarvestStatResult(
    val species: String,
    val totalWeightGrams: Double,
    val totalQuantity: Int,
    val harvestCount: Int,
    val totalStems: Int,
)

/** One ISO-8601 week's harvested-stem total within a season. */
data class HarvestWeekBucket(
    val isoWeek: Int,
    val stems: Int,
)
