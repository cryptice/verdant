package app.verdant.repository

import app.verdant.entity.Garden
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class GardenRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Garden? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM garden WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toGarden() else null }
            }
        }

    fun findByOwnerId(ownerId: Long): List<Garden> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM garden WHERE owner_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, ownerId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toGarden()) }
                }
            }
        }

    fun listAll(): List<Garden> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM garden ORDER BY id").use { ps ->
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toGarden()) }
                }
            }
        }

    fun persist(garden: Garden): Garden {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO garden (name, description, emoji, owner_id, latitude, longitude, address, boundary_json, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setString(1, garden.name)
                ps.setString(2, garden.description)
                ps.setString(3, garden.emoji)
                ps.setLong(4, garden.ownerId)
                if (garden.latitude != null) ps.setDouble(5, garden.latitude) else ps.setNull(5, java.sql.Types.DOUBLE)
                if (garden.longitude != null) ps.setDouble(6, garden.longitude) else ps.setNull(6, java.sql.Types.DOUBLE)
                ps.setString(7, garden.address)
                ps.setString(8, garden.boundaryJson)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return garden.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(garden: Garden) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE garden SET name = ?, description = ?, emoji = ?, latitude = ?, longitude = ?, address = ?, boundary_json = ?, updated_at = now() WHERE id = ?"
            ).use { ps ->
                ps.setString(1, garden.name)
                ps.setString(2, garden.description)
                ps.setString(3, garden.emoji)
                if (garden.latitude != null) ps.setDouble(4, garden.latitude) else ps.setNull(4, java.sql.Types.DOUBLE)
                if (garden.longitude != null) ps.setDouble(5, garden.longitude) else ps.setNull(5, java.sql.Types.DOUBLE)
                ps.setString(6, garden.address)
                ps.setString(7, garden.boundaryJson)
                ps.setLong(8, garden.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM garden WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeUpdate()
            }
        }
    }

    private fun ResultSet.toGarden() = Garden(
        id = getLong("id"),
        name = getString("name"),
        description = getString("description"),
        emoji = getString("emoji"),
        ownerId = getLong("owner_id"),
        latitude = getDouble("latitude").takeIf { !wasNull() },
        longitude = getDouble("longitude").takeIf { !wasNull() },
        address = getString("address"),
        boundaryJson = getString("boundary_json"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
