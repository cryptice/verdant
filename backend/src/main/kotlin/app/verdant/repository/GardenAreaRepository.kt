package app.verdant.repository

import app.verdant.entity.GardenArea
import app.verdant.entity.GardenAreaCategory
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class GardenAreaRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): GardenArea? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM garden_area WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toGardenArea() else null }
        }
    }

    fun findByGardenId(gardenId: Long): List<GardenArea> = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM garden_area WHERE garden_id = ? ORDER BY id").use { ps ->
            ps.setLong(1, gardenId)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.toGardenArea()) } }
        }
    }

    fun countByGardenIds(gardenIds: Set<Long>): Map<Long, Int> {
        if (gardenIds.isEmpty()) return emptyMap()
        val placeholders = gardenIds.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT garden_id, COUNT(*) FROM garden_area WHERE garden_id IN ($placeholders) GROUP BY garden_id"
            ).use { ps ->
                gardenIds.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong("garden_id"), rs.getInt("count")) }
                }
            }
        }
    }

    fun persist(area: GardenArea): GardenArea = ds.connection.use { conn ->
        conn.prepareStatement(
            """INSERT INTO garden_area (garden_id, name, description, category, boundary_json, size_sqm,
                                        created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, now(), now())""",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, area.gardenId)
            ps.setString(2, area.name)
            ps.setString(3, area.description)
            ps.setString(4, area.category.name)
            ps.setString(5, area.boundaryJson)
            area.sizeSqm?.let { ps.setDouble(6, it) } ?: ps.setNull(6, java.sql.Types.DOUBLE)
            ps.executeUpdate()
            ps.generatedKeys.use { rs -> rs.next(); area.copy(id = rs.getLong(1)) }
        }
    }

    fun update(area: GardenArea) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE garden_area
                   SET name = ?, description = ?, category = ?, boundary_json = ?, size_sqm = ?, updated_at = now()
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, area.name)
                ps.setString(2, area.description)
                ps.setString(3, area.category.name)
                ps.setString(4, area.boundaryJson)
                area.sizeSqm?.let { ps.setDouble(5, it) } ?: ps.setNull(5, java.sql.Types.DOUBLE)
                ps.setLong(6, area.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM garden_area WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toGardenArea() = GardenArea(
        id = getLong("id"),
        gardenId = getLong("garden_id"),
        name = getString("name"),
        description = getString("description"),
        category = GardenAreaCategory.valueOf(getString("category")),
        boundaryJson = getString("boundary_json"),
        sizeSqm = getObject("size_sqm") as? Double,
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
