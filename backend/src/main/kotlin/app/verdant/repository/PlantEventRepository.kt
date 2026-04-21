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
                   stem_count, stem_length_cm, quality_grade, vase_life_days, harvest_destination_id, supply_application_id, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
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
                ps.setObject(14, event.harvestDestinationId)
                event.supplyApplicationId?.let { ps.setLong(15, it) } ?: ps.setNull(15, java.sql.Types.BIGINT)
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
        harvestDestinationId = getObject("harvest_destination_id") as? Long,
        supplyApplicationId = getLong("supply_application_id").takeIf { !wasNull() },
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
