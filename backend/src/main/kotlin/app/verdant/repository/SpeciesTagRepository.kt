package app.verdant.repository

import app.verdant.entity.SpeciesTag
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet
import java.sql.Statement

@ApplicationScoped
class SpeciesTagRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): SpeciesTag? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_tag WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toTag() else null }
            }
        }

    fun findAll(): List<SpeciesTag> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_tag ORDER BY name").use { ps ->
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toTag()) }
                }
            }
        }

    fun findByOrgId(orgId: Long): List<SpeciesTag> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM species_tag WHERE org_id = ? OR org_id IS NULL ORDER BY name").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toTag()) }
                }
            }
        }

    fun persist(tag: SpeciesTag): SpeciesTag {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO species_tag (org_id, name) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                if (tag.orgId != null) ps.setLong(1, tag.orgId) else ps.setNull(1, java.sql.Types.BIGINT)
                ps.setString(2, tag.name)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return tag.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM species_tag WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Species tag not found")
            }
        }
    }

    private fun ResultSet.toTag() = SpeciesTag(
        id = getLong("id"),
        orgId = getObject("org_id") as? Long,
        name = getString("name"),
    )
}
