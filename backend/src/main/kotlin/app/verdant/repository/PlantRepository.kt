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

    fun findByBedId(bedId: Long): List<Plant> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM plant WHERE bed_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, bedId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPlant()) }
                }
            }
        }

    fun findByUserId(userId: Long, status: PlantStatus? = null): List<Plant> =
        ds.connection.use { conn ->
            val sql = buildString {
                append("SELECT p.* FROM plant p JOIN bed b ON p.bed_id = b.id JOIN garden g ON b.garden_id = g.id WHERE g.owner_id = ?")
                if (status != null) append(" AND p.status = ?")
                append(" ORDER BY p.id")
            }
            conn.prepareStatement(sql).use { ps ->
                ps.setLong(1, userId)
                if (status != null) ps.setString(2, status.name)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toPlant()) }
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
                """INSERT INTO plant (name, species_id, planted_date, status, seed_count, surviving_count, bed_id, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, plant.name)
                ps.setObject(2, plant.speciesId)
                ps.setDate(3, plant.plantedDate?.let { Date.valueOf(it) })
                ps.setString(4, plant.status.name)
                ps.setObject(5, plant.seedCount)
                ps.setObject(6, plant.survivingCount)
                ps.setLong(7, plant.bedId)
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
                   seed_count = ?, surviving_count = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, plant.name)
                ps.setObject(2, plant.speciesId)
                ps.setDate(3, plant.plantedDate?.let { Date.valueOf(it) })
                ps.setString(4, plant.status.name)
                ps.setObject(5, plant.seedCount)
                ps.setObject(6, plant.survivingCount)
                ps.setLong(7, plant.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun speciesSummary(userId: Long): List<SpeciesPlantSummary> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT p.species_id, s.common_name, s.scientific_name,
                          COUNT(*) as total_count,
                          COUNT(*) FILTER (WHERE p.status != 'REMOVED') as active_count
                   FROM plant p
                   JOIN bed b ON p.bed_id = b.id
                   JOIN garden g ON b.garden_id = g.id
                   JOIN species s ON p.species_id = s.id
                   WHERE g.owner_id = ? AND p.species_id IS NOT NULL
                   GROUP BY p.species_id, s.common_name, s.scientific_name
                   ORDER BY s.common_name"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            SpeciesPlantSummary(
                                speciesId = rs.getLong("species_id"),
                                speciesName = rs.getString("common_name"),
                                scientificName = rs.getString("scientific_name"),
                                activePlantCount = rs.getInt("active_count"),
                                totalPlantCount = rs.getInt("total_count"),
                            )
                        )
                    }
                }
            }
        }

    fun speciesLocations(userId: Long, speciesId: Long): List<PlantLocationGroup> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                """SELECT g.name as garden_name, b.name as bed_name, b.id as bed_id,
                          p.status, COUNT(*) as count,
                          EXTRACT(YEAR FROM p.created_at)::int as year
                   FROM plant p
                   JOIN bed b ON p.bed_id = b.id
                   JOIN garden g ON b.garden_id = g.id
                   WHERE g.owner_id = ? AND p.species_id = ?
                   GROUP BY g.name, b.name, b.id, p.status, EXTRACT(YEAR FROM p.created_at)
                   ORDER BY year DESC, g.name, b.name, p.status"""
            ).use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, speciesId)
                ps.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(
                            PlantLocationGroup(
                                gardenName = rs.getString("garden_name"),
                                bedName = rs.getString("bed_name"),
                                bedId = rs.getLong("bed_id"),
                                status = rs.getString("status"),
                                count = rs.getInt("count"),
                                year = rs.getInt("year"),
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
                ps.executeUpdate()
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
        bedId = getLong("bed_id"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
