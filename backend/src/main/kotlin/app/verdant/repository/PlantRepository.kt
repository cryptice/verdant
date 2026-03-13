package app.verdant.repository

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
                """INSERT INTO plant (name, species, planted_date, status, bed_id, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, plant.name)
                ps.setString(2, plant.species)
                ps.setDate(3, plant.plantedDate?.let { Date.valueOf(it) })
                ps.setString(4, plant.status.name)
                ps.setLong(5, plant.bedId)
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
                """UPDATE plant SET name = ?, species = ?, planted_date = ?, status = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, plant.name)
                ps.setString(2, plant.species)
                ps.setDate(3, plant.plantedDate?.let { Date.valueOf(it) })
                ps.setString(4, plant.status.name)
                ps.setLong(5, plant.id!!)
                ps.executeUpdate()
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
        species = getString("species"),
        plantedDate = getDate("planted_date")?.toLocalDate(),
        status = PlantStatus.valueOf(getString("status")),
        bedId = getLong("bed_id"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
