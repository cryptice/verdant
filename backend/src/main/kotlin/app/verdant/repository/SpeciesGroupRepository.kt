package app.verdant.repository

import app.verdant.entity.SpeciesGroup
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SpeciesGroupRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SpeciesGroup? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_group WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toGroup() else null }
            }
        }

    fun findAll(): List<SpeciesGroup> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_group ORDER BY name").use { ps ->
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toGroup()) }
                }
            }
        }

    fun findByOrgId(orgId: Long): List<SpeciesGroup> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_group WHERE org_id = ? OR org_id IS NULL ORDER BY name").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toGroup()) }
                }
            }
        }

    fun persist(group: SpeciesGroup): SpeciesGroup {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO species_group (org_id, name) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                if (group.orgId != null) ps.setLong(1, group.orgId) else ps.setNull(1, java.sql.Types.BIGINT)
                ps.setString(2, group.name)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return group.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_group WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Species group not found")
            }
        }
    }

    private fun ResultSet.toGroup() = SpeciesGroup(
        id = getLong("id"),
        orgId = getObject("org_id") as? Long,
        name = getString("name"),
    )
}
