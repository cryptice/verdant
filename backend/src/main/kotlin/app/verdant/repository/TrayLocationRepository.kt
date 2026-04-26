package app.verdant.repository

import app.verdant.entity.TrayLocation
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class TrayLocationRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): TrayLocation? = ds.connection.use { conn ->
        conn.prepareStatement("SELECT * FROM tray_location WHERE id = ?").use { ps ->
            ps.setLong(1, id)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toTrayLocation() else null }
        }
    }

    fun findByOrgId(orgId: Long, limit: Int = 100, offset: Int = 0): List<TrayLocation> =
        ds.connection.use { conn ->
            conn.prepareStatement(
                "SELECT * FROM tray_location WHERE org_id = ? ORDER BY name LIMIT ? OFFSET ?"
            ).use { ps ->
                ps.setLong(1, orgId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toTrayLocation()) }
                }
            }
        }

    fun persist(loc: TrayLocation): TrayLocation = ds.connection.use { conn ->
        conn.prepareStatement(
            "INSERT INTO tray_location (org_id, name, created_at) VALUES (?, ?, now())",
            Statement.RETURN_GENERATED_KEYS,
        ).use { ps ->
            ps.setLong(1, loc.orgId)
            ps.setString(2, loc.name)
            ps.executeUpdate()
            ps.generatedKeys.use { rs ->
                rs.next()
                loc.copy(id = rs.getLong(1))
            }
        }
    }

    fun update(loc: TrayLocation) {
        ds.connection.use { conn ->
            conn.prepareStatement("UPDATE tray_location SET name = ? WHERE id = ?").use { ps ->
                ps.setString(1, loc.name)
                ps.setLong(2, loc.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM tray_location WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Tray location not found")
            }
        }
    }

    /** Count active plants currently at this location (used for delete confirm). */
    fun countActivePlants(locationId: Long): Int = ds.connection.use { conn ->
        conn.prepareStatement(
            "SELECT COUNT(*) FROM plant WHERE tray_location_id = ? AND status <> 'REMOVED'"
        ).use { ps ->
            ps.setLong(1, locationId)
            ps.executeQuery().use { rs -> rs.next(); rs.getInt(1) }
        }
    }

    private fun ResultSet.toTrayLocation() = TrayLocation(
        id = getLong("id"),
        orgId = getLong("org_id"),
        name = getString("name"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
