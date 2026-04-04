package app.verdant.repository

import app.verdant.entity.Organization
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet

@ApplicationScoped
class OrganizationRepository(private val ds: AgroalDataSource) {

    fun findById(id: Long): Organization? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM organization WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toOrganization() else null }
            }
        }

    fun persist(organization: Organization): Organization {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO organization (name, emoji, created_at, updated_at) VALUES (?, ?, now(), now())",
                arrayOf("id")
            ).use { ps ->
                ps.setString(1, organization.name)
                ps.setString(2, organization.emoji)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return organization.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun update(organization: Organization) {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "UPDATE organization SET name = ?, emoji = ?, updated_at = now() WHERE id = ?"
            ).use { ps ->
                ps.setString(1, organization.name)
                ps.setString(2, organization.emoji)
                ps.setLong(3, organization.id!!)
                ps.executeUpdate()
            }
        }
    }

    fun delete(id: Long) {
        ds.connection.use { conn ->
            conn.prepareStatement("DELETE FROM organization WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Organization not found")
            }
        }
    }

    private fun ResultSet.toOrganization() = Organization(
        id = getLong("id"),
        name = getString("name"),
        emoji = getString("emoji"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
