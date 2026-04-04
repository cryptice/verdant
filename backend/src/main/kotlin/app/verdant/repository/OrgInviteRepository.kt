package app.verdant.repository

import app.verdant.entity.InviteStatus
import app.verdant.entity.OrgInvite
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import java.sql.ResultSet

@ApplicationScoped
class OrgInviteRepository(private val ds: AgroalDataSource) {

    fun findByOrgId(orgId: Long): List<OrgInvite> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM org_invite WHERE org_id = ? ORDER BY id").use { ps ->
                ps.setLong(1, orgId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toOrgInvite()) }
                }
            }
        }

    fun findPendingByEmail(email: String): List<OrgInvite> =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM org_invite WHERE email = ? AND status = 'PENDING' ORDER BY id").use { ps ->
                ps.setString(1, email)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.toOrgInvite()) }
                }
            }
        }

    fun findById(id: Long): OrgInvite? =
        ds.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM org_invite WHERE id = ?").use { ps ->
                ps.setLong(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toOrgInvite() else null }
            }
        }

    fun persist(invite: OrgInvite): OrgInvite {
        ds.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO org_invite (org_id, email, invited_by, status, created_at) VALUES (?, ?, ?, ?, now())",
                arrayOf("id")
            ).use { ps ->
                ps.setLong(1, invite.orgId)
                ps.setString(2, invite.email)
                ps.setLong(3, invite.invitedBy)
                ps.setString(4, invite.status.name)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    return invite.copy(id = rs.getLong(1))
                }
            }
        }
    }

    fun updateStatus(id: Long, status: InviteStatus) {
        ds.connection.use { conn ->
            conn.prepareStatement("UPDATE org_invite SET status = ? WHERE id = ?").use { ps ->
                ps.setString(1, status.name)
                ps.setLong(2, id)
                val rows = ps.executeUpdate()
                if (rows == 0) throw jakarta.ws.rs.NotFoundException("Org invite not found")
            }
        }
    }

    private fun ResultSet.toOrgInvite() = OrgInvite(
        id = getLong("id"),
        orgId = getLong("org_id"),
        email = getString("email"),
        invitedBy = getLong("invited_by"),
        status = InviteStatus.valueOf(getString("status")),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
