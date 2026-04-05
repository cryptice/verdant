package app.verdant.repository

import app.verdant.entity.SupplyCategory
import app.verdant.entity.SupplyType
import app.verdant.entity.SupplyUnit
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SupplyTypeRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SupplyType? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM supply_type WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toSupplyType() else null }
            }
        }

    fun findByOrgId(orgId: Long): List<SupplyType> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM supply_type WHERE org_id = ? ORDER BY name").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toSupplyType()) }
                }
            }
        }

    fun findNamesByIds(ids: Set<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        val placeholders = ids.joinToString(",") { "?" }
        return ds.connection.use { conn ->
            conn.prepareStatement("SELECT id, name FROM supply_type WHERE id IN ($placeholders)").use { ps ->
                ids.forEachIndexed { i, id -> ps.setLong(i + 1, id) }
                ps.executeQuery().use { rs ->
                    buildMap { while (rs.next()) put(rs.getLong("id"), rs.getString("name")) }
                }
            }
        }
    }

    fun persist(type: SupplyType): SupplyType {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """INSERT INTO supply_type (org_id, name, category, unit, properties, created_at)
                   VALUES (?, ?, ?, ?, ?, now())""",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, type.orgId)
                ps.setString(2, type.name)
                ps.setString(3, type.category.name)
                ps.setString(4, type.unit.name)
                ps.setObject(5, type.properties, java.sql.Types.OTHER)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return type.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(type: SupplyType) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                """UPDATE supply_type SET name = ?, category = ?, unit = ?, properties = ?
                   WHERE id = ?"""
            ).use { ps ->
                ps.setString(1, type.name)
                ps.setString(2, type.category.name)
                ps.setString(3, type.unit.name)
                ps.setObject(4, type.properties, java.sql.Types.OTHER)
                ps.setLong(5, type.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM supply_type WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Supply type not found")
            }
        }
    }

    private fun ResultSet.toSupplyType() = SupplyType(
        id = getLong("id"),
        orgId = getLong("org_id"),
        name = getString("name"),
        category = SupplyCategory.valueOf(getString("category")),
        unit = SupplyUnit.valueOf(getString("unit")),
        properties = getString("properties") ?: "{}",
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
