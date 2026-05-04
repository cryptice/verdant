package app.verdant.repository

import app.verdant.entity.Channel
import app.verdant.entity.Outlet
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class OutletRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Outlet? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM outlet WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toOutlet() else null }
            }
        }

    fun findByIds(ids: Set<Long>): Map<Long, Outlet> {
        if (ids.isEmpty()) return emptyMap()
        val placeholders = ids.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM outlet WHERE id IN ($placeholders)").use { ps ->
                ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) { val o = rs.toOutlet(); put(o.id!!, o) } }
                }
            }
        }
    }

    fun findByOrgId(orgId: Long): List<Outlet> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM outlet WHERE org_id = ? ORDER BY name").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toOutlet()) }
                }
            }
        }

    /** Admin-scope: all outlets across all orgs, ordered by org then name. */
    fun findAll(): List<Outlet> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM outlet ORDER BY org_id, name").use { ps ->
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toOutlet()) }
                }
            }
        }

    fun persist(outlet: Outlet): Outlet {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO outlet (org_id, name, channel, contact_info, notes, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, now(), now())""",
                Statement.RETURN_GENERATED_KEYS,
            ).use { ps ->
                ps.setLong(1, outlet.orgId)
                ps.setString(2, outlet.name)
                ps.setString(3, outlet.channel.name)
                ps.setString(4, outlet.contactInfo)
                ps.setString(5, outlet.notes)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return outlet.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(outlet: Outlet) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE outlet SET name = ?, channel = ?, contact_info = ?, notes = ?, updated_at = now()
                   WHERE id = ?""",
            ).use { ps ->
                ps.setString(1, outlet.name)
                ps.setString(2, outlet.channel.name)
                ps.setString(3, outlet.contactInfo)
                ps.setString(4, outlet.notes)
                ps.setLong(5, outlet.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM outlet WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Outlet not found")
            }
        }
    }

    private fun ResultSet.toOutlet() = Outlet(
        id = getLong("id"),
        orgId = getLong("org_id"),
        name = getString("name"),
        channel = Channel.valueOf(getString("channel")),
        contactInfo = getString("contact_info"),
        notes = getString("notes"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
